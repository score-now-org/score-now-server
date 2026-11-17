package com.scorenow.scorenow_api.domain.terms;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity @Table(name = "terms_edit_lock") @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsLock {
    @Id private Long id;
    TermsLock(long id) { this.id = id; }
}
