package ru.itmo.gymbro.shared.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

@Configuration
class WebFluxConfiguration implements WebFluxConfigurer {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;

    @Bean
    ServerWebExchangeContextFilter serverWebExchangeContextFilter() {
        return new ServerWebExchangeContextFilter();
    }

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(pageableResolver());
    }

    static ReactivePageableHandlerMethodArgumentResolver pageableResolver() {
        ReactivePageableHandlerMethodArgumentResolver pageable = new ReactivePageableHandlerMethodArgumentResolver();
        pageable.setFallbackPageable(PageRequest.of(0, DEFAULT_PAGE_SIZE));
        pageable.setMaxPageSize(MAX_PAGE_SIZE);
        return pageable;
    }
}
