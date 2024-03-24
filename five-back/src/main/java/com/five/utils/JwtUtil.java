package com.five.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtil {
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // Specify the signature algorithm used when signing, which is the header part
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        // Generate the time when the JWT is created
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // Set the body of the JWT
        JwtBuilder builder = Jwts.builder()
                // If there are private claims, you must set the private claims created by yourself first.
                // This is to assign a value to the builder's claim.
                // Once written after the assignment of standard claims, it will overwrite those standard claims.
                .setClaims(claims)
                // Set the signature algorithm and secret key used for signing
                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8))
                // Set the expiration time
                .setExpiration(exp);

        return builder.compact();
    }

   
    public static Claims parseJWT(String secretKey, String token) {
        // Get the DefaultJwtParser
        return Jwts.parser()
                // Set the signing key
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                // Set the JWT to be parsed
                .parseClaimsJws(token).getBody();
    }

}
