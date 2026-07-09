package com.proteinj.boilerplate.domain.member.service;

import com.proteinj.boilerplate.domain.member.domain.Member;
import com.proteinj.boilerplate.domain.member.domain.Role;
import com.proteinj.boilerplate.domain.member.domain.TokenDto;
import com.proteinj.boilerplate.domain.member.dto.LoginRequest;
import com.proteinj.boilerplate.domain.member.dto.SignupRequest;
import com.proteinj.boilerplate.domain.member.repository.MemberRepository;
import com.proteinj.boilerplate.domain.member.repository.RefreshTokenRepository;
import com.proteinj.boilerplate.global.error.BusinessException;
import com.proteinj.boilerplate.global.error.ErrorCode;
import com.proteinj.boilerplate.global.security.JwtProvider;
import com.proteinj.boilerplate.global.security.PrincipalDetails;
import com.proteinj.boilerplate.global.security.RefreshToken;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }
        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        memberRepository.save(member);
    }

    public TokenDto login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        TokenDto tokenDto = jwtProvider.createToken(authentication);
        saveRefreshToken(authentication.getName(), tokenDto.getRefreshToken());
        return tokenDto;
    }

    /**
     * Refresh Token 검증 후 새 토큰 쌍 발급 (회전 — 기존 RT는 즉시 폐기).
     * RT는 클레임이 없는 순수 서명 토큰이라, Redis에 key=RT문자열, value=email로
     * 저장해두고 그 매핑으로 사용자를 식별한다.
     */
    public TokenDto refresh(String refreshToken) {
        try {
            jwtProvider.validateTokenOrThrow(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        RefreshToken saved = refreshTokenRepository.findById(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        Member member = memberRepository.findByEmail(saved.getValue())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        refreshTokenRepository.deleteById(refreshToken); // 회전: 재사용 방지

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + member.getRole().name());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new PrincipalDetails(member.getId(), member.getEmail(), member.getPassword(), List.of(authority)),
                "",
                List.of(authority)
        );

        TokenDto tokenDto = jwtProvider.createToken(authentication);
        saveRefreshToken(member.getEmail(), tokenDto.getRefreshToken());
        return tokenDto;
    }

    /**
     * 현재 AT를 Redis 블랙리스트에 등록(남은 만료시간만큼 TTL) + RT 삭제.
     */
    public void logout(String accessToken, String refreshToken) {
        Long remainingMillis = jwtProvider.getExpiration(accessToken);
        if (remainingMillis > 0) {
            redisTemplate.opsForValue().set(accessToken, "logout", Duration.ofMillis(remainingMillis));
        }
        refreshTokenRepository.deleteById(refreshToken);
    }

    private void saveRefreshToken(String email, String refreshToken) {
        refreshTokenRepository.save(RefreshToken.builder()
                .key(refreshToken)
                .value(email)
                .build());
    }
}
