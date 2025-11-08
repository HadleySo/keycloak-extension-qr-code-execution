package com.hadleyso.keycloak.qrauth.resources;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.TokenVerifier;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;
import org.keycloak.services.resource.RealmResourceProvider;


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
        

        // Verify token first
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
                .path(QrAuthenticatorResourceProvider.class, "approveRemote");
        
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
        return Response.seeOther(URI.create("http://localhost:8080/realms/master/account")).build();
    }


}
