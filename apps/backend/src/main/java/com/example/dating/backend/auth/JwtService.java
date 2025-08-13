package com.example.dating.backend.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    @Value("${JWT_SECRET:secret}")
    private String secret;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long telegramId) {
        return Jwts.builder()
                .subject(String.valueOf(telegramId))
                .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
                .signWith(key())
                .compact();
    }

    public String parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
}
