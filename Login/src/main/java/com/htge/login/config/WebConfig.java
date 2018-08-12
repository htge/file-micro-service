package com.htge.login.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.concurrent.Executor;

@EnableWebMvc
@EnableAsync
@ImportResource(locations={"classpath:webmvc.xml"})
public class WebConfig extends WebMvcConfigurerAdapter implements AsyncConfigurer {
    private ThreadPoolTaskExecutor taskExecutor;

    public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //映射静态资源文件
        registry.addResourceHandler("/auth/css/**")
                .addResourceLocations("classpath:/web/css/");
        registry.addResourceHandler("/auth/js/**")
                .addResourceLocations("classpath:/web/js/");
        registry.addResourceHandler("/auth/images/**")
                .addResourceLocations("classpath:/web/images/");
        registry.addResourceHandler("/auth/localization/**")
                .addResourceLocations("classpath:/web/localization/");
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }
}