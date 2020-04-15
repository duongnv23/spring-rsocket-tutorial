package com.duongnv.tutorial.rsocket.channel;

import io.rsocket.*;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@SpringBootApplication
public class PingPong {
    static String reply(String in) {
        if (in.equalsIgnoreCase("ping")) {
            return "pong";
        } else if (in.equalsIgnoreCase("pong")) {
            return "ping";
        } else {
            throw new IllegalArgumentException("incoming value must be either'ping' or 'pong'! ");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(PingPong.class, args);
    }
}

@Component
@Slf4j
class Ping implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("starting " + this.getClass().getName());

        RSocketFactory
                .connect()
                .transport(TcpClientTransport.create(7000))
                .start()
                .flatMapMany(rSocket ->
                        rSocket
                                .requestChannel(Flux.interval(Duration.ofSeconds(1))
                                        .map(i -> DefaultPayload.create("ping")))
                                .map(Payload::getDataUtf8)
                                .doOnNext(s -> log.info("receive: '{}' in {}", s, getClass().getName()))
                                .take(10)
                                .doFinally(signalType -> rSocket.dispose())
                )
                .then()
                .block();

    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

@Component
@Slf4j
class Pong implements ApplicationListener<ApplicationReadyEvent>, Ordered, SocketAcceptor {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        RSocketFactory
                .receive()
                .acceptor(this)
                .transport(TcpServerTransport.create(7000))
                .start()
                .subscribe();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
        AbstractRSocket rs = new AbstractRSocket() {
            @Override
            public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                return Flux
                        .from(payloads)
                        .map(Payload::getDataUtf8)
                        .doOnNext(s -> log.info("received: '{}' in {}", s, getClass().getName()))
                        .map(PingPong::reply)
                        .map(DefaultPayload::create);
            }
        };
        return Mono.just(rs);
    }
}
