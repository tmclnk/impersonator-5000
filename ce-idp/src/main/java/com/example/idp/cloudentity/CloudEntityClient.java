package com.example.idp.cloudentity;

import com.example.idp.web.LoginCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

@Component
@Slf4j
public class CloudEntityClient {
    /**
     * The oauth2 client registration id. Don't change this unless you are ready to change the
     * corresponding configuration properties.
     */
    private static final String CLIENT_REGISTRATION_ID = "cloudentity";
    private final CloudEntityProperties cloudEntityProperties;
    private final WebClient webClient;

    public CloudEntityClient(CloudEntityProperties cloudEntityProperties, WebClient webClient) {
        this.cloudEntityProperties = cloudEntityProperties;
        this.webClient = webClient;
    }

    /**
     * POST to the CloudEntity "/accept" url.
     *
     * @return a redirect url (presumably to a consent screen)
     */
    public Mono<URI> accept(String subject, LoginCommand command) {
        var request = new AcceptRequest(subject, command.getLoginState());
        var acceptURI = cloudEntityProperties.acceptURI(command.getLoginId());
        decorate(command, request);
        return webClient
                .post()
                .uri(acceptURI)
                .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(CLIENT_REGISTRATION_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class).map(Exception::new))
                .bodyToMono(AcceptResponse.class)
                .map(AcceptResponse::getRedirectTo)
                .map(URI::create)
                .delayElement(Duration.ofSeconds(command.getDelaySeconds()));
    }


    private void decorate(LoginCommand loginCommand, AcceptRequest acceptRequest) {
        // put some fake CE AuthenticationContext attributes on to
        // help us trace them through the system
        var authenticationContext = acceptRequest.getAuthenticationContext();
        authenticationContext.put("my_attribute", "HELLO WORLD!!!!!!!!!!!");
        for (int i = 0; i < 10; i++) {
            var key = String.format("my_attribute%d", i);
            var value = String.format("i am my attribute %d", i);
            log.info("Placing {}='{}'", key, value);
            authenticationContext.put(key, value);
        }

        // "name" is what gets shown on the consent screen if using the "." mapping
        authenticationContext.put("name", loginCommand.getUsername().replaceAll("-", " "));
    }

}
