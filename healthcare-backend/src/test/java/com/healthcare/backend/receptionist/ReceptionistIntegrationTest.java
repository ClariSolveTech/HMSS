package com.healthcare.backend.receptionist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.appointment.dto.CancelAppointmentRequest;
import com.healthcare.backend.appointment.dto.CreateAppointmentRequest;
import com.healthcare.backend.appointment.dto.RescheduleAppointmentRequest;
import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.appointment.entity.BookingSource;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.dto.LoginRequest;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.queue.entity.DoctorQueueStatus;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.notification.entity.Notification;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.repository.NotificationRepository;
import com.healthcare.backend.patient.dto.CreatePatientRequest;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.queue.dto.CreateQueueEntryRequest;
import com.healthcare.backend.queue.entity.QueueEntry;
import com.healthcare.backend.queue.entity.QueuePriority;
import com.healthcare.backend.queue.entity.QueueStatus;
import com.healthcare.backend.queue.repository.QueueEntryRepository;
import com.healthcare.backend.schedule.entity.DoctorSchedule;
import com.healthcare.backend.schedule.entity.ScheduleStatus;
import com.healthcare.backend.schedule.repository.DoctorScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class ReceptionistIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.PaymentRepository paymentRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.InvoiceRepository invoiceRepository;

    @Autowired
    private com.healthcare.backend.prescription.repository.PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private com.healthcare.backend.prescription.repository.PrescriptionRepository prescriptionRepository;

    @Autowired
    private com.healthcare.backend.consultation.repository.ConsultationRepository consultationRepository;

    @Autowired
    private com.healthcare.backend.lab.repository.LabResultRepository labResultRepository;

    @Autowired
    private com.healthcare.backend.lab.repository.LabOrderRepository labOrderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Hospital hospitalA;
    private Hospital hospitalB;

    private User receptionistUserA;
    private User receptionistUserB;

    private Doctor doctorA;
    private Doctor doctorB;

    private String tokenReceptionistA;
    private String tokenReceptionistB;
    private String tokenDoctorA;

    private LocalDate today;
    private LocalDate futureDate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        notificationRepository.deleteAll();
        prescriptionItemRepository.deleteAll();
        prescriptionRepository.deleteAll();
        labResultRepository.deleteAll();
        labOrderRepository.deleteAll();
        paymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        consultationRepository.deleteAll();
        queueEntryRepository.deleteAll();
        appointmentRepository.deleteAll();
        patientRepository.deleteAll();
        doctorScheduleRepository.deleteAll();

        // Seed roles
        for (RoleName rn : RoleName.values()) {
            if (roleRepository.findByName(rn).isEmpty()) {
                roleRepository.save(new Role(rn));
            }
        }

        long ts = System.currentTimeMillis();
        today = LocalDate.now();
        futureDate = today.plusDays(7);

        // Hospitals
        hospitalA = createHospital("City Central Hospital", "CCH_" + ts, "contact.a." + ts + "@hospital.com");
        hospitalB = createHospital("Metro General Hospital", "MGH_" + ts, "contact.b." + ts + "@hospital.com");

        // Doctors
        User docUserA = createUser("Aarav", "Rao", "dr.aarav." + ts + "@citycentral.com", "91" + (ts % 100000000L), "Doctor@123", hospitalA, RoleName.DOCTOR);
        doctorA = createDoctor(docUserA, hospitalA, "General Medicine", BigDecimal.valueOf(500.0));

        User docUserB = createUser("Priya", "Nair", "dr.priya." + ts + "@metro.com", "92" + (ts % 100000000L), "Doctor@123", hospitalB, RoleName.DOCTOR);
        doctorB = createDoctor(docUserB, hospitalB, "Cardiology", BigDecimal.valueOf(800.0));

        // Receptionists
        receptionistUserA = createUser("Maya", "Sharma", "receptionist.a." + ts + "@citycentral.com", "93" + (ts % 100000000L), "Recep@123", hospitalA, RoleName.RECEPTIONIST);
        receptionistUserB = createUser("John", "Doe", "receptionist.b." + ts + "@metro.com", "94" + (ts % 100000000L), "Recep@123", hospitalB, RoleName.RECEPTIONIST);

        tokenReceptionistA = jwtService.generateToken(receptionistUserA);
        tokenReceptionistB = jwtService.generateToken(receptionistUserB);
        tokenDoctorA = jwtService.generateToken(docUserA);

        // Schedules for Doctor A (Today and Future)
        createSchedule(doctorA, hospitalA, today.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(18, 0), 15);
        if (!today.getDayOfWeek().equals(futureDate.getDayOfWeek())) {
            createSchedule(doctorA, hospitalA, futureDate.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(18, 0), 15);
        }
    }

    @Test
    @DisplayName("1. Receptionist can authenticate via JWT and receive ROLE_RECEPTIONIST")
    void test1_receptionistAuthentication_success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmailOrMobile(receptionistUserA.getEmail());
        loginRequest.setPassword("Recep@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(receptionistUserA.getId()))
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()))
                .andExpect(jsonPath("$.roles", hasItem("RECEPTIONIST")));
    }

    @Test
    @DisplayName("2. Receptionist can register a new patient in their hospital")
    void test2_receptionistRegisterPatient_success() throws Exception {
        CreatePatientRequest req = new CreatePatientRequest();
        req.setFirstName("Ramesh");
        req.setLastName("Kumar");
        req.setDateOfBirth(LocalDate.of(1990, 5, 20));
        req.setGender(Gender.MALE);
        req.setMobileNumber("9876500001");
        req.setEmail("ramesh.kumar@example.com");
        req.setAddress("42 MG Road");
        req.setCity("Bengaluru");
        req.setState("Karnataka");
        req.setPincode("560001");

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + tokenReceptionistA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.patientCode").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Ramesh"))
                .andExpect(jsonPath("$.lastName").value("Kumar"))
                .andExpect(jsonPath("$.mobileNumber").value("9876500001"))
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()));

        assertTrue(patientRepository.existsByMobileNumberAndHospitalId("9876500001", hospitalA.getId()));
    }

    @Test
    @DisplayName("3. Receptionist can search patients and retrieve patient by ID")
    void test3_receptionistSearchAndGetPatient_success() throws Exception {
        Patient p1 = createPatient("Anil", "Kapoor", "9876500002", hospitalA);
        Patient p2 = createPatient("Sunita", "Rao", "9876500003", hospitalA);

        // Search by query
        mockMvc.perform(get("/api/v1/patients")
                        .header("Authorization", "Bearer " + tokenReceptionistA)
                        .param("search", "Sunita"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Sunita"));

        // Get by ID
        mockMvc.perform(get("/api/v1/patients/" + p1.getId())
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p1.getId()))
                .andExpect(jsonPath("$.firstName").value("Anil"));
    }

    @Test
    @DisplayName("4. Receptionist cannot access patients belonging to another hospital (Cross-hospital denial)")
    void test4_receptionistCrossHospitalPatientAccess_denied() throws Exception {
        Patient patientInHospitalB = createPatient("Farhan", "Akhtar", "9876500004", hospitalB);

        mockMvc.perform(get("/api/v1/patients/" + patientInHospitalB.getId())
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. Receptionist can book an appointment for a patient")
    void test5_receptionistBookAppointment_success() throws Exception {
        Patient patient = createPatient("Kavita", "Menon", "9876500005", hospitalA);

        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patient.getId());
        req.setDoctorId(doctorA.getId());
        req.setAppointmentDate(futureDate);
        req.setStartTime(LocalTime.of(10, 0));
        req.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        req.setBookingSource(BookingSource.RECEPTION);
        req.setReason("General health consultation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenReceptionistA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.doctorId").value(doctorA.getId()))
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()));
    }

    @Test
    @DisplayName("6. Receptionist can confirm and reschedule an appointment")
    void test6_receptionistConfirmAndReschedule_success() throws Exception {
        Patient patient = createPatient("Deepak", "Joshi", "9876500006", hospitalA);
        Appointment appt = createAppointment(patient, doctorA, hospitalA, futureDate, LocalTime.of(11, 0), AppointmentStatus.BOOKED);

        // Confirm
        mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/confirm")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Reschedule
        RescheduleAppointmentRequest resched = new RescheduleAppointmentRequest();
        resched.setAppointmentDate(futureDate);
        resched.setStartTime(LocalTime.of(14, 0));

        mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/reschedule")
                        .header("Authorization", "Bearer " + tokenReceptionistA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resched)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.startTime").value("14:00:00"));
    }

    @Test
    @DisplayName("7. Receptionist can cancel an appointment with reason")
    void test7_receptionistCancelAppointment_success() throws Exception {
        Patient patient = createPatient("Manish", "Verma", "9876500007", hospitalA);
        Appointment appt = createAppointment(patient, doctorA, hospitalA, futureDate, LocalTime.of(12, 0), AppointmentStatus.BOOKED);

        CancelAppointmentRequest cancelReq = new CancelAppointmentRequest();
        cancelReq.setReason("Patient requested cancellation due to travel");

        mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenReceptionistA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Patient requested cancellation due to travel"));
    }

    @Test
    @DisplayName("8. Receptionist can view today's appointments and doctor appointments")
    void test8_receptionistViewTodayAppointments_success() throws Exception {
        Patient patient = createPatient("Tarun", "Gill", "9876500008", hospitalA);
        createAppointment(patient, doctorA, hospitalA, today, LocalTime.of(9, 0), AppointmentStatus.CONFIRMED);

        // View today's appointments
        mockMvc.perform(get("/api/v1/appointments/today")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

        // View doctor specific appointments
        mockMvc.perform(get("/api/v1/doctors/" + doctorA.getId() + "/appointments")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("9. Receptionist check-in appointment generates queue entry and notification")
    void test9_receptionistCheckInAppointment_generatesQueueToken() throws Exception {
        Patient patient = createPatient("Anita", "Desai", "9876500009", hospitalA);
        Appointment appt = createAppointment(patient, doctorA, hospitalA, today, LocalTime.of(9, 30), AppointmentStatus.CONFIRMED);

        mockMvc.perform(post("/api/v1/queue/check-in/" + appt.getId())
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.doctorId").value(doctorA.getId()));

        // Verify notification generated for doctor
        List<Notification> notifs = notificationRepository.findAll();
        boolean hasNotif = notifs.stream().anyMatch(n ->
                n.getType() == NotificationType.PATIENT_CHECKED_IN &&
                n.getRecipient().getId().equals(doctorA.getUser().getId()));
        assertTrue(hasNotif, "Doctor must receive PATIENT_CHECKED_IN notification on receptionist check-in");
    }

    @Test
    @DisplayName("10. Duplicate check-in is rejected (Duplicate Prevention)")
    void test10_receptionistDuplicateCheckIn_rejected() throws Exception {
        Patient patient = createPatient("Rohit", "Sharma", "9876500010", hospitalA);
        Appointment appt = createAppointment(patient, doctorA, hospitalA, today, LocalTime.of(10, 30), AppointmentStatus.CONFIRMED);

        // First check-in
        mockMvc.perform(post("/api/v1/queue/check-in/" + appt.getId())
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isCreated());

        // Second check-in attempt on the same appointment must fail with 409
        mockMvc.perform(post("/api/v1/queue/check-in/" + appt.getId())
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("11. Receptionist can add a walk-in patient to queue")
    void test11_receptionistCreateWalkInQueueEntry_success() throws Exception {
        Patient patient = createPatient("Vikram", "Batra", "9876500011", hospitalA);

        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setPatientId(patient.getId());
        req.setDoctorId(doctorA.getId());
        req.setPriority(QueuePriority.NORMAL);
        req.setNotes("Walk-in general consultation");

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + tokenReceptionistA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.tokenNumber").isNumber())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.patientId").value(patient.getId()));
    }

    @Test
    @DisplayName("12. Receptionist can view hospital-wide queue with filtering")
    void test12_receptionistViewHospitalQueue_success() throws Exception {
        Patient patient = createPatient("Suresh", "Raina", "9876500012", hospitalA);
        createQueueEntry(patient, doctorA, hospitalA, 1, QueueStatus.WAITING);

        mockMvc.perform(get("/api/v1/queue")
                        .header("Authorization", "Bearer " + tokenReceptionistA)
                        .param("doctorId", doctorA.getId().toString())
                        .param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tokenNumber").value(1));
    }

    @Test
    @DisplayName("13. Receptionist can call, skip, and recall queue entries")
    void test13_receptionistQueueActions_callSkipRecall() throws Exception {
        Patient patient = createPatient("Ajay", "Jadeja", "9876500013", hospitalA);
        QueueEntry entry = createQueueEntry(patient, doctorA, hospitalA, 1, QueueStatus.WAITING);

        // Call
        mockMvc.perform(patch("/api/v1/queue/" + entry.getId() + "/call")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"));

        // Skip
        mockMvc.perform(patch("/api/v1/queue/" + entry.getId() + "/skip")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"));

        // Recall
        mockMvc.perform(patch("/api/v1/queue/" + entry.getId() + "/recall")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("14. Receptionist cannot access queue or appointments of another hospital")
    void test14_receptionistCrossHospitalAccess_denied() throws Exception {
        Patient patientB = createPatient("Harbhajan", "Singh", "9876500014", hospitalB);
        Appointment apptB = createAppointment(patientB, doctorB, hospitalB, today, LocalTime.of(11, 0), AppointmentStatus.BOOKED);
        QueueEntry entryB = createQueueEntry(patientB, doctorB, hospitalB, 1, QueueStatus.WAITING);

        // Cross-hospital appointment check-in denied
        mockMvc.perform(post("/api/v1/queue/check-in/" + apptB.getId())
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isNotFound());

        // Cross-hospital queue entry detail denied
        mockMvc.perform(get("/api/v1/queue/" + entryB.getId())
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("15. Receptionist is denied access to doctor clinical actions (403 Forbidden)")
    void test15_receptionistClinicalActions_deniedWithForbidden() throws Exception {
        // Complete consultation requires DOCTOR
        mockMvc.perform(patch("/api/v1/consultations/99999/complete")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isForbidden());

        // Issue prescription requires DOCTOR
        mockMvc.perform(patch("/api/v1/prescriptions/99999/issue")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isForbidden());

        // View lab result requires DOCTOR/LAB_STAFF/HOSPITAL_ADMIN (not RECEPTIONIST)
        mockMvc.perform(get("/api/v1/lab/results/99999")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("16. Receptionist can view their own notifications")
    void test16_receptionistNotifications_lifecycle() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenReceptionistA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").isNumber());
    }

    // Helper methods
    private Hospital createHospital(String name, String code, String email) {
        Hospital h = new Hospital();
        h.setName(name);
        h.setCode(code);
        h.setEmail(email);
        h.setPhone("080-12345678");
        h.setAddress("Healthcare Blvd");
        h.setStatus(HospitalStatus.ACTIVE);
        return hospitalRepository.save(h);
    }

    private User createUser(String firstName, String lastName, String email, String mobile, String password, Hospital hospital, RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setMobileNumber(mobile);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setHospital(hospital);
        u.setStatus(UserStatus.ACTIVE);
        u.setRoles(Set.of(role));
        return userRepository.save(u);
    }

    private Doctor createDoctor(User user, Hospital hospital, String specialization, BigDecimal fee) {
        Doctor d = new Doctor();
        d.setUser(user);
        d.setHospital(hospital);
        d.setDoctorCode("DOC-" + user.getId());
        d.setSpecialization(specialization);
        d.setQualification("MBBS, MD");
        d.setLicenseNumber("LIC-" + user.getId());
        d.setConsultationFee(fee);
        d.setStatus(DoctorStatus.ACTIVE);
        d.setQueueStatus(DoctorQueueStatus.ARRIVED);
        return doctorRepository.save(d);
    }

    private Patient createPatient(String firstName, String lastName, String mobile, Hospital hospital) {
        Patient p = new Patient();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setPatientCode("PAT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        p.setMobileNumber(mobile);
        p.setDateOfBirth(LocalDate.of(1985, 1, 1));
        p.setGender(Gender.MALE);
        p.setHospital(hospital);
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p);
    }

    private DoctorSchedule createSchedule(Doctor doctor, Hospital hospital, DayOfWeek day, LocalTime start, LocalTime end, int slotMins) {
        DoctorSchedule s = new DoctorSchedule();
        s.setDoctor(doctor);
        s.setHospital(hospital);
        s.setDayOfWeek(day);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setSlotDurationMinutes(slotMins);
        s.setStatus(ScheduleStatus.ACTIVE);
        return doctorScheduleRepository.save(s);
    }

    private Appointment createAppointment(Patient patient, Doctor doctor, Hospital hospital, LocalDate date, LocalTime start, AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setHospital(hospital);
        a.setAppointmentDate(date);
        a.setStartTime(start);
        a.setEndTime(start.plusMinutes(15));
        a.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        a.setBookingSource(BookingSource.RECEPTION);
        a.setStatus(status);
        a.setAppointmentNumber("APT-" + System.currentTimeMillis() % 100000);
        a.setActiveSlotKey(Appointment.buildActiveSlotKey(doctor.getId(), date, start));
        return appointmentRepository.save(a);
    }

    private QueueEntry createQueueEntry(Patient patient, Doctor doctor, Hospital hospital, int token, QueueStatus status) {
        QueueEntry q = new QueueEntry();
        q.setPatient(patient);
        q.setDoctor(doctor);
        q.setHospital(hospital);
        q.setQueueDate(today);
        q.setTokenNumber(token);
        q.setPriority(QueuePriority.NORMAL);
        q.setStatus(status);
        q.setCheckedInAt(LocalDateTime.now());
        q.setActiveQueueKey(QueueEntry.buildActiveQueueKey(hospital.getId(), doctor.getId(), patient.getId(), today));
        return queueEntryRepository.save(q);
    }
}
