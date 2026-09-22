package com.scorenow.scorenow_api.domain.community.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentSort;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

public record CommunityCommentCursor(CommunityCommentSort sort, long likeCount, long commentId) {

	public static CommunityCommentCursor parse(String encodedCursor, CommunityCommentSort requestedSort) {
		if (encodedCursor == null) {
			return null;
		}

		try {
			String[] values = new String(Base64.getUrlDecoder().decode(encodedCursor), StandardCharsets.UTF_8)
				.split(":", -1);
			if (values.length != 3) {
				throw new IllegalArgumentException();
			}

			CommunityCommentSort cursorSort = CommunityCommentSort.valueOf(values[0]);
			long likeCount = Long.parseLong(values[1]);
			long commentId = Long.parseLong(values[2]);

			if (cursorSort != requestedSort || likeCount < 0 || commentId <= 0) {
				throw new IllegalArgumentException();
			}

			return new CommunityCommentCursor(cursorSort, likeCount, commentId);
		} catch (IllegalArgumentException | ArrayIndexOutOfBoundsException exception) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER);
		}
	}

	public static String of(CommunityCommentSort sort, CommunityComment comment) {
		String value = sort.name() + ":" + comment.getLikeCount() + ":" + comment.getId();
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}
}
