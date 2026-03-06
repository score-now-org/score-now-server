package com.scorenow.scorenow_api.domain.user.repository;

import com.scorenow.scorenow_api.domain.user.entity.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory,Long> {

    //최근 로그인 이력 10건 조회
    List<UserLoginHistory> findTop10ByUserIdOrderByLoginAtDesc(Long userId);
}
