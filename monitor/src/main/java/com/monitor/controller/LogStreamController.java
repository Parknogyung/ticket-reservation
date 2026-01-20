package com.monitor.controller;

import com.ticket.portfolio.LogMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class LogStreamController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Infinite timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        // Initial connection event
        try {
            emitter.send(SseEmitter.event().name("connect").data("Connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcastLog(LogMessage log) {
        // Format log as JSON or simple string
        // We'll send JSON object
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("log").data(formatLog(log)));
            } catch (Exception e) {
                // Client disconnected or network error, remove emitter
                emitters.remove(emitter);
            }
        }
    }

    private String formatLog(LogMessage log) {
        return String.format("{\"timestamp\": \"%s\", \"level\": \"%s\", \"service\": \"%s\", \"message\": \"%s\"}",
                log.getTimestamp(), log.getLevel(), log.getServiceName(), log.getMessage().replace("\"", "\\\""));
    }
}
