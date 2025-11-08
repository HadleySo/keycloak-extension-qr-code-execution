package com.hadleyso.keycloak.qrauth.token;

import java.util.UUID;

import org.keycloak.authentication.actiontoken.DefaultActionToken;
import org.keycloak.sessions.AuthenticationSessionModel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QrAuthenticatorActionToken extends DefaultActionToken {
    private static final String JSON_FIELD_SESSION_ID = "sid";
    private static final String JSON_FIELD_TAB_ID = "tid";

    public static final String TOKEN_ID = "com-hadleyso-qr-code-authenticator";


    @JsonProperty(value = JSON_FIELD_SESSION_ID)
    private String sessionId;

    @JsonProperty(value = JSON_FIELD_TAB_ID)
    private String tabId;


    public QrAuthenticatorActionToken(AuthenticationSessionModel authSession, String tabId, String nonce, int expirationTimeInSecs) {
        super(null, TOKEN_ID, expirationTimeInSecs, nonce(nonce));
        this.sessionId = authSession.getParentSession().getId();
        this.tabId = tabId;
    }

    static UUID nonce(String nonce) {
        try {
            return UUID.fromString(nonce);
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }

    private QrAuthenticatorActionToken() {
        // Class must have a private constructor without any arguments. This is necessary
        // to deserialize the token class from JWT.
    }

}
