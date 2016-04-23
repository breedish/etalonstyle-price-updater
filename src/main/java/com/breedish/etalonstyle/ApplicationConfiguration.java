package com.breedish.etalonstyle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * Class ApplicationConfiguration.
 *
 * @author zenind
 */
@Configuration
@ComponentScan(basePackages = "com.breedish.etalonstyle")
@PropertySource("classpath:system.properties")
public class ApplicationConfiguration {

    @Value("/styles/styles.css")
    private String styleSheetLocation;

    @Value("${version}")
    private String appVersion;

    @Bean
    public static PropertySourcesPlaceholderConfigurer configurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource(){
        ReloadableResourceBundleMessageSource messageSource=new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:message");
        return messageSource;
    }

    public String getStyleSheetLocation() {
        return styleSheetLocation;
    }

    public String getAppVersion() {
        return appVersion;
    }

}
