package com.scorenow.scorenow_api.domain.push.controller;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.push.dto.AdminPushHistoryResponse;
import com.scorenow.scorenow_api.domain.push.dto.AdminPushSendRequest;
import com.scorenow.scorenow_api.domain.push.enums.PushPlatform;
import com.scorenow.scorenow_api.domain.push.service.AdminPushService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/pushes")
@ConditionalOnProperty(
	name = "firebase.enabled",
	havingValue = "true"
)
public class AdminPushController {

	private final AdminPushService adminPushService;

	@PostMapping("/common")
	public ApiResponse<List<String>> sendCommon(
		@Valid @RequestBody AdminPushSendRequest request
	) {
		List<String> messageIds =
			adminPushService.sendCommon(request);

		return ApiResponse.success(messageIds);
	}

	@PostMapping("/{platform}")
	public ApiResponse<String> send(
		@PathVariable PushPlatform platform,
		@Valid @RequestBody AdminPushSendRequest request
	) {
		String messageId =
			adminPushService.send(request, platform);

		return ApiResponse.success(messageId);
	}

	@GetMapping("/histories")
	public ApiResponse<Page<AdminPushHistoryResponse>> getHistories(
		@RequestParam PushPlatform platform,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(
			adminPushService.getHistories(
				platform,
				pageable
			)
		);
	}
}