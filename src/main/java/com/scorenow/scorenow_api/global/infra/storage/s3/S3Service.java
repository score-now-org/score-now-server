package com.scorenow.scorenow_api.global.infra.storage.s3;

import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service implements FileStorage {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            return null;

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        uploadFileToS3(file, putObjectRequest);

        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder().bucket(bucket).key(fileName).build())
                .toString();
    }

    private void uploadFileToS3(MultipartFile file, PutObjectRequest putObjectRequest) {
        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (IOException e) {
            log.error("파일 스트림을 읽는 중 I/O 에러 발생. 파일명: {}", file.getOriginalFilename(), e);
            throw new UncheckedIOException("파일 읽는 중 오류가 발생했습니다.", e);
        } catch (S3Exception e) {
            log.error("S3 서비스 에러 발생. ", e);
            throw new IllegalStateException("S3 서비스에 오류가 발생했습니다.", e);
        } catch (SdkException e) {
            log.error("AWS SDK 통신 및 클라이언트 에러 발생", e);
            throw new IllegalStateException("AWS 통신 중 오류가 발생했습니다.", e);
        }
    }
}