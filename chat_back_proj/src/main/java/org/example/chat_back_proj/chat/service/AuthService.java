package org.example.chat_back_proj.chat.service;

import org.example.chat_back_proj.chat.dto.LoginRequest;
import org.example.chat_back_proj.chat.dto.TokenResponse;
import org.example.chat_back_proj.chat.entity.RefreshToken;
import org.example.chat_back_proj.chat.entity.User;
import org.example.chat_back_proj.chat.repository.RefreshTokenRepository;
import org.example.chat_back_proj.chat.repository.UserRepository;
import org.example.chat_back_proj.chat.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                      JwtTokenProvider tokenProvider,
                      RefreshTokenRepository refreshTokenRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(loginRequest.getUsername());

        User user = userRepository.findByUserId(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TokenResponse response = new TokenResponse(accessToken, refreshToken, "Bearer");
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        
        saveRefreshToken(loginRequest.getUsername(), refreshToken);

        return response;
    }

    private void saveRefreshToken(String username, String token) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsername(username);
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(Instant.now().plusMillis(604800000)); // 7일
        refreshTokenRepository.save(refreshToken);
    }

    public TokenResponse refreshToken(String refreshToken) {
        try {
            // refresh token 전용 검증
            if (!tokenProvider.validateRefreshToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }

            String username = tokenProvider.getUsernameFromToken(refreshToken);
            
            // DB에서 저장된 refresh token 조회
            RefreshToken savedRefreshToken = refreshTokenRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            // 토큰 만료 여부 확인
            if (savedRefreshToken.getExpiryDate().isBefore(Instant.now())) {
                refreshTokenRepository.delete(savedRefreshToken);
                throw new RuntimeException("Refresh token was expired");
            }

            // 토큰 일치 여부 확인
            if (!savedRefreshToken.getToken().equals(refreshToken)) {
                throw new RuntimeException("Refresh token does not match");
            }

            // 새로운 access token과 refresh token 모두 재발급
            Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, null);
            String newAccessToken = tokenProvider.generateAccessToken(authentication);
            String newRefreshToken = tokenProvider.generateRefreshToken(username);

            // 새로운 refresh token 저장
            saveRefreshToken(username, newRefreshToken);

            // 사용자 정보 조회
            User user = userRepository.findByUserId(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 응답 생성
            TokenResponse response = new TokenResponse(newAccessToken, newRefreshToken, "Bearer");
            response.setUserId(user.getUserId());
            response.setUsername(user.getUsername());

            return response;
        } catch (Exception e) {
            log.error("Error during token refresh", e);
            throw new RuntimeException("Token refresh failed: " + e.getMessage());
        }
    }
} 