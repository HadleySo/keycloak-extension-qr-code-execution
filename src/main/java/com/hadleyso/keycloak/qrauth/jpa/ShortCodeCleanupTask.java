package com.hadleyso.keycloak.qrauth.jpa;

import java.util.Date;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.ScheduledTask;

import jakarta.persistence.EntityManager;

public class ShortCodeCleanupTask implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(ShortCodeCleanupTask.class);
        
    @Override
    public void run(KeycloakSession session) {
        logger.debugf("%s startng", getTaskName());
        long currentTimeMillis = Time.currentTimeMillis();
        
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        Date cutoff = new Date(System.currentTimeMillis() - 12 * 60 * 60 * 1000);

        int deleted = em.createQuery(
            "DELETE FROM ShortCodeEntity e WHERE e.createdAt < :cutoff"
        )
        .setParameter("cutoff", cutoff)
        .executeUpdate();

        long took = Time.currentTimeMillis() - currentTimeMillis;
        logger.debugf("%s finished in %d ms removed %d", getTaskName(), took, deleted);

    }

}

