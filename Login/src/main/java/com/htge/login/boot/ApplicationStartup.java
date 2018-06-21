package com.htge.login.boot;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.UserinfoDao;
import org.jboss.logging.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.StopWatch;

public class ApplicationStartup implements ApplicationListener<ContextRefreshedEvent> {
    private UserinfoDao userinfoDao;
    private LoginProperties properties;

    private static boolean isDone = false;
    private final Logger logger = Logger.getLogger(ApplicationStartup.class);

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }

    public void setProperties(LoginProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!isDone) {
            isDone = true;

            logger.info("onApplicationEvent");

            //Spring启动完成后做的事件处理（Redis服务器没停可以不用重建缓存，需要则通过SQLTest重建）
            if (properties.isCacheEnabled()) {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                userinfoDao.buildUserCache();
                stopWatch.stop();
                logger.info("buildUserCache() elapsed: "+stopWatch.getTotalTimeMillis()+"ms");
            }
        }
    }
}
