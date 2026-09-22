package com.scorenow.scorenow_api.domain.community.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentSort;
import com.scorenow.scorenow_api.global.exception.BusinessException;

class CommunityCommentCursorTest {

	@Test
	void parsesRecommendedCursorWithItsSortKey() {
		CommunityCommentCursor cursor = CommunityCommentCursor.parse(encoded("RECOMMENDED:12:34"),
			CommunityCommentSort.RECOMMENDED);

		assertThat(cursor).isEqualTo(new CommunityCommentCursor(CommunityCommentSort.RECOMMENDED, 12, 34));
	}

	@Test
	void rejectsCursorUsedWithAnotherSort() {
		assertThatThrownBy(() -> CommunityCommentCursor.parse(encoded("LATEST:0:34"),
			CommunityCommentSort.RECOMMENDED))
			.isInstanceOf(BusinessException.class);
	}

	private String encoded(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}
}
