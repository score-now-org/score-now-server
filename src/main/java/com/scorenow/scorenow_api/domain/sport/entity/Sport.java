package com.scorenow.scorenow_api.domain.sport.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "sports")
@Getter @Setter
@NoArgsConstructor
public class Sport extends BaseEntity{
    @Id
    private String id;
    private String kName;
    private String eName;

}