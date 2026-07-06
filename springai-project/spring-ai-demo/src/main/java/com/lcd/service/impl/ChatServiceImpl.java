package com.lcd.service.impl;

import com.lcd.service.ChatService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
@AllArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    @Override
    @CircuitBreaker(name = "chatAnywhereApi", fallbackMethod = "chatFallback")
    public String sendMessage(String message, String convId) {
        return chatClient.prompt()
                .user(message)
                // 给记忆Advisor传入会话ID
                .advisors(a -> a.param("chat_memory_conversation_id", convId))
                .call()
                .content();
    }

    @Override
    @CircuitBreaker(name = "chatAnywhereApi", fallbackMethod = "chatFallback")
    public Flux<String> sendFlux(String message, String convId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param("chat_memory_conversation_id", convId))
                .stream()
                .content();
    }

    /**
     * 熔断降级回调：熔断打开/超时/网络异常进入这里
     */
    private String chatFallback(String question, Exception ex) {
        // 熔断埋点，单独记录熔断事件入库
        // 友好业务返回，不再请求API
        return "AI服务当前网络繁忙，暂时无法提供问答，请等待10秒后重新提问";
    }
}
