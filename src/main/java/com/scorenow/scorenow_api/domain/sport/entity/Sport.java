package com.scorenow.scorenow_api.domain.sport.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "sports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Sport extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String kName;
    private String eName;

    public String resolveSportName() {
        if (StringUtils.hasText(kName)) {
            return kName;
        }
        if (StringUtils.hasText(eName)) {
            return eName;
        }
        return "";
    }
}
