package com.scorenow.scorenow_api.domain.admin.member.service;

import com.scorenow.scorenow_api.domain.admin.member.dto.MemberBasicInfoResponseDto;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberListResponseDto;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberSecurityInfoResponseDto;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.entity.UserLoginHistory;
import com.scorenow.scorenow_api.domain.user.repository.UserLoginHistoryRepository;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<MemberListResponseDto> getMembers(Pageable pageable) {

        return userRepository.findAll(pageable)
                .map(MemberListResponseDto::from);
    }

    /**
     * 회원 상세 조회 - 기본정보
     * @param id
     * @return
     */
    public MemberBasicInfoResponseDto getMemberDetailBasic(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));

        return MemberBasicInfoResponseDto.from(user);
    }

    /**
     * 회원 상세 조회 - 보안정보
     * @param userId
     * @return
     */
    public MemberSecurityInfoResponseDto getMemberSecurityInfo(Long userId) {
        List<UserLoginHistory> histories = userLoginHistoryRepository
                .findTop10ByUserIdOrderByLoginAtDesc(userId);

        return MemberSecurityInfoResponseDto.from(histories);
    }
}
