package com.hadleyso.keycloak.qrauth;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.hadleyso.keycloak.qrauth.token.QrAuthenticatorActionToken;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class QrAuthenticator implements Authenticator {

    @Override
    public void close() {
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // TODO Auto-generated method stub
        log.debug("QrAuthenticator.action");

        throw new UnsupportedOperationException("Unimplemented method 'action'");
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // TODO Auto-generated method stub
        log.debug("QrAuthenticator.authenticate");

        // Create token and convert to link
        QrAuthenticatorActionToken token = QrUtils.createActionToken(context);
        String link = QrUtils.linkFromActionToken(context.getSession(), context.getRealm(), token);

        // Get execution ID for auto-refresh form
        String execId = context.getExecution().getId();

        // Show ftl template page with QR code
        context.forceChallenge(
            context.form()
                .setAttribute("QRauthExecId", execId)
                .setAttribute("QRauthToken", link)
                .createForm("login-qr-code.ftl")
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
