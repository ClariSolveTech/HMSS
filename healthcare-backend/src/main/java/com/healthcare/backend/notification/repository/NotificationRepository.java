package com.healthcare.backend.notification.repository;

import com.healthcare.backend.notification.entity.Notification;
import com.healthcare.backend.notification.entity.NotificationStatus;
import com.healthcare.backend.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndHospitalId(Long id, Long hospitalId);

    Optional<Notification> findByIdAndRecipientIdAndHospitalId(Long id, Long recipientId, Long hospitalId);

    Page<Notification> findByRecipientIdAndHospitalId(Long recipientId, Long hospitalId, Pageable pageable);

    Page<Notification> findByRecipientIdAndHospitalIdAndStatus(Long recipientId, Long hospitalId, NotificationStatus status, Pageable pageable);

    long countByRecipientIdAndHospitalIdAndStatus(Long recipientId, Long hospitalId, NotificationStatus status);

    boolean existsByHospitalIdAndRecipientIdAndTypeAndReferenceTypeAndReferenceId(
            Long hospitalId,
            Long recipientId,
            NotificationType type,
            String referenceType,
            Long referenceId
    );

    @Modifying
    @Query("UPDATE Notification n SET n.status = :targetStatus, n.readAt = :readAt WHERE n.recipient.id = :recipientId AND n.hospital.id = :hospitalId AND n.status = :currentStatus")
    int markAllAsReadForUser(
            @Param("recipientId") Long recipientId,
            @Param("hospitalId") Long hospitalId,
            @Param("currentStatus") NotificationStatus currentStatus,
            @Param("targetStatus") NotificationStatus targetStatus,
            @Param("readAt") LocalDateTime readAt
    );
}
