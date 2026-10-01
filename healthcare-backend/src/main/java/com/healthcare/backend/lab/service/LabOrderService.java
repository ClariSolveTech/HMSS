package com.healthcare.backend.lab.service;

import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.lab.dto.CreateLabOrderItemRequest;
import com.healthcare.backend.lab.dto.CreateLabOrderRequest;
import com.healthcare.backend.lab.dto.LabOrderResponse;
import com.healthcare.backend.lab.dto.UpdateLabOrderRequest;
import com.healthcare.backend.lab.entity.*;
import com.healthcare.backend.lab.repository.LabOrderItemRepository;
import com.healthcare.backend.lab.repository.LabOrderRepository;
import com.healthcare.backend.lab.repository.LabTestRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabOrderService {

    private final LabOrderRepository labOrderRepository;
    private final LabOrderItemRepository labOrderItemRepository;
    private final LabTestRepository labTestRepository;
    private final HospitalRepository hospitalRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ConsultationRepository consultationRepository;
    private final NotificationService notificationService;

    @Transactional
    public LabOrderResponse createLabOrder(CreateLabOrderRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Current user is not a registered doctor in this hospital"));

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Hospital not found with ID: " + hospitalId));

        Patient patient = patientRepository.findByIdAndHospitalId(request.getPatientId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + request.getPatientId()));

        Consultation consultation = null;
        if (request.getConsultationId() != null) {
            consultation = consultationRepository.findByIdAndHospitalId(request.getConsultationId(), hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Consultation not found in your hospital: " + request.getConsultationId()));

            if (!consultation.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Consultation does not belong to patient ID: " + patient.getId());
            }
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one lab test item is required");
        }

        String orderNumber = generateOrderNumber(hospitalId);

        LabOrder order = new LabOrder();
        order.setOrderNumber(orderNumber);
        order.setHospital(hospital);
        order.setPatient(patient);
        order.setDoctor(doctor);
        order.setConsultation(consultation);
        order.setStatus(LabOrderStatus.ORDERED);
        order.setPriority(request.getPriority() != null ? request.getPriority() : LabOrderPriority.NORMAL);
        order.setClinicalNotes(request.getClinicalNotes());
        order.setOrderedAt(LocalDateTime.now());

        for (CreateLabOrderItemRequest itemReq : request.getItems()) {
            LabTest labTest = labTestRepository.findByIdAndHospitalId(itemReq.getLabTestId(), hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Lab test not found in your hospital: " + itemReq.getLabTestId()));

            if (!labTest.isActive()) {
                throw new IllegalArgumentException("Lab test is not active: " + labTest.getTestName());
            }

            LabOrderItem item = new LabOrderItem();
            item.setLabOrder(order);
            item.setLabTest(labTest);
            item.setTestNameSnapshot(labTest.getTestName());
            item.setTestCodeSnapshot(labTest.getTestCode());
            item.setStatus(LabOrderStatus.ORDERED);
            item.setNotes(itemReq.getNotes());

            order.getItems().add(item);
        }

        LabOrder saved = labOrderRepository.save(order);
        return LabOrderResponse.fromEntity(saved);
    }

    public LabOrderResponse getLabOrderById(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LabOrder order = labOrderRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab order not found in your hospital with ID: " + id));
        return LabOrderResponse.fromEntity(order);
    }

    public Page<LabOrderResponse> getDoctorLabOrders(LabOrderStatus status, LabOrderPriority priority, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor profile not found in current hospital"));

        Page<LabOrder> orders;
        if (status != null) {
            orders = labOrderRepository.findByDoctorIdAndHospitalIdAndStatus(doctor.getId(), hospitalId, status, pageable);
        } else {
            orders = labOrderRepository.findByDoctorIdAndHospitalId(doctor.getId(), hospitalId, pageable);
        }
        return orders.map(LabOrderResponse::fromEntity);
    }

    public Page<LabOrderResponse> getPatientLabOrders(Long patientId, LabOrderStatus status, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + patientId));

        Page<LabOrder> orders = labOrderRepository.findByPatientIdAndHospitalId(patientId, hospitalId, pageable);
        return orders.map(LabOrderResponse::fromEntity);
    }

    public Page<LabOrderResponse> getHospitalLabOrders(Long patientId, Long doctorId, LabOrderStatus status, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Page<LabOrder> orders;

        if (status != null) {
            orders = labOrderRepository.findByHospitalIdAndStatus(hospitalId, status, pageable);
        } else if (patientId != null) {
            orders = labOrderRepository.findByPatientIdAndHospitalId(patientId, hospitalId, pageable);
        } else if (doctorId != null) {
            orders = labOrderRepository.findByDoctorIdAndHospitalId(doctorId, hospitalId, pageable);
        } else {
            orders = labOrderRepository.findByHospitalId(hospitalId, pageable);
        }
        return orders.map(LabOrderResponse::fromEntity);
    }

    @Transactional
    public LabOrderResponse updateLabOrder(Long id, UpdateLabOrderRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LabOrder order = labOrderRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab order not found in your hospital with ID: " + id));

        if (order.getStatus() != LabOrderStatus.ORDERED) {
            throw new IllegalStateException("Cannot modify lab order with status: " + order.getStatus() + ". Only ORDERED lab orders can be updated.");
        }

        if (request.getPriority() != null) {
            order.setPriority(request.getPriority());
        }
        if (request.getClinicalNotes() != null) {
            order.setClinicalNotes(request.getClinicalNotes());
        }

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            order.getItems().clear();

            for (CreateLabOrderItemRequest itemReq : request.getItems()) {
                LabTest labTest = labTestRepository.findByIdAndHospitalId(itemReq.getLabTestId(), hospitalId)
                        .orElseThrow(() -> new NoSuchElementException("Lab test not found in your hospital: " + itemReq.getLabTestId()));

                if (!labTest.isActive()) {
                    throw new IllegalArgumentException("Lab test is not active: " + labTest.getTestName());
                }

                LabOrderItem item = new LabOrderItem();
                item.setLabOrder(order);
                item.setLabTest(labTest);
                item.setTestNameSnapshot(labTest.getTestName());
                item.setTestCodeSnapshot(labTest.getTestCode());
                item.setStatus(LabOrderStatus.ORDERED);
                item.setNotes(itemReq.getNotes());

                order.getItems().add(item);
            }
        }

        LabOrder saved = labOrderRepository.save(order);
        return LabOrderResponse.fromEntity(saved);
    }

    @Transactional
    public LabOrderResponse updateLabOrderStatus(Long id, LabOrderStatus newStatus) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LabOrder order = labOrderRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab order not found in your hospital with ID: " + id));

        LabOrderStatus currentStatus = order.getStatus();

        if (currentStatus == LabOrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot transition from terminal status COMPLETED to " + newStatus);
        }
        if (currentStatus == LabOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot transition from terminal status CANCELLED to " + newStatus);
        }

        if (currentStatus == newStatus) {
            return LabOrderResponse.fromEntity(order);
        }

        boolean isValid = false;
        if (newStatus == LabOrderStatus.CANCELLED) {
            isValid = (currentStatus == LabOrderStatus.ORDERED || currentStatus == LabOrderStatus.SAMPLE_COLLECTED);
        } else if (currentStatus == LabOrderStatus.ORDERED && newStatus == LabOrderStatus.SAMPLE_COLLECTED) {
            isValid = true;
        } else if (currentStatus == LabOrderStatus.SAMPLE_COLLECTED && newStatus == LabOrderStatus.PROCESSING) {
            isValid = true;
        } else if (currentStatus == LabOrderStatus.PROCESSING && newStatus == LabOrderStatus.COMPLETED) {
            isValid = true;
        }

        if (!isValid) {
            throw new IllegalStateException("Invalid lab order status transition from " + currentStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);
        for (LabOrderItem item : order.getItems()) {
            item.setStatus(newStatus);
        }

        if (newStatus == LabOrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        } else if (newStatus == LabOrderStatus.CANCELLED) {
            order.setCancelledAt(LocalDateTime.now());
        }

        LabOrder saved = labOrderRepository.save(order);

        if (currentStatus != LabOrderStatus.COMPLETED && newStatus == LabOrderStatus.COMPLETED) {
            createLabOrderCompletedNotification(saved);
        }

        return LabOrderResponse.fromEntity(saved);
    }

    @Transactional
    public LabOrderResponse cancelLabOrder(Long id) {
        return updateLabOrderStatus(id, LabOrderStatus.CANCELLED);
    }

    private String generateOrderNumber(Long hospitalId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = labOrderRepository.countByHospitalId(hospitalId) + 1;
        String orderNumber = String.format("LAB-%s-%04d", dateStr, count);

        int attempts = 0;
        while (labOrderRepository.findByOrderNumberAndHospitalId(orderNumber, hospitalId).isPresent()) {
            count++;
            orderNumber = String.format("LAB-%s-%04d", dateStr, count);
            attempts++;
            if (attempts > 1000) {
                orderNumber = String.format("LAB-%s-%s", dateStr, System.currentTimeMillis() % 10000);
                break;
            }
        }
        return orderNumber;
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
