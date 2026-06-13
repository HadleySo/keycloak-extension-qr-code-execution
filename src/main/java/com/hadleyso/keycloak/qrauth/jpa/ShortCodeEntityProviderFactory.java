package com.hadleyso.keycloak.qrauth.jpa;

import org.keycloak.Config.Scope;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;

public class ShortCodeEntityProviderFactory implements JpaEntityProviderFactory {

  protected static final String ID = "com-hadleyso-keycloak-shortcode";

  @Override
  public JpaEntityProvider create(KeycloakSession session) {
    return new ShortCodeEntityProvider();
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public void init(Scope config) {
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            TimerProvider timer = session.getProvider(TimerProvider.class);

            long every30Min = 30 * 60 * 1000;

            timer.scheduleTask(
                new ShortCodeCleanupTask(),
                every30Min,
                "com-hadleyso-qrauth-shortcode-cleanup-task"
            );
        });
  }

  @Override
  public void close() {
  }

}