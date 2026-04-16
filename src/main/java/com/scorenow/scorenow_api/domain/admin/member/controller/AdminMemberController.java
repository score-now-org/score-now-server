package com.scorenow.scorenow_api.domain.admin.member.controller;

import com.scorenow.scorenow_api.domain.admin.member.dto.MemberListResponseDto;
import com.scorenow.scorenow_api.domain.admin.member.dto.MemberSecurityInfoResponseDto;
import com.scorenow.scorenow_api.domain.admin.member.service.AdminMemberService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin - Member", description = "관리자 회원 관리 API")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    /**
     * 회원 목록 전체 조회 (페이징)
     */
    @Operation(summary = "회원 목록 조회", description = "전체 회원 목록을 페이징으로 조회합니다.")
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
    @Operation(summary = "회원 기본정보 조회", description = "회원 ID로 기본 상세정보를 조회합니다.")
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
    @Operation(summary = "회원 보안정보 조회", description = "회원 ID로 보안 상세정보를 조회합니다.")
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
