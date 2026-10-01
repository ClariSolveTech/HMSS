package com.healthcare.backend.notification;

import com.healthcare.backend.appointment.dto.CreateAppointmentRequest;
import com.healthcare.backend.appointment.dto.RescheduleAppointmentRequest;
import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.appointment.entity.BookingSource;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.appointment.service.AppointmentService;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.CustomUserDetailsService;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.notification.entity.Notification;
import com.healthcare.backend.notification.entity.NotificationStatus;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.repository.NotificationRepository;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.queue.dto.CreateQueueEntryRequest;
import com.healthcare.backend.queue.dto.QueueEntryResponse;
import com.healthcare.backend.queue.entity.QueuePriority;
import com.healthcare.backend.queue.repository.QueueEntryRepository;
import com.healthcare.backend.queue.service.QueueService;
import com.healthcare.backend.schedule.dto.CreateDoctorScheduleRequest;
import com.healthcare.backend.schedule.service.DoctorScheduleService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AppointmentQueueNotificationIntegrationTest {

    private static final AtomicLong COUNTER = new AtomicLong(20000);

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
    private DoctorScheduleService doctorScheduleService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueService queueService;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Hospital testHospital;
    private Doctor testDoctor;
    private Doctor secondDoctor;
    private Patient testPatient;
    private String doctorEmail;
    private String secondDoctorEmail;
    private String adminEmail;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        queueEntryRepository.deleteAll();
        appointmentRepository.deleteAll();

        for (RoleName rn : RoleName.values()) {
            if (roleRepository.findByName(rn).isEmpty()) {
                roleRepository.save(new Role(rn));
            }
        }

        long ts = System.currentTimeMillis();
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Hospital
        testHospital = new Hospital();
        testHospital.setName("Event Test Hosp " + uid);
        testHospital.setCode("ETH" + uid);
        testHospital.setPhone("+919100" + (ts % 1000000L));
        testHospital.setEmail("hosp." + uid + "@test.com");
        testHospital.setStatus(HospitalStatus.ACTIVE);
        testHospital = hospitalRepository.save(testHospital);

        // Users
        adminEmail = "admin." + uid + "@test.com";
        User adminUser = createUserDirect("Admin", "User", adminEmail, "911" + (ts % 1000000L), "Pass@1234", testHospital, RoleName.HOSPITAL_ADMIN);

        doctorEmail = "doc1." + uid + "@test.com";
        User docUser1 = createUserDirect("Gregory", "House", doctorEmail, "912" + (ts % 1000000L), "Pass@1234", testHospital, RoleName.DOCTOR);

        secondDoctorEmail = "doc2." + uid + "@test.com";
        User docUser2 = createUserDirect("James", "Wilson", secondDoctorEmail, "913" + (ts % 1000000L), "Pass@1234", testHospital, RoleName.DOCTOR);

        // Doctor Entities
        testDoctor = createDoctorHelper(docUser1, "Cardiology", "LIC-100");
        secondDoctor = createDoctorHelper(docUser2, "Oncology", "LIC-200");

        // Authenticate Admin to create schedules & entities
        authenticate(adminEmail);

        // Doctor Schedules for today and tomorrow
        DayOfWeek todayDow = LocalDate.now().getDayOfWeek();
        DayOfWeek tomorrowDow = LocalDate.now().plusDays(1).getDayOfWeek();
        createScheduleHelper(testDoctor.getId(), todayDow, LocalTime.of(0, 0), LocalTime.of(23, 59), 15);
        createScheduleHelper(secondDoctor.getId(), todayDow, LocalTime.of(0, 0), LocalTime.of(23, 59), 15);
        createScheduleHelper(testDoctor.getId(), tomorrowDow, LocalTime.of(0, 0), LocalTime.of(23, 59), 15);
        createScheduleHelper(secondDoctor.getId(), tomorrowDow, LocalTime.of(0, 0), LocalTime.of(23, 59), 15);

        // Patient Entity
        testPatient = createPatientHelper(testHospital, "John", "Doe", "john." + uid + "@test.com", "914" + (ts % 1000000L));
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
        d.setConsultationFee(new BigDecimal("500.00"));
        d.setStatus(DoctorStatus.ACTIVE);
        return doctorRepository.save(d);
    }

    private Patient createPatientHelper(Hospital hospital, String f, String l, String e, String m) {
        long seq = COUNTER.incrementAndGet();
        Patient p = new Patient();
        p.setHospital(hospital);
        p.setPatientCode(hospital.getCode() + "-P" + seq);
        p.setFirstName(f); p.setLastName(l); p.setEmail(e); p.setMobileNumber(m);
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        p.setGender(Gender.MALE);
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p);
    }

    private void createScheduleHelper(Long doctorId, DayOfWeek dow, LocalTime start, LocalTime end, int slotMins) {
        CreateDoctorScheduleRequest req = new CreateDoctorScheduleRequest();
        req.setDoctorId(doctorId);
        req.setDayOfWeek(dow);
        req.setStartTime(start);
        req.setEndTime(end);
        req.setSlotDurationMinutes(slotMins);
        doctorScheduleService.createSchedule(req);
    }

    private void authenticate(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Appointment createTestAppointment() {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(testPatient.getId());
        req.setDoctorId(testDoctor.getId());
        req.setAppointmentDate(LocalDate.now().plusDays(1));
        req.setStartTime(LocalTime.of(10, 0));
        req.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        req.setBookingSource(BookingSource.RECEPTION);
        req.setReason("Routine checkup");

        return appointmentRepository.findById(appointmentService.createAppointment(req).getId()).orElseThrow();
    }

    // ==========================================
    // APPOINTMENT NOTIFICATION TESTS
    // ==========================================

    @Test
    @DisplayName("1. confirmAppointment_createsNotificationForDoctor")
    void confirmAppointment_createsNotificationForDoctor() {
        authenticate(adminEmail);
        Appointment appt = createTestAppointment();

        int notificationsBefore = notificationRepository.findAll().size();

        appointmentService.confirmAppointment(appt.getId());

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(notificationsBefore + 1, notifications.size());

        Notification n = notifications.get(notifications.size() - 1);
        assertEquals(NotificationType.APPOINTMENT_CONFIRMED, n.getType());
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
        assertEquals(testHospital.getId(), n.getHospital().getId());
        assertEquals("APPOINTMENT", n.getReferenceType());
        assertEquals(appt.getId(), n.getReferenceId());
        assertEquals(NotificationStatus.UNREAD, n.getStatus());
        assertTrue(n.getMessage().contains("confirmed"));
    }

    @Test
    @DisplayName("2. cancelAppointment_createsNotificationForDoctor")
    void cancelAppointment_createsNotificationForDoctor() {
        authenticate(adminEmail);
        Appointment appt = createTestAppointment();

        appointmentService.cancelAppointment(appt.getId(), "Patient requested cancellation");

        List<Notification> notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());

        Notification n = notifications.get(notifications.size() - 1);
        assertEquals(NotificationType.APPOINTMENT_CANCELLED, n.getType());
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
        assertEquals("APPOINTMENT", n.getReferenceType());
        assertEquals(appt.getId(), n.getReferenceId());
        assertTrue(n.getMessage().contains("cancelled"));
        assertTrue(n.getMessage().contains("Patient requested cancellation"));
    }

    @Test
    @DisplayName("3. rescheduleAppointment_createsNotificationForDoctor")
    void rescheduleAppointment_createsNotificationForDoctor() {
        authenticate(adminEmail);
        Appointment appt = createTestAppointment();

        RescheduleAppointmentRequest req = new RescheduleAppointmentRequest();
        req.setAppointmentDate(LocalDate.now().plusDays(1));
        req.setStartTime(LocalTime.of(11, 0));

        appointmentService.rescheduleAppointment(appt.getId(), req);

        List<Notification> notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());

        Notification n = notifications.get(notifications.size() - 1);
        assertEquals(NotificationType.APPOINTMENT_RESCHEDULED, n.getType());
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
        assertEquals("APPOINTMENT", n.getReferenceType());
        assertEquals(appt.getId(), n.getReferenceId());
        assertTrue(n.getMessage().contains("rescheduled"));
    }

    @Test
    @DisplayName("4. confirmAppointment_doesNotDuplicateNotification")
    void confirmAppointment_doesNotDuplicateNotification() {
        authenticate(adminEmail);
        Appointment appt = createTestAppointment();

        appointmentService.confirmAppointment(appt.getId());
        int countAfterFirst = notificationRepository.findAll().size();

        assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.confirmAppointment(appt.getId());
        });

        assertEquals(countAfterFirst, notificationRepository.findAll().size());
    }

    @Test
    @DisplayName("5. cancelAppointment_doesNotDuplicateNotification")
    void cancelAppointment_doesNotDuplicateNotification() {
        authenticate(adminEmail);
        Appointment appt = createTestAppointment();

        appointmentService.cancelAppointment(appt.getId(), "First cancel");
        int countAfterFirst = notificationRepository.findAll().size();

        assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.cancelAppointment(appt.getId(), "Second cancel");
        });

        assertEquals(countAfterFirst, notificationRepository.findAll().size());
    }

    @Test
    @DisplayName("6. invalidAppointmentOperation_doesNotCreateNotification")
    void invalidAppointmentOperation_doesNotCreateNotification() {
        authenticate(adminEmail);
        int initialCount = notificationRepository.findAll().size();

        assertThrows(Exception.class, () -> {
            appointmentService.confirmAppointment(999999L);
        });

        assertEquals(initialCount, notificationRepository.findAll().size());
    }

    // ==========================================
    // QUEUE NOTIFICATION TESTS
    // ==========================================

    @Test
    @DisplayName("7. createQueueEntry_createsPatientCheckedInNotification")
    void createQueueEntry_createsPatientCheckedInNotification() {
        authenticate(adminEmail);

        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(testDoctor.getId());
        req.setPatientId(testPatient.getId());
        req.setPriority(QueuePriority.NORMAL);

        QueueEntryResponse entryRes = queueService.createQueueEntry(req);

        List<Notification> notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());

        Notification n = notifications.get(notifications.size() - 1);
        assertEquals(NotificationType.PATIENT_CHECKED_IN, n.getType());
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
        assertEquals("QUEUE", n.getReferenceType());
        assertEquals(entryRes.getId(), n.getReferenceId());
        assertTrue(n.getMessage().contains("checked in"));
    }

    @Test
    @DisplayName("8. checkInAppointment_createsOnlyOneNotification")
    void checkInAppointment_createsOnlyOneNotification() {
        authenticate(adminEmail);
        
        long seq = COUNTER.incrementAndGet();
        Appointment appt = new Appointment();
        appt.setHospital(testHospital);
        appt.setDoctor(testDoctor);
        appt.setPatient(testPatient);
        appt.setAppointmentDate(LocalDate.now());
        appt.setStartTime(LocalTime.of(10, 0));
        appt.setEndTime(LocalTime.of(10, 15));
        appt.setAppointmentNumber("APPT-CHK-" + seq);
        appt.setStatus(AppointmentStatus.BOOKED);
        appt.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        appt.setBookingSource(BookingSource.RECEPTION);
        appt.setActiveSlotKey(Appointment.buildActiveSlotKey(testDoctor.getId(), LocalDate.now(), LocalTime.of(10, 0)));
        appt = appointmentRepository.save(appt);

        int initialCount = notificationRepository.findAll().size();

        queueService.checkInAppointment(appt.getId());

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(initialCount + 1, notifications.size(), "Should create exactly ONE notification on appointment check-in");

        Notification n = notifications.get(notifications.size() - 1);
        assertEquals(NotificationType.PATIENT_CHECKED_IN, n.getType());
    }

    @Test
    @DisplayName("9. recallPatient_createsPatientWaitingNotification")
    void recallPatient_createsPatientWaitingNotification() {
        authenticate(adminEmail);

        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(testDoctor.getId());
        req.setPatientId(testPatient.getId());
        QueueEntryResponse entryRes = queueService.createQueueEntry(req);

        authenticate(doctorEmail);
        queueService.skipPatient(entryRes.getId(), "Skipped first");

        queueService.recallPatient(entryRes.getId());

        List<Notification> notifications = notificationRepository.findAll();
        Notification n = notifications.get(notifications.size() - 1);
        assertEquals(NotificationType.PATIENT_WAITING, n.getType());
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
        assertTrue(n.getMessage().contains("waiting"));
    }

    @Test
    @DisplayName("10. callPatient_createsQueueUpdateNotification")
    void callPatient_createsQueueUpdateNotification() {
        authenticate(adminEmail);

        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(testDoctor.getId());
        req.setPatientId(testPatient.getId());
        QueueEntryResponse entryRes = queueService.createQueueEntry(req);

        authenticate(doctorEmail);

        queueService.callPatient(entryRes.getId());

        List<Notification> notifications = notificationRepository.findAll();
        Notification n = notifications.get(notifications.size() - 1);
        assertEquals(NotificationType.QUEUE_UPDATE, n.getType());
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
        assertTrue(n.getMessage().contains("called"));
    }

    @Test
    @DisplayName("11. skipPatient_createsQueueUpdateNotification")
    void skipPatient_createsQueueUpdateNotification() {
        authenticate(adminEmail);

        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(testDoctor.getId());
        req.setPatientId(testPatient.getId());
        QueueEntryResponse entryRes = queueService.createQueueEntry(req);

        authenticate(doctorEmail);

        queueService.skipPatient(entryRes.getId(), "Patient not present");

        List<Notification> notifications = notificationRepository.findAll();
        Notification n = notifications.get(notifications.size() - 1);
        assertEquals(NotificationType.QUEUE_UPDATE, n.getType());
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
        assertTrue(n.getMessage().contains("skipped"));
    }

    @Test
    @DisplayName("12. invalidQueueTransition_doesNotCreateNotification")
    void invalidQueueTransition_doesNotCreateNotification() {
        authenticate(adminEmail);
        int initialCount = notificationRepository.findAll().size();

        assertThrows(Exception.class, () -> {
            queueService.callPatient(999999L);
        });

        assertEquals(initialCount, notificationRepository.findAll().size());
    }

    @Test
    @DisplayName("13 & 14. notificationRecipientIsAssignedDoctor & notificationHospitalMatchesBusinessEntity")
    void notificationRecipientAndHospitalMatchEntity() {
        authenticate(adminEmail);
        Appointment appt = createTestAppointment();

        appointmentService.confirmAppointment(appt.getId());

        List<Notification> doctorNotifications = notificationRepository.findByRecipientIdAndHospitalId(
                testDoctor.getUser().getId(), testHospital.getId(), null
        ).getContent();

        assertFalse(doctorNotifications.isEmpty());
        Notification n = doctorNotifications.get(0);
        assertEquals(testDoctor.getUser().getId(), n.getRecipient().getId());
        assertEquals(testHospital.getId(), n.getHospital().getId());
    }

    @Test
    @DisplayName("15. doctorCannotSeeAnotherDoctorsGeneratedNotification")
    void doctorCannotSeeAnotherDoctorsGeneratedNotification() {
        authenticate(adminEmail);
        Appointment appt = createTestAppointment();
        appointmentService.confirmAppointment(appt.getId());

        // Query for secondDoctor's notifications
        List<Notification> secondDocNotifications = notificationRepository.findByRecipientIdAndHospitalId(
                secondDoctor.getUser().getId(), testHospital.getId(), null
        ).getContent();

        assertTrue(secondDocNotifications.isEmpty(), "Second doctor must not receive notifications generated for first doctor");
    }

    @Test
    @DisplayName("16. notificationPersistsAfterSuccessfulTransaction")
    void notificationPersistsAfterSuccessfulTransaction() {
        authenticate(adminEmail);
        Appointment appt = createTestAppointment();

        appointmentService.confirmAppointment(appt.getId());

        List<Notification> all = notificationRepository.findAll();
        assertFalse(all.isEmpty());
        assertEquals(NotificationType.APPOINTMENT_CONFIRMED, all.get(all.size() - 1).getType());
    }
}
