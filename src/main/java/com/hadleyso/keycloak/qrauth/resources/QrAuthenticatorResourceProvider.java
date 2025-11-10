package com.hadleyso.keycloak.qrauth.resources;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.TokenVerifier;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;

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
	public Response loginWithQrCode(@QueryParam(Constants.TOKEN) String token, @QueryParam(QrUtils.REQUEST_SOURCE_QUERY) String qr_code_originated) {        
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
            throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.INVALID_REQUEST);
        } 
        
        if (tokenVerified == null) {
                throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.EXPIRED_CODE);
        }


        // Get context realm
        RealmModel realm = session.realms().getRealm(tokenVerified.getRealmId());

        // Build redirect path
        UriBuilder builderPath = Urls.realmBase(session.getContext().getUri().getBaseUri())
            .path(realm.getName())
            .path(QrAuthenticatorResourceProviderFactory.getStaticId());
        UriBuilder builderToken = Urls.realmBase(session.getContext().getUri().getBaseUri())
            .path(realm.getName())
            .path(QrAuthenticatorResourceProviderFactory.getStaticId())
            .path(QrAuthenticatorResourceProvider.class, "verify")
            .queryParam("prompt", "login")
            .queryParam(Constants.TOKEN, token);
        
        String clientRedirects = builderPath.build().toString() + "*";
        String redirectURI = builderToken.build().toString();


        // Get account client and add redirect path
        ClientModel accountClient = session.clients().getClientByClientId(realm, "account");
        if (accountClient == null) {
            throw new ErrorPageException(session, 
                Response.Status.BAD_REQUEST, 
                Messages.INTERNAL_SERVER_ERROR);
        }

        Set<String> uris = new HashSet<>(accountClient.getRedirectUris());
        if (!uris.contains(clientRedirects)) {
            uris.add(clientRedirects);
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

                    
        // If username password page
        if (qr_code_originated != null) {
            uriBuilder.queryParam(QrUtils.REQUEST_SOURCE_QUERY, "");
        } 


        return Response.seeOther(uriBuilder.build()).build();

    }

    @GET
    @Path("verify")
    @Produces(MediaType.TEXT_HTML)
	public Response verify(@QueryParam(Constants.TOKEN) String token) {   
        log.info("QrAuthenticatorResourceProvider.verify");

        // Convert to action token
        QrAuthenticatorActionToken actionToken = convertActionToken(token);

        // Get remote session info
        String sid = actionToken.getSessionId();
        String tid = actionToken.getTabId();
        String rid = actionToken.getRealmId();
        RealmModel remoteRealm = session.realms().getRealm(rid);

        AuthenticationSessionProvider provider = session.authenticationSessions();
        RootAuthenticationSessionModel rootAuthSession = provider.getRootAuthenticationSession(remoteRealm, sid);

        // Check if rejected
        if (rootAuthSession != null) {
            // Then get the tab-specific authentication session
            Map<String, AuthenticationSessionModel> allSessions = rootAuthSession.getAuthenticationSessions();
            AuthenticationSessionModel authSession = allSessions.get(tid);
            
            if (authSession != null) {
                String rejectStatus = authSession.getAuthNote(QrUtils.REJECT); 
                if (rejectStatus != null) {
                    throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.EXPIRED_CODE);
                } 
            } else {
                throw new ErrorPageException(session, 
                    Response.Status.BAD_REQUEST, 
                    Messages.EXPIRED_CODE);
            }
        }
        
        // Get UA
        Map<String, String> ua = actionToken.getUA();

        // Get approve link
        RealmModel realm = session.realms().getRealm(actionToken.getRealmId());
        UriBuilder builder = Urls.realmBase(session.getContext().getUri().getBaseUri())
            .path(realm.getName())
            .path(QrAuthenticatorResourceProviderFactory.getStaticId())
            .path(QrAuthenticatorResourceProvider.class, "approveRemote")
            .queryParam("prompt", "login")
            .queryParam(Constants.TOKEN, token);
        String approveURL = builder.build().toString();

            // Get reject link
        UriBuilder builderReject = Urls.realmBase(session.getContext().getUri().getBaseUri())
            .path(realm.getName())
            .path(QrAuthenticatorResourceProviderFactory.getStaticId())
            .path(QrAuthenticatorResourceProvider.class, "rejectRemote")
            .queryParam(Constants.TOKEN, token);
        String rejectURL = builderReject.build().toString();

        // Create form
        LoginFormsProvider form = session.getProvider(LoginFormsProvider.class);
        form.setAttribute("approveURL", approveURL);
        form.setAttribute("rejectURL", rejectURL);
        form.setAttribute("ua_os", ua.get("ua_os"));
        form.setAttribute("ua_device", ua.get("ua_device"));
        form.setAttribute("ua_agent", ua.get("ua_agent"));
        form.setAttribute("tabId", actionToken.getTabId());
        form.setAttribute("local_localized", actionToken.getLocalLocalized());

        return form.createForm("qr-login-verify.ftl");

    }


    @GET
    @Path("approve")
    @Produces(MediaType.TEXT_HTML)
	public Response approveRemote(@QueryParam(Constants.TOKEN) String token) {   
        log.info("QrAuthenticatorResourceProvider.approveRemote");
        
        // Convert to action token
        QrAuthenticatorActionToken actionToken = convertActionToken(token);

        // Get remote session info
        String sid = actionToken.getSessionId();
        String tid = actionToken.getTabId();
        String rid = actionToken.getRealmId();
        RealmModel remoteRealm = session.realms().getRealm(rid);

        AuthenticationSessionProvider provider = session.authenticationSessions();
        RootAuthenticationSessionModel rootAuthSession = provider.getRootAuthenticationSession(remoteRealm, sid);

        // Check if rejected
        if (rootAuthSession != null) {
            // Then get the tab-specific authentication session
            Map<String, AuthenticationSessionModel> allSessions = rootAuthSession.getAuthenticationSessions();
            AuthenticationSessionModel authSession = allSessions.get(tid);
            
            if (authSession != null) {
                String rejectStatus = authSession.getAuthNote(QrUtils.REJECT); 
                if (rejectStatus != null) {
                    throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.EXPIRED_CODE);
                } 
            } else {
                throw new ErrorPageException(session, 
                    Response.Status.BAD_REQUEST, 
                    Messages.EXPIRED_CODE);
            }
        }

        // Verify token
        QrAuthenticatorActionToken tokenVerified = null;
        try {
            // request.token is the raw JWT string
            tokenVerified = TokenVerifier
                    .create(token, QrAuthenticatorActionToken.class)
                    .withChecks(TokenVerifier.IS_ACTIVE)   // validate exp, iat, etc.
                    .getToken();

        } catch (Exception e) {
            throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.INVALID_REQUEST);
        } 
        
        if (tokenVerified == null) {
            throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.EXPIRED_CODE);
        }

        // Get user            
        RealmModel realm = session.getContext().getRealm();
        AppAuthManager authManager = new AppAuthManager();
        AuthenticationManager.AuthResult auth = authManager.authenticateIdentityCookie(session, realm);
        UserModel user = auth.getUser();

        // Verify user valid
        String userId = null;
        if (user != null) {
            userId = user.getId();
        } else {
            throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.INTERNAL_SERVER_ERROR);
        }
        
        
        // Set remote session to valid
        if (rootAuthSession != null) {
            // Then get the tab-specific authentication session
            Map<String, AuthenticationSessionModel> allSessions = rootAuthSession.getAuthenticationSessions();
            AuthenticationSessionModel authSession = allSessions.get(tid);
            
            if (authSession != null) {
                // Set user
                authSession.setAuthNote(QrUtils.AUTHENTICATED_USER_ID, userId); 

            } else {
                throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.ALREADY_LOGGED_IN);
            }
        } else {
            throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.EXPIRED_ACTION);
        }

        // Build redirect path to success page
        UriBuilder builder = Urls.realmBase(session.getContext().getUri().getBaseUri())
            .path(realm.getName())
            .path(QrAuthenticatorResourceProviderFactory.getStaticId())
            .path(QrAuthenticatorResourceProvider.class, "successPage");

        return Response.seeOther(builder.build()).build();
    }


    @GET
    @Path("reject")
    @Produces(MediaType.TEXT_HTML)
	public Response rejectRemote(@QueryParam(Constants.TOKEN) String token) {  
        // Convert to action token
        QrAuthenticatorActionToken actionToken = convertActionToken(token); 

        // Get realm
        RealmModel realm = session.realms().getRealm(actionToken.getRealmId());

        // Get remote session info
        String sid = actionToken.getSessionId();
        String tid = actionToken.getTabId();

        AuthenticationSessionProvider provider = session.authenticationSessions();
        RootAuthenticationSessionModel rootAuthSession = provider.getRootAuthenticationSession(realm, sid);

        // Set remote session to valid
        if (rootAuthSession != null) {
            // Then get the tab-specific authentication session
            Map<String, AuthenticationSessionModel> allSessions = rootAuthSession.getAuthenticationSessions();
            AuthenticationSessionModel authSession = allSessions.get(tid);
            
            if (authSession != null) {
                authSession.setAuthNote(QrUtils.REJECT, QrUtils.REJECT); 
                log.info("QrAuthenticatorResourceProvider.rejectRemote " + sid);
            }
        }

        return session.getProvider(LoginFormsProvider.class).createForm("qr-login-canceled.ftl");
    }

    @GET
    @Path("success")
    @Produces(MediaType.TEXT_HTML)
	public Response successPage(@QueryParam(Constants.TOKEN) String token, @QueryParam(QrUtils.REQUEST_SOURCE_QUERY) String qr_code_originated) { 
        LoginFormsProvider form = session.getProvider(LoginFormsProvider.class);
        
        if (qr_code_originated != null) {
            form.setAttribute("qr_code_originated", true);
        }
        return form.createForm("qr-login-success.ftl");
    }

    private QrAuthenticatorActionToken convertActionToken(String token) {
        JWSInput jws;
        QrAuthenticatorActionToken actionToken;
        try {
            jws = new JWSInput(token);
            actionToken = jws.readJsonContent(QrAuthenticatorActionToken.class);
        } catch (JWSInputException e) {
            throw new ErrorPageException(session, 
                                Response.Status.BAD_REQUEST, 
                                Messages.EXPIRED_CODE);
        }
        
        return actionToken;

    }
}
