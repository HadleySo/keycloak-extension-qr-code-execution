package com.hadleyso.keycloak.qrauth;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import com.hadleyso.keycloak.qrauth.token.QrAuthenticatorActionToken;

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

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        UserModel user = null;

        if (authSession != null) {
            // Retrieve the note by key
            String authOkUserId = authSession.getAuthNote(QrUtils.AUTHENTICATED_USER_ID);

            if (authOkUserId != null) {
                log.info("QrAuthenticator.action 4");
                user = session.users().getUserById(realm, authOkUserId);
            } 
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
        String execId = context.getExecution().getId();

        // Show ftl template page with QR code
        context.forceChallenge(
            context.form()
                .setAttribute("QRauthExecId", execId)
                .setAttribute("QRauthToken", link)
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
    
}
