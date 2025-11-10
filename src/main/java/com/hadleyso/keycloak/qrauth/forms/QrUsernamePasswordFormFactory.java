package com.hadleyso.keycloak.qrauth.forms;

import java.util.List;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import com.hadleyso.keycloak.qrauth.QrUtils;


public class QrUsernamePasswordFormFactory extends UsernamePasswordFormFactory {
    
    public static final String PROVIDER_ID = "qr-code-auth-username-password-form";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
        AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public Authenticator create(KeycloakSession session) {
        return new QrUsernamePasswordForm();
    }


    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getDisplayType() {
        return "Username Password Form with optional QR Code login";
    }

    @Override
    public String getHelpText() {
        return "Validates a username and password from login form and provides optional QR Code login.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return QrUtils.configProperties;
    }


}
