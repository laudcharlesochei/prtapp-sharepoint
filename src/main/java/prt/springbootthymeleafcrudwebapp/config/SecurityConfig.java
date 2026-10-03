package prt.springbootthymeleafcrudwebapp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

/**
 * Security + Microsoft Graph wiring.
 *
 * delegated mode: every page requires sign-in with a University of Dundee Microsoft account.
 * app mode:       pages are open (as in the original app); Graph is called with the app's own identity.
 */
@Configuration
@EnableConfigurationProperties(SharePointProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SharePointProperties props) throws Exception {
        if (props.isDelegated()) {
            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/signin", "/error", "/favicon.ico").permitAll()
                    .anyRequest().authenticated())
                .oauth2Login(login -> login
                    .loginPage("/signin")
                    .defaultSuccessUrl("/index", false))
                .logout(logout -> logout.logoutSuccessUrl("/signin?logout"));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(OAuth2AuthorizedClientService service) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(service);
    }

    /** HTTP client for Microsoft Graph (JDK client supports PATCH). */
    @Bean
    public RestClient graphRestClient(RestClient.Builder builder) {
        return builder.requestFactory(new JdkClientHttpRequestFactory()).build();
    }
}
