package com.scorenow.scorenow_api.domain.player.service;

import static com.scorenow.scorenow_api.domain.player.entity.PlayerAvailabilityStatus.AVAILABLE;
import static com.scorenow.scorenow_api.domain.player.entity.PlayerAvailabilityStatus.INJURED;
import static com.scorenow.scorenow_api.domain.player.entity.PlayerAvailabilityStatus.REGISTERED;
import static com.scorenow.scorenow_api.domain.team.entity.TeamType.CLUB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.player.dto.request.PlayerAvailabilitySearchCondition;
import com.scorenow.scorenow_api.domain.player.dto.response.AdminPlayerAvailabilityResponse;
import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.player.repository.PlayerTeamDetailRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.global.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminPlayerAvailabilityServiceTest {

    @Mock
    private PlayerTeamDetailRepository playerTeamDetailRepository;

    @InjectMocks
    private AdminPlayerAvailabilityService adminPlayerAvailabilityService;

    @Test
    void 검색_조건이_없으면_전체_선수_가용_상태를_페이징_조회한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        PlayerTeamDetail detail = createPlayerTeamDetail();

        given(playerTeamDetailRepository.searchPlayers(null, null, null, null, pageable))
                .willReturn(new PageImpl<>(List.of(detail), pageable, 1));

        Page<AdminPlayerAvailabilityResponse> result =
                adminPlayerAvailabilityService.searchPlayers(new PlayerAvailabilitySearchCondition(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPlayerTeamDetailId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getPlayerId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getPlayerKName()).isEqualTo("손흥민");
        assertThat(result.getContent().get(0).getTeamId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getTeamKName()).isEqualTo("LAFC");
        assertThat(result.getContent().get(0).getAvailabilityStatus()).isEqualTo(AVAILABLE);
        then(playerTeamDetailRepository).should().searchPlayers(isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void 검색_조건의_문자열을_trim_해서_조회한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        PlayerAvailabilitySearchCondition condition = new PlayerAvailabilitySearchCondition();
        condition.setPlayerId(1L);
        condition.setPlayerName(" 손흥민 ");
        condition.setTeamId(10L);
        condition.setTeamName(" LAFC ");

        given(playerTeamDetailRepository.searchPlayers(1L, "손흥민", 10L, "LAFC", pageable))
                .willReturn(Page.empty(pageable));

        adminPlayerAvailabilityService.searchPlayers(condition, pageable);

        then(playerTeamDetailRepository).should()
                .searchPlayers(1L, "손흥민", 10L, "LAFC", pageable);
    }

    @Test
    void 빈_검색어는_null로_변환해서_조회한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        PlayerAvailabilitySearchCondition condition = new PlayerAvailabilitySearchCondition();
        condition.setPlayerName("   ");
        condition.setTeamName("");

        given(playerTeamDetailRepository.searchPlayers(null, null, null, null, pageable))
                .willReturn(Page.empty(pageable));

        adminPlayerAvailabilityService.searchPlayers(condition, pageable);

        then(playerTeamDetailRepository).should().searchPlayers(isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void 선수_가용_상태를_수정한다() {
        Long playerTeamDetailId = 100L;
        PlayerTeamDetail detail = createPlayerTeamDetail();
        given(playerTeamDetailRepository.findById(playerTeamDetailId)).willReturn(Optional.of(detail));

        adminPlayerAvailabilityService.updatePlayerAvailabilityStatus(playerTeamDetailId, INJURED);

        assertThat(detail.getAvailabilityStatus()).isEqualTo(INJURED);
    }

    @Test
    void 존재하지_않는_선수팀상세_ID면_예외가_발생한다() {
        Long invalidPlayerTeamDetailId = 999L;
        given(playerTeamDetailRepository.findById(invalidPlayerTeamDetailId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                adminPlayerAvailabilityService.updatePlayerAvailabilityStatus(invalidPlayerTeamDetailId, REGISTERED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void null_상태로_수정하면_AVAILABLE로_변경된다() {
        Long playerTeamDetailId = 100L;
        PlayerTeamDetail detail = createPlayerTeamDetail();
        detail.updateAvailabilityStatus(INJURED);
        given(playerTeamDetailRepository.findById(playerTeamDetailId)).willReturn(Optional.of(detail));

        adminPlayerAvailabilityService.updatePlayerAvailabilityStatus(playerTeamDetailId, null);

        assertThat(detail.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    private PlayerTeamDetail createPlayerTeamDetail() {
        Player player = Player.builder()
                .id(1L)
                .kName("손흥민")
                .eName("Son Heung-min")
                .build();

        Team team = Team.builder()
                .id(10L)
                .type(CLUB)
                .kName("LAFC")
                .eName("Los Angeles FC")
                .build();

        League league = League.builder()
                .id(20L)
                .kName("MLS")
                .eName("Major League Soccer")
                .build();

        return PlayerTeamDetail.builder()
                .id(100L)
                .player(player)
                .team(team)
                .league(league)
                .position("F")
                .shirtNumber("7")
                .squadOn(true)
                .availabilityStatus(AVAILABLE)
                .build();
    }

}
