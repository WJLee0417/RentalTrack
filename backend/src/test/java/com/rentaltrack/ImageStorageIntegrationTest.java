package com.rentaltrack;

import com.rentaltrack.storage.application.ConditionImageStorageService;
import com.rentaltrack.storage.application.StoredImage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ImageStorageIntegrationTest {

    @Autowired
    private ConditionImageStorageService conditionImageStorageService;

    @Test
    void storesAndLoadsImageThroughStorageAbstraction() {
        byte[] content = "rentaltrack-image".getBytes();

        StoredImage stored = conditionImageStorageService.store(
                1L,
                com.rentaltrack.condition.domain.ConditionRecordType.BEFORE_RENTAL,
                new MockMultipartFile(
                        "file",
                        "condition.jpg",
                        "image/jpeg",
                        content
                )
        );

        assertThat(stored.storageKey()).startsWith("rentals/1/before_rental/");
        assertThat(stored.originalFilename()).isEqualTo("condition.jpg");
        assertThat(stored.contentType()).isEqualTo("image/jpeg");
        assertThat(stored.fileSize()).isEqualTo(content.length);
        assertThat(conditionImageStorageService.load(stored.storageKey())).isEqualTo(content);
    }

    @Test
    void rejectsUnsupportedContentType() {
        assertThatThrownBy(() -> conditionImageStorageService.store(
                1L,
                com.rentaltrack.condition.domain.ConditionRecordType.BEFORE_RENTAL,
                new MockMultipartFile(
                        "file",
                        "condition.txt",
                        "text/plain",
                        "not-an-image".getBytes()
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
