package com.proteinj.boilerplate.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 최소 회원 엔티티. runApp의 Member는 Profile/SocialAccount 등 러닝 앱 전용
 * 연관관계를 갖고 있었는데, 범용 보일러플레이트라 전부 제거하고 인증에 꼭
 * 필요한 필드만 남겼다. 새 프로젝트에서 필요한 필드/연관관계를 추가해서 쓸 것.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column
    private String password; // 소셜 로그인 전용 계정이면 null 허용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public void updatePassword(String password) {
        this.password = password;
    }
}
