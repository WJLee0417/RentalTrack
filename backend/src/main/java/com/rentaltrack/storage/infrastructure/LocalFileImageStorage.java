package com.rentaltrack.storage.infrastructure;

import com.rentaltrack.storage.application.ImageStorage;
import com.rentaltrack.storage.application.ImageUpload;
import com.rentaltrack.storage.application.StoredImage;
import com.rentaltrack.storage.error.ImageStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

@Component
public class LocalFileImageStorage implements ImageStorage {

    private final Path rootDirectory;

    public LocalFileImageStorage(
            @Value("${rentaltrack.storage.local.root:${java.io.tmpdir}/rentaltrack-images}")
            String rootDirectory
    ) {
        this.rootDirectory = Path.of(rootDirectory).toAbsolutePath().normalize();
    }

    @Override
    public StoredImage store(ImageUpload upload) {
        String extension = extensionFor(upload.contentType());
        String storageKey = normalizeDirectory(upload.directory())
                + "/"
                + UUID.randomUUID()
                + extension;

        Path destination = resolve(storageKey);

        try {
            Files.createDirectories(destination.getParent());
            Files.write(
                    destination,
                    upload.content(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw new ImageStorageException("Failed to store condition image", exception);
        }

        return new StoredImage(
                storageKey,
                upload.originalFilename(),
                upload.contentType(),
                upload.content().length
        );
    }

    @Override
    public byte[] load(String storageKey) {
        Path source = resolve(storageKey);

        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new ImageStorageException("Stored image not found: " + storageKey);
        }

        try {
            return Files.readAllBytes(source);
        } catch (IOException exception) {
            throw new ImageStorageException("Failed to load condition image", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolve(storageKey);

        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new ImageStorageException("Failed to delete condition image", exception);
        }
    }

    private Path resolve(String storageKey) {
        Path resolved = rootDirectory.resolve(storageKey).normalize();

        if (!resolved.startsWith(rootDirectory)) {
            throw new ImageStorageException("Invalid storage key");
        }

        return resolved;
    }

    private String normalizeDirectory(String directory) {
        String normalized = directory
                .replace('\\', '/')
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");

        if (normalized.isBlank() || normalized.contains("..")) {
            throw new ImageStorageException("Invalid storage directory");
        }

        return normalized;
    }

    private String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}
