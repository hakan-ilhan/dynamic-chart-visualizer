// src/main/java/com/example/chartvisualizerbackend/service/CustomUserDetailsService.java
package com.example.chartvisualizerbackend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // Gerçek uygulamada bu bilgiler veritabanından gelir.
    // Şimdilik basit bir Map kullanarak sabit bir kullanıcı tanımlıyoruz.
    private Map<String, String> users = new HashMap<>();

    // PasswordEncoder'ı enjekte et
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // Uygulama başladığında örnek kullanıcıyı kaydet
    @PostConstruct
    public void init() {
        // Şifreyi BCrypt ile şifreleyerek kaydediyoruz
        users.put("user", passwordEncoder.encode("password"));
        users.put("admin", passwordEncoder.encode("adminpass"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!users.containsKey(username)) {
            throw new UsernameNotFoundException("Kullanıcı bulunamadı: " + username);
        }
        // Rolleri de ekleyebiliriz, şimdilik basit bir rol veriyoruz.
        return new User(username, users.get(username), new ArrayList<>()); // new ArrayList<>() boş roller listesi
    }
}