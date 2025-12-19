package com.marketplace.inventory.scheduler;

import com.marketplace.inventory.domain.model.Reservation;
import com.marketplace.inventory.domain.repository.ReservationRepository;
import com.marketplace.inventory.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled job to clean up expired reservations.
 * Runs every 1 minute to check for and expire reservations past their TTL.
 */
@Component
public class ReservationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public ReservationCleanupScheduler(ReservationRepository reservationRepository,
                                      ReservationService reservationService) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void cleanupExpiredReservations() {
        OffsetDateTime now = OffsetDateTime.now();

        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(
            now,
            PageRequest.of(0, BATCH_SIZE)
        );

        if (expiredReservations.isEmpty()) {
            log.debug("No expired reservations to clean up");
            return;
        }

        log.info("Found {} expired reservations to clean up", expiredReservations.size());

        int successCount = 0;
        int failureCount = 0;

        for (Reservation reservation : expiredReservations) {
            try {
                reservationService.expireReservation(reservation.getReservationId());
                successCount++;

                log.info("Expired reservation: {} for order: {}",
                    reservation.getReservationId(), reservation.getOrderId());

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to expire reservation: {} for order: {}",
                    reservation.getReservationId(), reservation.getOrderId(), e);
            }
        }

        log.info("Reservation cleanup completed: {} succeeded, {} failed", successCount, failureCount);
    }
}
