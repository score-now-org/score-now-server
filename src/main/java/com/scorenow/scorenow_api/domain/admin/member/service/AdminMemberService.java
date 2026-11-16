package com.scorenow.scorenow_api.domain.admin.member.service;

import com.scorenow.scorenow_api.domain.admin.member.dto.MemberBasicInfoResponseDto;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberListResponseDto;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberSearchCondition;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberSecurityInfoResponseDto;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.entity.UserLoginHistory;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.repository.UserLoginHistoryRepository;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final UserRepository userRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;
    /**
     * 회원 목록 전체 조회
     */
    @Transactional(readOnly = true)
    public Page<MemberListResponseDto> getMembers(MemberSearchCondition condition, Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(order -> !java.util.Set.of("id", "nickname", "createdAt", "warningCnt", "status", "deletedAt").contains(order.getProperty()))
                || pageable.getPageSize() > 100 || (condition.getMemberId() != null && condition.getMemberId() <= 0)
                || (condition.getMinWarningCnt() != null && condition.getMinWarningCnt() < 0)
                || LocalDate.MAX.equals(condition.getCreatedTo())
                || (condition.getCreatedFrom() != null && condition.getCreatedTo() != null
                    && condition.getCreatedFrom().isAfter(condition.getCreatedTo()))) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        UserStatus status = parseStatus(condition.getStatus());
        Specification<User> filter = (user, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(condition.getNickname())) {
                String escaped = condition.getNickname().trim().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicates.add(cb.like(cb.lower(user.get("nickname")), "%" + escaped + "%", '!'));
            }
            if (StringUtils.hasText(condition.getSocialId())) {
                String escaped = condition.getSocialId().trim().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicates.add(cb.like(cb.lower(user.get("socialId")), "%" + escaped + "%", '!'));
            }
            if (condition.getMemberId() != null) predicates.add(cb.equal(user.get("id"), condition.getMemberId()));
            if (StringUtils.hasText(condition.getProvider())) predicates.add(cb.equal(cb.lower(user.get("provider")), condition.getProvider().trim().toLowerCase(Locale.ROOT)));
            if (Boolean.TRUE.equals(condition.getHasProfileImage())) predicates.add(cb.and(cb.isNotNull(user.get("profileImageUrl")), cb.notEqual(user.get("profileImageUrl"), "")));
            if (status != null) predicates.add(cb.equal(user.get("status"), status));
            if (Boolean.TRUE.equals(condition.getExcludeWithdrawn())) predicates.add(cb.not(user.get("status").in(UserStatus.DELETED, UserStatus.PERMANENTLY_WITHDRAWN)));
            if (condition.getMinWarningCnt() != null) predicates.add(cb.ge(cb.coalesce(user.<Integer>get("warningCnt"), 0), condition.getMinWarningCnt()));
            if (condition.getCreatedFrom() != null) predicates.add(cb.greaterThanOrEqualTo(user.get("createdAt"), condition.getCreatedFrom().atStartOfDay()));
            if (condition.getCreatedTo() != null) predicates.add(cb.lessThan(user.get("createdAt"), condition.getCreatedTo().plusDays(1).atStartOfDay()));
            if (StringUtils.hasText(condition.getIp())) {
                Subquery<Long> exists = query.subquery(Long.class);
                Root<UserLoginHistory> history = exists.from(UserLoginHistory.class);
                exists.select(cb.literal(1L)).where(cb.equal(history.get("user"), user), cb.equal(history.get("ipAddress"), condition.getIp().trim()));
                predicates.add(cb.exists(exists));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return userRepository.findAll(filter, pageable).map(MemberListResponseDto::from);
    }

    private UserStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) return null;
        try { return UserStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new BusinessException(ErrorCode.INVALID_PARAMETER); }
    }

    /**
     * 회원 상세 조회 - 기본정보
     * @param id
     * @return
     */
    public MemberBasicInfoResponseDto getMemberDetailBasic(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return MemberBasicInfoResponseDto.from(user);
    }

    /**
     * 회원 상세 조회 - 보안정보
     * @param userId
     * @return
     */
    public MemberSecurityInfoResponseDto getMemberSecurityInfo(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        List<UserLoginHistory> histories = userLoginHistoryRepository
                .findTop10ByUserIdOrderByLoginAtDesc(userId);

        return MemberSecurityInfoResponseDto.from(histories);
    }
}
