package com.scorenow.scorenow_api.domain.match.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.mapper.MatchMapper;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.AdminMatchService;

@ExtendWith(MockitoExtension.class)
class AdminMatchControllerTest {

    @Mock
    private MatchRepository matchRepository;
    @Spy
    private MatchMapper matchMapper = new MatchMapper();
    @InjectMocks
    private AdminMatchService adminMatchService;

    @Test
    void 경기_목록에_외부_식별자와_기존_페이지_정보를_반환한다() throws Exception {
        PageRequest pageRequest = PageRequest.of(1, 4, Sort.by(Sort.Direction.DESC, "startAt"));
        List<Match> matches = List.of(
                match(103L, DataOrigin.BETS, "00123456", false),
                match(102L, DataOrigin.BETS, "evt-7", true),
                match(101L, DataOrigin.MANUAL, null, true),
                match(100L, null, null, false));
        given(matchRepository.searchMatches(any(MatchSearchCondition.class), eq(pageRequest)))
                .willReturn(new PageImpl<>(matches, pageRequest, 10));

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json().serializationInclusion(JsonInclude.Include.NON_NULL).build());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminMatchController(adminMatchService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(jsonConverter)
                .build();

        mvc.perform(get("/api/v1/admin/matches")
                        .param("leagueId", "12")
                        .param("status", "NOT_STARTED")
                        .param("sortBy", "date")
                        .param("sortDirection", "DESC")
                        .param("page", "1")
                        .param("size", "4")
                        .param("sort", "startAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id", contains(103, 102, 101, 100)))
                .andExpect(jsonPath("$.data.content[0].dataOrigin").value("BETS"))
                .andExpect(jsonPath("$.data.content[0].apiMatchId").isString())
                .andExpect(jsonPath("$.data.content[0].apiMatchId").value("00123456"))
                .andExpect(jsonPath("$.data.content[1].dataOrigin").value("BETS"))
                .andExpect(jsonPath("$.data.content[1].apiMatchId").value("evt-7"))
                .andExpect(jsonPath("$.data.content[2].dataOrigin").value("MANUAL"))
                .andExpect(jsonPath("$.data.content[2].apiMatchId").value(nullValue()))
                .andExpect(jsonPath("$.data.content[3].dataOrigin").value(nullValue()))
                .andExpect(jsonPath("$.data.content[3].apiMatchId").value(nullValue()))
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.size").value(4))
                .andExpect(jsonPath("$.data.totalElements").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        ArgumentCaptor<MatchSearchCondition> condition = ArgumentCaptor.forClass(MatchSearchCondition.class);
        then(matchRepository).should().searchMatches(condition.capture(), eq(pageRequest));
        assertThat(condition.getValue().getLeagueId()).isEqualTo(12L);
        assertThat(condition.getValue().getStatus()).isEqualTo("NOT_STARTED");
        assertThat(condition.getValue().getSortBy()).isEqualTo("date");
        assertThat(condition.getValue().getSortDirection()).isEqualTo("DESC");
    }

    private Match match(Long id, DataOrigin dataOrigin, String apiMatchId, boolean manual) {
        return Match.builder()
                .id(id)
                .dataOrigin(dataOrigin)
                .apiMatchId(apiMatchId)
                .isManual(manual)
                .statusCode(MatchStatus.NOT_STARTED)
                .build();
    }
}
