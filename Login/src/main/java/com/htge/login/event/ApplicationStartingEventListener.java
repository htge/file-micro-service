package com.htge.login.event;

import com.htge.login.util.Crypto;
import org.jboss.logging.Logger;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;

@SuppressWarnings({"Convert2MethodRef", "CodeBlock2Expr"})
public class ApplicationStartingEventListener implements ApplicationListener<ApplicationStartingEvent> {
    private final Logger logger = Logger.getLogger(ApplicationStartingEventListener.class);

    @Override
    public void onApplicationEvent(ApplicationStartingEvent applicationStartingEvent) {
        logger.info("starting event...");
        Crypto.initRSA();
    }
}
