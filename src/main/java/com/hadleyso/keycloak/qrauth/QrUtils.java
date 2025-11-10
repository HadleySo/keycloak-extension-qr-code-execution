package com.hadleyso.keycloak.qrauth;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;

import com.hadleyso.keycloak.qrauth.resources.QrAuthenticatorResourceProvider;
import com.hadleyso.keycloak.qrauth.resources.QrAuthenticatorResourceProviderFactory;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.jbosslog.JBossLog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua_parser.Parser;
import ua_parser.Client;

@JBossLog
public class QrUtils {
    public static final String AUTHENTICATED_USER_ID = "AUTHENTICATED_USER_ID";
    public static final String BRUTE_FORCE_USER_ID = "BRUTE_FORCE_USER_ID";
    public static final String NOTE_QR_LINK = "QR-LINK-PUBLIC";
    public static final String REJECT = "REJECT";
    public static final String TIMEOUT = "TIMEOUT";

    public static final String ORIGIN_UA_AGENT = "QR-UA_AGENT";
    public static final String ORIGIN_UA_OS = "QR-UA_OS";
    public static final String ORIGIN_UA_DEVICE = "QR-UA_DEVICE";
    public static final String ORIGIN_LOCALE = "QR-ORI_LOCALE";

    public static final String PUBLIC_QR_PARAM_SESSION_ID = "ida";
    public static final String PUBLIC_QR_PARAM_TAB_ID = "idb";

    public static final String REQUEST_SOURCE = "REQUEST_SOURCE";
    public static final String REQUEST_SOURCE_QUERY = "qr_code_originated";


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

        ProviderConfigProperty alignmentProperty = new ProviderConfigProperty();
        alignmentProperty.setName("display.alignment");
        alignmentProperty.setLabel("QR Code Alignment");
        alignmentProperty.setType(ProviderConfigProperty.LIST_TYPE);
        alignmentProperty.setHelpText("How to align the QR code.");
        alignmentProperty.setOptions(Arrays.asList("Left", "Center", "Right"));
        alignmentProperty.setRequired(true);
        configProperties.add(alignmentProperty);
    }

    
    public static String createPublicToken(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        // Get user agent
        String userAgent = context.getHttpRequest().getHttpHeaders().getHeaderString("User-Agent");
        Parser uaParser = new Parser();
        Client uaClient = uaParser.parse(userAgent);

        String ua_os = uaClient.os.family;
        String ua_device = uaClient.device.family;
        String ua_agent = uaClient.userAgent.family;
        
        // Get locale
        Locale resolvedLocale = context.getSession().getContext().resolveLocale(context.getUser());
        String local_localized = resolvedLocale.getDisplayName();

        authSession.setAuthNote(ORIGIN_UA_AGENT, ua_agent);
        authSession.setAuthNote(ORIGIN_UA_OS, ua_os);
        authSession.setAuthNote(ORIGIN_UA_DEVICE, ua_device);
        authSession.setAuthNote(ORIGIN_LOCALE, local_localized);

        // Create URL query parameters
        String sid = authSession.getParentSession().getId();
        String tid = authSession.getTabId();

        Map<String, String> sessionIdInfo = new LinkedHashMap<>();
        sessionIdInfo.put(PUBLIC_QR_PARAM_SESSION_ID, sid);
        sessionIdInfo.put(PUBLIC_QR_PARAM_TAB_ID, tid);

        ObjectMapper objectMapper = new ObjectMapper();
        String sessionIdInfoJson = null;
        try {
            sessionIdInfoJson = objectMapper.writeValueAsString(sessionIdInfo);
        } catch (JsonProcessingException e) {
            return null;
        }
            
        return Base64Url.encode(sessionIdInfoJson.getBytes());
    }

    public static Map<String, String> decodePublicToken(String token) {
        byte[] decodedBytes = Base64Url.decode(token);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(decodedBytes, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            return null;
        }
    }

    public static String linkFromActionToken(KeycloakSession session, RealmModel realm, String token, Boolean usernamePasswordPage) {
        UriInfo uriInfo = session.getContext().getUri();
        String realmName = realm.getName();
        
        // Exception for master realm
        if (Config.getAdminRealm().equals(realm.getName())) {
            throw new IllegalStateException(String.format("Disabled for admin / master realm: %s", Config.getAdminRealm()));
        }

        UriBuilder builder = actionTokenBuilder(uriInfo.getBaseUri(), token, realmName);

        if (usernamePasswordPage == true) {
            builder.queryParam(QrUtils.REQUEST_SOURCE_QUERY, true);
        }
        
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
