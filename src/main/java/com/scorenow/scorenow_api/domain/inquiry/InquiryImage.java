package com.scorenow.scorenow_api.domain.inquiry;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "inquiry_images") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryImage {
    @Id @Column(name = "inquiry_id") private Long inquiryId;
    @Column(nullable = false, length = 20) private String contentType;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMBLOB") private byte[] data;
    public InquiryImage(Long inquiryId, String contentType, byte[] data) {
        this.inquiryId = inquiryId; this.contentType = contentType; this.data = data;
    }
}
