package com.duongnv.tutorial.rsocket.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
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

@Controller
class GreetingRSocketController {
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
