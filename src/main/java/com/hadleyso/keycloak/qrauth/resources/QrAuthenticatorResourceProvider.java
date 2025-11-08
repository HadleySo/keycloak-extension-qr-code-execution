package com.hadleyso.keycloak.qrauth.resources;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.TokenVerifier;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.AuthenticationSessionProvider;

import com.hadleyso.keycloak.qrauth.QrUtils;
import com.hadleyso.keycloak.qrauth.token.QrAuthenticatorActionToken;

import lombok.extern.jbosslog.JBossLog;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@JBossLog
public class QrAuthenticatorResourceProvider implements RealmResourceProvider {

    protected final KeycloakSession session;

    public QrAuthenticatorResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Path("scan")
    @Produces(MediaType.TEXT_HTML)
	public Response loginWithQrCode(@QueryParam(Constants.TOKEN) String token) {        
        log.info("QrAuthenticatorResourceProvider.loginWithQrCode");
        

        // Verify token
        QrAuthenticatorActionToken tokenVerified = null;
        try {
            // request.token is the raw JWT string
            tokenVerified = TokenVerifier
                    .create(token, QrAuthenticatorActionToken.class)
                    .withChecks(TokenVerifier.IS_ACTIVE)   // validate exp, iat, etc.
                    .getToken();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid token: " + e.getMessage()).build();
        } 
        
        if (tokenVerified == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid token").build();
        }


        // Get context realm
        RealmModel realm = session.realms().getRealm(tokenVerified.getRealmId());

        // Build redirect path
        UriBuilder builder = Urls.realmBase(session.getContext().getUri().getBaseUri())
                .path(realm.getName())
                .path(QrAuthenticatorResourceProviderFactory.getStaticId())
                .path(QrAuthenticatorResourceProvider.class, "approveRemote")
                .queryParam(Constants.TOKEN, token);
        
        String redirectURI = builder.build(realm.getName()).toString();


        // Get account client and add redirect path
        ClientModel accountClient = session.clients().getClientByClientId(realm, "account");
        if (accountClient == null) {
            throw new IllegalStateException("Account client not found in realm " + realm.getName());
        }

        Set<String> uris = new HashSet<>(accountClient.getRedirectUris());
        if (!uris.contains(redirectURI)) {
            uris.add(redirectURI);
            accountClient.setRedirectUris(uris);
        }


        // Serve login
        UriBuilder uriBuilder = UriBuilder.fromUri(session.getContext().getUri().getBaseUri())
            .path("realms")
            .path(realm.getName())
            .path("protocol/openid-connect/auth")
            .queryParam("client_id", accountClient.getClientId())
            .queryParam("redirect_uri", redirectURI)
            .queryParam("response_type", "code");

        return Response.seeOther(uriBuilder.build()).build();

    }


    @GET
    @Path("approve")
    @Produces(MediaType.TEXT_HTML)
	public Response approveRemote(@QueryParam(Constants.TOKEN) String token) {   
        
        // Verify token
        QrAuthenticatorActionToken tokenVerified = null;
        try {
            // request.token is the raw JWT string
            tokenVerified = TokenVerifier
                    .create(token, QrAuthenticatorActionToken.class)
                    .withChecks(TokenVerifier.IS_ACTIVE)   // validate exp, iat, etc.
                    .getToken();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid token: " + e.getMessage()).build();
        } 
        
        if (tokenVerified == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid token").build();
        }

        // Get user
        UserModel user = session.getContext().getAuthenticationSession() != null
                ? session.getContext().getAuthenticationSession().getAuthenticatedUser()
                : null;

            
        // Verify user valid
        String userId = null;
        if (user != null) {
            userId = user.getId();
        } else {
            return null; 
        }

        // Get remote session info
        String sid = tokenVerified.getSessionId();
        String tid = tokenVerified.getTabId();

        // Set remote session to valid
        RealmModel realm = session.realms().getRealm(tokenVerified.getRealmId());
        AuthenticationSessionProvider provider = session.authenticationSessions();
        var rootAuthSession = provider.getRootAuthenticationSession(realm, sid);

        ClientModel remoteClient = session.clients().getClientByClientId(realm, tokenVerified.getClientId());

        if (rootAuthSession != null) {
            // Then get the tab-specific authentication session
            AuthenticationSessionModel authSession = rootAuthSession.getAuthenticationSession(remoteClient, tid);

            if (authSession != null) {
                authSession.setAuthNote(QrUtils.AUTHENTICATED_USER_ID, userId);
            } else {
                throw new IllegalStateException("No tab found for that session");
            }
        } else {
            throw new IllegalStateException("No root authentication session found for id=" + sid);
        }

        return Response.seeOther(URI.create("http://localhost:8080/realms/master/account")).build();
    }


}
