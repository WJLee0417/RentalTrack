package com.rentaltrack.storage.application;

public record StoredImage(
        String storageKey,
        String originalFilename,
        String contentType,
        long fileSize
) {
}
