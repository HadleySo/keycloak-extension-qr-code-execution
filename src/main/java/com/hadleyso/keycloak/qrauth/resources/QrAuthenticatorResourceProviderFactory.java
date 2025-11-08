package com.hadleyso.keycloak.qrauth.resources;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class QrAuthenticatorResourceProviderFactory implements RealmResourceProviderFactory {

    private static final String ID = "qr-code-auth";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new QrAuthenticatorResourceProvider(session);
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
        return ID;
    }

    public static String getStaticId() {
        return ID;
    }

}
