package prt.springbootthymeleafcrudwebapp.sharepoint;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import prt.springbootthymeleafcrudwebapp.config.SharePointProperties;

/**
 * Gets Microsoft Graph access tokens through Spring Security's OAuth2 client.
 * Tokens are cached and refreshed automatically.
 *
 * delegated: the signed-in user's token (registration "azure"), refreshed with the refresh token.
 * app:       an app-only token (registration "graph-app") via the client-credentials grant.
 */
@Component
public class OAuth2GraphTokenProvider implements GraphTokenProvider {

    public static final String DELEGATED_REGISTRATION = "azure";
    public static final String APP_REGISTRATION = "graph-app";
    private static final String APP_PRINCIPAL = "prtapp-service";

    private final SharePointProperties props;
    private final OAuth2AuthorizedClientManager delegatedManager;
    private final OAuth2AuthorizedClientManager appManager;

    public OAuth2GraphTokenProvider(SharePointProperties props,
                                    ClientRegistrationRepository registrations,
                                    OAuth2AuthorizedClientRepository authorizedClientRepository,
                                    OAuth2AuthorizedClientService authorizedClientService) {
        this.props = props;

        DefaultOAuth2AuthorizedClientManager delegated =
                new DefaultOAuth2AuthorizedClientManager(registrations, authorizedClientRepository);
        delegated.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build());
        this.delegatedManager = delegated;

        AuthorizedClientServiceOAuth2AuthorizedClientManager app =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClientService);
        app.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        this.appManager = app;
    }

    @Override
    public String getAccessToken() {
        OAuth2AuthorizedClient client;
        if (props.isDelegated()) {
            Authentication user = SecurityContextHolder.getContext().getAuthentication();
            if (user == null || user instanceof AnonymousAuthenticationToken) {
                // Picked up by Spring Security, which redirects the browser to Microsoft sign-in
                throw new ClientAuthorizationRequiredException(DELEGATED_REGISTRATION);
            }
            client = delegatedManager.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId(DELEGATED_REGISTRATION)
                    .principal(user)
                    .build());
        } else {
            client = appManager.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId(APP_REGISTRATION)
                    .principal(APP_PRINCIPAL)
                    .build());
        }
        if (client == null || client.getAccessToken() == null) {
            throw new SharePointException(401, "Could not obtain a Microsoft Graph access token. "
                    + "Check AZURE_TENANT_ID, AZURE_CLIENT_ID and AZURE_CLIENT_SECRET.");
        }
        return client.getAccessToken().getTokenValue();
    }
}
