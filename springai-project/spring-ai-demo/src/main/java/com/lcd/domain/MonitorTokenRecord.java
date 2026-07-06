package com.lcd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("monitor_token_stat")
public class MonitorTokenRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对话发生时间 */
    private LocalDateTime statTime;

    /** 输入token数量 */
    private Integer inputToken;

    /** 输出token数量 */
    private Integer outputToken;

    /** 总token */
    private Integer totalToken;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}