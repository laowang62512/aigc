package com.lcd.tools;


import com.lcd.annotation.ToolRetry;
import com.lcd.domain.DrugSafetyResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DrugSuggestSafetyTool {

    @Tool(
            description = "校验指定药品对目标患者的适配性与用药冲突风险：根据患者ID读取患者基础慢性病，判断药品是否适配该病，是否存在基础病禁忌冲突；用户询问患者能不能吃某类降糖/降压/降脂药、用药安全、药物副作用、联合用药风险时调用"
    )
    @ToolRetry(maxRetry = 1, sleepMs = 600, retryExceptions = {RuntimeException.class})
    public List<DrugSafetyResult> checkDrugSafetyForPatient(
            @ToolParam(description = "待校验药品名称，多个药品用英文逗号分隔，如：二甲双胍,缬沙坦,阿托伐他汀") String drugNames,
            @ToolParam(description = "需要校验用药安全的患者唯一ID") Long patientId
    ) {
        // 模拟患者基础疾病，替代数据库查询
        String patientDisease;
        if (patientId == 1L) {
            patientDisease = "2型糖尿病";
        } else if (patientId == 2L) {
            patientDisease = "原发性高血压";
        } else if (patientId == 3L) {
            patientDisease = "混合型高脂血症";
        } else {
            patientDisease = "未知疾病";
        }

        String[] drugArr = drugNames.split(",");
        List<DrugSafetyResult> resultList = new ArrayList<>();

        for (String drug : drugArr) {
            String drugName = drug.trim();
            DrugSafetyResult result = new DrugSafetyResult();
            result.setDrugName(drugName);
            boolean match = false;
            boolean conflict = false;
            String conflictText = "";
            String suggestText = "";

            switch (drugName) {
                case "二甲双胍":
                    match = "2型糖尿病".equals(patientDisease);
                    suggestText = "2型糖尿病一线基础用药，餐后服用，每日监测空腹血糖";
                    if ("原发性高血压".equals(patientDisease)) {
                        conflict = false;
                        conflictText = "无用药冲突，可与ARB类降压药联用";
                    } else if ("混合型高脂血症".equals(patientDisease)) {
                        conflict = false;
                        conflictText = "可搭配他汀类降脂药同步使用";
                    }
                    break;
                case "缬沙坦":
                    match = "原发性高血压".equals(patientDisease);
                    suggestText = "ARB类降压药，糖尿病合并肾病人群首选，晨起服用";
                    if ("2型糖尿病".equals(patientDisease)) {
                        conflict = false;
                        conflictText = "糖尿病合并高血压推荐搭配，可保护肾脏微血管";
                    }
                    break;
                case "阿托伐他汀":
                    match = "混合型高脂血症".equals(patientDisease);
                    suggestText = "他汀类降脂一线药物，睡前服用，每3个月复查肝功能";
                    break;
                default:
                    match = false;
                    conflict = true;
                    conflictText = "暂无该药品适配慢性病数据，存在未知风险";
                    suggestText = "禁止自行服用，前往内分泌/心血管内科面诊调整用药";
            }
            result.setIsMatchDisease(match);
            result.setHasConflict(conflict);
            result.setConflictDesc(conflictText);
            result.setSuggest(suggestText);
            resultList.add(result);
        }
        return resultList;
    }
}