package com.proteinj.boilerplate.domain.member.repository;

import com.proteinj.boilerplate.global.security.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    void deleteByKey(String key);
}
