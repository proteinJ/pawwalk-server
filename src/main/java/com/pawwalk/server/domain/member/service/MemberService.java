package com.pawwalk.server.domain.member.service;

import com.pawwalk.server.domain.member.domain.Member;
import com.pawwalk.server.domain.member.dto.MemberResponse;
import com.pawwalk.server.domain.member.dto.PasswordChangeRequest;
import com.pawwalk.server.domain.member.repository.MemberRepository;
import com.pawwalk.server.global.error.BusinessException;
import com.pawwalk.server.global.error.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberResponse getMe(UUID memberId) {
        Member member = findMember(memberId);
        return new MemberResponse(member.getId(), member.getEmail(), member.getRole().name());
    }

    @Transactional
    public void changePassword(UUID memberId, PasswordChangeRequest request) {
        Member member = findMember(memberId);
        if (member.getPassword() == null || !passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        member.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void deleteMe(UUID memberId) {
        Member member = findMember(memberId);
        memberRepository.delete(member);
        // 주의: 남아있는 Refresh Token은 Redis에 key=토큰문자열로 저장되어 있어 memberId로
        // 일괄 삭제가 안 됨 — 자연 TTL(7일)로 만료됨. 탈퇴 즉시 완전 무효화가 필요하면
        // RefreshToken에 memberId 보조 인덱스를 추가하는 걸 고려할 것.
    }

    private Member findMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
