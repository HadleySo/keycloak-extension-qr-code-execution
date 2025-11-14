package com.hadleyso.keycloak.qrauth;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.authenticators.conditional.ConditionalCredentialAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalCredentialAuthenticatorFactory;
import org.keycloak.authentication.authenticators.util.AcrStore;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;

import com.hadleyso.keycloak.qrauth.resources.QrAuthenticatorResourceProvider;
import com.hadleyso.keycloak.qrauth.resources.QrAuthenticatorResourceProviderFactory;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua_parser.Parser;
import ua_parser.Client;

@JBossLog
public class QrUtils {
    public static final String CLIENT_ID = "com-hadleyso-keycloak-qrauth-rest-client";
    public static final String TOKEN = "qrToken";

    public static final String AUTHENTICATED_USER_ID = "AUTHENTICATED_USER_ID";
    public static final String AUTHENTICATED_ACR = "AUTHENTICATED_ACR";
    public static final String AUTHENTICATED_CREDENTIALS = "AUTHENTICATED_CREDENTIALS";
    public static final String BRUTE_FORCE_USER_ID = "BRUTE_FORCE_USER_ID";
    public static final String NOTE_QR_LINK = "QR-LINK-PUBLIC";
    public static final String REJECT = "REJECT";
    public static final String TIMEOUT = "TIMEOUT";

    public static final String ORIGIN_ACR = "QR-ORIGIN-ACR";
    public static final String ORIGIN_UA_AGENT = "QR-UA_AGENT";
    public static final String ORIGIN_UA_OS = "QR-UA_OS";
    public static final String ORIGIN_UA_DEVICE = "QR-UA_DEVICE";
    public static final String ORIGIN_LOCALE = "QR-ORI_LOCALE";

    public static final String PUBLIC_QR_PARAM_SESSION_ID = "ida";
    public static final String PUBLIC_QR_PARAM_TAB_ID = "idb";

    public static final String REQUEST_SOURCE = "REQUEST_SOURCE";
    public static final String REQUEST_SOURCE_QUERY = "qr_code_originated";

    public static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    private static final Logger logger = Logger.getLogger(QrUtils.class);
    
    static {
        ProviderConfigProperty refreshProperty = new ProviderConfigProperty();
        refreshProperty.setName("refresh.rate");
        refreshProperty.setLabel("Check Refresh Rate");
        refreshProperty.setType(ProviderConfigProperty.INTEGER_TYPE);
        refreshProperty.setHelpText(
                "How often in seconds to reload the page to check if the authentication is approved. Zero disables refresh.");
        refreshProperty.setDefaultValue(15);
        refreshProperty.setRequired(true);
        configProperties.add(refreshProperty);

        ProviderConfigProperty timeoutProperty = new ProviderConfigProperty();
        timeoutProperty.setName("timeout.rate");
        timeoutProperty.setLabel("Login Timeout");
        timeoutProperty.setType(ProviderConfigProperty.INTEGER_TYPE);
        timeoutProperty
                .setHelpText("How long in seconds a QR code can be displayed before timeout. Zero disables timeout.");
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

        ProviderConfigProperty acrProperty = new ProviderConfigProperty();
        acrProperty.setName("acr.allow.transfer");
        acrProperty.setLabel("Allow ACR Transfer");
        acrProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        acrProperty.setHelpText(
                "Should ACR level completed on the alternate device apply to the originating authentication. Set to true to enable ACR to transfer.");
        acrProperty.setRequired(true);
        acrProperty.setDefaultValue(false);
        configProperties.add(acrProperty);
    }

    public static String serializeList(List<String> values) {
        String serialized = values.stream().collect(Collectors.joining(","));
        return serialized;
    }

    public static List<String> deserializeList(String serialized) {
        if (serialized == null) {
            return List.of();
        }
        return Arrays.asList(serialized.split(","));
    }

