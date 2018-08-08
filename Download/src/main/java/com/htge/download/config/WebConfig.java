package com.htge.download.config;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.Resource;

@Configuration
@EnableWebMvc
@EnableAsync
@ImportResource({"classpath:download.xml", "classpath:webmvc.xml"})
public class WebConfig extends WebMvcConfigurerAdapter {
    @Resource(name = "properties")
    private FileProperties properties;

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer(){
        return new RedirectCustomizer(properties);
    }

    private static class RedirectCustomizer implements EmbeddedServletContainerCustomizer {
        private FileProperties properties;

        RedirectCustomizer(FileProperties properties) {
            this.properties = properties;
        }

        @Override
        public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
            configurableEmbeddedServletContainer.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, properties.getServerRoot()));
        }
    }
}