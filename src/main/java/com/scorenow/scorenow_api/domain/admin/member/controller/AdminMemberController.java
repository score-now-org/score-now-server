package com.scorenow.scorenow_api.domain.admin.member.controller;

import com.scorenow.scorenow_api.domain.admin.member.dto.MemberListResponseDto;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberSecurityInfoResponseDto;
import com.scorenow.scorenow_api.domain.admin.member.service.AdminMemberService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    /**
     * 회원 목록 전체 조회 (페이징)
     */
    @GetMapping
    public ApiResponse<Page<MemberListResponseDto>> getMembers(
            @PageableDefault(size = 15) Pageable pageable
    ) {
        return ApiResponse.success(adminMemberService.getMembers(pageable));
    }


    /**
     * 회원상세조회 > 기본정보
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ApiResponse<MemberListResponseDto> getUserDetailBasic(
            @PathVariable Long id
    ) {

        MemberListResponseDto user = adminMemberService.getMemberDetailBasic(id);
        return ApiResponse.success(user);
    }

    /**
     * 회원상세조회 > 보안정보
     * @param id
     * @return
     */
    @GetMapping("/{id}/security")
    public ApiResponse<MemberSecurityInfoResponseDto> getMemberDetailSecurity(
            @PathVariable Long id
    ) {

        MemberSecurityInfoResponseDto user = adminMemberService.getMemberSecurityInfo(id);
        return ApiResponse.success(user);
    }

    /*
     * TODO :
     *  /warnings : 경고정보
     *  /activities : 활동정보
     */
}
