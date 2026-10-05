package com.rentaltrack.condition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "condition_image")
public class ConditionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condition_record_id", nullable = false)
    private ConditionRecord conditionRecord;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "capture_order", nullable = false)
    private int captureOrder;

    @Column(name = "viewpoint", length = 30)
    private String viewpoint;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ConditionImage() {
    }

    public ConditionImage(
            ConditionRecord conditionRecord,
            String storageKey,
            String originalFilename,
            String contentType,
            Long fileSize,
            int captureOrder,
            String viewpoint,
            LocalDateTime capturedAt
    ) {
        this.conditionRecord = conditionRecord;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.captureOrder = captureOrder;
        this.viewpoint = viewpoint;
        this.capturedAt = capturedAt;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ConditionRecord getConditionRecord() {
        return conditionRecord;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public int getCaptureOrder() {
        return captureOrder;
    }

    public String getViewpoint() {
        return viewpoint;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
