package com.lcd.tools;

import com.lcd.annotation.ToolRetry;
import com.lcd.domain.PatientBloodRecord;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PatientBloodSugarRecordTool {

    @Tool(
            description = "根据患者唯一ID查询该患者全部历史空腹血糖随访记录，包含每次检测时间、血糖数值、医生随访备注；当用户询问患者过往血糖变化、历次检测数据、血糖随访情况时必须调用此工具"
    )
    @ToolRetry(maxRetry = 1, sleepMs = 600, retryExceptions = {RuntimeException.class})
    public List<PatientBloodRecord> queryPatientBloodSugarHistory(
            @ToolParam(description = "需要查询的患者唯一主键ID") Long patientId
    ) {
        // 模拟固定测试数据，无需查数据库
        if (patientId == 1L) {
            return List.of(
                    PatientBloodRecord.builder()
                            .patientId(1L)
                            .recordTime(LocalDateTime.of(2026, 6, 1, 8, 0))
                            .bloodSugar(8.2)
                            .remark("空腹血糖偏高，建议减少精米白面，餐后1小时快走30分钟")
                            .build(),
                    PatientBloodRecord.builder()
                            .patientId(1L)
                            .recordTime(LocalDateTime.of(2026, 6, 15, 8, 0))
                            .bloodSugar(7.5)
                            .remark("二甲双胍持续服用，血糖小幅下降，继续保持运动")
                            .build(),
                    PatientBloodRecord.builder()
                            .patientId(1L)
                            .recordTime(LocalDateTime.of(2026, 7, 1, 8, 0))
                            .bloodSugar(7.1)
                            .remark("糖化血红蛋白复查6.6%，控制达标，禁止含糖饮料")
                            .build()
            );
        } else if (patientId == 2L) {
            return List.of(
                    PatientBloodRecord.builder()
                            .patientId(2L)
                            .recordTime(LocalDateTime.of(2026,6,10,8,0))
                            .bloodSugar(6.3)
                            .remark("合并高血压，使用缬沙坦，血糖稳定")
                            .build()
            );
        }
        // 无该患者返回空列表
        return List.of();
    }
}