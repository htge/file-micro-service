package com.htge.upload.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
@EnableAsync
@ImportResource({"classpath:webmvc.xml", "classpath:upload.xml"})
public class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //映射静态资源文件
        registry.addResourceHandler("/up/html/**")
                .addResourceLocations("classpath:/web/html/");
        registry.addResourceHandler("/up/images/**")
                .addResourceLocations("classpath:/web/images/");
        registry.addResourceHandler("/up/css/**")
                .addResourceLocations("classpath:/web/css/");
        registry.addResourceHandler("/up/js/**")
                .addResourceLocations("classpath:/web/js/");
    }
}