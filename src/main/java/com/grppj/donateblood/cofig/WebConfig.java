package com.grppj.donateblood.cofig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	 @Autowired
	    private LoginInterceptor loginInterceptor; // ✅ inject the interceptor

	    @Override
	    public void addInterceptors(InterceptorRegistry registry) {
	        registry.addInterceptor(loginInterceptor)
	                .addPathPatterns(
	                        "/profile",
	                        "/index",
	                        "/indexL",
	                        "/indexR",
	                        "/appointment",
	                        "/bloodrequest/**",
	                        "/indexR",
	                        "/superadmin/**",
	                        "/admin/**"
	                )
	                .excludePathPatterns(
	                        "/login",
	                        "/register",
	                        "/error",
	                        "/admin/login",
	                        "/admin/send-code",
	                        "/admin/verify-code",
	                        "/admin/set-password",
	                        "/css/**",
	                        "/js/**",
	                        "/images/**",
	                        "/uploads/**"
	                );
	    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This makes /uploads/ URLs serve files from uploads/ directory
        registry.addResourceHandler("/uploads/hospitals/**")
                .addResourceLocations("file:uploads/hospitals/");
    }
}