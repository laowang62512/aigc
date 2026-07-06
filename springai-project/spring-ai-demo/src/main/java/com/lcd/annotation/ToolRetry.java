package com.lcd.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolRetry {
    /**
     * 最大重试次数
     */
    int maxRetry() default 2;

    /**
     * 每次重试休眠间隔，单位ms
     */
    long sleepMs() default 500;

    /**
     * 触发重试的异常类型
     */
    Class<? extends Exception>[] retryExceptions() default {Exception.class};
}