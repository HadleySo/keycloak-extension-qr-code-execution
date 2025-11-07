package com.hadleyso.keycloak.qrauth.token;

import org.keycloak.authentication.actiontoken.DefaultActionToken;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QrAuthenticatorActionToken extends DefaultActionToken {
    private static final String JSON_FIELD_SESSION_ID = "sid";
    private static final String JSON_FIELD_TAB_ID = "tid";

    @JsonProperty(value = JSON_FIELD_SESSION_ID)
    private String sessionId;
    
    @JsonProperty(value = JSON_FIELD_TAB_ID)
    private String tabId;

}
