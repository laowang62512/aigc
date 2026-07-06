package com.lcd.controller;

import com.lcd.service.ChatService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@AllArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    /**
     * 发送消息返回字符串
     * @return 字符串响应
     */
    @PostMapping("/send")
    public String send(String message, String convId) {
        return chatService.sendMessage(message, convId);
    }

    /**
     * 发送消息返回流式输出
     * @return 流式输出
     */
    @GetMapping("/sendFlux")
    public Flux< String> sendFlux(String message,String convId) {
        return chatService.sendFlux(message,convId);
    }

}
