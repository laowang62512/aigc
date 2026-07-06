package com.lcd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lcd.domain.Patient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientMapper extends BaseMapper<Patient> {
}
