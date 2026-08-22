package com.scorenow.scorenow_api.domain.team.service;

import static com.scorenow.scorenow_api.domain.team.entity.TeamType.CLUB;
import static com.scorenow.scorenow_api.domain.team.entity.TeamType.NATIONAL;
import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.BETS;
import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
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
import org.springframework.data.domain.Sort;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SportRepository sportRepository;

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
        assertThat(team.getCc()).isEqualTo("NEW CC");
        assertThat(team.getImageUrl()).isEqualTo("old.png");
    }

    @Test
    void 수정할_팀명은_공백일_수_없다() {
        Long teamId = 1L;
        Team team = Team.builder().id(teamId).kName("기존팀").build();
        AdminTeamUpdateRequest request = new AdminTeamUpdateRequest();
        request.setKName("   ");
        given(teamRepository.findByIdAndIsActiveTrue(teamId)).willReturn(Optional.of(team));

        assertThatThrownBy(() -> adminTeamService.updateTeam(teamId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("한글 팀명은(는) 비워둘 수 없습니다.");
    }

    @Test
    void 팀을_삭제한다() {
        Long teamId = 1L;
        Team team = Team.builder()
                .id(teamId)
                .type(CLUB)
                .kName("서울")
                .dataOrigin(MANUAL)
                .build();

        given(teamRepository.findByIdAndIsActiveTrue(teamId)).willReturn(Optional.of(team));

        adminTeamService.deleteTeam(teamId);

        assertThat(team.isActive()).isFalse();
    }

    @Test
    void 외부_연동_팀은_삭제할_수_없다() {
        Long teamId = 1L;
        Team team = Team.builder()
                .id(teamId)
                .type(CLUB)
                .kName("서울")
                .dataOrigin(BETS)
                .build();

        given(teamRepository.findByIdAndIsActiveTrue(teamId)).willReturn(Optional.of(team));

        assertThatThrownBy(() -> adminTeamService.deleteTeam(teamId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("수동 등록된 팀만 삭제할 수 있습니다.");

        assertThat(team.isActive()).isTrue();
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
                .dataOrigin(BETS)
                .build();

        given(teamRepository.searchTeams(null, null, pageable))
                .willReturn(new PageImpl<>(List.of(team), pageable, 1));

        Page<AdminTeamResponse> result = adminTeamService.searchTeams(new TeamSearchCondition(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getKName()).isEqualTo("서울");
        assertThat(result.getContent().get(0).getEName()).isEqualTo("Seoul");
        assertThat(result.getContent().get(0).getDataOrigin()).isEqualTo(BETS);
        then(teamRepository).should().searchTeams(isNull(), isNull(), eq(pageable));
    }

    @Test
    void 팀_관리_화면의_팀_타입과_종목_옵션을_조회한다() {
        Sport football = Sport.builder()
                .id(1L)
                .sportCode(SportCode.FOOTBALL)
                .kName("축구")
                .build();
        given(sportRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))).willReturn(List.of(football));

        var result = adminTeamService.getSearchOptions();

        assertThat(result.getTeamTypes()).extracting("code").containsExactly("CLUB", "NATIONAL");
        assertThat(result.getSports()).hasSize(1);
        assertThat(result.getSports().get(0).getCode()).isEqualTo("FOOTBALL");
        assertThat(result.getSports().get(0).getName()).isEqualTo("축구");
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

        given(teamRepository.searchTeams(null, "manchester", pageable))
                .willReturn(new PageImpl<>(List.of(manchesterUnited, manchesterCity), pageable, 2));

        TeamSearchCondition condition = new TeamSearchCondition();
        condition.setKeyword("  manchester  ");

        Page<AdminTeamResponse> result = adminTeamService.searchTeams(condition, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.stream().allMatch(res -> res.getKName().contains("맨체스터"))).isTrue();
        then(teamRepository).should().searchTeams(null, "manchester", pageable);
    }

}
