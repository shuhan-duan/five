package com.five.interceptor;

import com.five.pojo.pojo.JwtProperties;
import com.five.utils.BaseContext;
import com.five.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Check if the intercepted object is a method of a controller or other resource
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // If it's not a dynamic method, allow the request to proceed
            return true;
        }

        // Allow access if the requested class is a Swagger controller
        if (handlerMethod.getBean().getClass().getName().equals("springfox.documentation.swagger.web.ApiResourceController")) {
            return true;
        }

        // 1. Get the token from the request header
        String token = request.getHeader("jwt");
        if (token == null) {
            // 3. If there is no token, respond with status code 401
            response.setStatus(401);
            return false;
        }
        // 2. Validate the token
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
            Object userId = claims.get("userId");
            BaseContext.setCurrentId(((Integer) userId).longValue());
            return true;
        } catch (Exception ex) {
            log.info("Validation failed, error message: {}", ex.getMessage());
            // 4. If validation fails, respond with status code 401
            response.setStatus(401);
            return false;
        }
    }
}
