package com.renthouse.util;

import com.renthouse.enums.OperatorRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "MySecretKeyForJWTTokenGenerationAndValidation2024RentHouseSystem";
    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String generateUserToken(Long userId, Long accountId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("principalType", "USER");
        claims.put("userId", userId);
        claims.put("accountId", accountId);
        return createToken(claims, String.valueOf(userId));
    }

    public String generateOperatorToken(Long operatorId, OperatorRole role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("principalType", "OPERATOR");
        claims.put("operatorId", operatorId);
        claims.put("operatorRole", role.name());
        return createToken(claims, String.valueOf(operatorId));
    }

    public String extractPrincipalType(String token) {
        return extractClaim(token, claims -> claims.get("principalType", String.class));
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public Long extractAccountId(String token) {
        return extractClaim(token, claims -> claims.get("accountId", Long.class));
    }

    public Long extractOperatorId(String token) {
        return extractClaim(token, claims -> claims.get("operatorId", Long.class));
    }

    public String extractOperatorRole(String token) {
        return extractClaim(token, claims -> claims.get("operatorRole", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
