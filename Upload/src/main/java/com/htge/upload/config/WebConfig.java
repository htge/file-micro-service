package com.htge.upload.config;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
@EnableAsync
@ImportResource("classpath:upload.xml")
public class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //映射静态资源文件
        registry.addResourceHandler("/up/css/**")
                .addResourceLocations("classpath:/html/css/");
        registry.addResourceHandler("/up/js/**")
                .addResourceLocations("classpath:/html/js/");
        registry.addResourceHandler("/up/fonts/**")
                .addResourceLocations("classpath:/html/fonts/");
        registry.addResourceHandler("/up/images/**")
                .addResourceLocations("classpath:/html/images/");
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer(){
        return new RedirectCustomizer();
    }

    private static class RedirectCustomizer implements EmbeddedServletContainerCustomizer {
        @Override
        public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
            configurableEmbeddedServletContainer.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/up/"));
        }
    }
}