package com.rentaltrack.storage.application;

import com.rentaltrack.condition.domain.ConditionRecordType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class ConditionImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ImageStorage imageStorage;
    private final long maxImageBytes;

    public ConditionImageStorageService(
            ImageStorage imageStorage,
            @Value("${rentaltrack.storage.max-image-bytes:10485760}") long maxImageBytes
    ) {
        this.imageStorage = imageStorage;
        this.maxImageBytes = maxImageBytes;
    }

    public StoredImage store(
            Long rentalContractId,
            ConditionRecordType recordType,
            MultipartFile file
    ) {
        validate(file);

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String originalFilename = safeOriginalFilename(file.getOriginalFilename());

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read uploaded image", exception);
        }

        StoredImage stored = imageStorage.store(new ImageUpload(
                "rentals/" + rentalContractId + "/" + recordType.name().toLowerCase(Locale.ROOT),
                originalFilename,
                contentType,
                content
        ));

        registerRollbackCleanup(stored.storageKey());
        return stored;
    }

    public byte[] load(String storageKey) {
        return imageStorage.load(storageKey);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if (file.getSize() > maxImageBytes) {
            throw new IllegalArgumentException(
                    "Image file size must not exceed " + maxImageBytes + " bytes"
            );
        }

        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG and WEBP images are allowed");
        }
    }

    private String safeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "image";
        }

        String normalized = originalFilename.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);

        if (name.length() > 255) {
            return name.substring(name.length() - 255);
        }

        return name;
    }

    private void registerRollbackCleanup(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            imageStorage.delete(storageKey);
                        }
                    }
                }
        );
    }
}
