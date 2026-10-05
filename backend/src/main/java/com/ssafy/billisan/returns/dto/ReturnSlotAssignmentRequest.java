package com.ssafy.billisan.returns.dto;

import java.util.UUID;

public record ReturnSlotAssignmentRequest(UUID returnAttemptId, UUID stationId) {
}
