package com.htge.download.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jboss.logging.Logger;
import org.springframework.util.StopWatch;

@Aspect
public class ElapsedTime {

    private Logger logger = Logger.getLogger(ElapsedTime.class);

    @Around("execution(* com.htge.download.controller.FileController.*(..))")
    public Object elapsed(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getName();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object ret = proceedingJoinPoint.proceed();
        stopWatch.stop();
        logger.info(className+"."+methodName+"() elapsed: " + stopWatch.getTotalTimeMillis() + "ms");
        return ret;
    }
}
