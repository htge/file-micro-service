package com.htge.upload.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jboss.logging.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
@Aspect
public class ElapsedTime {

    private Logger logger = Logger.getLogger(ElapsedTime.class);

    @Around("execution(* com.htge.upload.controller.UploadController.*(..))")
    public Object elapsed(ProceedingJoinPoint proceedingJoinPoint) {
        Object ret = null;
        try {
            String methodName = proceedingJoinPoint.getSignature().getName();
            String className = proceedingJoinPoint.getTarget().getClass().getName();
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            ret = proceedingJoinPoint.proceed();
            stopWatch.stop();

            //仅打印10ms以上的信息
            final long millis = stopWatch.getTotalTimeMillis();
            if (millis > 10) {
                logger.warn(className + "." + methodName + "() elapsed: " + stopWatch.getTotalTimeMillis() + "ms");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return ret;
    }
}
