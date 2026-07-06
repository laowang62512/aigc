package com.lcd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("monitor_tool_stat")
public class MonitorToolRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工具调用时间 */
    private LocalDateTime statTime;

    /** 工具名称 */
    private String toolName;

    /** 1成功 0失败 */
    private Integer success;

    /** 执行耗时ms */
    private Long costMs;

    /** 创建时间 */
    private LocalDateTime createTime;
}