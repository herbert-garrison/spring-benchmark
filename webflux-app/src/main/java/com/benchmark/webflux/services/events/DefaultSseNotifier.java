package com.benchmark.webflux.services.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
public class DefaultSseNotifier implements SseNotifier {
    private final Map<String, Sinks.Many<ServerSentEvent<Object>>> sinks = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    @Override
    public Flux<ServerSentEvent<Object>> createConnection(String userId) {
        Sinks.Many<ServerSentEvent<Object>> sink = this.sinks.computeIfAbsent(userId, id -> Sinks.many().multicast().onBackpressureBuffer());

        ServerSentEvent<Object> event = this.buildEvent("connected", "ok");

        sink.tryEmitNext(event);

        log.debug("SSE connected: {}, total={}", userId, this.sinks.size());

        return sink.asFlux()
            .doFinally(signalType -> {
                this.sinks.remove(userId);
                log.debug("SSE closed: userId={}, reason={}", userId, signalType);
            });
    }

    // Unused at the moment of loading tests
    @Override
    public void broadcast(String event, Object payload) {
        ServerSentEvent<Object> newEvent = this.buildEvent(event, payload);
        this.sinks.forEach((userId, sink) -> {
            Sinks.EmitResult result = sink.tryEmitNext(newEvent);

            // Client has been disconnected while event was broadcasted
            if (result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
                this.sinks.remove(userId);
            }
        });
    }

    @Override
    public int size() {
        return this.sinks.size();
    }

    private ServerSentEvent<Object> buildEvent(String name, Object data) {
        return ServerSentEvent.builder()
            .id(String.valueOf(this.counter.incrementAndGet()))
            .event(name)
            .data(data)
            .build();
    }
}
