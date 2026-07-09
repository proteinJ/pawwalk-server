package com.proteinj.boilerplate.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class PrincipalDetails extends User {
    private final Long memberId; // 실제 DB의 PK 값

    public PrincipalDetails(Long memberId, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.memberId = memberId;
    }
}
