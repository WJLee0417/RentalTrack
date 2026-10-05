package com.ssafy.billisan.returns.dto;

import java.util.UUID;

public record ReturnAttemptRequest(UUID requestId, UUID sessionId, UUID stationId, UUID userRef) {
}
