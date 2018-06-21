package com.htge.login.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //映射静态资源文件
        registry.addResourceHandler("/auth/css/**")
                .addResourceLocations("classpath:/web/css/");
        registry.addResourceHandler("/auth/js/**")
                .addResourceLocations("classpath:/web/js/");
    }
}