package com.benchmark.webflux.services.events;

import org.springframework.http.codec.ServerSentEvent;

import reactor.core.publisher.Flux;

public interface SseNotifier {
   Flux<ServerSentEvent<Object>> createConnection(String userId);
   void broadcast(String event, Object payload);
   int size();
}
