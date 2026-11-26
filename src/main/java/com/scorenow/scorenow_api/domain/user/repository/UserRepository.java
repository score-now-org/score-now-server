package com.scorenow.scorenow_api.domain.user.repository;

import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Query("select u.id from User u where u.provider = :provider and u.socialId = :socialId")
    Optional<Long> findIdByProviderAndSocialId(@Param("provider") String provider, @Param("socialId") String socialId);

    @Modifying(flushAutomatically = true)
    @Query("update User u set u.status = :active, u.banExpiresAt = null " +
            "where u.status = :banned and u.banExpiresAt is not null and u.banExpiresAt <= :now")
    int expireBans(@Param("active") UserStatus active, @Param("banned") UserStatus banned,
                   @Param("now") LocalDateTime now);

    boolean existsByIdAndStatus(Long id, UserStatus status);

    boolean existsByNickname(String nickname);

    Optional<User> findBySocialId(String socialId);

    int countByNicknameStartingWith(String nickname);

}
