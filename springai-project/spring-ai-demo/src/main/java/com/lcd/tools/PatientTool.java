package com.lcd.tools;

import com.lcd.annotation.ToolRetry;
import com.lcd.domain.Patient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class PatientTool {

    @Tool(description = "根据患者ID查询患者完整病历信息，包含姓名、年龄、疾病类型、血糖血压检测数据，当用户提问涉及指定patientId患者的病情、指标、病史时必须调用此工具")
    @ToolRetry(maxRetry = 1, sleepMs = 600, retryExceptions = {RuntimeException.class})
    public String getPatientInfo(String patientId) {
        // 模拟数据库查询
        Patient patient = new Patient();
        patient.setId(Long.parseLong(patientId));
        patient.setPatientName("张三");
        patient.setAge(30);
        patient.setDiseaseType("糖尿病");
        patient.setBloodSugar(180.0);
        patient.setBpHigh(120);
        patient.setBpLow(80);
        patient.setIdCard("321284199608168210");
        //数据清洗，避免返回敏感数据（加**）
        patient.setIdCard(patient.getIdCard().substring(0, 6) + "**" + patient.getIdCard().substring(14));
        return patient.toString();
    }

}
