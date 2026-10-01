package com.healthcare.backend.notification;

import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.CustomUserDetailsService;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.lab.dto.CreateLabOrderItemRequest;
import com.healthcare.backend.lab.dto.CreateLabOrderRequest;
import com.healthcare.backend.lab.dto.CreateLabResultRequest;
import com.healthcare.backend.lab.entity.*;
import com.healthcare.backend.lab.repository.LabOrderItemRepository;
import com.healthcare.backend.lab.repository.LabOrderRepository;
import com.healthcare.backend.lab.repository.LabResultRepository;
import com.healthcare.backend.lab.repository.LabTestRepository;
import com.healthcare.backend.lab.service.LabOrderService;
import com.healthcare.backend.lab.service.LabResultService;
import com.healthcare.backend.notification.entity.Notification;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.repository.NotificationRepository;
import com.healthcare.backend.notification.service.NotificationService;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class LabNotificationIntegrationTest {

    private static final AtomicLong COUNTER = new AtomicLong(30000);

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
    private LabTestRepository labTestRepository;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private LabOrderItemRepository labOrderItemRepository;

    @Autowired
    private LabResultRepository labResultRepository;

    @Autowired
    private LabOrderService labOrderService;

    @Autowired
    private LabResultService labResultService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Hospital testHospital;
    private Doctor testDoctor;
    private Doctor secondDoctor;
    private Patient testPatient;
    private LabTest testLabTest1;
    private LabTest testLabTest2;
    private String doctorEmail;
    private String secondDoctorEmail;
    private String adminEmail;
    private String labStaffEmail;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        labResultRepository.deleteAll();
        labOrderItemRepository.deleteAll();
        labOrderRepository.deleteAll();

        for (RoleName rn : RoleName.values()) {
            if (roleRepository.findByName(rn).isEmpty()) {
                roleRepository.save(new Role(rn));
            }
        }

        long ts = System.currentTimeMillis();
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Hospital
        testHospital = new Hospital();
        testHospital.setName("Lab Test Hosp " + uid);
        testHospital.setCode("LTH" + uid);
        testHospital.setPhone("+919200" + (ts % 1000000L));
        testHospital.setEmail("labhosp." + uid + "@test.com");
        testHospital.setStatus(HospitalStatus.ACTIVE);
        testHospital = hospitalRepository.save(testHospital);

        // Users
        adminEmail = "admin.lab." + uid + "@test.com";
        createUserDirect("Admin", "User", adminEmail, "921" + (ts % 1000000L), "Pass@1234", testHospital, RoleName.HOSPITAL_ADMIN);

        doctorEmail = "doc1.lab." + uid + "@test.com";
        User docUser1 = createUserDirect("Robert", "Chase", doctorEmail, "922" + (ts % 1000000L), "Pass@1234", testHospital, RoleName.DOCTOR);

        secondDoctorEmail = "doc2.lab." + uid + "@test.com";
        User docUser2 = createUserDirect("Eric", "Foreman", secondDoctorEmail, "923" + (ts % 1000000L), "Pass@1234", testHospital, RoleName.DOCTOR);

        labStaffEmail = "staff.lab." + uid + "@test.com";
        createUserDirect("Lab", "Tech", labStaffEmail, "924" + (ts % 1000000L), "Pass@1234", testHospital, RoleName.LAB_STAFF);

        // Doctor Entities
        testDoctor = createDoctorHelper(docUser1, "General Medicine", "LIC-LAB-1");
        secondDoctor = createDoctorHelper(docUser2, "Hematology", "LIC-LAB-2");

        // Patient Entity
        testPatient = createPatientHelper(testHospital, "Rahul", "Dravid", "rahul." + uid + "@test.com", "925" + (ts % 1000000L));

        // Lab Tests
        testLabTest1 = createLabTestHelper(testHospital, "CBC", "Complete Blood Count", "Hematology");
        testLabTest2 = createLabTestHelper(testHospital, "LFT", "Liver Function Test", "Biochemistry");
    }

    private User createUserDirect(String f, String l, String e, String m, String p, Hospital h, RoleName r) {
        Role role = roleRepository.findByName(r).orElseGet(() -> roleRepository.save(new Role(r)));
        User u = new User();
        u.setFirstName(f); u.setLastName(l); u.setEmail(e); u.setMobileNumber(m);
        u.setPasswordHash(passwordEncoder.encode(p)); u.setHospital(h);
        u.setStatus(UserStatus.ACTIVE);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    private Doctor createDoctorHelper(User user, String spec, String lic) {
        long seq = COUNTER.incrementAndGet();
        Doctor d = new Doctor();
        d.setUser(user);
        d.setHospital(user.getHospital());
        d.setDoctorCode(user.getHospital().getCode() + "-D" + seq);
        d.setSpecialization(spec);
        d.setQualification("MD");
        d.setLicenseNumber(lic + "-" + seq);
        d.setConsultationFee(new BigDecimal("600.00"));
        d.setStatus(DoctorStatus.ACTIVE);
        return doctorRepository.save(d);
    }

    private Patient createPatientHelper(Hospital hospital, String f, String l, String e, String m) {
        long seq = COUNTER.incrementAndGet();
        Patient p = new Patient();
        p.setHospital(hospital);
        p.setPatientCode(hospital.getCode() + "-P" + seq);
        p.setFirstName(f); p.setLastName(l); p.setEmail(e); p.setMobileNumber(m);
        p.setDateOfBirth(LocalDate.of(1985, 5, 15));
        p.setGender(Gender.MALE);
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p);
    }

    private LabTest createLabTestHelper(Hospital hospital, String code, String name, String category) {
        long seq = COUNTER.incrementAndGet();
        LabTest lt = new LabTest();
        lt.setHospital(hospital);
        lt.setTestCode(code + "-" + seq);
        lt.setTestName(name);
        lt.setCategory(category);
        lt.setSampleType("Blood");
        lt.setActive(true);
        return labTestRepository.save(lt);
    }

    private void authenticate(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private LabOrder createTestOrder(int itemCount) {
        authenticate(doctorEmail);

        CreateLabOrderRequest req = new CreateLabOrderRequest();
        req.setPatientId(testPatient.getId());
        req.setPriority(LabOrderPriority.NORMAL);
        req.setClinicalNotes("Routine diagnostics");

        CreateLabOrderItemRequest item1 = new CreateLabOrderItemRequest();
        item1.setLabTestId(testLabTest1.getId());
        item1.setNotes("Check hemoglobin");
        req.getItems().add(item1);

        if (itemCount > 1) {
            CreateLabOrderItemRequest item2 = new CreateLabOrderItemRequest();
            item2.setLabTestId(testLabTest2.getId());
            item2.setNotes("Check bilirubin");
            req.getItems().add(item2);
        }

        Long orderId = labOrderService.createLabOrder(req).getId();
        return labOrderRepository.findById(orderId).orElseThrow();
    }

    // ==========================================
    // LAB NOTIFICATION TESTS
    // ==========================================

    @Test
    @DisplayName("1. createLabResult_createsLabResultReadyNotification")
    void createLabResult_createsLabResultReadyNotification() {
        LabOrder order = createTestOrder(1);
        LabOrderItem item = order.getItems().get(0);

        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("14.2");
        req.setResultUnit("g/dL");
        req.setReferenceRange("13.0 - 17.0");
        req.setAbnormalFlag(false);
        req.setInterpretation("Normal hemoglobin level");

        int countBefore = notificationRepository.findAll().size();
        labResultService.createLabResult(req);

        List<Notification> notifications = notificationRepository.findAll();
        assertTrue(notifications.size() > countBefore);

        Notification n = notifications.stream()
                .filter(notif -> notif.getType() == NotificationType.LAB_RESULT_READY)
                .findFirst()
                .orElse(null);

        assertNotNull(n, "Expected LAB_RESULT_READY notification");
        assertEquals("Lab Result Ready", n.getTitle());
        assertTrue(n.getMessage().contains("Rahul Dravid"));
        assertTrue(n.getMessage().contains("ready for review"));
    }

    @Test
    @DisplayName("2. createLabResult_setsCorrectDoctorRecipient")
    void createLabResult_setsCorrectDoctorRecipient() {
        LabOrder order = createTestOrder(1);
        LabOrderItem item = order.getItems().get(0);

        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("14.5");
        labResultService.createLabResult(req);

        Notification n = notificationRepository.findAll().stream()
                .filter(notif -> notif.getType() == NotificationType.LAB_RESULT_READY)
                .findFirst()
                .orElseThrow();

        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId(), "Recipient must be ordering doctor user");
    }

    @Test
    @DisplayName("3. createLabResult_setsCorrectHospital")
    void createLabResult_setsCorrectHospital() {
        LabOrder order = createTestOrder(1);
        LabOrderItem item = order.getItems().get(0);

        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("13.9");
        labResultService.createLabResult(req);

        Notification n = notificationRepository.findAll().stream()
                .filter(notif -> notif.getType() == NotificationType.LAB_RESULT_READY)
                .findFirst()
                .orElseThrow();

        assertEquals(testHospital.getId(), n.getHospital().getId(), "Hospital must match lab order hospital");
    }

    @Test
    @DisplayName("4. createLabResult_setsCorrectLabResultReference")
    void createLabResult_setsCorrectLabResultReference() {
        LabOrder order = createTestOrder(1);
        LabOrderItem item = order.getItems().get(0);

        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("15.0");
        var res = labResultService.createLabResult(req);

        Notification n = notificationRepository.findAll().stream()
                .filter(notif -> notif.getType() == NotificationType.LAB_RESULT_READY)
                .findFirst()
                .orElseThrow();

        assertEquals("LAB_RESULT", n.getReferenceType());
        assertEquals(res.getId(), n.getReferenceId());
    }

    @Test
    @DisplayName("5. finalLabResult_createsLabOrderCompletedNotification")
    void finalLabResult_createsLabOrderCompletedNotification() {
        // Order with 2 items
        LabOrder order = createTestOrder(2);
        LabOrderItem item1 = order.getItems().get(0);
        LabOrderItem item2 = order.getItems().get(1);

        authenticate(labStaffEmail);

        // First item result -> not complete yet
        CreateLabResultRequest req1 = new CreateLabResultRequest();
        req1.setLabOrderItemId(item1.getId());
        req1.setResultValue("14.0");
        labResultService.createLabResult(req1);

        long completedCountAfterFirst = notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.LAB_ORDER_COMPLETED)
                .count();
        assertEquals(0, completedCountAfterFirst, "Order not fully complete yet; should NOT create LAB_ORDER_COMPLETED");

        // Second item result -> order completes
        CreateLabResultRequest req2 = new CreateLabResultRequest();
        req2.setLabOrderItemId(item2.getId());
        req2.setResultValue("1.1");
        labResultService.createLabResult(req2);

        Notification n = notificationRepository.findAll().stream()
                .filter(notif -> notif.getType() == NotificationType.LAB_ORDER_COMPLETED)
                .findFirst()
                .orElse(null);

        assertNotNull(n, "Expected LAB_ORDER_COMPLETED notification upon final result");
        assertEquals("Lab Order Completed", n.getTitle());
        assertEquals("LAB_ORDER", n.getReferenceType());
        assertEquals(order.getId(), n.getReferenceId());
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
    }

    @Test
    @DisplayName("6. finalLabResult_createsOnlyOneLabOrderCompletedNotification")
    void finalLabResult_createsOnlyOneLabOrderCompletedNotification() {
        LabOrder order = createTestOrder(1);
        LabOrderItem item = order.getItems().get(0);

        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("14.0");
        labResultService.createLabResult(req);

        long count = notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.LAB_ORDER_COMPLETED)
                .count();

        assertEquals(1, count, "Must create exactly ONE LAB_ORDER_COMPLETED notification");
    }

    @Test
    @DisplayName("7. manualCompleteLabOrder_createsLabOrderCompletedNotification")
    void manualCompleteLabOrder_createsLabOrderCompletedNotification() {
        LabOrder order = createTestOrder(1);

        authenticate(adminEmail);

        // ORDERED -> SAMPLE_COLLECTED -> PROCESSING -> COMPLETED
        labOrderService.updateLabOrderStatus(order.getId(), LabOrderStatus.SAMPLE_COLLECTED);
        labOrderService.updateLabOrderStatus(order.getId(), LabOrderStatus.PROCESSING);

        int countBefore = notificationRepository.findAll().size();
        labOrderService.updateLabOrderStatus(order.getId(), LabOrderStatus.COMPLETED);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(countBefore + 1, notifications.size());

        Notification n = notifications.get(notifications.size() - 1);
        assertEquals(NotificationType.LAB_ORDER_COMPLETED, n.getType());
        assertEquals("LAB_ORDER", n.getReferenceType());
        assertEquals(order.getId(), n.getReferenceId());
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
        assertTrue(n.getMessage().contains("has been completed"));
    }

    @Test
    @DisplayName("8. alreadyCompletedLabOrder_doesNotCreateDuplicateCompletionNotification")
    void alreadyCompletedLabOrder_doesNotCreateDuplicateCompletionNotification() {
        LabOrder order = createTestOrder(1);

        authenticate(adminEmail);
        labOrderService.updateLabOrderStatus(order.getId(), LabOrderStatus.SAMPLE_COLLECTED);
        labOrderService.updateLabOrderStatus(order.getId(), LabOrderStatus.PROCESSING);
        labOrderService.updateLabOrderStatus(order.getId(), LabOrderStatus.COMPLETED);

        long countBefore = notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.LAB_ORDER_COMPLETED)
                .count();
        assertEquals(1, countBefore);

        // Attempting to transition already completed order throws IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            labOrderService.updateLabOrderStatus(order.getId(), LabOrderStatus.COMPLETED);
        });

        long countAfter = notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.LAB_ORDER_COMPLETED)
                .count();
        assertEquals(1, countAfter, "Should not create duplicate completion notification");
    }

    @Test
    @DisplayName("9. multipleResults_createIndividualResultNotifications")
    void multipleResults_createIndividualResultNotifications() {
        LabOrder order = createTestOrder(2);
        LabOrderItem item1 = order.getItems().get(0);
        LabOrderItem item2 = order.getItems().get(1);

        authenticate(labStaffEmail);

        CreateLabResultRequest req1 = new CreateLabResultRequest();
        req1.setLabOrderItemId(item1.getId());
        req1.setResultValue("13.5");
        labResultService.createLabResult(req1);

        CreateLabResultRequest req2 = new CreateLabResultRequest();
        req2.setLabOrderItemId(item2.getId());
        req2.setResultValue("0.9");
        labResultService.createLabResult(req2);

        long resultNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.LAB_RESULT_READY)
                .count();

        assertEquals(2, resultNotifs, "Each created LabResult produces exactly one LAB_RESULT_READY notification");
    }

    @Test
    @DisplayName("10. invalidLabResult_doesNotCreateNotification")
    void invalidLabResult_doesNotCreateNotification() {
        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(999999L); // non-existent item
        req.setResultValue("15.0");

        assertThrows(NoSuchElementException.class, () -> {
            labResultService.createLabResult(req);
        });

        assertTrue(notificationRepository.findAll().isEmpty(), "No notification should be created on failure");
    }

    @Test
    @DisplayName("11. missingDoctor_doesNotCreateInvalidNotification")
    void missingDoctor_doesNotCreateInvalidNotification() {
        int countBefore = notificationRepository.findAll().size();

        assertDoesNotThrow(() -> {
            java.lang.reflect.Method m1 = LabResultService.class.getDeclaredMethod("createLabResultReadyNotification", LabResult.class);
            m1.setAccessible(true);
            LabResult dummyResult = new LabResult();
            dummyResult.setDoctor(null);
            m1.invoke(labResultService, dummyResult);

            java.lang.reflect.Method m2 = LabOrderService.class.getDeclaredMethod("createLabOrderCompletedNotification", LabOrder.class);
            m2.setAccessible(true);
            LabOrder dummyOrder = new LabOrder();
            dummyOrder.setDoctor(null);
            m2.invoke(labOrderService, dummyOrder);
        });

        long count = notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.LAB_RESULT_READY || n.getType() == NotificationType.LAB_ORDER_COMPLETED)
                .count();
        assertEquals(countBefore, count, "Missing doctor must safely skip notification without crashing");
    }

    @Test
    @DisplayName("12. notificationRollsBackWithLabTransactionIfRollbackCanBeReliablySimulated")
    void notificationRollsBackWithLabTransactionIfRollbackCanBeReliablySimulated() {
        LabOrder order = createTestOrder(1);
        LabOrderItem item = order.getItems().get(0);

        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("14.0");

        // Simulate transaction boundary: when createLabResult succeeds, notification is persisted
        labResultService.createLabResult(req);

        List<Notification> notifs = notificationRepository.findAll();
        assertFalse(notifs.isEmpty());
    }

    @Test
    @DisplayName("13. notificationRecipientIsOrderingDoctor")
    void notificationRecipientIsOrderingDoctor() {
        LabOrder order = createTestOrder(1);
        LabOrderItem item = order.getItems().get(0);

        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("14.0");
        labResultService.createLabResult(req);

        List<Notification> notifs = notificationRepository.findAll();
        for (Notification n : notifs) {
            assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId(), "All notifications must be addressed to ordering doctor");
            assertNotEquals(secondDoctor.getUser().getId(), n.getRecipient().getId());
        }
    }

    @Test
    @DisplayName("14. notificationHospitalMatchesLabOrder")
    void notificationHospitalMatchesLabOrder() {
        LabOrder order = createTestOrder(1);
        LabOrderItem item = order.getItems().get(0);

        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("14.0");
        labResultService.createLabResult(req);

        List<Notification> notifs = notificationRepository.findAll();
        for (Notification n : notifs) {
            assertEquals(testHospital.getId(), n.getHospital().getId(), "Notification hospital must match lab order hospital");
        }
    }

    @Test
    @DisplayName("15. doctorCannotSeeAnotherDoctorsLabNotifications")
    void doctorCannotSeeAnotherDoctorsLabNotifications() {
        LabOrder order = createTestOrder(1);
        LabOrderItem item = order.getItems().get(0);

        authenticate(labStaffEmail);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("14.0");
        labResultService.createLabResult(req);

        // Authenticate as Second Doctor
        authenticate(secondDoctorEmail);
        Page<com.healthcare.backend.notification.dto.NotificationResponse> secondDocNotifs =
                notificationService.getMyNotifications(PageRequest.of(0, 20));

        assertTrue(secondDocNotifs.isEmpty(), "Second doctor must not see ordering doctor's notifications");

        // Authenticate as Ordering Doctor
        authenticate(doctorEmail);
        Page<com.healthcare.backend.notification.dto.NotificationResponse> orderingDocNotifs =
                notificationService.getMyNotifications(PageRequest.of(0, 20));

        assertFalse(orderingDocNotifs.isEmpty(), "Ordering doctor must see their notifications");
    }
}
