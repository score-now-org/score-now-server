package com.scorenow.scorenow_api.domain.push.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.push.dto.AdminPushSendRequest;
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

	@PostMapping
	public ApiResponse<String> send(@Valid @RequestBody AdminPushSendRequest request) {
		String messageId = adminPushService.send(request);

		return ApiResponse.success(messageId);
	}
}
