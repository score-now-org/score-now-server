package com.scorenow.scorenow_api.domain.odds.dto.cursor;

import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;

/**
 * 배당률 이력을 {@code publishedAt DESC, id DESC} 순서로 이어서 조회하기 위한 커서다.
 * 카테고리·마켓·등록 방식으로 요청 문맥을 검증하고, 마지막 이력의 등록 시각과 ID를 다음 조회 경계로 사용한다.
 * 클라이언트에는 내부 값을 직접 노출하지 않도록 버전이 포함된 문자열을 URL-safe Base64로 인코딩해 전달한다.
 * 인코딩된 커서가 없으면 최초 페이지로 판단하며, 형식이나 요청 문맥이 다르면 잘못된 요청으로 처리한다.
 */
public record MatchOddsHistoryCursor(
        OddsCategory category,
        OddsMarketType marketType,
        OddsRegistrationType registrationType,
        Instant publishedAt,
        Long historyId
) {
    private static final String VERSION = "v1";
    private static final String DELIMITER = "|";
    private static final int FIELD_COUNT = 6;

    public static MatchOddsHistoryCursor parse(
            String encodedCursor,
            OddsCategory requestedCategory,
            OddsMarketType requestedMarketType,
            OddsRegistrationType requestedRegistrationType
    ) {
        if (encodedCursor == null) {
            return null;
        }

        try {
            String decodedCursor = new String(Base64.getUrlDecoder().decode(encodedCursor), StandardCharsets.UTF_8);
            String[] values = decodedCursor.split("\\|", -1);

            if (values.length != FIELD_COUNT || !VERSION.equals(values[0])) {
                throw new IllegalArgumentException();
            }

            OddsCategory category = OddsCategory.valueOf(values[1]);
            OddsMarketType marketType = OddsMarketType.valueOf(values[2]);
            OddsRegistrationType registrationType = OddsRegistrationType.valueOf(values[3]);
            Instant publishedAt = Instant.parse(values[4]);
            long historyId = Long.parseLong(values[5]);

            if (category != requestedCategory
                    || marketType != requestedMarketType
                    || registrationType != requestedRegistrationType
                    || historyId <= 0) {
                throw new IllegalArgumentException();
            }

            return new MatchOddsHistoryCursor(
                    category,
                    marketType,
                    registrationType,
                    publishedAt,
                    historyId
            );
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
    }

    public String encode() {
        String value = String.join(
                DELIMITER,
                VERSION,
                category.name(),
                marketType.name(),
                registrationType.name(),
                publishedAt.toString(),
                historyId.toString()
        );

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
