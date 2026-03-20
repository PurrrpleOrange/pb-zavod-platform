package com.pb.booking.client;

import com.pb.booking.client.dto.*;
import com.pb.booking.exception.BookingException;
import com.pb.booking.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulingClient {

    private final WebClient schedulingWebClient;

    public HoldSlotResponse holdSlot(UUID gameSlotId, UUID bookingId) {
        log.info("Requesting slot hold: slotId={}, bookingId={}", gameSlotId, bookingId);

        return schedulingWebClient.post()
                .uri("/slots/{slotId}/holds", gameSlotId)
                .bodyValue(new HoldSlotRequest(bookingId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Slot hold failed: status={}", response.statusCode());
                    return Mono.error(new BookingException(ErrorCode.BOOKING_SLOT_HOLD_FAILED));
                })
                .bodyToMono(HoldSlotResponse.class)
                .block();
    }

    public void confirmSlotHold(UUID slotReservationId, UUID bookingId) {
        log.info("Confirming slot hold: reservationId={}, bookingId={}", slotReservationId, bookingId);

        schedulingWebClient.post()
                .uri("/slot-holds/{slotReservationId}/confirm", slotReservationId)
                .bodyValue(new ConfirmByBookingRequest(bookingId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Slot confirm failed: status={}", response.statusCode());
                    return Mono.error(new BookingException(ErrorCode.BOOKING_SLOT_HOLD_FAILED,
                            "Failed to confirm slot hold"));
                })
                .toBodilessEntity()
                .block();
    }

    public void cancelSlotHold(UUID slotReservationId, UUID bookingId) {
        log.info("Cancelling slot hold: reservationId={}, bookingId={}", slotReservationId, bookingId);

        schedulingWebClient.post()
                .uri("/slot-holds/{slotReservationId}/cancel", slotReservationId)
                .bodyValue(new ConfirmByBookingRequest(bookingId))
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class).doOnNext(body ->
                                log.warn("Slot cancel failed: status={}, body={}", response.statusCode(), body)
                        ).then();
                    }
                    return response.releaseBody();
                })
                .block();
    }

    public HoldZoneResponse holdZone(UUID zoneId, UUID bookingId, UUID slotReservationId) {
        log.info("Requesting zone hold: zoneId={}, bookingId={}", zoneId, bookingId);

        return schedulingWebClient.post()
                .uri("/zones/{zoneId}/holds", zoneId)
                .bodyValue(new HoldZoneRequest(bookingId, slotReservationId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Zone hold failed: status={}", response.statusCode());
                    return Mono.error(new BookingException(ErrorCode.BOOKING_ZONE_HOLD_FAILED));
                })
                .bodyToMono(HoldZoneResponse.class)
                .block();
    }

    public void confirmZoneHold(UUID zoneReservationId, UUID bookingId) {
        log.info("Confirming zone hold: reservationId={}, bookingId={}", zoneReservationId, bookingId);

        schedulingWebClient.post()
                .uri("/zone-holds/{zoneReservationId}/confirm", zoneReservationId)
                .bodyValue(new ConfirmByBookingRequest(bookingId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Zone confirm failed: status={}", response.statusCode());
                    return Mono.error(new BookingException(ErrorCode.BOOKING_ZONE_HOLD_FAILED,
                            "Failed to confirm zone hold"));
                })
                .toBodilessEntity()
                .block();
    }

    public void cancelZoneHold(UUID zoneReservationId, UUID bookingId) {
        log.info("Cancelling zone hold: reservationId={}, bookingId={}", zoneReservationId, bookingId);

        schedulingWebClient.post()
                .uri("/zone-holds/{zoneReservationId}/cancel", zoneReservationId)
                .bodyValue(new ConfirmByBookingRequest(bookingId))
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class).doOnNext(body ->
                                log.warn("Zone cancel failed: status={}, body={}", response.statusCode(), body)
                        ).then();
                    }
                    return response.releaseBody();
                })
                .block();
    }

    public void finishZoneChain(UUID zoneReservationId, UUID bookingId) {
        log.info("Finishing zone chain: zoneReservationId={}, bookingId={}", zoneReservationId, bookingId);

        schedulingWebClient.post()
                .uri("/zone-reservations/{zoneReservationId}/finish-chain", zoneReservationId)
                .bodyValue(new ConfirmByBookingRequest(bookingId))
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class).doOnNext(body ->
                                log.warn("Zone chain finish failed: status={}, body={}", response.statusCode(), body)
                        ).then();
                    }
                    return response.releaseBody();
                })
                .block();
    }
}
