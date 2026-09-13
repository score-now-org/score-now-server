package com.scorenow.scorenow_api.global.infra.storage.gcs;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class GcsService {

    private final Storage storage;

    @Value("${gcs.bucket}")
    private String bucketName;

    public String upload(MultipartFile file, String objectName) {

        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                    .setContentType(file.getContentType())
                    .setCacheControl("no-cache, max-age=0")
                    .build();

            storage.create(
                    blobInfo,
                    file.getBytes()
            );

            return String.format(
                    "https://storage.googleapis.com/%s/%s",
                    bucketName,
                    objectName
            );

        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패", e);
        }
    }

    public void delete(String objectName) {

        boolean deleted = storage.delete(bucketName, objectName);

        if (!deleted) {
            throw new RuntimeException("삭제할 이미지가 존재하지 않습니다.");
        }
    }
}
