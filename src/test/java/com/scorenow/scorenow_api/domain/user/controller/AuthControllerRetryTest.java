package com.scorenow.scorenow_api.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.scorenow.scorenow_api.domain.user.dto.SocialLoginRequestDto;
import com.scorenow.scorenow_api.domain.user.jwt.TokenService;
import com.scorenow.scorenow_api.domain.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;

class AuthControllerRetryTest {
    @Test void preservesTheOriginalErrorWhenNoMatchingIdentityExists() {
        var auth = mock(AuthService.class);
        var controller = new AuthController(auth, mock(TokenService.class));
        var request = new SocialLoginRequestDto();
        var http = new MockHttpServletRequest();
        var failure = new DataIntegrityViolationException("other duplicate key");
        when(auth.socialLogin(request, http)).thenThrow(new AuthService.ConcurrentSocialRegistration(failure));
        when(auth.retryExistingSocialLogin(request, http)).thenThrow(new AuthService.SocialIdentityAbsent());
        assertThatThrownBy(() -> controller.socialLogin(request, http)).isSameAs(failure);
    }
}
