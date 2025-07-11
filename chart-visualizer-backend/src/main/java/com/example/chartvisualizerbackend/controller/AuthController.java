// src/main/java/com/example/chartvisualizerbackend/controller/AuthController.java
package com.example.chartvisualizerbackend.controller;

import com.example.chartvisualizerbackend.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000") // Frontend portunuza göre ayarlayın
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        try {
            // Spring Security'nin AuthenticationManager'ı ile kimlik doğrulamasını dene
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // Kimlik doğrulama başarılıysa JWT oluştur
            final String jwt = jwtUtil.generateToken(username);

            // JWT'yi Response body olarak döndür
            Map<String, String> response = new HashMap<>();
            response.put("jwt", jwt);
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            // Kimlik doğrulama başarısız olursa hata döndür
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kullanıcı adı veya parola yanlış.");
        } catch (Exception e) {
            // Diğer beklenmedik hatalar
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Giriş sırasında bir hata oluştu: " + e.getMessage());
        }
    }
}