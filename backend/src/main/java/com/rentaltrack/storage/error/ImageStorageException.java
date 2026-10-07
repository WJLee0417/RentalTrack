package com.rentaltrack.storage.error;

public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageStorageException(String message) {
        super(message);
    }
}
