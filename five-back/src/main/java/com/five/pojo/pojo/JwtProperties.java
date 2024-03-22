package com.five.pojo.pojo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "five.jwt")
@Data
public class JwtProperties {

    private String SecretKey;
    private long Ttl;
}
