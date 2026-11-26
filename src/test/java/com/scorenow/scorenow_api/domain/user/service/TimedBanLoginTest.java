package com.scorenow.scorenow_api.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import com.scorenow.scorenow_api.domain.user.dto.SocialLoginRequestDto;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.NicknameGenerator;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.jwt.TokenPair;
import com.scorenow.scorenow_api.domain.user.jwt.TokenService;
import com.scorenow.scorenow_api.domain.user.repository.UserLoginHistoryRepository;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.external.social.SocialLoginVerifier;
import com.scorenow.scorenow_api.global.exception.BusinessException;

class TimedBanLoginTest {
    @Test void loginReleasesOnlyAnExpiredTimedBanWhenSchedulerHasNotRun() {
        var users = mock(UserRepository.class);
        var verifier = mock(SocialLoginVerifier.class);
        var tokens = mock(TokenService.class);
        var service = new AuthService(users, mock(GeoService.class), tokens, verifier,
                mock(UserReferenceService.class), mock(NicknameGenerator.class), mock(UserLoginHistoryRepository.class));
        var request = new SocialLoginRequestDto();
        var identity = new SocialLoginVerifier.VerifiedUser("google", "id", "name");
        when(verifier.verify(request)).thenReturn(identity);
        when(tokens.createToken(anyLong())).thenReturn(new TokenPair("access", "refresh"));
        User user = User.builder().id(1L).provider("google").socialId("id").status(UserStatus.BANNED)
                .banExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1)).build();
        when(users.findByProviderAndSocialId("google", "id")).thenReturn(Optional.of(user));
        service.socialLogin(request, new MockHttpServletRequest());
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getBanExpiresAt()).isNull();
        user.setStatus(UserStatus.BANNED);
        user.setBanExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1));
        assertThatThrownBy(() -> service.socialLogin(request, new MockHttpServletRequest())).isInstanceOf(BusinessException.class);
        user.setBanExpiresAt(null);
        assertThatThrownBy(() -> service.socialLogin(request, new MockHttpServletRequest())).isInstanceOf(BusinessException.class);
    }
}
