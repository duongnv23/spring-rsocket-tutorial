package com.duongnv.tutorial.rsocket.rr;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@SpringBootApplication
public class RequestResponse {

    public static void main(String[] args) {
        SpringApplication.run(RequestResponse.class, args);
    }
}

@Component
class Producer implements Ordered, ApplicationListener<ApplicationReadyEvent> {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    Flux<String> notification(String name) {
        return Flux
                .fromStream(Stream.generate(() -> "Hello " + name + " @ " + Instant.now().toString()))
                .delayElements(Duration.ofSeconds(1));

    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        SocketAcceptor acceptor = (setup, sendingSocket) -> {

            AbstractRSocket abstractRSocket = new AbstractRSocket() {
                @Override
                public Flux<Payload> requestStream(Payload payload) {
                    String name = payload.getDataUtf8();
                    return notification(name)
                            .map(DefaultPayload::create);
                }
            };

            return Mono.just(abstractRSocket);
        };

        // can use tcp or websocket
        TcpServerTransport serverTransport = TcpServerTransport.create(700);
//        WebsocketServerTransport serverTransport1 = WebsocketServerTransport.create(7000);

        RSocketFactory
                .receive()
                .acceptor(acceptor)
                .transport(serverTransport)
                .start()
                .block();
    }
}

@Component
@Slf4j
class Consumer implements Ordered, ApplicationListener<ApplicationReadyEvent> {
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        RSocketFactory
                .connect()
                .transport(TcpClientTransport.create(700))
                .start()
                .flatMapMany(sender ->
                        sender.requestStream(DefaultPayload.create("Duongnv"))
                                .map(Payload::getDataUtf8)
                )
                .subscribe(result -> log.info("processing new result: {}", result));
    }
}

