package com.rentaltrack.storage.application;

public interface ImageStorage {

    StoredImage store(ImageUpload upload);

    byte[] load(String storageKey);

    void delete(String storageKey);
}
