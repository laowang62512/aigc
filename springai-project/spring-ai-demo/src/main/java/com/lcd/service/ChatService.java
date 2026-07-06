package com.lcd.service;

import reactor.core.publisher.Flux;

public interface ChatService {
    String sendMessage(String message, String convId);

    Flux<String> sendFlux(String message, String convId);
}
