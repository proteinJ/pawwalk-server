package com.proteinj.boilerplate.domain.member.dto;

public record MemberResponse(
        Long id,
        String email,
        String role
) {
}
