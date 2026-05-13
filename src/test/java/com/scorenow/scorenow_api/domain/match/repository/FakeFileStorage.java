package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public class FakeFileStorage implements FileStorage {
    @Override
    public String uploadFile(MultipartFile file) {
        String imageUrl = UUID.randomUUID().toString().substring(0, 8).replace("-", "");
        System.out.println("이미지 업로드 성공 => 저장 URL 반환: " + imageUrl);
        return imageUrl;
    }
}
