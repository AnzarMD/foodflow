package com.foodflow.order_service.auth.service;

import com.foodflow.order_service.auth.dto.*;
import com.foodflow.order_service.auth.entity.RefreshToken;
import com.foodflow.order_service.auth.entity.User;
import com.foodflow.order_service.auth.repository.RefreshTokenRepository;
import com.foodflow.order_service.auth.repository.UserRepository;
import com.foodflow.order_service.common.exception.TokenException;
import com.foodflow.order_service.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-token-expiry}")
    private long refreshTokenExpiryMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());

        return buildAuthResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Revoke all previous refresh tokens on new login
        refreshTokenRepository.revokeAllUserTokens(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new TokenException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new TokenException("Refresh token has expired or been revoked");
        }

        // Rotate: revoke old token, issue new one
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );

        String rawRefreshToken = jwtService.generateRefreshToken();
        saveRefreshToken(user, rawRefreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(900)
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    private void saveRefreshToken(User user, String token) {
        LocalDateTime expiry = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiryMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiry)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }
}