package com.scorenow.scorenow_api.global.infra.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String uploadFile(MultipartFile file);

    default String uploadFileAndReturnKey(MultipartFile file, String directory) {
        return uploadFile(file);
    }

    default String getFileUrl(String key) {
        return key;
    }
}
