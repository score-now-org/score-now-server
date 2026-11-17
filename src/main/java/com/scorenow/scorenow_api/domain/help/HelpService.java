package com.scorenow.scorenow_api.domain.help;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HelpService {
    private final HelpDocumentRepository documents;
    private final ObjectMapper mapper;
    private final FileStorage storage;

    public record ItemInput(@NotBlank @Size(max = 36) String id,
                            @Size(max = 3000) String content,
                            @Size(max = 500) String imageKey) { }
    public record PublishRequest(@NotNull Long version,
                                 @NotNull @Size(max = 50) List<@NotNull @Valid ItemInput> items) { }
    public record VersionRequest(@NotNull Long version) { }
    public record StoredItem(String id, String content, String imageKey, LocalDateTime createdAt) { }
    public record Item(String id, String content, String imageKey, String imageUrl, LocalDateTime createdAt) { }
    public record Snapshot(long version, boolean unpublished, List<Item> items) { }

    @Transactional(readOnly = true)
    public Snapshot get(boolean published) {
        return documents.findById(1L).map(doc -> snapshot(doc, published)).orElseGet(() -> new Snapshot(-1, false, List.of()));
    }

    @Transactional
    public Snapshot saveDraft(PublishRequest request) {
        HelpDocument document = documents.findById(1L).orElse(null);
        long currentVersion = document == null ? -1 : document.getVersion();
        if (currentVersion != request.version()) throw conflict();

        Map<String, LocalDateTime> created = new HashMap<>();
        if (document != null) read(document.getDraftJson()).forEach(item -> created.put(item.id(), item.createdAt()));
        Set<String> ids = new HashSet<>();
        List<StoredItem> items = new ArrayList<>();
        for (ItemInput item : request.items()) {
            String content = item.content() == null ? "" : item.content().trim();
            String imageKey = item.imageKey() == null || item.imageKey().isBlank() ? null : item.imageKey();
            if (!ids.add(item.id()) || (content.isEmpty() && imageKey == null)
                    || (imageKey != null && (!imageKey.startsWith("help/") || imageKey.contains("..")))) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            }
            items.add(new StoredItem(item.id(), content, imageKey,
                    created.getOrDefault(item.id(), LocalDateTime.now())));
        }
        try {
            String json = mapper.writeValueAsString(items);
            if (document == null) document = new HelpDocument(json);
            else document.update(json);
            return snapshot(documents.saveAndFlush(document), false);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("도움말을 저장하지 못했습니다.", e);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            throw conflict();
        }
    }

    @Transactional
    public Snapshot publish(VersionRequest request) {
        HelpDocument document = documents.findById(1L).orElseThrow(this::conflict);
        if (!document.getVersion().equals(request.version())) throw conflict();
        try {
            document.publish();
            return snapshot(documents.saveAndFlush(document), false);
        } catch (OptimisticLockingFailureException e) {
            throw conflict();
        }
    }

    private Snapshot snapshot(HelpDocument document, boolean published) {
        return new Snapshot(document.getVersion(), !published && !document.getDraftJson().equals(document.getPublishedJson()), read(published ? document.getPublishedJson() : document.getDraftJson()).stream()
                .map(item -> new Item(item.id(), item.content(), item.imageKey(),
                        item.imageKey() == null ? null : storage.getFileUrl(item.imageKey()), item.createdAt()))
                .toList());
    }

    private List<StoredItem> read(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<StoredItem>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("도움말을 읽지 못했습니다.", e);
        }
    }

    private BusinessException conflict() {
        return new BusinessException(ErrorCode.HELP_EDIT_CONFLICT);
    }
}
