package com.hadleyso.keycloak.qrauth;

import java.net.URI;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;

import com.hadleyso.keycloak.qrauth.resources.QrAuthenticatorResourceProvider;
import com.hadleyso.keycloak.qrauth.resources.QrAuthenticatorResourceProviderFactory;
import com.hadleyso.keycloak.qrauth.token.QrAuthenticatorActionToken;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class QrUtils {
    public static final String AUTHENTICATED_USER_ID = "AUTHENTICATED_USER_ID";

    public static QrAuthenticatorActionToken createActionToken(
        AuthenticationFlowContext context) {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            String tabId = authSession.getTabId();
            String nonce = authSession.getClientNote(OIDCLoginProtocol.NONCE_PARAM);
            RealmModel realm = authSession.getRealm();
            ClientModel client = authSession.getClient();
            int expirationTimeInSecs = Time.currentTime() + 300;

            QrAuthenticatorActionToken token = new QrAuthenticatorActionToken(
                                                    authSession, 
                                                    tabId, 
                                                    realm,
                                                    client,
                                                    nonce, 
                                                    expirationTimeInSecs);
            return token;
    }

    public static String linkFromActionToken(KeycloakSession session, RealmModel realm, QrAuthenticatorActionToken token) {
        UriInfo uriInfo = session.getContext().getUri();
        String realmName = realm.getName();

        // Exception for master realm
        if (Config.getAdminRealm().equals(realm.getName())) {
            throw new IllegalStateException(String.format("Disabled for admin / master realm: %s", Config.getAdminRealm()));
        }

        UriBuilder builder = actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo), realmName);

        return builder.build(realm.getName()).toString();
    }

    private static UriBuilder actionTokenBuilder(URI baseUri, String tokenString, String realmName) {
        return Urls.realmBase(baseUri)
                .path(realmName)
                .path(QrAuthenticatorResourceProviderFactory.getStaticId())
                .path(QrAuthenticatorResourceProvider.class, "loginWithQrCode")
                .queryParam(Constants.TOKEN, tokenString);
    }
}
