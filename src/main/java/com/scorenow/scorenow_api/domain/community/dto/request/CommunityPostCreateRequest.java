package com.scorenow.scorenow_api.domain.community.dto.request;

import java.util.ArrayList;
import java.util.List;

import com.scorenow.scorenow_api.domain.community.entity.CommunityCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommunityPostCreateRequest {

	@NotNull(message = "카테고리는 필수입니다.")
	private CommunityCategory category;

	@NotBlank(message = "제목은 필수입니다.")
	@Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
	private String title;

	@NotBlank(message = "내용은 필수입니다.")
	@Size(max = 5000, message = "내용은 5000자를 초과할 수 없습니다.")
	private String content;

	@Size(max = 10, message = "이미지는 최대 10개까지 첨부할 수 있습니다.")
	private List<@NotBlank(message = "이미지 키가 비어있습니다.") String> imageKeys = new ArrayList<>();
}
