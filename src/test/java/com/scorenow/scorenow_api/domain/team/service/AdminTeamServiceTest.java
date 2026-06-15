package com.scorenow.scorenow_api.domain.team.service;

import static com.scorenow.scorenow_api.domain.team.entity.TeamType.CLUB;
import static com.scorenow.scorenow_api.domain.team.entity.TeamType.NATIONAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.team.dto.request.TeamSearchCondition;
import com.scorenow.scorenow_api.domain.team.dto.request.AdminTeamUpdateRequest;
import com.scorenow.scorenow_api.domain.team.dto.response.AdminTeamResponse;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private AdminTeamService adminTeamService;

    @Test
    void 팀_정보를_부분_수정한다() {
        Long teamId = 1L;
        Team team = Team.builder()
                .id(teamId)
                .type(NATIONAL)
                .kName("기존팀")
                .eName("Old Team")
                .sName("OLD")
                .cc("old cc")
                .imageUrl("old.png")
                .build();

        // 업데이트 요청 정보 (이미지는 변경 안함)
        AdminTeamUpdateRequest request = new AdminTeamUpdateRequest();
        request.setTeamType(CLUB);
        request.setKName("변경팀");
        request.setEName("New Team");
        request.setSName("NEW");
        request.setCc("new cc");

        given(teamRepository.findByIdAndIsActiveTrue(teamId)).willReturn(Optional.of(team));

        adminTeamService.updateTeam(teamId, request);

        assertThat(team.getType()).isEqualTo(CLUB);
        assertThat(team.getKName()).isEqualTo("변경팀");
        assertThat(team.getEName()).isEqualTo("New Team");
        assertThat(team.getSName()).isEqualTo("NEW");
        assertThat(team.getCc()).isEqualTo("new cc");
        assertThat(team.getImageUrl()).isEqualTo("old.png");
    }

    @Test
    void 팀을_삭제한다() {
        Long teamId = 1L;
        Team team = Team.builder()
                .id(teamId)
                .type(CLUB)
                .kName("서울")
                .build();

        given(teamRepository.findByIdAndIsActiveTrue(teamId)).willReturn(Optional.of(team));

        adminTeamService.deleteTeam(teamId);

        assertThat(team.isActive()).isFalse();
    }

    @Test
    void 삭제할_팀이_없으면_예외가_발생한다() {
        Long invalidTeamId = 1L;
        given(teamRepository.findByIdAndIsActiveTrue(invalidTeamId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminTeamService.deleteTeam(invalidTeamId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 검색_조건이_없으면_전체_팀을_페이징_조회한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        Team team = Team.builder()
                .id(1L)
                .type(CLUB)
                .kName("서울")
                .eName("Seoul")
                .build();

        given(teamRepository.searchTeams(null, null, null, pageable))
                .willReturn(new PageImpl<>(List.of(team), pageable, 1));

        Page<AdminTeamResponse> result = adminTeamService.searchTeams(new TeamSearchCondition(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getKName()).isEqualTo("서울");
        assertThat(result.getContent().get(0).getEName()).isEqualTo("Seoul");
        then(teamRepository).should().searchTeams(isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void 검색_조건에_맞춰_팀을_페이징_조회한다() {
        PageRequest pageable = PageRequest.of(0, 20);

        Team manchesterUnited = Team.builder()
                .id(1L)
                .type(CLUB)
                .kName("맨체스터 유나이티드")
                .eName("Manchester United")
                .build();

        Team manchesterCity = Team.builder()
                .id(2L)
                .type(CLUB)
                .kName("맨체스터 시티")
                .eName("Manchester City")
                .build();

        // 한국어로 "맨체스터" 라고 검색하는 경우
        given(teamRepository.searchTeams(null, "맨체스터", null, pageable))
                .willReturn(new PageImpl<>(List.of(manchesterUnited, manchesterCity), pageable, 2));

        // 영어로 "manchester" 라고 검색하는 경우
        given(teamRepository.searchTeams(null, null, "manchester", pageable))
                .willReturn(new PageImpl<>(List.of(manchesterUnited, manchesterCity), pageable, 2));


        TeamSearchCondition koreanSearchCondition = new TeamSearchCondition();
        koreanSearchCondition.setKName("맨체스터");

        TeamSearchCondition englishSearchCondition = new TeamSearchCondition();
        englishSearchCondition.setEName("manchester");

        Page<AdminTeamResponse> koreanSearchResult = adminTeamService.searchTeams(koreanSearchCondition, pageable);
        Page<AdminTeamResponse> englishSearchResult = adminTeamService.searchTeams(englishSearchCondition, pageable);

        assertThat(koreanSearchResult.getTotalElements()).isEqualTo(2);
        assertThat(koreanSearchResult.stream().allMatch(res -> res.getKName().contains("맨체스터"))).isTrue();

        assertThat(englishSearchResult.getTotalElements()).isEqualTo(2);
        assertThat(englishSearchResult.stream().allMatch(res -> res.getKName().contains("맨체스터"))).isTrue();
    }

}
