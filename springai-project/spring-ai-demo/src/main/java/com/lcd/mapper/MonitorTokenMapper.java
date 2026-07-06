package com.lcd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lcd.domain.MonitorTokenRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MonitorTokenMapper extends BaseMapper<MonitorTokenRecord> {
}