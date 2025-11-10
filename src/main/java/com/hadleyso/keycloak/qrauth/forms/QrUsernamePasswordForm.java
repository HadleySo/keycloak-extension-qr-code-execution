package com.hadleyso.keycloak.qrauth.forms;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.LoginBean;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import com.hadleyso.keycloak.qrauth.QrUtils;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class QrUsernamePasswordForm extends UsernamePasswordForm {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.info("QrUsernamePasswordForm.authenticate");
        final AuthenticationSessionModel authSession = context.getAuthenticationSession();
        final KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();


        // Rejected then cancel
        String reject = authSession.getAuthNote(QrUtils.REJECT);
        if (reject == QrUtils.REJECT) {
            QrUtils.rejectedBruteForce(context);
            context.cancelLogin();
            context.clearUser();
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        // Timeout
        if (QrUtils.timeoutPassed(context)) {
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

        // NOT LOGGED IN BY QR


        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        // Check if already made
        String link = authSession.getAuthNote(QrUtils.NOTE_QR_LINK);

        if (link == null) {
            // Create token and convert to link
            String token = QrUtils.createPublicToken(context);
            if (token == null) {
                context.failure(AuthenticationFlowError.INTERNAL_ERROR);
                return;
            }
            link = QrUtils.linkFromActionToken(context.getSession(), context.getRealm(), token, true);
            authSession.setAuthNote(QrUtils.NOTE_QR_LINK, link);
        }

        // Get execution ID for auto-refresh form
        // and TabID
        String execId = context.getExecution().getId();
        String tabId = authSession.getTabId();

        // Get alignment
        String alignment = "Center";
        if (config != null) {
            alignment = config.getConfig().get("display.alignment");
            if (alignment == null) alignment = "Center";
        }



        // https://github.com/keycloak/keycloak/blob/39c4c1ed942a4bdcc0a3c4d68a9b853a082ea9a2/services/src/main/java/org/keycloak/authentication/authenticators/browser/UsernamePasswordForm.java#L127-L133

        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();

        formData.add("QRauthExecId", execId);
        formData.add("QRauthToken", link);
        formData.add("tabId", tabId);
        formData.add("alignment", alignment);

        String loginHint = context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);

        String rememberMeUsername = AuthenticationManager.getRememberMeUsername(context.getSession());

        if (context.getUser() != null) {
            if (alreadyAuthenticatedUsingPasswordlessCredential(context)) {
                // if already authenticated using passwordless webauthn just success
                context.success();
                return;
            }

            LoginFormsProvider form = context.form();
            form.setAttribute(LoginFormsProvider.USERNAME_HIDDEN, true);
            form.setAttribute(LoginFormsProvider.REGISTRATION_DISABLED, true);
            context.getAuthenticationSession().setAuthNote(USER_SET_BEFORE_USERNAME_PASSWORD_AUTH, "true");
        } else {
            context.getAuthenticationSession().removeAuthNote(USER_SET_BEFORE_USERNAME_PASSWORD_AUTH);
            if (loginHint != null || rememberMeUsername != null) {
                if (loginHint != null) {
                    formData.add(AuthenticationManager.FORM_USERNAME, loginHint);
                } else {
                    formData.add(AuthenticationManager.FORM_USERNAME, rememberMeUsername);
                    formData.add("rememberMe", "on");
                }
            }
        }
        // setup webauthn data when passkeys enabled
        if (isConditionalPasskeysEnabled(context.getUser())) {
            webauthnAuth.fillContextForm(context);
        }

        // Response challengeResponse = challenge(context, formData);
        Response challengeResponse = setFormData(context, formData);
        context.challenge(challengeResponse);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        log.info("QrUsernamePasswordForm.action");
        super.action(context);
    }

    protected Response setFormData(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        log.info("QrUsernamePasswordForm.setFormData");

        LoginFormsProvider forms = context.form();

        if (!formData.isEmpty()) {
            forms.setAttribute("login", new LoginBean(formData));
            for (String key : formData.keySet()) {
                forms.setAttribute(key, formData.getFirst(key));
            } 
            forms.setFormData(formData);

        } else {
            log.info("QrUsernamePasswordForm.setFormData formData.isEmpty");
            forms.setAttribute("login", new LoginBean(new MultivaluedHashMap<>()));
        }

        return forms.createForm("qr-login.ftl");
    }
}
