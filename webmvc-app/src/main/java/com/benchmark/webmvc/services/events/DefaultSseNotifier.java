package com.benchmark.webmvc.services.events;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DefaultSseNotifier implements SseNotifier {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    @Override
    public SseEmitter createConnection(String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        this.emitters.put(userId, emitter);

        this.send(emitter, userId, "connected", "ok");

        emitter.onCompletion(() -> this.emitters.remove(userId));
        emitter.onTimeout(() -> {
            this.emitters.remove(userId);
            emitter.complete();
            log.debug("SSE cancelled: {}", userId);
        });
        emitter.onError(e -> this.emitters.remove(userId));

        log.debug("SSE connected: {}, total={}", userId, this.emitters.size());
        return emitter;
    }

    // Unused at the moment of loading tests
    @Override
    public void broadcast(String event, Object payload) {
        this.emitters.forEach((userId, emitter) -> this.send(emitter, userId, event, payload));
    }

    @Override
    public int size() {
        return this.emitters.size();
    }

    private void send(
        SseEmitter emitter,
        String userId,
        String name,
        Object data
    ) {
        try {
            emitter.send(SseEmitter.event()
                .id(String.valueOf(this.counter.incrementAndGet()))
                .name(name)
                .data(data)
            );
        } catch (IOException e) {
            this.emitters.remove(userId);
            emitter.completeWithError(e);
        }
    }
}
