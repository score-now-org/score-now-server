package com.scorenow.scorenow_api.global.infra.storage.gcs;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcsService implements FileStorage {

    private final Storage storage;

    @Value("${gcs.bucket}")
    private String bucketName;

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String objectName = uploadFileAndReturnKey(file, "");

        return getFileUrl(objectName);
    }

    @Override
    public String uploadFileAndReturnKey(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String objectName = buildObjectName(file, directory);

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(file.getContentType())
                .setCacheControl("no-cache, max-age=0")
                .build();

        uploadFileToGcs(file, blobInfo);

        return objectName;
    }

    @Override
    public String getFileUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }

        return String.format(
                "https://storage.googleapis.com/%s/%s",
                bucketName,
                objectName
        );
    }

    @Override
    public void deleteFile(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return;
        }

        try {
            boolean deleted = storage.delete(bucketName, objectName);
            if (!deleted) {
                log.warn("GCS에서 삭제할 파일을 찾을 수 없습니다. objectName: {}", objectName);
            }

        } catch (StorageException e) {
            log.error("GCS 파일 삭제 실패. objectName: {}", objectName, e);

            throw new BusinessException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    @Override
    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        String objectName = extractObjectName(fileUrl);

        deleteFile(objectName);
    }

    private String buildObjectName(MultipartFile file, String directory) {
        String normalizedDirectory = normalizeDirectory(directory);

        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("");

        String extension = extractExtension(originalFilename);

        return normalizedDirectory + UUID.randomUUID() + extension;
    }

    private String normalizeDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return "";
        }

        String normalized = directory.trim().replace("\\", "/");

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (!normalized.endsWith("/")) {
            normalized += "/";
        }

        return normalized;
    }

    private String extractExtension(String filename) {
        int index = filename.lastIndexOf('.');

        if (index < 0 || index == filename.length() - 1) {
            return "";
        }

        return filename.substring(index);
    }

    private String extractObjectName(String fileUrl) {
        String prefix = String.format(
                "https://storage.googleapis.com/%s/",
                bucketName
        );

        if (!fileUrl.startsWith(prefix)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        String objectName = fileUrl.substring(prefix.length());

        if (objectName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        return objectName;
    }

    private void uploadFileToGcs(MultipartFile file, BlobInfo blobInfo) {
        try (InputStream inputStream = file.getInputStream()) {
            storage.createFrom(blobInfo, inputStream);
        } catch (IOException e) {
            log.error("파일 스트림 읽기 실패. 파일명: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (StorageException e) {
            log.error("GCS 파일 업로드 실패. 파일명: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

}