    public static String createPublicToken(AuthenticationFlowContext context, Boolean setACR) {
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

        // Get ACR
        AcrStore acrStore = new AcrStore(context.getSession(), authSession);
        int reqAcr = acrStore.getRequestedLevelOfAuthentication(context.getTopLevelFlow());
        String noteACR = setACR ? String.valueOf(reqAcr): "";

        authSession.setAuthNote(ORIGIN_UA_AGENT, ua_agent);
        authSession.setAuthNote(ORIGIN_UA_OS, ua_os);
        authSession.setAuthNote(ORIGIN_UA_DEVICE, ua_device);
        authSession.setAuthNote(ORIGIN_LOCALE, local_localized);
        authSession.setAuthNote(ORIGIN_ACR, noteACR);

        // Create URL query parameters
        String sid = authSession.getParentSession().getId();
        String tid = authSession.getTabId();

        RealmModel realm = context.getRealm();

        // Create client if not exist
        ClientModel existingClient = realm.getClientByClientId(CLIENT_ID);
        if (existingClient == null) {

            // Create client
            ClientModel client = realm.addClient(CLIENT_ID);
            client.setEnabled(true);
            client.setProtocol("openid-connect");
            client.setName("QR Code Login Extension");
            client.setDescription(
                    "Client for QR Code execution in browser logins. Can be deleted and will automatically regenerate on next browser login. See https://github.com/HadleySo/keycloak-extension-qr-code-execution");

            // Scope to only "acr"
            client.setFullScopeAllowed(false);
            realm.getDefaultClientScopesStream(true).forEach(scope -> client.removeClientScope(scope));
            realm.getDefaultClientScopesStream(false).forEach(scope -> client.removeClientScope(scope));
            ClientScopeModel acrScope = realm.getClientScopesStream()
                    .filter(scope -> "acr".equals(scope.getName()))
                    .findFirst().get();
            client.addClientScope(acrScope, true);

            // Set Auth type
            client.setClientAuthenticatorType("client-secret");
            KeycloakModelUtils.generateSecret(client);

            // Config other
            client.setPublicClient(false);
            client.setDirectAccessGrantsEnabled(false);
        }

        // Create token
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
            return objectMapper.readValue(decodedBytes, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            return null;
        }
    }

    public static String linkFromActionToken(KeycloakSession session, RealmModel realm, String token,
            Boolean usernamePasswordPage) {
        UriInfo uriInfo = session.getContext().getUri();
        String realmName = realm.getName();

        // Exception for master realm
        if (Config.getAdminRealm().equals(realm.getName())) {
            throw new IllegalStateException(
                    String.format("Disabled for admin / master realm: %s", Config.getAdminRealm()));
        }

        UriBuilder builder = actionTokenBuilder(uriInfo.getBaseUri(), token, realmName);

        if (usernamePasswordPage == true) {
            builder.queryParam(QrUtils.REQUEST_SOURCE_QUERY, true);
        }

        String url = builder.build(realm.getName()).toString();

        // https://github.com/davidshimjs/qrcodejs/issues/78
        if (url.length() >= 192 && url.length() <= 220) {
            url = url + "&davidshimjs-qrcodejs=issue28";
        }
        return url;
    }

    private static UriBuilder actionTokenBuilder(URI baseUri, String tokenString, String realmName) {
        return Urls.realmBase(baseUri)
                .path(realmName)
                .path(QrAuthenticatorResourceProviderFactory.getStaticId())
                .path(QrAuthenticatorResourceProvider.class, "loginWithQrCode")
                .queryParam(QrUtils.TOKEN, tokenString)
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

    public static void handleACR(AuthenticatorConfigModel config, AuthenticationFlowContext context,
            AuthenticationSessionModel authSession) {

        if (config == null)
            return;

        final AcrStore acrStore = new AcrStore(context.getSession(), authSession);

        if (Boolean.parseBoolean(config.getConfig().get("acr.allow.transfer")) == true) {
            // Attach ACR
            String authOkAcrRaw = authSession.getAuthNote(QrUtils.AUTHENTICATED_ACR);
            int authOkACR = -1;
            if (authOkAcrRaw != null) {
                authOkACR = Integer.valueOf(authOkAcrRaw);

                if (authOkACR != -1) {
                    log.info("QrUtils.handleACR - attaching ACR: " + authOkACR);
                    acrStore.setLevelAuthenticated(authOkACR);
                }
            }
        }
    }

    public static Boolean transferAcrEnabled(AuthenticatorConfigModel config) {
        if (config == null)
            return false;
        return Boolean.parseBoolean(config.getConfig().get("acr.allow.transfer"));
    }

    public static void handleCredTransfer(AuthenticatorConfigModel config, AuthenticationFlowContext context,
            AuthenticationSessionModel authSession) {
        if (logger.isTraceEnabled()) {
            logger.tracef("Handling credential transfer to origin session");
        }

        if (config == null)
            return;

        if (Boolean.parseBoolean(config.getConfig().get("credential.allow.transfer")) != true) {
            return;
        }

        String authOkCredentialsRaw = authSession.getAuthNote(QrUtils.AUTHENTICATED_CREDENTIALS);
        List<String> authOkCredentials = deserializeList(authOkCredentialsRaw);

        for (String authOkCredential : authOkCredentials) {
            AuthenticatorUtil.addAuthCredential(authSession, authOkCredential);
        }
        
    }
}
