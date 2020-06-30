package cn.jmu.pictureprocess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(byteArrayHttpMessageConverter());
        converters.add(bufferedImageHttpMessageConverter());
        converters.add(resourceHttpMessageConverter());
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(60000);
        configurer.registerCallableInterceptors(timeoutInterceptor());
    }

    @Bean
    public TimeoutCallableProcessingInterceptor timeoutInterceptor() {
        return new TimeoutCallableProcessingInterceptor();
    }

    private ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        return new ByteArrayHttpMessageConverter();
    }


    private BufferedImageHttpMessageConverter bufferedImageHttpMessageConverter() {
        return new BufferedImageHttpMessageConverter();
    }


    private ResourceHttpMessageConverter resourceHttpMessageConverter() {
        ResourceHttpMessageConverter converter = new ResourceHttpMessageConverter();
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        return new ResourceHttpMessageConverter();
    }
}
