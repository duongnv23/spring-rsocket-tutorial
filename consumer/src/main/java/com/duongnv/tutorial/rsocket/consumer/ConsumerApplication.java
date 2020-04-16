package com.duongnv.tutorial.rsocket.consumer;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @Bean
    RSocket rSocket() {
        return RSocketFactory
                .connect()
                .dataMimeType(MimeTypeUtils.APPLICATION_JSON_VALUE)
                .frameDecoder(PayloadDecoder.ZERO_COPY)
                .transport(TcpClientTransport.create(7000))
                .start()
                .block();
    }

    @Bean
    RSocketRequester requester(RSocketStrategies rSocketStrategies) {
        return RSocketRequester
                .builder()
                .rsocketStrategies(rSocketStrategies)
                .connect(TcpClientTransport.create(7000))
                .block();

    }
}

@RestController
class GreetingRestController {
    private final RSocketRequester requester;

    GreetingRestController(RSocketRequester requester) {
        this.requester = requester;
    }

    @GetMapping("/error")
    Publisher<GreetingResponse> error() {
        return this.requester
                .route("error")
                .data(Mono.empty())
                .retrieveFlux(GreetingResponse.class);
    }

    @GetMapping(value = "/greet/sse/{name}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Publisher<GreetingResponse> greetStream(@PathVariable String name) {
        return this.requester
                .route("greet-stream")
                .data(new GreetingRequest(name))
                .retrieveFlux(GreetingResponse.class);
    }

    @GetMapping("/greet/{name}")
    Publisher<GreetingResponse> greet(@PathVariable String name) {
        return this.requester
                .route("greet")
                .data(new GreetingRequest(name))
                .retrieveMono(GreetingResponse.class);
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

}