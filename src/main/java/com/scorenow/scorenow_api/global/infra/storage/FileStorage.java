package com.scorenow.scorenow_api.global.infra.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String uploadFile(MultipartFile file);
}
