package com.hadleyso.keycloak.qrauth.token;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.actiontoken.ActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class QrAuthenticatorActionTokenHandlerFactory implements ActionTokenHandlerFactory<QrAuthenticatorActionToken> {

    @Override
    public ActionTokenHandler<QrAuthenticatorActionToken> create(KeycloakSession session) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public void init(Scope config) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'init'");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'postInit'");
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getId'");
    }
    
}
