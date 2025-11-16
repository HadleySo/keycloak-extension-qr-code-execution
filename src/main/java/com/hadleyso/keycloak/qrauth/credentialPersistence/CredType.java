package com.hadleyso.keycloak.qrauth.credentialPersistence;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorUtil;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.hadleyso.keycloak.qrauth.QrUtils;

import lombok.extern.jbosslog.JBossLog;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jboss.logging.Logger;

@JBossLog
public class CredType implements Authenticator {
    private static final Logger logger = Logger.getLogger(CredType.class);

    @Override
    public void close() {
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        log.info("CredType.action");
        return;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.info("CredType.authenticate");

        List<String> authCredentials = AuthenticatorUtil.getAuthnCredentials(context.getAuthenticationSession());
        String userId = context.getUser().getId();
        UserModel user = context.getSession().users().getUserById(context.getRealm(), userId);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        if (logger.isTraceEnabled()) {
            logger.tracef("Setting UserSingleAttribute on user '%s' in realm '%s' for credentials '%s'",
                    userId, context.getRealm().getName(), QrUtils.serializeList(authCredentials));
        }
        user.setSingleAttribute(QrUtils.AUTHENTICATED_CREDENTIALS, QrUtils.serializeList(authCredentials));
        user.setSingleAttribute(QrUtils.AUTHENTICATED_CREDENTIALS_AGE, timestamp);

        context.success();

    }

    @Override
    public boolean configuredFor(KeycloakSession arg0, RealmModel arg1, UserModel arg2) {
        return true;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession arg0, RealmModel arg1, UserModel arg2) {
    }

}
