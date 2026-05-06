package com.scorenow.scorenow_api.domain.sport.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sports")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sport extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String kName;
    private String eName;
}