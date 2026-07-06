package com.lcd.monitor;


import com.lcd.domain.MonitorTokenRecord;
import com.lcd.domain.MonitorToolRecord;
import com.lcd.mapper.MonitorTokenMapper;
import com.lcd.mapper.MonitorToolMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@AllArgsConstructor
public class MetricMonitorUtil {
    // 内存缓冲队列，定时任务批量入库，减少DB压力
    private final ConcurrentLinkedQueue<MonitorTokenRecord> tokenQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MonitorToolRecord> toolQueue = new ConcurrentLinkedQueue<>();

    private MonitorTokenMapper monitorTokenMapper;
    private MonitorToolMapper monitorToolMapper;

    // ====================== 1. 对话耗时埋点 ======================
    public void recordChatCost(String chatType, long costMs) {
        // 可对接Prometheus/Micrometer指标，这里先日志记录
        System.out.printf("[MONITOR] chatType=%s cost=%dms%n", chatType, costMs);
    }

    public void recordChatError(String chatType, long costMs) {
        System.out.printf("[MONITOR ERROR] chatType=%s cost=%dms%n", chatType, costMs);
    }

    // ====================== 2. Token消耗埋点（入内存队列） ======================
    public void recordTokenUsage(LocalDateTime time, int input, int output) {
        MonitorTokenRecord record = new MonitorTokenRecord();
        record.setStatTime(time);
        record.setInputToken(input);
        record.setOutputToken(output);
        record.setTotalToken(input + output);
        tokenQueue.offer(record);
    }

    // ====================== 3. 工具调用埋点（成功/失败、耗时） ======================
    public void recordToolInvoke(LocalDateTime time, String toolName, boolean success, long costMs) {
        MonitorToolRecord record = new MonitorToolRecord();
        record.setStatTime(time);
        record.setToolName(toolName);
        record.setSuccess(success ? 1 : 0);
        record.setCostMs(costMs);
        toolQueue.offer(record);
    }

    // ====================== 4. RAG各阶段耗时埋点（给RagSearchUtil、EmbeddingController调用） ======================
    /** PDF解析耗时 */
    public void recordPdfParseCost(long cost) {
        System.out.printf("[MONITOR RAG] pdf_parse cost=%dms%n", cost);
    }
    /** 文本切片耗时 */
    public void recordChunkCost(long cost) {
        System.out.printf("[MONITOR RAG] text_chunk cost=%dms%n", cost);
    }
    /** 向量入库耗时 */
    public void recordVectorStoreCost(long cost) {
        System.out.printf("[MONITOR RAG] vector_save cost=%dms%n", cost);
    }
    /** 混合检索耗时 */
    public void recordHybridSearchCost(long cost) {
        System.out.printf("[MONITOR RAG] hybrid_search cost=%dms%n", cost);
    }

    // ====================== 定时任务调用：批量刷入数据库 ======================
    public void flushTokenToDb() {
        while (!tokenQueue.isEmpty()) {
            MonitorTokenRecord item = tokenQueue.poll();
            if (item != null) {
                monitorTokenMapper.insert(item);
            }
        }
    }

    public void flushToolToDb() {
        while (!toolQueue.isEmpty()) {
            MonitorToolRecord item = toolQueue.poll();
            if (item != null) {
                monitorToolMapper.insert(item);
            }
        }
    }

}
