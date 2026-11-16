package com.scorenow.scorenow_api.domain.user.jwt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.repository.RefreshTokenRepository;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;

class TokenVersionRefreshTest {
    @Test void previousRefreshTokenIsRejectedEvenAfterRestoration() {
        JwtProvider jwt = new JwtProvider("12345678901234567890123456789012");
        UserReferenceService users = mock(UserReferenceService.class);
        RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
        TokenService service = new TokenService(jwt, tokens, users);
        User user = User.builder().id(5L).status(UserStatus.ACTIVE).build();
        String oldToken = jwt.createRefreshToken(5L, user.currentTokenVersion());
        user.revokeTokens();
        when(users.requireActiveUser(5L)).thenReturn(user);
        assertThatThrownBy(() -> service.reRefreshToken(oldToken)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(tokens);
    }
}
