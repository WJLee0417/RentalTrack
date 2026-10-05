package com.rentaltrack.condition.api;

import com.rentaltrack.access.application.AccessControlService;
import com.rentaltrack.condition.application.ConditionApplicationService;
import com.rentaltrack.condition.domain.ConditionImage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequestMapping("/api/v1/rentals/{rentalContractId}/conditions/after")
public class AfterConditionController {

    private final ConditionApplicationService conditionApplicationService;
    private final AccessControlService accessControlService;

    public AfterConditionController(
            ConditionApplicationService conditionApplicationService,
            AccessControlService accessControlService
    ) {
        this.conditionApplicationService = conditionApplicationService;
        this.accessControlService = accessControlService;
    }

    @PostMapping
    public ConditionRecordResponse createAfterCondition(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId,
            @Valid @org.springframework.web.bind.annotation.RequestBody AfterConditionRequest request
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return conditionApplicationService.createAfterCondition(rentalContractId, request);
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ConditionImageResponse addImage(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId,
            @Valid @ModelAttribute ConditionImageMetadataRequest metadata,
            @RequestPart("file") MultipartFile file
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return conditionApplicationService.addAfterConditionImage(
                rentalContractId,
                metadata,
                file
        );
    }

    @GetMapping
    public ConditionRecordResponse getAfterCondition(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return conditionApplicationService.getAfterCondition(rentalContractId);
    }

    @GetMapping("/images/{imageId}/content")
    public ResponseEntity<ByteArrayResource> getImage(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId,
            @PathVariable @Positive Long imageId
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);

        ConditionImage metadata = conditionApplicationService.getImageMetadata(rentalContractId, imageId);
        byte[] content = conditionApplicationService.loadImage(rentalContractId, imageId);

        MediaType mediaType = metadata.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(metadata.getContentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(content.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(
                                        metadata.getOriginalFilename() == null
                                                ? "image"
                                                : metadata.getOriginalFilename(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .body(new ByteArrayResource(content));
    }
}
