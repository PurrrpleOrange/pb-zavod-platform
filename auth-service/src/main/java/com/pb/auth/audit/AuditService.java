package com.pb.auth.audit;

import com.pb.auth.domain.AuthAuditLog;
import com.pb.auth.domain.EventType;
import com.pb.auth.repository.AuthAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuthAuditLogRepository auditLogRepository;

    public void log(EventType eventType, UUID actorId, UUID targetId, String result, String errorCode, Map<String, Object> metadata) {
        AuthAuditLog entry = AuthAuditLog.builder()
                .eventType(eventType)
                .actorUserId(actorId)
                .targetUserId(targetId)
                .result(result)
                .errorCode(errorCode)
                .metadata(metadata)
                .build();

        auditLogRepository.save(entry);
        log.info("Audit: {} actor={} target={} result={}", eventType, actorId, targetId, result);
    }

    public void logSuccess(EventType eventType, UUID actorId, UUID targetId) {
        log(eventType, actorId, targetId, "SUCCESS", null, null);
    }

    public void logFailure(EventType eventType, UUID actorId, UUID targetId, String errorCode) {
        log(eventType, actorId, targetId, "FAILURE", errorCode, null);
    }
}
