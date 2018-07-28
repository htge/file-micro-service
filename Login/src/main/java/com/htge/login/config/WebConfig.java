package com.htge.login.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

@Configuration
@EnableWebMvc
@EnableAsync
@ImportResource(locations={"classpath:webmvc.xml"})
public class WebConfig extends WebMvcConfigurerAdapter implements AsyncConfigurer {
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

    @Resource(name = "threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    @Override
    public Executor getAsyncExecutor() {
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer(){
        return new RedirectCustomizer();
    }

    private static class RedirectCustomizer implements EmbeddedServletContainerCustomizer {
        @Override
        public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
            configurableEmbeddedServletContainer.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/auth/"));
        }
    }
}