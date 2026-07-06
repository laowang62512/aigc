package com.lcd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("patient")
public class Patient {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String patientName; // 患者姓名
    private String idCard; // 身份证号码
    private Integer age;   // 年龄
    private String diseaseType; // 患病类型：糖尿病/高血压/高血脂
    private Double bloodSugar; // 空腹血糖
    private Integer bpHigh; // 收缩压
    private Integer bpLow; // 舒张压
}