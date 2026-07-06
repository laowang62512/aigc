package com.lcd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("patient_blood_sugar_record")
public class PatientBloodRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long patientId; // 关联患者ID
    private LocalDateTime recordTime; // 血糖检测时间
    private Double bloodSugar; // 空腹血糖数值
    private String remark; // 随访医生备注
}