package com.scorenow.scorenow_api.domain.notice;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notice_edit_lock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeLock {
    @Id
    private Long id;

    NoticeLock(long id) { this.id = id; }
}
