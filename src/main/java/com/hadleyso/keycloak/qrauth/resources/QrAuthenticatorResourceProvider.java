package com.hadleyso.keycloak.qrauth.resources;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
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
    public Response loginWithQrCode(@QueryParam(QrUtils.TOKEN) String token,
            @QueryParam(QrUtils.REQUEST_SOURCE_QUERY) String qr_code_originated) {
        log.info("QrAuthenticatorResourceProvider.loginWithQrCode");

        final Map<String, String> decodeToken = QrUtils.decodePublicToken(token);

        if (decodeToken == null) {
            throw new ErrorPageException(session,
                    Response.Status.INTERNAL_SERVER_ERROR,
                    Messages.INVALID_PARAMETER);
        }

        // Get remote session info
        String sid = decodeToken.get(QrUtils.PUBLIC_QR_PARAM_SESSION_ID);
        String tid = decodeToken.get(QrUtils.PUBLIC_QR_PARAM_TAB_ID);
        String rid = session.getContext().getRealm().getId();

        AuthenticationSessionModel originSession = getOriginSession(rid, sid, tid);
        if (originSession == null) {
            throw new ErrorPageException(session,
                    Response.Status.BAD_REQUEST,
                    Messages.EXPIRED_CODE);
        }

        // Get context realm
        RealmModel realm = session.getContext().getRealm();

        // Build redirect path
        UriBuilder builderPath = Urls.realmBase(session.getContext().getUri().getBaseUri())
                .path(realm.getName())
                .path(QrAuthenticatorResourceProviderFactory.getStaticId());
        UriBuilder builderToken = Urls.realmBase(session.getContext().getUri().getBaseUri())
                .path(realm.getName())
                .path(QrAuthenticatorResourceProviderFactory.getStaticId())
                .path(QrAuthenticatorResourceProvider.class, "verify")
                .queryParam(QrUtils.TOKEN, token);

        String clientRedirects = builderPath.build().toString() + "/*";
        String redirectURI = builderToken.build().toString();

        // Get client and add redirect path
        ClientModel qrAuthClient = session.clients().getClientByClientId(realm, QrUtils.CLIENT_ID);
        if (qrAuthClient == null) {
            throw new ErrorPageException(session,
                    Response.Status.BAD_REQUEST,
                    Messages.INTERNAL_SERVER_ERROR);
        }

        Set<String> uris = new HashSet<>(qrAuthClient.getRedirectUris());
        if (!uris.contains(clientRedirects)) {
            uris.add(clientRedirects);
            qrAuthClient.setRedirectUris(uris);
        }

        // Get origin requested ACR
        String originAcrRaw = originSession.getAuthNote(QrUtils.ORIGIN_ACR);

        // Serve login
        UriBuilder uriBuilder = UriBuilder.fromUri(session.getContext().getUri().getBaseUri())
                .path("realms")
                .path(realm.getName())
                .path("protocol/openid-connect/auth")
                .queryParam("client_id", QrUtils.CLIENT_ID)
                .queryParam("redirect_uri", redirectURI)
                .queryParam("acr_values", originAcrRaw)
                .queryParam("scope", "openid")
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
    public Response verify(@QueryParam(QrUtils.TOKEN) String token, @QueryParam("code") String code) {
        log.info("QrAuthenticatorResourceProvider.verify");

        final Map<String, String> decodeToken = QrUtils.decodePublicToken(token);

        if (decodeToken == null) {
            throw new ErrorPageException(session,
                    Response.Status.INTERNAL_SERVER_ERROR,
                    Messages.INVALID_PARAMETER);
        }

        // Get remote session info
        String sid = decodeToken.get(QrUtils.PUBLIC_QR_PARAM_SESSION_ID);
        String tid = decodeToken.get(QrUtils.PUBLIC_QR_PARAM_TAB_ID);
        String rid = session.getContext().getRealm().getId();

        AuthenticationSessionModel originSession = getOriginSession(rid, sid, tid);

        // Check if rejected
        if (originSession != null) {
            String rejectStatus = originSession.getAuthNote(QrUtils.REJECT);
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

        // Get approve link
        RealmModel realm = session.getContext().getRealm();
        UriBuilder builder = Urls.realmBase(session.getContext().getUri().getBaseUri())
                .path(realm.getName())
                .path(QrAuthenticatorResourceProviderFactory.getStaticId())
                .path(QrAuthenticatorResourceProvider.class, "approveRemote")
                .queryParam("prompt", "login")
                .queryParam("code", code)
                .queryParam(QrUtils.TOKEN, token);
        String approveURL = builder.build().toString();

        // Get reject link
        UriBuilder builderReject = Urls.realmBase(session.getContext().getUri().getBaseUri())
                .path(realm.getName())
                .path(QrAuthenticatorResourceProviderFactory.getStaticId())
                .path(QrAuthenticatorResourceProvider.class, "rejectRemote")
                .queryParam(QrUtils.TOKEN, token);
        String rejectURL = builderReject.build().toString();

        // Create form
        LoginFormsProvider form = session.getProvider(LoginFormsProvider.class);
        form.setAttribute("approveURL", approveURL);
        form.setAttribute("rejectURL", rejectURL);
        form.setAttribute("ua_os", originSession.getAuthNote(QrUtils.ORIGIN_UA_OS));
        form.setAttribute("ua_device", originSession.getAuthNote(QrUtils.ORIGIN_UA_DEVICE));
        form.setAttribute("ua_agent", originSession.getAuthNote(QrUtils.ORIGIN_UA_AGENT));
        form.setAttribute("tabId", tid);
        form.setAttribute("local_localized", originSession.getAuthNote(QrUtils.ORIGIN_LOCALE));

        return form.createForm("qr-login-verify.ftl");

    }

    @GET
    @Path("approve")
    @Produces(MediaType.TEXT_HTML)
    public Response approveRemote(@QueryParam(QrUtils.TOKEN) String token, @QueryParam("code") String code) {
        log.info("QrAuthenticatorResourceProvider.approveRemote");

        final Map<String, String> decodeToken = QrUtils.decodePublicToken(token);

        if (decodeToken == null) {
            throw new ErrorPageException(session,
                    Response.Status.INTERNAL_SERVER_ERROR,
                    Messages.INVALID_PARAMETER);
        }

        // Get remote session info
        String sid = decodeToken.get(QrUtils.PUBLIC_QR_PARAM_SESSION_ID);
        String tid = decodeToken.get(QrUtils.PUBLIC_QR_PARAM_TAB_ID);
        String rid = session.getContext().getRealm().getId();

        AuthenticationSessionModel originSession = getOriginSession(rid, sid, tid);

        // Check if rejected
        if (originSession != null) {
            String rejectStatus = originSession.getAuthNote(QrUtils.REJECT);
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

        // Get user
        RealmModel realm = session.getContext().getRealm();
        AppAuthManager authManager = new AppAuthManager();
        AuthenticationManager.AuthResult auth = authManager.authenticateIdentityCookie(session, realm);
        UserModel user = auth.getUser();

        // Get token
        ClientModel qrClient = realm.getClientByClientId(QrUtils.CLIENT_ID);
        SimpleHttp simpleHttp = SimpleHttp.create(session);
        SimpleHttpRequest request = simpleHttp.doPost(getTokenEndpoint());

        UriBuilder builderPath = Urls.realmBase(session.getContext().getUri().getBaseUri())
                .path(realm.getName())
                .path(QrAuthenticatorResourceProviderFactory.getStaticId())
                .path(QrAuthenticatorResourceProvider.class, "verify")
                .queryParam(QrUtils.TOKEN, token);
        String redirectURI = builderPath.build().toString();

        request.param("grant_type", "authorization_code")
                .param("code", code)
                .param("client_id", QrUtils.CLIENT_ID)
                .param("client_secret", qrClient.getSecret())
                .param("redirect_uri", redirectURI);

        AccessTokenResponse tokenResponse;
        try {
            tokenResponse = request.asJson(AccessTokenResponse.class);
        } catch (IOException e) {
            log.info("QrAuthenticatorResourceProvider.approveRemote AccessTokenResponse tokenResponse - Error " + e);
            throw new ErrorPageException(session,
                    Response.Status.BAD_REQUEST,
                    Messages.EXPIRED_CODE);
        }

        String idToken = tokenResponse.getIdToken();
        JWSInput jws;
        IDToken parsedIdToken;
        try {
            jws = new JWSInput(idToken);
            parsedIdToken = jws.readJsonContent(IDToken.class);
        } catch (JWSInputException e) {
            log.info("QrAuthenticatorResourceProvider.approveRemote JWSInput or IDToken - Error " + e);
            throw new ErrorPageException(session,
                    Response.Status.BAD_REQUEST,
                    Messages.EXPIRED_CODE);
        }


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
        originSession.setAuthNote(QrUtils.AUTHENTICATED_USER_ID, userId);

        // Set remote session ACR
        String acrRaw = parsedIdToken.getAcr();
        originSession.setAuthNote(QrUtils.AUTHENTICATED_ACR, acrRaw);

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
    public Response rejectRemote(@QueryParam(QrUtils.TOKEN) String token) {
        log.info("QrAuthenticatorResourceProvider.rejectRemote");

        final Map<String, String> decodeToken = QrUtils.decodePublicToken(token);

        if (decodeToken == null) {
            throw new ErrorPageException(session,
                    Response.Status.INTERNAL_SERVER_ERROR,
                    Messages.INVALID_PARAMETER);
        }

        // Get remote session info
        String sid = decodeToken.get(QrUtils.PUBLIC_QR_PARAM_SESSION_ID);
        String tid = decodeToken.get(QrUtils.PUBLIC_QR_PARAM_TAB_ID);
        String rid = session.getContext().getRealm().getId();

        // Set remote session to invalid
        AuthenticationSessionModel originSession = getOriginSession(rid, sid, tid);
        if (originSession != null) {
            originSession.setAuthNote(QrUtils.REJECT, QrUtils.REJECT);
            log.info("QrAuthenticatorResourceProvider.rejectRemote " + rid + " " + sid + " " + tid);
        }
        return session.getProvider(LoginFormsProvider.class).createForm("qr-login-canceled.ftl");
    }

    @GET
    @Path("success")
    @Produces(MediaType.TEXT_HTML)
    public Response successPage(@QueryParam(QrUtils.TOKEN) String token,
            @QueryParam(QrUtils.REQUEST_SOURCE_QUERY) String qr_code_originated) {
        LoginFormsProvider form = session.getProvider(LoginFormsProvider.class);

        if (qr_code_originated != null) {
            form.setAttribute("qr_code_originated", true);
        }
        return form.createForm("qr-login-success.ftl");
    }

    private AuthenticationSessionModel getOriginSession(String realmId, String sessionId, String tabId) {
        AuthenticationSessionProvider provider = session.authenticationSessions();
        RealmModel realm = session.realms().getRealm(realmId);
        RootAuthenticationSessionModel rootAuthSession = provider.getRootAuthenticationSession(realm, sessionId);

        if (rootAuthSession == null) {
            return null;
        }
        Map<String, AuthenticationSessionModel> allSessions = rootAuthSession.getAuthenticationSessions();
        AuthenticationSessionModel authSession = allSessions.get(tabId);

        return authSession;
    }

    private String getTokenEndpoint() {
        RealmModel realm = session.getContext().getRealm();
        String baseUrl = session.getContext().getUri().getBaseUri().toString();
        return baseUrl + "realms/" + realm.getName() + "/protocol/openid-connect/token";
    }
}
