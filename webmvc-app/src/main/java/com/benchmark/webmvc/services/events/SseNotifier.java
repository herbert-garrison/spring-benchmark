package com.benchmark.webmvc.services.events;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseNotifier {
   SseEmitter createConnection(String userId);
   void broadcast(String event, Object payload);
   int size();
}
