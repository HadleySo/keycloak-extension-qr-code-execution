package com.hadleyso.keycloak.qrauth.jpa;

import java.util.Arrays;
import java.util.List;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

public class ShortCodeEntityProvider implements JpaEntityProvider {

    private static Class<?>[] entities = { ShortCodeEntity.class };

    @Override
    public List<Class<?>> getEntities() {
        return Arrays.<Class<?>>asList(entities);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/jpa-changelog-qr-auth.xml";
    }

    @Override
    public void close() {
    }

    @Override
    public String getFactoryId() {
        return com.hadleyso.keycloak.qrauth.jpa.ShortCodeEntityProviderFactory.ID;
    }
}