package com.scorenow.scorenow_api.domain.stadium.service;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;
import static com.scorenow.scorenow_api.domain.sport.model.SportCode.FOOTBALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumUpdateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.StadiumSearchCondition;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.repository.FakeStadiumRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class AdminStadiumServiceTest {

    private FakeStadiumRepository stadiumRepository;
    private SportRepository sportRepository;
    private AdminStadiumService adminStadiumService;

    @BeforeEach
    void setUp() {
        stadiumRepository = new FakeStadiumRepository();
        sportRepository = mock(SportRepository.class);
        adminStadiumService = new AdminStadiumService(stadiumRepository, sportRepository);
    }

    @Test
    void 수동_등록_경기장을_삭제하면_비활성화된다() {
        Stadium stadium = saveManualStadium();

        adminStadiumService.deleteStadium(stadium.getId());

        assertThat(stadium.isActive()).isFalse();
        assertThat(stadiumRepository.findById(stadium.getId())).containsSame(stadium);
    }

    @Test
    void 존재하지_않는_경기장을_삭제하면_예외가_발생한다() {
        assertThatThrownBy(() -> adminStadiumService.deleteStadium(999L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STADIUM_NOT_FOUND));
    }

    @Test
    void 이미_삭제된_경기장을_다시_삭제하면_예외가_발생한다() {
        Stadium stadium = saveManualStadium();
        adminStadiumService.deleteStadium(stadium.getId());

        assertThatThrownBy(() -> adminStadiumService.deleteStadium(stadium.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STADIUM_NOT_FOUND));
    }

    @Test
    void 비활성_경기장은_수정할_수_없다() {
        Stadium stadium = saveManualStadium();
        adminStadiumService.deleteStadium(stadium.getId());
        AdminStadiumUpdateRequest request = new AdminStadiumUpdateRequest();
        request.setName("변경된 경기장명");

        assertThatThrownBy(() -> adminStadiumService.updateStadium(stadium.getId(), request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STADIUM_NOT_FOUND));
        assertThat(stadium.getName()).isEqualTo("서울월드컵경기장");
    }

    @Test
    void 경기장_조회시_종목_ID_코드_이름을_반환한다() {
        saveManualStadium();
        Sport football = Sport.builder()
                .id(1L)
                .sportCode(FOOTBALL)
                .kName("축구")
                .build();
        given(sportRepository.findAllById(Set.of(1L))).willReturn(List.of(football));

        var result = adminStadiumService.searchStadiums(
                new StadiumSearchCondition(),
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).singleElement().satisfies(stadium -> {
            assertThat(stadium.getSportId()).isEqualTo(1L);
            assertThat(stadium.getSportCode()).isEqualTo(FOOTBALL);
            assertThat(stadium.getSportName()).isEqualTo("축구");
        });
    }

    private Stadium saveManualStadium() {
        return stadiumRepository.save(Stadium.of("서울월드컵경기장", 1L, "서울", MANUAL));
    }
}
