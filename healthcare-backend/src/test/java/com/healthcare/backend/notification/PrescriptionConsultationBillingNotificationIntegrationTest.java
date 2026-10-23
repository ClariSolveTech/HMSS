package com.healthcare.backend.notification;

import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.CustomUserDetailsService;
import com.healthcare.backend.billing.dto.InvoiceItemRequest;
import com.healthcare.backend.billing.dto.CreateInvoiceRequest;
import com.healthcare.backend.billing.dto.CreatePaymentRequest;
import com.healthcare.backend.billing.dto.InvoiceResponse;
import com.healthcare.backend.billing.dto.PaymentResponse;
import com.healthcare.backend.billing.entity.Invoice;
import com.healthcare.backend.billing.entity.InvoiceItemType;
import com.healthcare.backend.billing.entity.InvoiceStatus;
import com.healthcare.backend.billing.entity.PaymentMethod;
import com.healthcare.backend.billing.repository.InvoiceItemRepository;
import com.healthcare.backend.billing.repository.InvoiceRepository;
import com.healthcare.backend.billing.repository.PaymentRepository;
import com.healthcare.backend.billing.service.BillingService;
import com.healthcare.backend.consultation.dto.ConsultationResponse;
import com.healthcare.backend.consultation.dto.CreateConsultationRequest;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.entity.ConsultationStatus;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.consultation.service.ConsultationService;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.notification.dto.NotificationResponse;
import com.healthcare.backend.notification.entity.Notification;
import com.healthcare.backend.notification.entity.NotificationPriority;
import com.healthcare.backend.notification.entity.NotificationStatus;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.repository.NotificationRepository;
import com.healthcare.backend.notification.service.NotificationService;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.prescription.dto.CreatePrescriptionRequest;
import com.healthcare.backend.prescription.dto.PrescriptionItemRequest;
import com.healthcare.backend.prescription.dto.PrescriptionResponse;
import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.entity.PrescriptionStatus;
import com.healthcare.backend.prescription.repository.PrescriptionItemRepository;
import com.healthcare.backend.prescription.repository.PrescriptionRepository;
import com.healthcare.backend.prescription.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PrescriptionConsultationBillingNotificationIntegrationTest {

    private static final AtomicLong COUNTER = new AtomicLong(50000);

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BillingService billingService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Hospital testHospitalA;
    private Hospital testHospitalB;
    private Doctor doctorA;
    private Doctor doctorB;
    private Patient patientA;
    private String doctorEmailA;
    private String doctorEmailB;
    private String adminEmailA;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        paymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        prescriptionItemRepository.deleteAll();
        prescriptionRepository.deleteAll();
        consultationRepository.deleteAll();

        for (RoleName rn : RoleName.values()) {
            if (roleRepository.findByName(rn).isEmpty()) {
                roleRepository.save(new Role(rn));
            }
        }

        testHospitalA = createHospitalHelper("Hospital Alpha " + COUNTER.incrementAndGet(), "ALPHA-" + COUNTER.incrementAndGet());
        testHospitalB = createHospitalHelper("Hospital Beta " + COUNTER.incrementAndGet(), "BETA-" + COUNTER.incrementAndGet());

        doctorEmailA = "doc.alpha." + COUNTER.incrementAndGet() + "@hospital.com";
        doctorA = createDoctorHelper(testHospitalA, doctorEmailA, "Dr. Alice", "Smith", "Cardiology");

        doctorEmailB = "doc.beta." + COUNTER.incrementAndGet() + "@hospital.com";
        doctorB = createDoctorHelper(testHospitalB, doctorEmailB, "Dr. Bob", "Jones", "Neurology");

        adminEmailA = "admin.alpha." + COUNTER.incrementAndGet() + "@hospital.com";
        createUserHelper(testHospitalA, adminEmailA, "Admin", "Alpha", RoleName.HOSPITAL_ADMIN);

        patientA = createPatientHelper(testHospitalA, "John", "Doe", "john.doe." + COUNTER.incrementAndGet() + "@example.com", "98765" + (COUNTER.incrementAndGet() % 100000));
    }

    private Hospital createHospitalHelper(String name, String code) {
        Hospital h = new Hospital();
        h.setName(name);
        h.setCode(code);
        h.setEmail(code.toLowerCase() + "@hospital.com");
        h.setPhone("1234567890");
        h.setStatus(HospitalStatus.ACTIVE);
        return hospitalRepository.save(h);
    }

    private User createUserHelper(Hospital hospital, String email, String first, String last, RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));
        User u = new User();
        u.setHospital(hospital);
        u.setEmail(email);
        u.setMobileNumber("98" + (COUNTER.incrementAndGet() % 100000000));
        u.setPasswordHash(passwordEncoder.encode("Password@123"));
        u.setFirstName(first);
        u.setLastName(last);
        u.setStatus(UserStatus.ACTIVE);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    private Doctor createDoctorHelper(Hospital hospital, String email, String first, String last, String spec) {
        User u = createUserHelper(hospital, email, first, last, RoleName.DOCTOR);
        long seq = COUNTER.incrementAndGet();
        Doctor d = new Doctor();
        d.setHospital(hospital);
        d.setUser(u);
        d.setDoctorCode(hospital.getCode() + "-D" + seq);
        d.setSpecialization(spec);
        d.setQualification("MD");
        d.setLicenseNumber("LIC-" + seq);
        d.setStatus(DoctorStatus.ACTIVE);
        d.setConsultationFee(new BigDecimal("500.00"));
        return doctorRepository.save(d);
    }

    private Patient createPatientHelper(Hospital hospital, String f, String l, String e, String m) {
        long seq = COUNTER.incrementAndGet();
        Patient p = new Patient();
        p.setHospital(hospital);
        p.setPatientCode(hospital.getCode() + "-P" + seq);
        p.setFirstName(f);
        p.setLastName(l);
        p.setEmail(e);
        p.setMobileNumber(m);
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        p.setGender(Gender.MALE);
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p);
    }

    private void authenticate(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Consultation createTestConsultation() {
        authenticate(doctorEmailA);
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA.getId());
        req.setDoctorId(doctorA.getId());
        req.setChiefComplaint("Chest discomfort");
        req.setSymptoms("Shortness of breath");
        ConsultationResponse resp = consultationService.createConsultation(req);
        return consultationRepository.findById(resp.getId()).orElseThrow();
    }

    private Prescription createTestPrescription(Consultation consultation) {
        authenticate(doctorEmailA);
        CreatePrescriptionRequest req = new CreatePrescriptionRequest();
        req.setConsultationId(consultation.getId());
        req.setNotes("Take after meals");

        PrescriptionItemRequest item = new PrescriptionItemRequest();
        item.setMedicineName("Atorvastatin");
        item.setDosage("10mg");
        item.setFrequency("Once daily");
        item.setDuration("30 days");
        item.setRoute("Oral");
        req.setItems(List.of(item));

        PrescriptionResponse resp = prescriptionService.createPrescription(req);
        return prescriptionRepository.findById(resp.getId()).orElseThrow();
    }

    private Invoice createTestInvoice(Consultation consultation) {
        authenticate(doctorEmailA);
        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setPatientId(patientA.getId());
        req.setDoctorId(doctorA.getId());
        req.setConsultationId(consultation.getId());
        req.setNotes("Consultation invoice");

        InvoiceItemRequest item = new InvoiceItemRequest();
        item.setItemType(InvoiceItemType.CONSULTATION);
        item.setDescription("Consultation Fee");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("500.00"));
        req.setItems(List.of(item));

        InvoiceResponse resp = billingService.createInvoice(req);
        return invoiceRepository.findById(resp.getId()).orElseThrow();
    }

    // ==========================================
    // 1. PRESCRIPTION NOTIFICATIONS
    // ==========================================

    @Test
    @DisplayName("1. issuePrescription_createsPrescriptionIssuedNotification")
    void issuePrescription_createsPrescriptionIssuedNotification() {
        Consultation consultation = createTestConsultation();
        Prescription prescription = createTestPrescription(consultation);

        authenticate(doctorEmailA);
        PrescriptionResponse issued = prescriptionService.issuePrescription(prescription.getId());
        assertEquals(PrescriptionStatus.ISSUED, issued.getStatus());

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());

        Notification n = notifications.get(0);
        assertEquals(NotificationType.PRESCRIPTION_ISSUED, n.getType());
        assertEquals("Prescription Issued", n.getTitle());
        assertTrue(n.getMessage().contains(prescription.getPrescriptionCode()));
        assertTrue(n.getMessage().contains("John Doe"));
        assertEquals("PRESCRIPTION", n.getReferenceType());
        assertEquals(prescription.getId(), n.getReferenceId());
        assertEquals(doctorA.getUser().getId(), n.getRecipient().getId());
        assertEquals(testHospitalA.getId(), n.getHospital().getId());
        assertEquals(NotificationPriority.NORMAL, n.getPriority());
        assertEquals(NotificationStatus.UNREAD, n.getStatus());
        assertNotNull(n.getCreatedAt());
        assertNull(n.getReadAt());
    }

    @Test
    @DisplayName("2. issuePrescription_alreadyIssued_doesNotDuplicateNotification")
    void issuePrescription_alreadyIssued_doesNotDuplicateNotification() {
        Consultation consultation = createTestConsultation();
        Prescription prescription = createTestPrescription(consultation);

        authenticate(doctorEmailA);
        prescriptionService.issuePrescription(prescription.getId());

        assertEquals(1, notificationRepository.findAll().size());

        // Attempting to issue again throws IllegalStateException and must not create duplicate notification
        assertThrows(IllegalStateException.class, () -> {
            prescriptionService.issuePrescription(prescription.getId());
        });

        assertEquals(1, notificationRepository.findAll().size(), "Duplicate notification must not be created");
    }

    @Test
    @DisplayName("3. cancelPrescription_doesNotCreatePrescriptionIssuedNotification")
    void cancelPrescription_doesNotCreatePrescriptionIssuedNotification() {
        Consultation consultation = createTestConsultation();
        Prescription prescription = createTestPrescription(consultation);

        authenticate(doctorEmailA);
        prescriptionService.cancelPrescription(prescription.getId());

        assertEquals(0, notificationRepository.findAll().size(), "Cancelling prescription should not trigger PRESCRIPTION_ISSUED");
    }

    // ==========================================
    // 2. CONSULTATION NOTIFICATIONS
    // ==========================================

    @Test
    @DisplayName("4. completeConsultation_createsConsultationCompletedNotification")
    void completeConsultation_createsConsultationCompletedNotification() {
        Consultation consultation = createTestConsultation();

        authenticate(doctorEmailA);
        ConsultationResponse completed = consultationService.completeConsultation(consultation.getId());
        assertEquals(ConsultationStatus.COMPLETED, completed.getStatus());

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());

        Notification n = notifications.get(0);
        assertEquals(NotificationType.CONSULTATION_COMPLETED, n.getType());
        assertEquals("Consultation Completed", n.getTitle());
        assertTrue(n.getMessage().contains("#" + consultation.getId()));
        assertTrue(n.getMessage().contains("John Doe"));
        assertEquals("CONSULTATION", n.getReferenceType());
        assertEquals(consultation.getId(), n.getReferenceId());
        assertEquals(doctorA.getUser().getId(), n.getRecipient().getId());
        assertEquals(testHospitalA.getId(), n.getHospital().getId());
        assertEquals(NotificationPriority.NORMAL, n.getPriority());
        assertEquals(NotificationStatus.UNREAD, n.getStatus());
    }

    @Test
    @DisplayName("5. completeConsultation_alreadyCompleted_doesNotDuplicateNotification")
    void completeConsultation_alreadyCompleted_doesNotDuplicateNotification() {
        Consultation consultation = createTestConsultation();

        authenticate(doctorEmailA);
        consultationService.completeConsultation(consultation.getId());

        assertEquals(1, notificationRepository.findAll().size());

        // Attempting to complete already completed consultation throws IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            consultationService.completeConsultation(consultation.getId());
        });

        assertEquals(1, notificationRepository.findAll().size(), "Duplicate notification must not be created");
    }

    // ==========================================
    // 3. BILLING / PAYMENT NOTIFICATIONS
    // ==========================================

    @Test
    @DisplayName("6. addPayment_createsPaymentReceivedNotification")
    void addPayment_createsPaymentReceivedNotification() {
        Consultation consultation = createTestConsultation();
        Invoice invoice = createTestInvoice(consultation);

        authenticate(doctorEmailA);
        billingService.issueInvoice(invoice.getId());

        CreatePaymentRequest payReq = new CreatePaymentRequest();
        payReq.setAmount(new BigDecimal("500.00"));
        payReq.setPaymentMethod(PaymentMethod.CASH);
        payReq.setTransactionReference("TXN-001");
        payReq.setNotes("Full cash payment");

        PaymentResponse paymentResp = billingService.addPayment(invoice.getId(), payReq);
        assertNotNull(paymentResp.getId());

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());

        Notification n = notifications.get(0);
        assertEquals(NotificationType.PAYMENT_RECEIVED, n.getType());
        assertEquals("Payment Received", n.getTitle());
        assertTrue(n.getMessage().contains("500.00"));
        assertTrue(n.getMessage().contains(invoice.getInvoiceNumber()));
        assertTrue(n.getMessage().contains("John Doe"));
        assertEquals("PAYMENT", n.getReferenceType());
        assertEquals(paymentResp.getId(), n.getReferenceId());
        assertEquals(doctorA.getUser().getId(), n.getRecipient().getId());
        assertEquals(testHospitalA.getId(), n.getHospital().getId());
        assertEquals(NotificationPriority.NORMAL, n.getPriority());
        assertEquals(NotificationStatus.UNREAD, n.getStatus());
    }

    @Test
    @DisplayName("7. addPayment_multiplePartialPayments_createIndividualNotifications")
    void addPayment_multiplePartialPayments_createIndividualNotifications() {
        Consultation consultation = createTestConsultation();
        Invoice invoice = createTestInvoice(consultation);

        authenticate(doctorEmailA);
        billingService.issueInvoice(invoice.getId());

        // First partial payment $200
        CreatePaymentRequest pay1 = new CreatePaymentRequest();
        pay1.setAmount(new BigDecimal("200.00"));
        pay1.setPaymentMethod(PaymentMethod.UPI);
        PaymentResponse resp1 = billingService.addPayment(invoice.getId(), pay1);

        assertEquals(1, notificationRepository.findAll().size());
        Notification n1 = notificationRepository.findAll().get(0);
        assertEquals(resp1.getId(), n1.getReferenceId());
        assertTrue(n1.getMessage().contains("200.00"));

        // Second partial payment $300 (full remaining)
        CreatePaymentRequest pay2 = new CreatePaymentRequest();
        pay2.setAmount(new BigDecimal("300.00"));
        pay2.setPaymentMethod(PaymentMethod.CARD);
        PaymentResponse resp2 = billingService.addPayment(invoice.getId(), pay2);

        List<Notification> allNotifications = notificationRepository.findAll();
        assertEquals(2, allNotifications.size());

        Notification n2 = allNotifications.stream().filter(n -> n.getReferenceId().equals(resp2.getId())).findFirst().orElseThrow();
        assertEquals(NotificationType.PAYMENT_RECEIVED, n2.getType());
        assertTrue(n2.getMessage().contains("300.00"));
    }

    @Test
    @DisplayName("8. addPayment_alreadyPaidInvoice_rejectsAndDoesNotDuplicate")
    void addPayment_alreadyPaidInvoice_rejectsAndDoesNotDuplicate() {
        Consultation consultation = createTestConsultation();
        Invoice invoice = createTestInvoice(consultation);

        authenticate(doctorEmailA);
        billingService.issueInvoice(invoice.getId());

        CreatePaymentRequest fullPay = new CreatePaymentRequest();
        fullPay.setAmount(new BigDecimal("500.00"));
        fullPay.setPaymentMethod(PaymentMethod.CASH);
        billingService.addPayment(invoice.getId(), fullPay);

        assertEquals(1, notificationRepository.findAll().size());

        // Overpaying/adding payment to fully paid invoice throws IllegalStateException
        CreatePaymentRequest excessPay = new CreatePaymentRequest();
        excessPay.setAmount(new BigDecimal("50.00"));
        excessPay.setPaymentMethod(PaymentMethod.CASH);

        assertThrows(IllegalStateException.class, () -> {
            billingService.addPayment(invoice.getId(), excessPay);
        });

        assertEquals(1, notificationRepository.findAll().size(), "No additional notification should be created");
    }

    // ==========================================
    // 4. SECURITY, ISOLATION & QUERY VERIFICATION
    // ==========================================

    @Test
    @DisplayName("9. doctorRecipientIsolation_andHospitalIsolation")
    void doctorRecipientIsolation_andHospitalIsolation() {
        Consultation consultation = createTestConsultation();
        Prescription prescription = createTestPrescription(consultation);

        authenticate(doctorEmailA);
        prescriptionService.issuePrescription(prescription.getId());

        // Doctor A has 1 notification and unread count 1
        authenticate(doctorEmailA);
        Page<NotificationResponse> docANotifs = notificationService.getMyNotifications(PageRequest.of(0, 10));
        assertEquals(1, docANotifs.getTotalElements());
        assertEquals(1, notificationService.getUnreadCount());

        // Doctor B in Hospital B has 0 notifications and unread count 0
        authenticate(doctorEmailB);
        Page<NotificationResponse> docBNotifs = notificationService.getMyNotifications(PageRequest.of(0, 10));
        assertEquals(0, docBNotifs.getTotalElements());
        assertEquals(0, notificationService.getUnreadCount());
    }

    @Test
    @DisplayName("10. markAsRead_reducesUnreadCount")
    void markAsRead_reducesUnreadCount() {
        Consultation consultation = createTestConsultation();

        authenticate(doctorEmailA);
        consultationService.completeConsultation(consultation.getId());

        assertEquals(1, notificationService.getUnreadCount());
        Notification n = notificationRepository.findAll().get(0);

        NotificationResponse updated = notificationService.markAsRead(n.getId());
        assertEquals(NotificationStatus.READ, updated.getStatus());
        assertNotNull(updated.getReadAt());
        assertEquals(0, notificationService.getUnreadCount());
    }

    @Test
    @DisplayName("11. invalidStateTransitions_preventNotificationCreation")
    void invalidStateTransitions_preventNotificationCreation() {
        Consultation consultation = createTestConsultation();
        Invoice invoice = createTestInvoice(consultation);

        authenticate(doctorEmailA);
        // Cancel invoice
        billingService.cancelInvoice(invoice.getId());

        // Attempting payment on cancelled invoice throws IllegalStateException
        CreatePaymentRequest pay = new CreatePaymentRequest();
        pay.setAmount(new BigDecimal("100.00"));
        pay.setPaymentMethod(PaymentMethod.CASH);

        assertThrows(IllegalStateException.class, () -> {
            billingService.addPayment(invoice.getId(), pay);
        });

        assertEquals(0, notificationRepository.findAll().size());
    }

    @Test
    @DisplayName("12. allThreeNotificationTypes_canCoexistInSinglePatientWorkflow")
    void allThreeNotificationTypes_canCoexistInSinglePatientWorkflow() {
        // Complete consultation -> CONSULTATION_COMPLETED
        Consultation consultation = createTestConsultation();
        authenticate(doctorEmailA);
        consultationService.completeConsultation(consultation.getId());

        // Issue prescription -> PRESCRIPTION_ISSUED
        Prescription prescription = createTestPrescription(consultation);
        prescriptionService.issuePrescription(prescription.getId());

        // Issue invoice and record payment -> PAYMENT_RECEIVED
        Invoice invoice = createTestInvoice(consultation);
        billingService.issueInvoice(invoice.getId());
        CreatePaymentRequest payReq = new CreatePaymentRequest();
        payReq.setAmount(new BigDecimal("500.00"));
        payReq.setPaymentMethod(PaymentMethod.CASH);
        billingService.addPayment(invoice.getId(), payReq);

        // Verify all 3 notifications created for Doctor A
        Page<NotificationResponse> notifs = notificationService.getMyNotifications(PageRequest.of(0, 10));
        assertEquals(3, notifs.getTotalElements());

        List<NotificationResponse> list = notifs.getContent();
        assertTrue(list.stream().anyMatch(n -> n.getType() == NotificationType.CONSULTATION_COMPLETED));
        assertTrue(list.stream().anyMatch(n -> n.getType() == NotificationType.PRESCRIPTION_ISSUED));
        assertTrue(list.stream().anyMatch(n -> n.getType() == NotificationType.PAYMENT_RECEIVED));

        assertEquals(3, notificationService.getUnreadCount());
    }
}
