package com.lcd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugSafetyResult {
    private String drugName; // 药品名称
    private Boolean isMatchDisease; // 是否适配患者基础病
    private Boolean hasConflict; // 是否存在用药冲突
    private String conflictDesc; // 冲突风险说明
    private String suggest; // 安全用药建议
}