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

    private Logger logger = Logger.getLogger(ElapsedTime.class);

    @Around("execution(* com.htge.login.rabbit.AuthRPCData.parseData(..))")
    public Object parseElapsed(ProceedingJoinPoint proceedingJoinPoint) {
        return elapsed(proceedingJoinPoint);
    }

    @Around("execution(* com.htge.login.controller.*Controller.*(..))")
    public Object controllerElapsed(ProceedingJoinPoint proceedingJoinPoint) {
        return elapsed(proceedingJoinPoint);
    }

    private Object elapsed(ProceedingJoinPoint proceedingJoinPoint) {
        Object ret = null;
        try {
            String methodName = proceedingJoinPoint.getSignature().getName();
            String className = proceedingJoinPoint.getTarget().getClass().getName();
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            ret = proceedingJoinPoint.proceed();
            stopWatch.stop();
            logger.info(className+"."+methodName+"() elapsed: " + stopWatch.getTotalTimeMillis() + "ms");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return ret;
    }
}
