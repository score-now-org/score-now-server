package com.scorenow.scorenow_api.domain.admin.forbiddenword;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.scorenow.scorenow_api.global.exception.BusinessException;

class ForbiddenWordFilterTest {
    @Test void filtersBothFieldsFromOneRuleSnapshotAndRejectsOversizedInput() {
        ForbiddenWordRepository repository = mock(ForbiddenWordRepository.class);
        ForbiddenWordFilter filter = new ForbiddenWordFilter(repository);
        when(repository.findAllForFiltering()).thenReturn(List.of(new ForbiddenWord("bad", "???")));
        var result = filter.applyPair("BaD title", 100, "BAD content", 5000);
        assertThat(result.first()).isEqualTo("??? title");
        assertThat(result.second()).isEqualTo("??? content");
        verify(repository, times(1)).findAllForFiltering();
        assertThatThrownBy(() -> filter.apply("bad too long", 3)).isInstanceOf(BusinessException.class);
        verifyNoMoreInteractions(repository);
    }
}
