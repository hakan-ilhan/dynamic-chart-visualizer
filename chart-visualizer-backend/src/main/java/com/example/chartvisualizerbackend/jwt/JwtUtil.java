// src/main/java/com/example/chartvisualizerbackend/jwt/JwtUtil.java
package com.example.chartvisualizerbackend.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // JWT için gizli anahtar. application.properties'dan okunacak.
    // Güvenlik açısından, bu anahtarın güçlü ve güvenli bir şekilde saklanması gerekir.
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    // JWT'den kullanıcı adını çıkarır
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // JWT'den son kullanma tarihini çıkarır
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // JWT'den belirli bir claim'i (iddia) çıkarır
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // JWT'deki tüm claim'leri çıkarır
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
    }

    // JWT'nin geçerlilik süresinin dolup dolmadığını kontrol eder
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Kullanıcı detaylarına göre JWT doğrulaması yapar
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Yeni bir JWT oluşturur
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    // JWT oluşturma mantığı
    private String createToken(Map<String, Object> claims, String username) {
        // Token'ın geçerlilik süresi (örneğin 10 saat)
        long expirationTime = System.currentTimeMillis() + 1000 * 60 * 60 * 10; // 10 saat

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(expirationTime))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Gizli anahtarı bayt dizisine dönüştürür
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}