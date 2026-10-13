package com.scorenow.scorenow_api.domain.user.repository;

import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndSocialId(String provider, String socialId);

    boolean existsByIdAndStatus(Long id, UserStatus status);

    boolean existsByNickname(String nickname);

    Optional<User> findBySocialId(String socialId);

    int countByNicknameStartingWith(String nickname);

}
