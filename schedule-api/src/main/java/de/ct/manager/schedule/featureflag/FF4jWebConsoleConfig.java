package de.ct.manager.schedule.featureflag;

import org.ff4j.FF4j;
import org.ff4j.web.FF4jDispatcherServlet;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@ConditionalOnClass({ FF4jDispatcherServlet.class })
@AutoConfigureAfter(FF4jConfig.class)
@ConditionalOnProperty(value = "ff4j.webconsole.enable", havingValue = "true", matchIfMissing = false)
public class FF4jWebConsoleConfig extends SpringBootServletInitializer implements WebMvcConfigurer {

    private static final String URL_FEATURE_FLAGS_CONSOLE = "/ff4j-web-console";

    @Bean
    @ConditionalOnMissingBean
    public FF4jDispatcherServlet getFF4jDispatcherServlet(FF4j ff4j) {
        log.info(
            "Initializing the web console servlet on '{}' as ff4j as been found in contexts",
            URL_FEATURE_FLAGS_CONSOLE
        );
        FF4jDispatcherServlet ff4jConsoleServlet = new FF4jDispatcherServlet();
        ff4jConsoleServlet.setFf4j(ff4j);
        return ff4jConsoleServlet;
    }

    @Bean
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ServletRegistrationBean ff4jDispatcherServletRegistrationBean(FF4jDispatcherServlet ff4jDispatcherServlet) {
        log.info(
            "Exposing FF4j web console on '{}' as property 'ff4j.webconsole.enable' is true",
            URL_FEATURE_FLAGS_CONSOLE
        );
        return new ServletRegistrationBean(ff4jDispatcherServlet, URL_FEATURE_FLAGS_CONSOLE + "/*");
    }
}

