package com.healthcare.backend.lab.service;

import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.lab.dto.CreateLabResultRequest;
import com.healthcare.backend.lab.dto.LabResultResponse;
import com.healthcare.backend.lab.dto.ReviewLabResultRequest;
import com.healthcare.backend.lab.entity.*;
import com.healthcare.backend.lab.repository.LabOrderItemRepository;
import com.healthcare.backend.lab.repository.LabOrderRepository;
import com.healthcare.backend.lab.repository.LabResultRepository;
import com.healthcare.backend.notification.dto.CreateNotificationRequest;
import com.healthcare.backend.notification.entity.NotificationPriority;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.service.NotificationService;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabResultService {

    private final LabResultRepository labResultRepository;
    private final LabOrderRepository labOrderRepository;
    private final LabOrderItemRepository labOrderItemRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;

    @Transactional
    public LabResultResponse createLabResult(CreateLabResultRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        LabOrderItem item = labOrderItemRepository.findByIdAndLabOrderHospitalId(request.getLabOrderItemId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab order item not found in your hospital: " + request.getLabOrderItemId()));

        LabOrder order = item.getLabOrder();
        if (order.getStatus() == LabOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add result to a cancelled lab order");
        }

        if (labResultRepository.findByLabOrderItemIdAndHospitalId(item.getId(), hospitalId).isPresent()) {
            throw new IllegalStateException("Result already exists for lab order item ID: " + item.getId());
        }

        LabResult result = new LabResult();
        result.setLabOrderItem(item);
        result.setLabOrder(order);
        result.setPatient(order.getPatient());
        result.setDoctor(order.getDoctor());
        result.setHospital(order.getHospital());
        result.setResultValue(request.getResultValue());
        result.setResultUnit(request.getResultUnit());
        result.setReferenceRange(request.getReferenceRange());
        result.setAbnormalFlag(request.getAbnormalFlag() != null ? request.getAbnormalFlag() : false);
        result.setInterpretation(request.getInterpretation());
        result.setRemarks(request.getRemarks());
        result.setResultStatus(LabResultStatus.COMPLETED);
        result.setResultedAt(LocalDateTime.now());

        LabResult savedResult = labResultRepository.save(result);

        item.setStatus(LabOrderStatus.COMPLETED);

        createLabResultReadyNotification(savedResult);

        LabOrderStatus prevOrderStatus = order.getStatus();
        List<LabResult> existingResults = labResultRepository.findByLabOrderIdAndHospitalId(order.getId(), hospitalId);
        if (existingResults.size() >= order.getItems().size()) {
            if (prevOrderStatus != LabOrderStatus.COMPLETED) {
                order.setStatus(LabOrderStatus.COMPLETED);
                order.setCompletedAt(LocalDateTime.now());
                LabOrder savedOrder = labOrderRepository.save(order);
                createLabOrderCompletedNotification(savedOrder);
            }
        }

        return LabResultResponse.fromEntity(savedResult);
    }

    public LabResultResponse getLabResultById(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LabResult result = labResultRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab result not found in your hospital with ID: " + id));
        return LabResultResponse.fromEntity(result);
    }

    public List<LabResultResponse> getResultsByLabOrder(Long orderId) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        labOrderRepository.findByIdAndHospitalId(orderId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab order not found in your hospital with ID: " + orderId));

        List<LabResult> results = labResultRepository.findByLabOrderIdAndHospitalId(orderId, hospitalId);
        return results.stream().map(LabResultResponse::fromEntity).collect(Collectors.toList());
    }

    public Page<LabResultResponse> getDoctorLabResults(LabResultStatus status, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor profile not found in current hospital"));

        Page<LabResult> results;
        if (status == LabResultStatus.PENDING) {
            results = labResultRepository.findPendingByDoctorAndHospital(doctor.getId(), hospitalId, pageable);
        } else {
            results = labResultRepository.findByDoctorIdAndHospitalId(doctor.getId(), hospitalId, pageable);
        }
        return results.map(LabResultResponse::fromEntity);
    }

    public Page<LabResultResponse> getPatientLabResults(Long patientId, LabResultStatus status, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + patientId));

        Page<LabResult> results;
        if (status == LabResultStatus.REVIEWED) {
            results = labResultRepository.findReviewedByPatientAndHospital(patientId, hospitalId, pageable);
        } else {
            results = labResultRepository.findByPatientIdAndHospitalId(patientId, hospitalId, pageable);
        }
        return results.map(LabResultResponse::fromEntity);
    }

    @Transactional
    public LabResultResponse reviewLabResult(Long id, ReviewLabResultRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Current user is not a registered doctor in this hospital"));

        LabResult result = labResultRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab result not found in your hospital with ID: " + id));

        if (result.getResultStatus() == LabResultStatus.PENDING) {
            throw new IllegalStateException("Cannot review a PENDING lab result");
        }

        result.setResultStatus(LabResultStatus.REVIEWED);
        result.setReviewedAt(LocalDateTime.now());
        result.setReviewedBy("Dr. " + doctor.getUser().getFirstName() + " " + doctor.getUser().getLastName());

        if (request != null) {
            if (request.getInterpretation() != null && !request.getInterpretation().isBlank()) {
                result.setInterpretation(request.getInterpretation());
            }
            if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
                result.setRemarks(request.getRemarks());
            }
        }

        LabResult saved = labResultRepository.save(result);
        return LabResultResponse.fromEntity(saved);
    }

    private void createLabResultReadyNotification(LabResult result) {
        if (notificationService == null || result == null || result.getDoctor() == null || result.getDoctor().getUser() == null || result.getHospital() == null) {
            return;
        }
        try {
            Patient patient = result.getPatient();
            String patientName = patient != null ? ((patient.getFirstName() != null ? patient.getFirstName() : "") +
                    (patient.getLastName() != null && !patient.getLastName().isBlank() ? " " + patient.getLastName() : "")).trim() : "Patient";
            String testName = result.getLabOrderItem() != null && result.getLabOrderItem().getTestNameSnapshot() != null
                    ? result.getLabOrderItem().getTestNameSnapshot() : "Test";

            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setHospitalId(result.getHospital().getId());
            req.setRecipientUserId(result.getDoctor().getUser().getId());
            req.setType(NotificationType.LAB_RESULT_READY);
            req.setTitle("Lab Result Ready");
            req.setMessage("Lab result for " + patientName + " — " + testName + " is ready for review.");
            req.setPriority(NotificationPriority.NORMAL);
            req.setReferenceType("LAB_RESULT");
            req.setReferenceId(result.getId());

            notificationService.createNotification(req);
        } catch (Exception ignored) {
            // Safe fallback so core lab operation is preserved
        }
    }

    private void createLabOrderCompletedNotification(LabOrder order) {
        if (notificationService == null || order == null || order.getDoctor() == null || order.getDoctor().getUser() == null || order.getHospital() == null) {
            return;
        }
        try {
            Patient patient = order.getPatient();
            String patientName = patient != null ? ((patient.getFirstName() != null ? patient.getFirstName() : "") +
                    (patient.getLastName() != null && !patient.getLastName().isBlank() ? " " + patient.getLastName() : "")).trim() : "Patient";

            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setHospitalId(order.getHospital().getId());
            req.setRecipientUserId(order.getDoctor().getUser().getId());
            req.setType(NotificationType.LAB_ORDER_COMPLETED);
            req.setTitle("Lab Order Completed");
            req.setMessage("Lab order for " + patientName + " has been completed.");
            req.setPriority(NotificationPriority.NORMAL);
            req.setReferenceType("LAB_ORDER");
            req.setReferenceId(order.getId());

            notificationService.createNotification(req);
        } catch (Exception ignored) {
            // Safe fallback so core lab operation is preserved
        }
    }
}
