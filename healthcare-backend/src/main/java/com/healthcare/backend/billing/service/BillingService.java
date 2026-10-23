package com.healthcare.backend.billing.service;

import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.billing.dto.*;
import com.healthcare.backend.billing.entity.*;
import com.healthcare.backend.billing.repository.InvoiceItemRepository;
import com.healthcare.backend.billing.repository.InvoiceRepository;
import com.healthcare.backend.billing.repository.PaymentRepository;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.notification.dto.CreateNotificationRequest;
import com.healthcare.backend.notification.entity.NotificationPriority;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingService {

    private static final AtomicLong INVOICE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000L);
    private static final AtomicLong PAYMENT_SEQ = new AtomicLong(System.currentTimeMillis() % 100000L);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;
    private final HospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final NotificationService notificationService;

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Hospital not found: " + hospitalId));

        Patient patient = patientRepository.findByIdAndHospitalId(request.getPatientId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + request.getPatientId()));

        Doctor doctor = resolveAndValidateDoctor(request.getDoctorId(), hospitalId);

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findByIdAndHospitalId(request.getAppointmentId(), hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Appointment not found in your hospital: " + request.getAppointmentId()));

            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Appointment patient does not match request patient");
            }
            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                throw new IllegalArgumentException("Appointment doctor does not match request doctor");
            }
        }

        Consultation consultation = null;
        if (request.getConsultationId() != null) {
            consultation = consultationRepository.findByIdAndHospitalId(request.getConsultationId(), hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Consultation not found in your hospital: " + request.getConsultationId()));

            if (!consultation.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Consultation patient does not match request patient");
            }
            if (!consultation.getDoctor().getId().equals(doctor.getId())) {
                throw new IllegalArgumentException("Consultation doctor does not match request doctor");
            }
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateUniqueInvoiceNumber(hospitalId));
        invoice.setHospital(hospital);
        invoice.setPatient(patient);
        invoice.setDoctor(doctor);
        invoice.setAppointment(appointment);
        invoice.setConsultation(consultation);
        invoice.setNotes(request.getNotes());
        invoice.setStatus(InvoiceStatus.DRAFT);

        buildAndAttachInvoiceItems(invoice, request.getItems());
        calculateAndSetInvoiceAmounts(invoice);

        Invoice saved = invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Invoice invoice = invoiceRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + id));

        validateDoctorAccessIfDoctorRole(invoice.getDoctor());
        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional
    public InvoiceResponse updateInvoice(Long id, UpdateInvoiceRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Invoice invoice = invoiceRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + id));

        validateDoctorAccessIfDoctorRole(invoice.getDoctor());

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be updated. Current status: " + invoice.getStatus());
        }

        invoice.getItems().clear();
        buildAndAttachInvoiceItems(invoice, request.getItems());
        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        calculateAndSetInvoiceAmounts(invoice);

        Invoice updated = invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(updated);
    }

    @Transactional
    public InvoiceResponse issueInvoice(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Invoice invoice = invoiceRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + id));

        validateDoctorAccessIfDoctorRole(invoice.getDoctor());

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be issued. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());

        Invoice issued = invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(issued);
    }

    @Transactional
    public InvoiceResponse cancelInvoice(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Invoice invoice = invoiceRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + id));

        validateDoctorAccessIfDoctorRole(invoice.getDoctor());

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be cancelled. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);

        Invoice cancelled = invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(cancelled);
    }

    @Transactional
    public PaymentResponse addPayment(Long invoiceId, CreatePaymentRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Invoice invoice = invoiceRepository.findByIdAndHospitalId(invoiceId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + invoiceId));

        validateDoctorAccessIfDoctorRole(invoice.getDoctor());

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add payment to a cancelled invoice");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already fully paid");
        }

        BigDecimal paymentAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        if (paymentAmount.compareTo(invoice.getDueAmount()) > 0) {
            throw new IllegalStateException("Payment amount (" + paymentAmount + ") exceeds remaining due amount (" + invoice.getDueAmount() + ")");
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setHospital(invoice.getHospital());
        payment.setPaymentReference(generateUniquePaymentReference(hospitalId));
        payment.setAmount(paymentAmount);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionReference(request.getTransactionReference());
        payment.setNotes(request.getNotes());
        payment.setPaidAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        invoice.getPayments().add(savedPayment);

        // Recalculate paid amount and due amount
        BigDecimal newPaidAmount = invoice.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        invoice.setPaidAmount(newPaidAmount);
        BigDecimal newDueAmount = invoice.getTotalAmount().subtract(newPaidAmount).setScale(2, RoundingMode.HALF_UP);
        if (newDueAmount.compareTo(BigDecimal.ZERO) < 0) {
            newDueAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        invoice.setDueAmount(newDueAmount);

        // Update status according to paidAmount vs totalAmount
        if (newDueAmount.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else {
            invoice.setStatus(InvoiceStatus.ISSUED);
        }

        invoiceRepository.save(invoice);

        createPaymentReceivedNotification(savedPayment, invoice);

        return PaymentResponse.fromEntity(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoiceId(Long invoiceId) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Invoice invoice = invoiceRepository.findByIdAndHospitalId(invoiceId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + invoiceId));

        validateDoctorAccessIfDoctorRole(invoice.getDoctor());

        List<Payment> payments = paymentRepository.findByInvoiceIdAndHospitalId(invoiceId, hospitalId);
        return payments.stream().map(PaymentResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getPatientInvoices(Long patientId, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + patientId));
        Page<Invoice> invoices = invoiceRepository.findByPatientIdAndHospitalId(patientId, hospitalId, pageable);
        return invoices.map(InvoiceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getDoctorOwnInvoices(Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        UserPrincipal userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();
        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Doctor profile not found for authenticated user"));

        Page<Invoice> invoices = invoiceRepository.findByDoctorIdAndHospitalId(doctor.getId(), hospitalId, pageable);
        return invoices.map(InvoiceResponse::fromEntity);
    }

    private Doctor resolveAndValidateDoctor(Long requestedDoctorId, Long hospitalId) {
        UserPrincipal userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();
        boolean isDoctorRole = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        if (isDoctorRole) {
            Doctor loggedInDoctor = doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), hospitalId)
                    .orElseThrow(() -> new AccessDeniedException("Doctor profile not found for authenticated user"));

            if (requestedDoctorId != null && !requestedDoctorId.equals(loggedInDoctor.getId())) {
                throw new AccessDeniedException("Doctors can only create invoices for themselves");
            }
            return loggedInDoctor;
        }

        if (requestedDoctorId == null) {
            throw new IllegalArgumentException("Doctor ID is required");
        }

        return doctorRepository.findByIdAndHospitalId(requestedDoctorId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found in your hospital: " + requestedDoctorId));
    }

    private void validateDoctorAccessIfDoctorRole(Doctor doctor) {
        UserPrincipal userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();
        boolean isDoctorRole = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        if (isDoctorRole) {
            Doctor loggedInDoctor = doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), userPrincipal.getHospitalId())
                    .orElse(null);

            if (loggedInDoctor != null && !loggedInDoctor.getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctors can only access their own invoices");
            }
        }
    }

    private void buildAndAttachInvoiceItems(Invoice invoice, List<InvoiceItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("At least one invoice item is required");
        }

        for (InvoiceItemRequest req : itemRequests) {
            BigDecimal unitPrice = req.getUnitPrice() != null ? req.getUnitPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal discount = req.getDiscount() != null ? req.getDiscount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = req.getTax() != null ? req.getTax().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            int qty = req.getQuantity() != null ? req.getQuantity() : 1;

            BigDecimal baseAmount = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = baseAmount.subtract(discount).add(tax).setScale(2, RoundingMode.HALF_UP);

            if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
                lineTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            InvoiceItem item = new InvoiceItem();
            item.setItemType(req.getItemType());
            item.setDescription(req.getDescription());
            item.setQuantity(qty);
            item.setUnitPrice(unitPrice);
            item.setDiscount(discount);
            item.setTax(tax);
            item.setLineTotal(lineTotal);

            invoice.addItem(item);
        }
    }

    private void calculateAndSetInvoiceAmounts(Invoice invoice) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal base = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(base);
                discountTotal = discountTotal.add(item.getDiscount());
                taxTotal = taxTotal.add(item.getTax());
            }
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        discountTotal = discountTotal.setScale(2, RoundingMode.HALF_UP);
        taxTotal = taxTotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = subtotal.subtract(discountTotal).add(taxTotal).setScale(2, RoundingMode.HALF_UP);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal paidAmount = invoice.getPaidAmount() != null
                ? invoice.getPaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal dueAmount = totalAmount.subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);
        if (dueAmount.compareTo(BigDecimal.ZERO) < 0) {
            dueAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discountTotal);
        invoice.setTax(taxTotal);
        invoice.setTotalAmount(totalAmount);
        invoice.setPaidAmount(paidAmount);
        invoice.setDueAmount(dueAmount);
    }

    private String generateUniqueInvoiceNumber(Long hospitalId) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String invoiceNum;
        do {
            long seq = INVOICE_SEQ.incrementAndGet();
            invoiceNum = String.format("INV-%s-%05d", datePrefix, seq % 100000L);
        } while (invoiceRepository.existsByInvoiceNumberAndHospitalId(invoiceNum, hospitalId));
        return invoiceNum;
    }

    private String generateUniquePaymentReference(Long hospitalId) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String payRef;
        do {
            long seq = PAYMENT_SEQ.incrementAndGet();
            payRef = String.format("PAY-%s-%05d", datePrefix, seq % 100000L);
        } while (paymentRepository.existsByPaymentReferenceAndHospitalId(payRef, hospitalId));
        return payRef;
    }

    private void createPaymentReceivedNotification(Payment payment, Invoice invoice) {
        if (notificationService == null || payment == null || invoice == null || invoice.getDoctor() == null ||
                invoice.getDoctor().getUser() == null || invoice.getHospital() == null) {
            return;
        }
        try {
            Long hospitalId = invoice.getHospital().getId();
            Long recipientUserId = invoice.getDoctor().getUser().getId();
            if (notificationService.existsDuplicate(hospitalId, recipientUserId, NotificationType.PAYMENT_RECEIVED, "PAYMENT", payment.getId())) {
                return;
            }

            Patient patient = invoice.getPatient();
            String patientName = patient != null ? ((patient.getFirstName() != null ? patient.getFirstName() : "") +
                    (patient.getLastName() != null && !patient.getLastName().isBlank() ? " " + patient.getLastName() : "")).trim() : "Patient";

            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setHospitalId(hospitalId);
            req.setRecipientUserId(recipientUserId);
            req.setType(NotificationType.PAYMENT_RECEIVED);
            req.setTitle("Payment Received");
            req.setMessage("Payment of " + payment.getAmount() + " received for invoice #" + invoice.getInvoiceNumber() + " (" + patientName + ").");
            req.setPriority(NotificationPriority.NORMAL);
            req.setReferenceType("PAYMENT");
            req.setReferenceId(payment.getId());

            notificationService.createNotification(req);
        } catch (Exception ignored) {
            // Safe fallback so core billing operation is preserved
        }
    }
}
