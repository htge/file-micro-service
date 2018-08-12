package com.htge.login.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jboss.logging.Logger;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
@Aspect
@EnableCaching(proxyTargetClass = true)
public class ElapsedTime {

    final private Logger logger = Logger.getLogger(ElapsedTime.class);

    //异常要扔出去的原因是注册了统一异常处理机制，这里不用再处理了
    @Around("execution(* com.htge.login.rabbit.AuthRPCData.parseData(..))")
    public Object parseElapsed(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return elapsed(proceedingJoinPoint);
    }

    @Around("execution(* com.htge.login.controller.*Controller.*(..))")
    public Object controllerElapsed(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return elapsed(proceedingJoinPoint);
    }

    private Object elapsed(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getName();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object ret = proceedingJoinPoint.proceed();
        stopWatch.stop();
        logger.info(className+"."+methodName+"() elapsed: "+stopWatch.getTotalTimeMillis()+"ms");
        return ret;
    }
}
