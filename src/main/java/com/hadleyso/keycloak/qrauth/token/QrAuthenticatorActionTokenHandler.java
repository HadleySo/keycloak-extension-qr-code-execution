package com.hadleyso.keycloak.qrauth.token;

import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.events.EventType;

import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class QrAuthenticatorActionTokenHandler extends AbstractActionTokenHandler<QrAuthenticatorActionToken> {
    
    public QrAuthenticatorActionTokenHandler(String id, Class<QrAuthenticatorActionToken> tokenClass,
            String defaultErrorMessage, EventType defaultEventType, String defaultEventError) {
        super(id, tokenClass, defaultErrorMessage, defaultEventType, defaultEventError);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Response handleToken(QrAuthenticatorActionToken token,
            ActionTokenContext<QrAuthenticatorActionToken> tokenContext) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleToken'");
    }

}
