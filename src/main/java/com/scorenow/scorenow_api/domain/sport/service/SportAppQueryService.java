package com.scorenow.scorenow_api.domain.sport.service;

import com.scorenow.scorenow_api.domain.match.dto.response.SportAppItemResponse;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SportAppQueryService {

    private final SportRepository sportRepository;

    public List<SportAppItemResponse> getSupportedSports() {
        return sportRepository.findAllByOrderByIdAsc().stream()
                .map(SportAppItemResponse::from)
                .toList();
    }
}
