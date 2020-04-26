package com.duongnv.tutorial.rsocket.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@SpringBootApplication
public class ProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

}

@Configuration
@EnableRSocketSecurity
class RSocketSecurityConfiguration {

    @Bean
    RSocketMessageHandler messageHandler(RSocketStrategies socketStrategies) {
        var mh = new RSocketMessageHandler();
        mh.getArgumentResolverConfigurer().addCustomResolver(new AuthenticationPrincipalArgumentResolver());
        mh.setRSocketStrategies(socketStrategies);
        return mh;
    }

    @Bean
    PayloadSocketAcceptorInterceptor authorization(RSocketSecurity security) {
        return security
                .authorizePayload(spec ->
                        spec
                                .route("greetings").authenticated()
                                .anyExchange().permitAll()
                )
                .simpleAuthentication(Customizer.withDefaults())
                .build();
    }

    @Bean
    MapReactiveUserDetailsService authentication() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("user")
                .password("pw")
                .roles("USER").build();
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("pw")
                .roles("ADMIN", "USER").build();

        return new MapReactiveUserDetailsService(user, admin);
    }
}

@Controller
@Slf4j
class GreetingRSocketController {
    private static Flux<GreetingResponse> greet(String name) {
        return Flux.fromStream(
                Stream.generate(() -> GreetingResponse.with(name))
        ).delayElements(Duration.ofSeconds(1));
    }

    @MessageMapping("error")
    Flux<GreetingResponse> error() {
        return Flux.error(new IllegalArgumentException());
    }

    @MessageExceptionHandler
    Flux<GreetingResponse> errorHandler(IllegalArgumentException iae) {
        return Flux.just(new GreetingResponse("OH NO! "));
    }

    @MessageMapping("greet-stream")
    Flux<GreetingResponse> greetStream(GreetingRequest request) {
        return Flux
                .fromStream(Stream.generate(() -> GreetingResponse.with(request.getName())))
                .delayElements(Duration.ofSeconds(1));
    }

    @MessageMapping("greet")
    Mono<GreetingResponse> greet(GreetingRequest request) {
        return Mono.just(GreetingResponse.with(request.getName()));
    }


    @MessageMapping("greetings")
    Flux<GreetingResponse> greeting(@AuthenticationPrincipal Mono<UserDetails> user) {
        return /*ReactiveSecurityContextHolder
                .getContext()
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName)*/
                user
                        .map(u -> {
                            System.out.println(u.getUsername());
                            return u.getUsername();
                        })
                        .flatMapMany(GreetingRSocketController::greet)
                ;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class GreetingRequest {
    private String name;

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class GreetingResponse {

    private String greeting;

    static GreetingResponse with(String name) {
        return new GreetingResponse("Hello " + name + " @ " + Instant.now().toString());
    }
}
