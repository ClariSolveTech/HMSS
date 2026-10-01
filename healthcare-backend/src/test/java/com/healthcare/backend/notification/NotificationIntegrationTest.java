package com.healthcare.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.notification.dto.CreateNotificationRequest;
import com.healthcare.backend.notification.dto.NotificationResponse;
import com.healthcare.backend.notification.entity.Notification;
import com.healthcare.backend.notification.entity.NotificationPriority;
import com.healthcare.backend.notification.entity.NotificationStatus;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.repository.NotificationRepository;
import com.healthcare.backend.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NotificationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private User doctorUserA1;
    private User doctorUserA2;
    private User doctorUserB;

    private String tokenDoctorA1;
    private String tokenDoctorA2;
    private String tokenDoctorB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        notificationRepository.deleteAll();
        SecurityContextHolder.clearContext();

        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
            }
        }

        long ts = System.currentTimeMillis();

        // Hospital A
        hospitalA = new Hospital();
        hospitalA.setName("Notification Hospital A " + ts);
        hospitalA.setCode("NHA" + (ts % 10000));
        hospitalA.setPhone("+919200000001");
        hospitalA.setEmail("hospitalA." + ts + "@test.com");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        // Hospital B
        hospitalB = new Hospital();
        hospitalB.setName("Notification Hospital B " + ts);
        hospitalB.setCode("NHB" + (ts % 10000));
        hospitalB.setPhone("+919200000002");
        hospitalB.setEmail("hospitalB." + ts + "@test.com");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        // Users
        doctorUserA1 = createUser("DoctorA1", "One", "doca1." + ts + "@test.com", "991" + (ts % 10000000L), hospitalA, RoleName.DOCTOR);
        doctorUserA2 = createUser("DoctorA2", "Two", "doca2." + ts + "@test.com", "992" + (ts % 10000000L), hospitalA, RoleName.DOCTOR);
        doctorUserB = createUser("DoctorB", "Beta", "docb." + ts + "@test.com", "993" + (ts % 10000000L), hospitalB, RoleName.DOCTOR);

        tokenDoctorA1 = jwtService.generateToken(doctorUserA1);
        tokenDoctorA2 = jwtService.generateToken(doctorUserA2);
        tokenDoctorB = jwtService.generateToken(doctorUserB);
    }

    private User createUser(String firstName, String lastName, String email, String mobile, Hospital hospital, RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setMobileNumber(mobile);
        u.setPasswordHash(passwordEncoder.encode("Pass@1234"));
        u.setHospital(hospital);
        u.setStatus(UserStatus.ACTIVE);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    private void setSecurityContext(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private NotificationResponse createNotificationFor(User recipient, NotificationType type, String title, String message, NotificationPriority priority, String refType, Long refId) {
        setSecurityContext(recipient);
        CreateNotificationRequest req = CreateNotificationRequest.builder()
                .recipientUserId(recipient.getId())
                .hospitalId(recipient.getHospital().getId())
                .type(type)
                .title(title)
                .message(message)
                .priority(priority)
                .referenceType(refType)
                .referenceId(refId)
                .build();
        NotificationResponse resp = notificationService.createNotification(req);
        SecurityContextHolder.clearContext();
        return resp;
    }

    // ==========================================
    // BASIC FUNCTIONALITY TESTS (1 - 10)
    // ==========================================

    @Test
    @DisplayName("1. Create Notification")
    void test1_createNotification() {
        NotificationResponse response = createNotificationFor(
                doctorUserA1,
                NotificationType.APPOINTMENT_CONFIRMED,
                "Appointment Confirmed",
                "Your appointment with patient John has been confirmed.",
                NotificationPriority.HIGH,
                "APPOINTMENT",
                101L
        );

        assertNotNull(response.getId());
        assertEquals(NotificationType.APPOINTMENT_CONFIRMED, response.getType());
        assertEquals("Appointment Confirmed", response.getTitle());
        assertEquals(NotificationStatus.UNREAD, response.getStatus());
        assertEquals(NotificationPriority.HIGH, response.getPriority());
        assertEquals("APPOINTMENT", response.getReferenceType());
        assertEquals(101L, response.getReferenceId());
        assertNotNull(response.getCreatedAt());
        assertNull(response.getReadAt());
    }

    @Test
    @DisplayName("2. Get Notification")
    void test2_getNotification() throws Exception {
        NotificationResponse created = createNotificationFor(
                doctorUserA1,
                NotificationType.LAB_RESULT_READY,
                "Lab Result Ready",
                "Lab result for patient is ready.",
                NotificationPriority.NORMAL,
                "LAB_RESULT",
                202L
        );

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications/{id}", created.getId())
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.title").value("Lab Result Ready"))
                .andExpect(jsonPath("$.type").value("LAB_RESULT_READY"));
    }

    @Test
    @DisplayName("3. List Notifications")
    void test3_listNotifications() throws Exception {
        createNotificationFor(doctorUserA1, NotificationType.PATIENT_CHECKED_IN, "Patient Checked In 1", "Patient 1 checked in.", NotificationPriority.NORMAL, null, null);
        createNotificationFor(doctorUserA1, NotificationType.PATIENT_CHECKED_IN, "Patient Checked In 2", "Patient 2 checked in.", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("4. List Unread Notifications")
    void test4_listUnreadNotifications() throws Exception {
        NotificationResponse n1 = createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "System Update", "Maintenance scheduled.", NotificationPriority.LOW, null, null);
        createNotificationFor(doctorUserA1, NotificationType.PRESCRIPTION_ISSUED, "Prescription Issued", "Prescription #123 issued.", NotificationPriority.NORMAL, "PRESCRIPTION", 123L);

        setSecurityContext(doctorUserA1);
        notificationService.markAsRead(n1.getId());
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/notifications/unread")
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Prescription Issued"));
    }

    @Test
    @DisplayName("5. Get Unread Count")
    void test5_getUnreadCount() throws Exception {
        createNotificationFor(doctorUserA1, NotificationType.QUEUE_UPDATE, "Queue Update", "Queue position changed.", NotificationPriority.NORMAL, null, null);
        createNotificationFor(doctorUserA1, NotificationType.PAYMENT_RECEIVED, "Payment Received", "Payment received for invoice #456.", NotificationPriority.NORMAL, "PAYMENT", 456L);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @DisplayName("6. Mark Notification as Read")
    void test6_markNotificationAsRead() throws Exception {
        NotificationResponse created = createNotificationFor(doctorUserA1, NotificationType.CONSULTATION_COMPLETED, "Consultation Completed", "Consultation completed.", NotificationPriority.NORMAL, "CONSULTATION", 789L);

        SecurityContextHolder.clearContext();
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", created.getId())
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.readAt").exists());
    }

    @Test
    @DisplayName("7. Mark All as Read")
    void test7_markAllAsRead() throws Exception {
        createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Title 1", "Msg 1", NotificationPriority.NORMAL, null, null);
        createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Title 2", "Msg 2", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));

        setSecurityContext(doctorUserA1);
        assertEquals(0, notificationService.getUnreadCount());
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("8. Pagination")
    void test8_pagination() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Notification " + i, "Message " + i, NotificationPriority.NORMAL, null, null);
        }

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications?page=0&size=2")
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("9. New Notifications Default to UNREAD")
    void test9_newNotificationsDefaultToUnread() {
        NotificationResponse response = createNotificationFor(doctorUserA1, NotificationType.LAB_ORDER_COMPLETED, "Lab Order Completed", "Lab order #888 completed.", NotificationPriority.NORMAL, "LAB_ORDER", 888L);

        assertEquals(NotificationStatus.UNREAD, response.getStatus());
        assertNull(response.getReadAt());
    }

    @Test
    @DisplayName("10. readAt Persistence")
    void test10_readAtPersistence() {
        NotificationResponse created = createNotificationFor(doctorUserA1, NotificationType.APPOINTMENT_RESCHEDULED, "Appointment Rescheduled", "Appointment rescheduled to tomorrow.", NotificationPriority.NORMAL, "APPOINTMENT", 999L);

        setSecurityContext(doctorUserA1);
        NotificationResponse read = notificationService.markAsRead(created.getId());
        assertNotNull(read.getReadAt());

        Notification inDb = notificationRepository.findById(created.getId()).orElseThrow();
        assertEquals(NotificationStatus.READ, inDb.getStatus());
        assertNotNull(inDb.getReadAt());
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // SECURITY TESTS (11 - 16)
    // ==========================================

    @Test
    @DisplayName("11. Unauthenticated Request -> 401")
    void test11_unauthenticatedRequest_returns401() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("12. Doctor Can Access Own Notification")
    void test12_doctorCanAccessOwnNotification() throws Exception {
        NotificationResponse created = createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Personal Notification", "For Doctor A1 only.", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications/{id}", created.getId())
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()));
    }

    @Test
    @DisplayName("13. Doctor Cannot Access Another User's Notification")
    void test13_doctorCannotAccessAnotherUsersNotification() throws Exception {
        NotificationResponse createdA1 = createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Doctor A1 Notification", "Private message.", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications/{id}", createdA1.getId())
                        .header("Authorization", "Bearer " + tokenDoctorA2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("14. Cross-Hospital Notification Access -> Denied (404)")
    void test14_crossHospitalNotificationAccess_denied() throws Exception {
        NotificationResponse createdA1 = createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Hospital A Notification", "Private hospital A message.", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications/{id}", createdA1.getId())
                        .header("Authorization", "Bearer " + tokenDoctorB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("15. Client hospitalId Override -> Rejected if User Mismatch")
    void test15_clientHospitalIdOverride_rejectedIfMismatch() {
        setSecurityContext(doctorUserA1);
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientUserId(doctorUserA1.getId())
                .hospitalId(hospitalB.getId())
                .type(NotificationType.SYSTEM)
                .title("Hospital Override Attack")
                .message("Attempting cross hospital creation.")
                .build();

        assertThrows(IllegalArgumentException.class, () -> notificationService.createNotification(request));
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("16. Client recipientUserId Override -> Ignored by Public API")
    void test16_clientRecipientUserIdOverride_ignoredByPublicApi() throws Exception {
        createNotificationFor(doctorUserA2, NotificationType.SYSTEM, "Doctor A2 Notification", "Doctor A2 data.", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ==========================================
    // LIFECYCLE TESTS (17 - 19)
    // ==========================================

    @Test
    @DisplayName("17. UNREAD -> READ Works")
    void test17_unreadToReadWorks() {
        NotificationResponse created = createNotificationFor(doctorUserA1, NotificationType.PATIENT_WAITING, "Patient Waiting", "Patient in waiting room.", NotificationPriority.NORMAL, null, null);

        assertEquals(NotificationStatus.UNREAD, created.getStatus());
        assertNull(created.getReadAt());

        setSecurityContext(doctorUserA1);
        NotificationResponse updated = notificationService.markAsRead(created.getId());
        assertEquals(NotificationStatus.READ, updated.getStatus());
        assertNotNull(updated.getReadAt());
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("18. READ -> READ is Idempotent")
    void test18_readToReadIsIdempotent() {
        NotificationResponse created = createNotificationFor(doctorUserA1, NotificationType.PATIENT_WAITING, "Patient Waiting", "Patient in waiting room.", NotificationPriority.NORMAL, null, null);

        setSecurityContext(doctorUserA1);
        NotificationResponse firstRead = notificationService.markAsRead(created.getId());
        LocalDateTime firstReadAt = firstRead.getReadAt();

        NotificationResponse secondRead = notificationService.markAsRead(created.getId());
        assertEquals(NotificationStatus.READ, secondRead.getStatus());
        assertEquals(firstReadAt, secondRead.getReadAt());
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("19. Deleted Notification Cannot Be Retrieved")
    void test19_deletedNotificationCannotBeRetrieved() throws Exception {
        NotificationResponse created = createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "To Be Deleted", "This will be deleted.", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(delete("/api/v1/notifications/{id}", created.getId())
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/{id}", created.getId())
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // DATA ISOLATION TESTS (20 - 22)
    // ==========================================

    @Test
    @DisplayName("20. Hospital A Notifications Cannot Appear for Hospital B")
    void test20_hospitalANotificationsCannotAppearForHospitalB() throws Exception {
        createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Hospital A Event", "Event in Hospital A.", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenDoctorB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("21. Doctor A Notifications Cannot Appear for Doctor B")
    void test21_doctorANotificationsCannotAppearForDoctorB() throws Exception {
        createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Doctor A1 Notification", "Message for A1.", NotificationPriority.NORMAL, null, null);
        createNotificationFor(doctorUserB, NotificationType.SYSTEM, "Doctor B Notification", "Message for B.", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenDoctorB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Doctor B Notification"));
    }

    @Test
    @DisplayName("22. Unread Count Only Counts Current User's Unread Notifications")
    void test22_unreadCountOnlyCountsCurrentUserUnread() throws Exception {
        createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Doc A1 Msg 1", "Unread", NotificationPriority.NORMAL, null, null);
        createNotificationFor(doctorUserA1, NotificationType.SYSTEM, "Doc A1 Msg 2", "Unread", NotificationPriority.NORMAL, null, null);

        createNotificationFor(doctorUserA2, NotificationType.SYSTEM, "Doc A2 Msg 1", "Unread", NotificationPriority.NORMAL, null, null);

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenDoctorA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenDoctorA2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }
}
