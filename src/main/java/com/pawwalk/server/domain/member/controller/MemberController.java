package com.pawwalk.server.domain.member.controller;

import com.pawwalk.server.domain.member.dto.MemberResponse;
import com.pawwalk.server.domain.member.dto.PasswordChangeRequest;
import com.pawwalk.server.domain.member.service.MemberService;
import com.pawwalk.server.global.common.ApiResponse;
import com.pawwalk.server.global.security.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMe(@AuthenticationPrincipal PrincipalDetails principal) {
        return ApiResponse.success("조회 성공", memberService.getMe(principal.getMemberId()));
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal PrincipalDetails principal,
            @Valid @RequestBody PasswordChangeRequest request) {
        memberService.changePassword(principal.getMemberId(), request);
        return ApiResponse.success("비밀번호 변경 완료");
    }

    /**
     * 회원 탈퇴. Apple App Store 심사 가이드라인 5.1.1(v) — 가입 기능이 있는 앱은
     * 앱 내 계정 삭제 기능이 필수라, iOS 클라이언트를 붙일 프로젝트라면 반드시 유지할 것.
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe(@AuthenticationPrincipal PrincipalDetails principal) {
        memberService.deleteMe(principal.getMemberId());
        return ApiResponse.success("탈퇴 완료");
    }
}
