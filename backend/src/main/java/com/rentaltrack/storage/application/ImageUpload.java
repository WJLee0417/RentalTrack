package com.rentaltrack.storage.application;

public record ImageUpload(
        String directory,
        String originalFilename,
        String contentType,
        byte[] content
) {
}
