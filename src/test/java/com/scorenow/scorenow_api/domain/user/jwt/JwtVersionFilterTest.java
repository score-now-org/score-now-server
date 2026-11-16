package com.scorenow.scorenow_api.domain.user.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;

class JwtVersionFilterTest {
    private final JwtProvider jwt = new JwtProvider("12345678901234567890123456789012");
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users);
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void previousTokenIsRejectedAfterAccountRestoration() throws Exception {
        User user = User.builder().status(UserStatus.ACTIVE).build();
        when(users.findById(5L)).thenReturn(Optional.of(user));
        String token = jwt.createAccessToken(5L, user.currentTokenVersion());
        user.revokeTokens();
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        var newRequest = new MockHttpServletRequest();
        newRequest.addHeader("Authorization", "Bearer " + jwt.createAccessToken(5L, user.currentTokenVersion()));
        filter.doFilter(newRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(5L);
    }
}
