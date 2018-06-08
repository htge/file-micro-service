package com.htge.login.boot;

import com.htge.login.model.UserinfoDao;
import org.jboss.logging.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ApplicationStartup implements ApplicationListener<ContextRefreshedEvent> {
    @Resource(name = "UserinfoDao")
    private UserinfoDao userinfoDao;

    private static boolean isDone = false;
    private final Logger logger = Logger.getLogger(ApplicationStartup.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!isDone) {
            isDone = true;

            logger.info("onApplicationEvent");

            //Spring启动完成后做的事件处理
            userinfoDao.getAllUser();
        }
    }
}
