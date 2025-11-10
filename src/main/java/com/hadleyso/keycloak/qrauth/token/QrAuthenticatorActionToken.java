package com.hadleyso.keycloak.qrauth.token;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.keycloak.authentication.actiontoken.DefaultActionToken;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QrAuthenticatorActionToken extends DefaultActionToken {
    private static final String JSON_FIELD_SESSION_ID = "sid";
    private static final String JSON_FIELD_TAB_ID = "tid";
    private static final String JSON_FIELD_REALM = "realm";
    private static final String JSON_FIELD_UA_OS = "ua_os";
    private static final String JSON_FIELD_UA_DEVICE = "ua_device";
    private static final String JSON_FIELD_UA_AGENT = "ua_agent";
    private static final String JSON_FIELD_LOCAL_LOCALIZED = "local_localized";
    

    public static final String TOKEN_ID = "com-hadleyso-qr-code-authenticator";


    @JsonProperty(value = JSON_FIELD_SESSION_ID)
    private String sessionId;

    @JsonProperty(value = JSON_FIELD_TAB_ID)
    private String tabId;

    @JsonProperty(value = JSON_FIELD_REALM)
    private String realmId;
    
    @JsonProperty(value = JSON_FIELD_UA_OS)
    private String ua_os;
    
    @JsonProperty(value = JSON_FIELD_UA_DEVICE)
    private String ua_device;
    
    @JsonProperty(value = JSON_FIELD_UA_AGENT)
    private String ua_agent;
    
    @JsonProperty(value = JSON_FIELD_LOCAL_LOCALIZED)
    private String local_localized;
    


    public QrAuthenticatorActionToken(
        AuthenticationSessionModel authSession, 
        String tabId, 
        RealmModel realm,
        String nonce, 
        int expirationTimeInSecs,
        String ua_os,
        String ua_device,
        String ua_agent,
        String local_localized) {
            super(null, TOKEN_ID, expirationTimeInSecs, nonce(nonce));
            this.sessionId = authSession.getParentSession().getId();
            this.tabId = tabId;
            this.realmId = realm.getId();
            this.ua_os = ua_os;
            this.ua_device = ua_device;
            this.ua_agent = ua_agent;
            this.local_localized = local_localized;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public String getTabId() {
        return this.tabId;
    }

    public String getRealmId() {
        return this.realmId;
    }

    public String getLocalLocalized() {
        return this.local_localized;
    }

    public Map<String, String> getUA() {
        Map<String, String> ua = new HashMap<String, String>();
        ua.put("ua_os", this.ua_os);
        ua.put("ua_device", this.ua_device);
        ua.put("ua_agent", this.ua_agent);
        return ua;
    }

    static UUID nonce(String nonce) {
        try {
            return UUID.fromString(nonce);
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }

    @SuppressWarnings("unused")
    private QrAuthenticatorActionToken() {
        // Class must have a private constructor without any arguments. This is necessary
        // to deserialize the token class from JWT.
    }

}
