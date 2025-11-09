package com.hadleyso.keycloak.qrauth.auth;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class QrAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "ext-qr-code-login";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
        AuthenticationExecutionModel.Requirement.DISABLED
    };

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty refreshProperty = new ProviderConfigProperty();
        refreshProperty.setName("refresh.rate");
        refreshProperty.setLabel("Check Refresh Rate");
        refreshProperty.setType(ProviderConfigProperty.INTEGER_TYPE);
        refreshProperty.setHelpText("How often in seconds to reload the page to check if the authentication is approved. Zero disables refresh.");
        refreshProperty.setDefaultValue(15);
        refreshProperty.setRequired(true);
        configProperties.add(refreshProperty);

        ProviderConfigProperty timeoutProperty = new ProviderConfigProperty();
        timeoutProperty.setName("timeout.rate");
        timeoutProperty.setLabel("Login Timeout");
        timeoutProperty.setType(ProviderConfigProperty.INTEGER_TYPE);
        timeoutProperty.setHelpText("How long in seconds a QR code can be displayed before timeout. Zero disables timeout.");
        timeoutProperty.setDefaultValue(300);
        timeoutProperty.setRequired(true);
        configProperties.add(timeoutProperty);
    }

    @Override
    public void close() {
    }

    @Override
    public Authenticator create(KeycloakSession arg0) {
        return new QrAuthenticator();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Scope arg0) {
    }

    @Override
    public void postInit(KeycloakSessionFactory arg0) {
    }

    @Override
    public String getDisplayType() {
        return "QR Code Sign In";
    }

    @Override
    public String getReferenceCategory() {
        return "alternate-auth";
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
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getHelpText() {
        return "Sign in using another device by scanning a QR code.";
    }
    
}
