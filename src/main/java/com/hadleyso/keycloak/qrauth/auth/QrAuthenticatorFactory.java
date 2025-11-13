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

import com.hadleyso.keycloak.qrauth.QrUtils;

public class QrAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "ext-qr-code-login";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
        AuthenticationExecutionModel.Requirement.DISABLED
    };

    private static final List<ProviderConfigProperty> properties = new ArrayList<ProviderConfigProperty>(QrUtils.configProperties);        


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
        return null;
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
        
        return properties;
    }

    @Override
    public String getHelpText() {
        return "Sign in using another device by scanning a QR code.";
    }
    
}
