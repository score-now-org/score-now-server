package com.scorenow.scorenow_api.domain.match.service;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchStadiumServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @InjectMocks
    private MatchStadiumService matchStadiumService;

    @Test
    void 활성_경기장을_경기에_배정한다() {
        Long matchId = 1L;
        Long stadiumId = 10L;
        Match match = Match.builder().id(matchId).build();
        Stadium stadium = Stadium.builder()
                .id(stadiumId)
                .sportId(1L)
                .name("서울월드컵경기장")
                .city("서울")
                .dataOrigin(MANUAL)
                .build();
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(stadiumRepository.findActiveById(stadiumId)).willReturn(Optional.of(stadium));

        matchStadiumService.assignStadiumToMatch(matchId, stadiumId);

        assertThat(match.getStadiumId()).isEqualTo(stadiumId);
    }

    @Test
    void 비활성_경기장은_신규로_배정할_수_없고_기존_경기장도_유지된다() {
        Long matchId = 1L;
        Long currentStadiumId = 10L;
        Long inactiveStadiumId = 20L;
        Match match = Match.builder()
                .id(matchId)
                .stadiumId(currentStadiumId)
                .build();
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(stadiumRepository.findActiveById(inactiveStadiumId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> matchStadiumService.assignStadiumToMatch(matchId, inactiveStadiumId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STADIUM_NOT_FOUND));
        assertThat(match.getStadiumId()).isEqualTo(currentStadiumId);
    }
}
