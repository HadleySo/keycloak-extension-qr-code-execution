package com.hadleyso.keycloak.qrauth;

import java.time.ZonedDateTime;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import com.hadleyso.keycloak.qrauth.token.QrAuthenticatorActionToken;

import jakarta.ws.rs.core.UriInfo;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class QrAuthenticator implements Authenticator {

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
            rejectedBruteForce(context);
            context.cancelLogin();
            context.clearUser();
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        // Timeout
        if (timeoutPassed(context)) {
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
            context.setUser(user);
            context.success();
            return;
        }


        // NOT LOGGED IN

        // Check if already made
        String link = authSession.getAuthNote(QrUtils.JWT_REQ);

        if (link == null) {
            // Create token and convert to link
            QrAuthenticatorActionToken token = QrUtils.createActionToken(context);
            link = QrUtils.linkFromActionToken(context.getSession(), context.getRealm(), token);
            authSession.setAuthNote(QrUtils.JWT_REQ, link);
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

        // Show ftl template page with QR code
        context.challenge(
            context.form()
                .setAttribute("QRauthExecId", execId)
                .setAttribute("QRauthToken", link)
                .setAttribute("tabId", tabId)
                .setAttribute("refreshRate", refreshRate)
                .createForm("qr-login-scan.ftl")
        );
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

    private boolean timeoutPassed(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String timeout = authSession.getAuthNote(QrUtils.TIMEOUT);

        if (StringUtil.isNotBlank(timeout)) {
            ZonedDateTime maxTimestamp = ZonedDateTime.parse(timeout);
            return maxTimestamp.isBefore(ZonedDateTime.now());

        } 

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        int timeoutRate = 300;
        if (config != null) {
            timeoutRate = Integer.valueOf(config.getConfig().get("timeout.rate"));

            if (timeoutRate > 0) {
                ZonedDateTime maxTimestamp = ZonedDateTime.now().plusSeconds(timeoutRate);
                authSession.setAuthNote(QrUtils.TIMEOUT, maxTimestamp.toString());
            }
        }
        
        return false;
    }

    private void rejectedBruteForce(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        KeycloakSession session = context.getSession();

        RealmModel realm = context.getRealm();
        String bruteUserId = authSession.getAuthNote(QrUtils.BRUTE_FORCE_USER_ID);

        ClientConnection clientConnection = session.getContext().getConnection();
        UriInfo uriInfo = session.getContext().getUri();

        if (StringUtil.isNotBlank(bruteUserId)) {
            UserModel user = session.users().getUserById(realm, bruteUserId);

            BruteForceProtector protector = session.getProvider(BruteForceProtector.class);
            protector.failedLogin(realm, user, clientConnection, uriInfo);
        }
        
    }
    
}
