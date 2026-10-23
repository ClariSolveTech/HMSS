package com.healthcare.backend.notification.service;

import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.notification.dto.CreateNotificationRequest;
import com.healthcare.backend.notification.dto.NotificationResponse;
import com.healthcare.backend.notification.entity.Notification;
import com.healthcare.backend.notification.entity.NotificationPriority;
import com.healthcare.backend.notification.entity.NotificationStatus;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            HospitalRepository hospitalRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        Long hospitalId = request.getHospitalId();
        if (hospitalId == null) {
            hospitalId = SecurityUtils.getCurrentHospitalId();
        }

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Hospital not found with id: " + request.getHospitalId()));

        User recipient = userRepository.findById(request.getRecipientUserId())
                .orElseThrow(() -> new NoSuchElementException("Recipient user not found with id: " + request.getRecipientUserId()));

        if (!recipient.getHospital().getId().equals(hospital.getId())) {
            throw new IllegalArgumentException("Recipient user does not belong to hospital with id: " + hospital.getId());
        }

        Notification notification = new Notification();
        notification.setHospital(hospital);
        notification.setRecipient(recipient);
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setPriority(request.getPriority() != null ? request.getPriority() : NotificationPriority.NORMAL);
        notification.setReferenceType(request.getReferenceType());
        notification.setReferenceId(request.getReferenceId());
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setReadAt(null);

        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }

    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();
        Long currentHospitalId = SecurityUtils.getCurrentHospitalId();

        Pageable effectivePageable = sanitizePageable(pageable);
        return notificationRepository.findByRecipientIdAndHospitalId(currentUserId, currentHospitalId, effectivePageable)
                .map(NotificationResponse::fromEntity);
    }

    public Page<NotificationResponse> getUnreadNotifications(Pageable pageable) {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();
        Long currentHospitalId = SecurityUtils.getCurrentHospitalId();

        Pageable effectivePageable = sanitizePageable(pageable);
        return notificationRepository.findByRecipientIdAndHospitalIdAndStatus(currentUserId, currentHospitalId, NotificationStatus.UNREAD, effectivePageable)
                .map(NotificationResponse::fromEntity);
    }

    public long getUnreadCount() {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();
        Long currentHospitalId = SecurityUtils.getCurrentHospitalId();

        return notificationRepository.countByRecipientIdAndHospitalIdAndStatus(currentUserId, currentHospitalId, NotificationStatus.UNREAD);
    }

    public boolean existsDuplicate(Long hospitalId, Long recipientUserId, NotificationType type, String referenceType, Long referenceId) {
        if (hospitalId == null || recipientUserId == null || type == null || referenceType == null || referenceId == null) {
            return false;
        }
        return notificationRepository.existsByHospitalIdAndRecipientIdAndTypeAndReferenceTypeAndReferenceId(
                hospitalId, recipientUserId, type, referenceType, referenceId
        );
    }

    public NotificationResponse getNotification(Long id) {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();
        Long currentHospitalId = SecurityUtils.getCurrentHospitalId();

        Notification notification = notificationRepository.findByIdAndRecipientIdAndHospitalId(id, currentUserId, currentHospitalId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found with id: " + id));

        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();
        Long currentHospitalId = SecurityUtils.getCurrentHospitalId();

        Notification notification = notificationRepository.findByIdAndRecipientIdAndHospitalId(id, currentUserId, currentHospitalId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found with id: " + id));

        if (notification.getStatus() != NotificationStatus.READ) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public void markAllAsRead() {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();
        Long currentHospitalId = SecurityUtils.getCurrentHospitalId();

        notificationRepository.markAllAsReadForUser(
                currentUserId,
                currentHospitalId,
                NotificationStatus.UNREAD,
                NotificationStatus.READ,
                LocalDateTime.now()
        );
    }

    @Transactional
    public void deleteNotification(Long id) {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();
        Long currentHospitalId = SecurityUtils.getCurrentHospitalId();

        Notification notification = notificationRepository.findByIdAndRecipientIdAndHospitalId(id, currentUserId, currentHospitalId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found with id: " + id));

        notificationRepository.delete(notification);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");

        return PageRequest.of(pageable.getPageNumber(), pageSize, sort);
    }
}
