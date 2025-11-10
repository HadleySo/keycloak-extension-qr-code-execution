package com.hadleyso.keycloak.qrauth;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;

import com.hadleyso.keycloak.qrauth.resources.QrAuthenticatorResourceProvider;
import com.hadleyso.keycloak.qrauth.resources.QrAuthenticatorResourceProviderFactory;
import com.hadleyso.keycloak.qrauth.token.QrAuthenticatorActionToken;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.jbosslog.JBossLog;
import ua_parser.Parser;
import ua_parser.Client;

@JBossLog
public class QrUtils {
    public static final String AUTHENTICATED_USER_ID = "AUTHENTICATED_USER_ID";
    public static final String BRUTE_FORCE_USER_ID = "BRUTE_FORCE_USER_ID";
    public static final String JWT_REQ = "JTW_REQ_TOKEN";
    public static final String REJECT = "REJECT";
    public static final String TIMEOUT = "TIMEOUT";

    public static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty refreshProperty = new ProviderConfigProperty();
        refreshProperty.setName("refresh.rate");
        refreshProperty.setLabel("Check Refresh Rate");
        refreshProperty.setType(ProviderConfigProperty.INTEGER_TYPE);
        refreshProperty.setHelpText("How often in seconds to reload the page to check if the authentication is approved. Zero disables refresh.");
        refreshProperty.setDefaultValue(15);
        refreshProperty.setRequired(true);
        configProperties.add(refreshProperty);

        ProviderConfigProperty timeoutProperty = new ProviderConfigProperty();
        timeoutProperty.setName("timeout.rate");
        timeoutProperty.setLabel("Login Timeout");
        timeoutProperty.setType(ProviderConfigProperty.INTEGER_TYPE);
        timeoutProperty.setHelpText("How long in seconds a QR code can be displayed before timeout. Zero disables timeout.");
        timeoutProperty.setDefaultValue(300);
        timeoutProperty.setRequired(true);
        configProperties.add(timeoutProperty);
    }

    public static QrAuthenticatorActionToken createActionToken(
        AuthenticationFlowContext context) {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            String tabId = authSession.getTabId();
            String nonce = authSession.getClientNote(OIDCLoginProtocol.NONCE_PARAM);
            RealmModel realm = authSession.getRealm();
            int expirationTimeInSecs = Time.currentTime() + 300;

            // Get user agent
            String userAgent = context.getHttpRequest().getHttpHeaders().getHeaderString("User-Agent");
            Parser uaParser = new Parser();
            Client uaClient = uaParser.parse(userAgent);

            String ua_os = uaClient.os.family;
            String ua_device = uaClient.device.family;
            String ua_agent = uaClient.userAgent.family;
            
            QrAuthenticatorActionToken token = new QrAuthenticatorActionToken(
                                                    authSession, 
                                                    tabId, 
                                                    realm,
                                                    nonce, 
                                                    expirationTimeInSecs,
                                                    ua_os, ua_device, ua_agent);
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
                .queryParam(Constants.TOKEN, tokenString)
                .queryParam("prompt", "login");
    }

    public static void rejectedBruteForce(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        KeycloakSession session = context.getSession();

        RealmModel realm = context.getRealm();
        String bruteUserId = authSession.getAuthNote(QrUtils.BRUTE_FORCE_USER_ID);

        ClientConnection clientConnection = session.getContext().getConnection();
        UriInfo uriInfo = session.getContext().getUri();

        if (StringUtil.isNotBlank(bruteUserId)) {
            UserModel user = session.users().getUserById(realm, bruteUserId);

            BruteForceProtector protector = session.getProvider(BruteForceProtector.class);
            protector.failedLogin(realm, user, clientConnection, uriInfo);
        }
        
    }

    public static boolean timeoutPassed(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String timeout = authSession.getAuthNote(QrUtils.TIMEOUT);

        if (StringUtil.isNotBlank(timeout)) {
            ZonedDateTime maxTimestamp = ZonedDateTime.parse(timeout);
            return maxTimestamp.isBefore(ZonedDateTime.now());

        } 

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        int timeoutRate = 300;
        if (config != null) {
            timeoutRate = Integer.valueOf(config.getConfig().get("timeout.rate"));

            if (timeoutRate > 0) {
                ZonedDateTime maxTimestamp = ZonedDateTime.now().plusSeconds(timeoutRate);
                authSession.setAuthNote(QrUtils.TIMEOUT, maxTimestamp.toString());
            }
        }
        
        return false;
    }
}
