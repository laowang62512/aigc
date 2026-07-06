package com.lcd.aop;


import com.lcd.annotation.ToolRetry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
public class ToolRetryAop {

    @Pointcut("@annotation(com.lcd.annotation.ToolRetry)")
    public void toolRetryPointCut() {}

    @Around("toolRetryPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        ToolRetry retryAnno = signature.getMethod().getAnnotation(ToolRetry.class);
        int maxRetry = retryAnno.maxRetry();
        long sleep = retryAnno.sleepMs();
        Class<? extends Exception>[] targetEx = retryAnno.retryExceptions();
        String toolMethodName = signature.getMethod().getName();

        Exception lastException = null;
        for (int current = 0; current <= maxRetry; current++) {
            try {
                return joinPoint.proceed();
            } catch (Exception e) {
                lastException = e;
                // 判断是否为允许重试的异常
                boolean matchEx = Arrays.stream(targetEx)
                        .anyMatch(clazz -> clazz.isAssignableFrom(e.getClass()));
                if (!matchEx) {
                    throw e;
                }
                // 未到最大次数，休眠重试
                if (current < maxRetry) {
                    Thread.sleep(sleep);
                    continue;
                }
            }
        }
        // 重试全部耗尽，拼接失败信息，包装原始异常抛出
        String errorMsg = String.format("工具【%s】已达到最大重试次数%d次，最终异常信息：%s",
                toolMethodName, maxRetry, lastException.getMessage());
        throw new RuntimeException(errorMsg, lastException);
    }
}