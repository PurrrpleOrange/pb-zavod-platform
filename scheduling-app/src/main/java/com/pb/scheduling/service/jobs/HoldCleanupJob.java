package com.pb.scheduling.service.jobs;

import com.pb.scheduling.config.SchedulingProperties;
import com.pb.scheduling.repository.SlotReservationRepository;
import com.pb.scheduling.repository.ZoneReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoldCleanupJob {
    private final ZoneReservationRepository zoneReservationRepository;
    private final SlotReservationRepository slotReservationRepository;
    private final SchedulingProperties properties;

    @Scheduled(fixedDelayString = "${pb.scheduling.hold-cleanup-interval-ms:60000}")
    @Transactional
    public void cancelExpiredHolds() {
        OffsetDateTime now = OffsetDateTime.now();
        int zoneCancelled = zoneReservationRepository.cancelExpiredHolds(now);
        int slotCancelled = slotReservationRepository.cancelExpiredHolds(now);
        if (zoneCancelled > 0 || slotCancelled > 0) {
            log.info("Hold cleanup cancelled expired holds: zones={}, slots={}, now={}",
                    zoneCancelled, slotCancelled, now);
        } else {
            log.debug("Hold cleanup: no expired holds found, now={}", now);
        }
    }
}
