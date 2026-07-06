/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lcd.advisor;

import com.lcd.monitor.MetricMonitorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI对话全链路监控埋点拦截器
 * 适配SpringAI 1.0 AssistantMessage.ToolCall 官方API
 * 采集：对话耗时、异常、Prompt/Completion Token、AI发起的工具调用次数
 */
public class SimpleLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(SimpleLoggerAdvisor.class);
    private final MetricMonitorUtil metricMonitorUtil;

    // 构造注入埋点工具
    public SimpleLoggerAdvisor(MetricMonitorUtil metricMonitorUtil) {
        this.metricMonitorUtil = metricMonitorUtil;
    }

    @Override
    public String getName() {
        return "SimpleLoggerAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    /** 同步对话拦截 */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest req, CallAroundAdvisorChain chain) {
        long startTs = System.currentTimeMillis();
        LocalDateTime recordTime = LocalDateTime.now();
        try {
            logger.debug("[同步对话] 请求上下文: {}", req);
            AdvisedResponse advisedRes = chain.nextAroundCall(req);
            long costMs = System.currentTimeMillis() - startTs;
            logger.debug("[同步对话] 执行耗时{}ms，响应封装: {}", costMs, advisedRes);
            metricMonitorUtil.recordChatCost("sync_chat", costMs);

            ChatResponse chatResponse = advisedRes.response();
            // 采集Token用量（对齐Usage标准接口）
            recordTokenMetric(recordTime, chatResponse);
            // 采集AI发起的工具调用记录
            recordToolCallMetric(recordTime, costMs, chatResponse);

            return advisedRes;
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - startTs;
            metricMonitorUtil.recordChatError("sync_chat", costMs);
            logger.error("[同步对话] 异常中断，耗时{}ms", costMs, e);
            throw e;
        }
    }

    /** 流式对话拦截 */
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest req, StreamAroundAdvisorChain chain) {
        long startTs = System.currentTimeMillis();
        LocalDateTime recordTime = LocalDateTime.now();
        logger.debug("[流式对话] 请求上下文: {}", req);
        Flux<AdvisedResponse> flux = chain.nextAroundStream(req);

        return flux
                .doOnNext(advisedRes -> {
                    ChatResponse chatResponse = advisedRes.response();
                    if (chatResponse == null) return;
                    long currentCost = System.currentTimeMillis() - startTs;
                    recordToolCallMetric(recordTime, currentCost, chatResponse);
                })
                .doOnComplete(() -> {
                    long totalCost = System.currentTimeMillis() - startTs;
                    logger.debug("[流式对话] 会话正常结束，总耗时{}ms", totalCost);
                    metricMonitorUtil.recordChatCost("stream_chat", totalCost);
                })
                .doOnError(err -> {
                    long costMs = System.currentTimeMillis() - startTs;
                    metricMonitorUtil.recordChatError("stream_chat", costMs);
                    logger.error("[流式对话] 异常终止，耗时{}ms", costMs, err);
                });
    }

    /**
     * 采集Token消耗
     * Usage: getPromptTokens()=输入令牌, getCompletionTokens()=输出令牌
     */
    private void recordTokenMetric(LocalDateTime time, ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getMetadata() == null) return;
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) return;

        Integer promptToken = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
        Integer completionToken = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
        metricMonitorUtil.recordTokenUsage(time, promptToken, completionToken);
    }

    /**
     * 采集AI发起的工具调用（正确API：AssistantMessage#getToolCalls()）
     * 层级：ChatResponse -> Generation -> AssistantMessage -> List<ToolCall>
     */
    private void recordToolCallMetric(LocalDateTime recordTime, long costMs, ChatResponse chatResponse) {
        if (chatResponse == null || CollectionUtils.isEmpty(chatResponse.getResults())) return;

        for (Generation generation : chatResponse.getResults()) {
            AssistantMessage assistantMsg = generation.getOutput();
            if (assistantMsg == null || !assistantMsg.hasToolCalls()) continue;

            List<AssistantMessage.ToolCall> toolCallList = assistantMsg.getToolCalls();
            for (AssistantMessage.ToolCall toolCall : toolCallList) {
                String toolName = toolCall.name();
                // AI发起调用=待执行，默认标记待执行；实际成功/失败由业务工具层埋点
                boolean pendingExecute = true;
                metricMonitorUtil.recordToolInvoke(recordTime, toolName, pendingExecute, costMs);
            }
        }
    }
}