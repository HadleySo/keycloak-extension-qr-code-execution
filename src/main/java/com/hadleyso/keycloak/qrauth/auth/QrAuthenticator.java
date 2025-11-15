package com.hadleyso.keycloak.qrauth.auth;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import com.hadleyso.keycloak.qrauth.QrUtils;

import lombok.extern.jbosslog.JBossLog;
import org.jboss.logging.Logger;

@JBossLog
public class QrAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(QrAuthenticator.class);

    @Override
    public void close() {
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        log.info("QrAuthenticator.action");
        return;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.info("QrAuthenticator.authenticate");

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        final AuthenticationSessionModel authSession = context.getAuthenticationSession();
        final KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        // Rejected then cancel
        String reject = authSession.getAuthNote(QrUtils.REJECT);
        if (reject == QrUtils.REJECT) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Flow '%s' was rejected by remote by user", context.toString());
            }
            QrUtils.rejectedBruteForce(context);
            context.cancelLogin();
            context.clearUser();
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        // Timeout
        if (QrUtils.timeoutPassed(context)) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Flow '%s' has expired", context.toString());
            }
            context.failure(AuthenticationFlowError.EXPIRED_CODE);
            return;
        }

        // Check if authenticated
        UserModel user = null;
        String authOkUserId = authSession.getAuthNote(QrUtils.AUTHENTICATED_USER_ID);
        if (authOkUserId != null) {
            user = session.users().getUserById(realm, authOkUserId);
        }
        if (user != null) {
            // Attach the user to the flow
            if (logger.isTraceEnabled()) {
                logger.tracef("Attaching user '%s' '%s' to flow '%s'", user.getId(), user.getUsername(), context.toString());
            }
            context.setUser(user);
            QrUtils.handleACR(config, context);
            QrUtils.handleCredTransfer(config, context);
            context.success();
            return;
        }

        // NOT LOGGED IN

        // Check if already made
        String link = authSession.getAuthNote(QrUtils.NOTE_QR_LINK);

        if (link == null) {
            // Create token and convert to link
            String token = QrUtils.createPublicToken(context, QrUtils.transferAcrEnabled(config));
            if (token == null) {
                context.failure(AuthenticationFlowError.INTERNAL_ERROR);
                return;
            }
            link = QrUtils.linkFromActionToken(context.getSession(), context.getRealm(), token, false);
            authSession.setAuthNote(QrUtils.NOTE_QR_LINK, link);
            if (logger.isTraceEnabled()) {
                logger.tracef("Created new token with link - token: '%s;", token);
            }
        }

        // Get execution ID for auto-refresh form
        // and TabID
        String execId = context.getExecution().getId();
        String tabId = authSession.getTabId();

        // Get refresh rate
        int refreshRate = 15;
        if (config != null) {
            refreshRate = Integer.valueOf(config.getConfig().get("refresh.rate"));
            if (refreshRate < 0) {
                refreshRate = 0;
            }
        }

        // Get alignment
        String alignment = "Center";
        if (config != null) {
            alignment = config.getConfig().get("display.alignment");
            if (alignment == null)
                alignment = "Center";
        }

        if (logger.isTraceEnabled()) {
            logger.tracef("Serving session '%s' with tabId '%s' with token in link: '%s;", execId, tabId, link);
        }
        // Show ftl template page with QR code
        context.challenge(
                context.form()
                        .setAttribute("QRauthExecId", execId)
                        .setAttribute("QRauthToken", link)
                        .setAttribute("tabId", tabId)
                        .setAttribute("refreshRate", refreshRate)
                        .setAttribute("alignment", alignment)
                        .createForm("qr-login-scan.ftl"));
    }

    @Override
    public boolean configuredFor(KeycloakSession arg0, RealmModel arg1, UserModel arg2) {
        return true;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession arg0, RealmModel arg1, UserModel arg2) {
    }

}
