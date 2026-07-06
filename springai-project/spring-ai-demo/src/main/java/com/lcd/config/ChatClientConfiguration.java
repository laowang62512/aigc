package com.lcd.config;

import com.lcd.advisor.SimpleLoggerAdvisor;
import com.lcd.memory.JdbcMemory;
import com.lcd.monitor.MetricMonitorUtil;
import com.lcd.tools.PatientTool;
import com.lcd.utils.PromptFileReader;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
@Configuration
@AllArgsConstructor
public class ChatClientConfiguration {

    private final PromptFileReader promptFileReader;
    private final VectorStore vectorStore;
    private final MetricMonitorUtil metricMonitorUtil;

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, JdbcMemory jdbcMemory, PatientTool patientTool) {
        var qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.6d).topK(6).build())
                .build();


        String mainSystemPrompt = promptFileReader.readPromptFile("prompt/system.txt");
        // 把自定义JDBC记忆包装成Advisor
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(jdbcMemory)
                .build();
        return chatClientBuilder
                .defaultSystem(mainSystemPrompt)
                .defaultTools(patientTool)
                .defaultAdvisors(new SimpleLoggerAdvisor(metricMonitorUtil),memoryAdvisor,qaAdvisor)
                .build();
    }

    @Bean("summaryChatClient")
    public ChatClient summaryChatClient(ChatClient.Builder chatClientBuilder) {
        // 不注入任何会话记忆Advisor，纯裸客户端，和JdbcMemory无依赖关系
        return chatClientBuilder
                .defaultSystem("你是对话摘要助手，精简对话内容，300字以内输出")
                .defaultAdvisors(new SimpleLoggerAdvisor(metricMonitorUtil))
                .build();
    }
}