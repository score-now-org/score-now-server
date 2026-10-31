package com.scorenow.scorenow_api.domain.odds.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 외부 데이터 동기화 시 사용하는 용도 (기존의 다른 도메인 처럼, Admin이 prefix 로 안붙어있는 경우 보통 외부 API 동기화하면서 호출되는 도메인 로직을 담는다.)
 */
@Service
@RequiredArgsConstructor
public class MatchOddsService {
}
