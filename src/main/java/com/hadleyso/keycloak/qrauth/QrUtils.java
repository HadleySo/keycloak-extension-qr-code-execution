package com.hadleyso.keycloak.qrauth;

import java.net.URI;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.models.Constants;

import com.hadleyso.keycloak.qrauth.token.QrAuthenticatorActionToken;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class QrUtils {
    public static QrAuthenticatorActionToken createActionToken(
        AuthenticationFlowContext context) {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            String tabId = authSession.getTabId();
            String nonce = authSession.getClientNote(OIDCLoginProtocol.NONCE_PARAM);
            
            int expirationTimeInSecs = Time.currentTime() + 300;

            QrAuthenticatorActionToken token = new QrAuthenticatorActionToken(authSession, tabId, nonce, expirationTimeInSecs);
            return token;
    }

    public static String linkFromActionToken(KeycloakSession session, RealmModel realm, QrAuthenticatorActionToken token) {
        UriInfo uriInfo = session.getContext().getUri();

        // Exception for master realm
        if (Config.getAdminRealm().equals(realm.getName())) {
            throw new IllegalStateException(String.format("Disabled for admin / master realm: %s", Config.getAdminRealm()));
        }

        UriBuilder builder = actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo));

        return builder.build(realm.getName()).toString();
    }

    private static UriBuilder actionTokenBuilder(URI baseUri, String tokenString) {
        return Urls.realmBase(baseUri)
                .path(RealmsResource.class, "getLoginActionsService")
                .path(LoginActionsService.class, "executeActionToken")
                .queryParam(Constants.KEY, tokenString);
    }
}
