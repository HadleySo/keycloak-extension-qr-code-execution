package com.hadleyso.keycloak.qrauth.credentialPersistence;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorUtil;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import com.hadleyso.keycloak.qrauth.QrUtils;

import lombok.extern.jbosslog.JBossLog;

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
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        if (authCredentials != null) {
            RootAuthenticationSessionModel rootAuthSession = authSession.getParentSession();
            UserSessionModel userSession = context.getSession()
                    .sessions()
                    .getUserSession(context.getRealm(), rootAuthSession.getId());

            if (logger.isTraceEnabled()) {
                logger.tracef("Getting UserSessionModel from rootAuthSession '%s' in realm '%s'",
                        rootAuthSession.getId(), context.getRealm().getName());
            }

            if (userSession != null) {
                if (logger.isTraceEnabled()) {
                    logger.tracef(
                            "Got UserSessionModel from rootAuthSession '%s' in realm '%s' for user '%s' - setting authCredentials '%s'",
                            rootAuthSession.getId(), context.getRealm().getName(), context.getUser().getId(),
                            authCredentials.toString());
                }
                userSession.setNote(QrUtils.AUTHENTICATED_CREDENTIALS, QrUtils.serializeList(authCredentials));
            }
        }

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
