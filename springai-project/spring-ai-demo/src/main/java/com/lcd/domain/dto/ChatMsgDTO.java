package com.lcd.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMsgDTO {
    /** 消息类型：user / assistant / system */
    private String role;
    /** 消息内容 */
    private String content;
}