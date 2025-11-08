package com.hadleyso.keycloak.qrauth.token;

import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;


import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class QrAuthenticatorActionTokenHandler extends AbstractActionTokenHandler<QrAuthenticatorActionToken> {
    
    public QrAuthenticatorActionTokenHandler() {
        super(
            QrAuthenticatorActionToken.TOKEN_ID, 
            QrAuthenticatorActionToken.class, 
            Messages.INVALID_REQUEST,
            EventType.EXECUTE_ACTION_TOKEN, 
            Errors.INVALID_REQUEST);
    }

    @Override
    public Response handleToken(QrAuthenticatorActionToken token, ActionTokenContext<QrAuthenticatorActionToken> tokenContext) {
        log.info("QrAuthenticatorActionTokenHandler.handleToken");
        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
        UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
        KeycloakSession session = tokenContext.getSession();

        final UriInfo uriInfo = tokenContext.getUriInfo();
        final RealmModel realm = tokenContext.getRealm();
        
        if (tokenContext.isAuthenticationSessionFresh()) { 
            // User was not logged in before

            // Update the authentication session in the token
            String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
            token.setCompoundAuthenticationSessionId(authSessionEncodedId);

            // Constructs a URL that the user can click to confirm QR code login
            UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                    authSession.getClient().getClientId(), authSession.getTabId(), AuthenticationProcessor.getClientData(session, authSession));
            String confirmUri = builder.build(realm.getName()).toString();

            return session.getProvider(LoginFormsProvider.class)
                .setAuthenticationSession(authSession)
                .setError("Sign in to continue")   // or a custom message
                .createLoginUsernamePassword();                  // redirect to login form

            // return session.getProvider(LoginFormsProvider.class)
            //         .setAuthenticationSession(authSession)
            //         .setSuccess(Messages.CONFIRM_ORGANIZATION_MEMBERSHIP, organization.getName())
            //         .setAttribute("messageHeader", Messages.CONFIRM_ORGANIZATION_MEMBERSHIP_TITLE)
            //         .setAttribute(Constants.TEMPLATE_ATTR_ACTION_URI, confirmUri)
            //         .setAttribute(OrganizationModel.ORGANIZATION_NAME_ATTRIBUTE, organization.getName())
            //         .createInfoPage();
        }
        

        return null;
    }

}
