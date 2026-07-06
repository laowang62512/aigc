package com.lcd.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcd.domain.ChatRecord;
import com.lcd.domain.dto.ChatMsgDTO;
import com.lcd.mapper.ChatRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JdbcMemory implements ChatMemory {

    private final ChatRecordMapper chatRecordMapper;
    private final ChatClient summaryChatClient;
    private final ObjectMapper objectMapper;
    private static final Long DEFAULT_USER_ID = 1L;
    private static final TypeReference<List<ChatMsgDTO>> DTO_LIST_TYPE = new TypeReference<>() {};
    //token上限
    @Value("${lcd.max-token}")
    private Integer MAX_CONTEXT_TOKEN ;



    /** Message 转 存储DTO */
    private ChatMsgDTO toDTO(Message msg) {
        String role;
        String content = msg.getText();
        if (msg instanceof UserMessage) {
            role = "user";
        } else if (msg instanceof AssistantMessage) {
            role = "assistant";
        } else if (msg instanceof SystemMessage) {
            role = "system";
        } else {
            role = "unknown";
        }
        return ChatMsgDTO.builder().role(role).content(content).build();
    }

    /** DTO 转回 Message */
    private Message toMessage(ChatMsgDTO dto) {
        return switch (dto.getRole()) {
            case "user" -> new UserMessage(dto.getContent());
            case "assistant" -> new AssistantMessage(dto.getContent());
            case "system" -> new SystemMessage(dto.getContent());
            default -> throw new RuntimeException("未知消息类型:" + dto.getRole());
        };
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        LambdaQueryWrapper<ChatRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatRecord::getConversationId, conversationId);
        ChatRecord existRecord = chatRecordMapper.selectOne(queryWrapper);

        try {
            LocalDateTime now = LocalDateTime.now();
            // 新消息转DTO列表
            List<ChatMsgDTO> newDtoList = messages.stream().map(this::toDTO).collect(Collectors.toList());
            String newJson = objectMapper.writeValueAsString(newDtoList);

            if (existRecord == null) {
                ChatRecord newRecord = ChatRecord.builder()
                        .conversationId(conversationId)
                        .data(newJson)
                        .createTime(now)
                        .updateTime(now)
                        .creater(DEFAULT_USER_ID)
                        .updater(DEFAULT_USER_ID)
                        .build();
                chatRecordMapper.insert(newRecord);
            } else {
                // 读取历史DTO
                List<ChatMsgDTO> historyDtoList = objectMapper.readValue(existRecord.getData(), DTO_LIST_TYPE);
                historyDtoList.addAll(newDtoList);
                String fullJson = objectMapper.writeValueAsString(historyDtoList);

                LambdaUpdateWrapper<ChatRecord> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(ChatRecord::getConversationId, conversationId)
                        .set(ChatRecord::getData, fullJson)
                        .set(ChatRecord::getUpdateTime, now)
                        .set(ChatRecord::getUpdater, DEFAULT_USER_ID);
                chatRecordMapper.update(null, updateWrapper);
            }
        } catch (Exception e) {
            throw new RuntimeException("保存聊天会话记录失败", e);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        LambdaQueryWrapper<ChatRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatRecord::getConversationId, conversationId);
        ChatRecord record = chatRecordMapper.selectOne(queryWrapper);

        if (record == null || record.getData() == null || record.getData().isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<ChatMsgDTO> dtoList = objectMapper.readValue(record.getData(), DTO_LIST_TYPE);
            List<Message> allMessages = dtoList.stream().map(this::toMessage).collect(Collectors.toList());


            int usedToken = 0;
            List<Message> recentWindowMsg = new ArrayList<>();
            int splitIndex = -1;

            // 1. 倒序收集最新消息，找到超限分割点
            for (int i = allMessages.size() - 1; i >= 0; i--) {
                Message msg = allMessages.get(i);
                int token = calculateTextToken(msg.getText());
                if (usedToken + token > MAX_CONTEXT_TOKEN) {
                    splitIndex = i; // 下标0~splitIndex是需要摘要的旧消息
                    break;
                }
                usedToken += token;
                recentWindowMsg.add(0, msg);
            }

            // 2. 没有超限，直接返回最新窗口消息
            if (splitIndex == -1) {
                return recentWindowMsg;
            }

            // 3. 存在超限：0~splitIndex的早期消息做摘要
            List<Message> oldHistory = allMessages.subList(0, splitIndex + 1);
            String historySummary = generateSummary(oldHistory);

            // 4. 组装最终上下文：摘要System提示 + 最新窗口对话
            List<Message> finalContext = new ArrayList<>();
            finalContext.add(new SystemMessage("【早期对话摘要】" + historySummary));
            finalContext.addAll(recentWindowMsg);
            return finalContext;
        } catch (Exception e) {
            throw new RuntimeException("读取聊天会话记录失败", e);
        }
    }

    @Override
    public void clear(String conversationId) {
        LambdaUpdateWrapper<ChatRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatRecord::getConversationId, conversationId)
                .set(ChatRecord::getData, "")
                .set(ChatRecord::getUpdateTime, LocalDateTime.now())
                .set(ChatRecord::getUpdater, DEFAULT_USER_ID);
        chatRecordMapper.update(null, updateWrapper);
    }
    /**
     * Token估算工具
     */
    private int calculateTextToken(String text) {
        if (text == null || text.isBlank()) return 0;
        long chineseCharCount = text.chars().filter(c -> c >= 0x4E00 && c <= 0x9FFF).count();
        String enPart = text.replaceAll("[^a-zA-Z0-9 ]", "");
        long wordCount = enPart.isBlank() ? 0 : enPart.split("\\s+").length;
        return (int) (chineseCharCount * 1.5 + wordCount);
    }

    public String generateSummary(List<Message> oldHistory) {
        // 把原来 buildHistorySummary 方法完整挪到这里
        StringBuilder historyText = new StringBuilder();
        for (Message msg : oldHistory) {
            String role;
            if (msg instanceof UserMessage) {
                role = "用户";
            } else if (msg instanceof AssistantMessage) {
                role = "助手";
            } else if (msg instanceof SystemMessage) {
                role = "系统";
            } else {
                role = "未知";
            }
            historyText.append(role).append("：").append(msg.getText()).append("\n");
        }
        String prompt = "下面是一段用户和AI的历史对话，请用简短中文提炼核心对话内容，控制在300字以内，只输出摘要不要多余解释：\n" + historyText;
        try {
            return summaryChatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            return "本次对话存在较长历史记录，早期内容已做省略处理";
        }
    }


}