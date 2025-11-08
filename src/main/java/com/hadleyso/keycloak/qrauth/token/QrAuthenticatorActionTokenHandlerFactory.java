package com.hadleyso.keycloak.qrauth.token;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.actiontoken.ActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class QrAuthenticatorActionTokenHandlerFactory implements ActionTokenHandlerFactory<QrAuthenticatorActionToken> {

    @Override
    public ActionTokenHandler<QrAuthenticatorActionToken> create(KeycloakSession session) {
        return new QrAuthenticatorActionTokenHandler();
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return QrAuthenticatorActionToken.TOKEN_ID;
    }
    
}
