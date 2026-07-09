package com.pawwalk.server.domain.member.dto;

import java.util.UUID;

public record MemberResponse(
        UUID id,
        String email,
        String role
) {
}
