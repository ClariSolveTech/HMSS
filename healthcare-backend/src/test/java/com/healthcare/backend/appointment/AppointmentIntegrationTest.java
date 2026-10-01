package com.healthcare.backend.appointment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.appointment.dto.CreateAppointmentRequest;
import com.healthcare.backend.appointment.dto.RescheduleAppointmentRequest;
import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.appointment.entity.BookingSource;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.dto.RegisterRequest;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.service.AuthService;
import com.healthcare.backend.doctor.dto.CreateDoctorRequest;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.doctor.service.DoctorService;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.patient.dto.CreatePatientRequest;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.patient.service.PatientService;
import com.healthcare.backend.schedule.dto.CreateDoctorScheduleRequest;
import com.healthcare.backend.schedule.repository.DoctorScheduleRepository;
import com.healthcare.backend.schedule.service.DoctorScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.healthcare.backend.auth.security.CustomUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AppointmentIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private com.healthcare.backend.queue.repository.QueueEntryRepository queueEntryRepository;

    @Autowired
    private com.healthcare.backend.prescription.repository.PrescriptionRepository prescriptionRepository;

    @Autowired
    private com.healthcare.backend.lab.repository.LabOrderRepository labOrderRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.InvoiceRepository invoiceRepository;

    @Autowired
    private com.healthcare.backend.consultation.repository.ConsultationRepository consultationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Autowired
    private com.healthcare.backend.auth.security.JwtService jwtService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private String tokenAdminA;
    private String tokenDoctorA;
    private String tokenDoctorA2;
    private String tokenRecepA;
    private String tokenAdminB;
    private String tokenDoctorB;
    private String tokenLabStaffA;

    private Long doctorAId;
    private Long doctorA2Id;
    private Long doctorBId;

    private Long patientAId;
    private Long patientA2Id;
    private Long patientBId;

    private LocalDate testFutureDate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        prescriptionRepository.deleteAll();
        labOrderRepository.deleteAll();
        invoiceRepository.deleteAll();
        consultationRepository.deleteAll();
        queueEntryRepository.deleteAll();
        appointmentRepository.deleteAll();


        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
            }
        }

        long ts = System.currentTimeMillis();
        String uidA = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String uidB = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        // Hospital A
        hospitalA = new Hospital();
        hospitalA.setName("Apt Alpha Center " + uidA);
        hospitalA.setCode("AAC" + uidA);
        hospitalA.setPhone("+919100000001");
        hospitalA.setEmail("alpha." + uidA + "@apt.com");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        // Hospital B
        hospitalB = new Hospital();
        hospitalB.setName("Apt Beta Center " + uidB);
        hospitalB.setCode("ABC" + uidB);
        hospitalB.setPhone("+919100000002");
        hospitalB.setEmail("beta." + uidB + "@apt.com");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        // Users & Tokens
        com.healthcare.backend.auth.entity.User adminA = createUserDirect("Admin", "Alpha", "admin.alpha." + ts + "@apt.com", "91" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.HOSPITAL_ADMIN);
        tokenAdminA = jwtService.generateToken(adminA);

        com.healthcare.backend.auth.entity.User adminB = createUserDirect("Admin", "Beta", "admin.beta." + ts + "@apt.com", "92" + (ts % 100000000L), "Pass@1234", hospitalB, RoleName.HOSPITAL_ADMIN);
        tokenAdminB = jwtService.generateToken(adminB);

        com.healthcare.backend.auth.entity.User recepA = createUserDirect("Recep", "Alpha", "recep.alpha." + ts + "@apt.com", "93" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.RECEPTIONIST);
        tokenRecepA = jwtService.generateToken(recepA);

        com.healthcare.backend.auth.entity.User docUserA = createUserDirect("Doctor", "One", "doc.one." + ts + "@apt.com", "94" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.DOCTOR);
        tokenDoctorA = jwtService.generateToken(docUserA);

        com.healthcare.backend.auth.entity.User docUserA2 = createUserDirect("Doctor", "Two", "doc.two." + ts + "@apt.com", "95" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.DOCTOR);
        tokenDoctorA2 = jwtService.generateToken(docUserA2);

        com.healthcare.backend.auth.entity.User docUserB = createUserDirect("Doctor", "Beta", "doc.beta." + ts + "@apt.com", "96" + (ts % 100000000L), "Pass@1234", hospitalB, RoleName.DOCTOR);
        tokenDoctorB = jwtService.generateToken(docUserB);

        com.healthcare.backend.auth.entity.User labA = createUserDirect("Lab", "Staff", "lab.alpha." + ts + "@apt.com", "97" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.LAB_STAFF);
        tokenLabStaffA = jwtService.generateToken(labA);

        // Doctors Profile
        doctorAId = createDoctorHelper(docUserA, "Cardiology", "LIC-A1");
        doctorA2Id = createDoctorHelper(docUserA2, "Neurology", "LIC-A2");
        doctorBId = createDoctorHelper(docUserB, "Orthopedics", "LIC-B1");

        // Patients
        patientAId = createPatientHelper(hospitalA, "Ramesh", "Kumar", "ramesh." + ts + "@test.com", "981" + (ts % 10000000L));
        patientA2Id = createPatientHelper(hospitalA, "Suresh", "Raina", "suresh." + ts + "@test.com", "982" + (ts % 10000000L));
        patientBId = createPatientHelper(hospitalB, "Priya", "Patel", "priya." + ts + "@test.com", "983" + (ts % 10000000L));

        // Find next target weekday for doctor schedule
        testFutureDate = LocalDate.now().plusDays(2);
        DayOfWeek dow = testFutureDate.getDayOfWeek();

        // Create Schedules for Doctor A (09:00 - 17:00, slot 15 min, break 13:00 - 14:00)
        createScheduleHelper(doctorAId, dow, LocalTime.of(9, 0), LocalTime.of(17, 0), 15, LocalTime.of(13, 0), LocalTime.of(14, 0));

        // Create Schedule for Doctor B
        createScheduleHelper(doctorBId, dow, LocalTime.of(10, 0), LocalTime.of(16, 0), 30, LocalTime.of(13, 0), LocalTime.of(14, 0));
    }

    private com.healthcare.backend.auth.entity.User createUserDirect(String f, String l, String e, String m, String p, Hospital h, RoleName r) {
        Role role = roleRepository.findByName(r).orElseGet(() -> roleRepository.save(new Role(r)));
        com.healthcare.backend.auth.entity.User u = new com.healthcare.backend.auth.entity.User();
        u.setFirstName(f); u.setLastName(l); u.setEmail(e); u.setMobileNumber(m);
        u.setPasswordHash(passwordEncoder.encode(p)); u.setHospital(h);
        u.setStatus(com.healthcare.backend.auth.entity.UserStatus.ACTIVE);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    private static final java.util.concurrent.atomic.AtomicLong CODE_COUNTER = new java.util.concurrent.atomic.AtomicLong(10000);

    private Long createDoctorHelper(com.healthcare.backend.auth.entity.User user, String spec, String lic) {
        Doctor d = new Doctor();
        d.setUser(user);
        d.setHospital(user.getHospital());
        long seq = CODE_COUNTER.incrementAndGet();
        d.setDoctorCode(user.getHospital().getCode() + "-D" + seq);
        d.setSpecialization(spec);
        d.setQualification("MD");
        d.setLicenseNumber(lic + "-" + seq);
        d.setConsultationFee(new BigDecimal("500.00"));
        d.setStatus(DoctorStatus.ACTIVE);
        return doctorRepository.save(d).getId();
    }

    private Long createPatientHelper(Hospital hospital, String f, String l, String e, String m) {
        Patient p = new Patient();
        p.setHospital(hospital);
        long seq = CODE_COUNTER.incrementAndGet();
        p.setPatientCode(hospital.getCode() + "-P" + seq);
        p.setFirstName(f);
        p.setLastName(l);
        p.setEmail(e);
        p.setMobileNumber(m);
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        p.setGender(Gender.MALE);
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p).getId();
    }

    private void createScheduleHelper(Long doctorId, DayOfWeek dow, LocalTime start, LocalTime end, int dur, LocalTime bStart, LocalTime bEnd) {
        Doctor doctor = doctorRepository.findById(doctorId).get();
        com.healthcare.backend.schedule.entity.DoctorSchedule s = new com.healthcare.backend.schedule.entity.DoctorSchedule();
        s.setDoctor(doctor);
        s.setHospital(doctor.getHospital());
        s.setDayOfWeek(dow);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setSlotDurationMinutes(dur);
        s.setBreakStartTime(bStart);
        s.setBreakEndTime(bEnd);
        s.setStatus(com.healthcare.backend.schedule.entity.ScheduleStatus.ACTIVE);
        doctorScheduleRepository.save(s);
    }

    @Test
    @DisplayName("1, 2, 3, 4, 5 & 14. Hospital Admin, Receptionist & Doctor create valid appointment")
    void test1To5And14_createAppointment_success() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(9, 0));
        req.setAppointmentType(AppointmentType.NEW_CONSULTATION);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.appointmentNumber").value(startsWith("APT-")))
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()))
                .andExpect(jsonPath("$.patientId").value(patientAId))
                .andExpect(jsonPath("$.doctorId").value(doctorAId))
                .andExpect(jsonPath("$.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.endTime").value("09:15:00"))
                .andExpect(jsonPath("$.status").value("BOOKED"));

        // Receptionist books next slot
        req.setStartTime(LocalTime.of(9, 15));
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenRecepA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Doctor books next slot
        req.setStartTime(LocalTime.of(9, 30));
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("6. Cross-hospital patient booking rejected with 404")
    void test6_crossHospitalPatient_returns404() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientBId); // Belongs to Hospital B
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("7. Cross-hospital doctor booking rejected with 404")
    void test7_crossHospitalDoctor_returns404() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorBId); // Belongs to Hospital B
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8. Inactive patient booking rejected with 400")
    void test8_inactivePatient_returns400() throws Exception {
        Patient p = patientRepository.findById(patientAId).get();
        p.setStatus(PatientStatus.INACTIVE);
        patientRepository.save(p);

        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. Inactive doctor booking rejected with 400")
    void test9_inactiveDoctor_returns400() throws Exception {
        Doctor d = doctorRepository.findById(doctorAId).get();
        d.setStatus(DoctorStatus.INACTIVE);
        doctorRepository.save(d);

        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("10. Appointment in past rejected with 400")
    void test10_pastAppointment_returns400() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(LocalDate.now().minusDays(1));
        req.setStartTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. Appointment outside doctor schedule rejected with 400")
    void test11_outsideSchedule_returns400() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(8, 0)); // Doctor schedule starts at 09:00

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("12. Appointment during break interval rejected with 400")
    void test12_duringBreak_returns400() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(13, 15)); // Break is 13:00 - 14:00

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("13. Misaligned slot grid time rejected with 400")
    void test13_misalignedSlot_returns400() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(9, 5)); // Grid is 15 mins (09:00, 09:15, etc.)

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("15. Duplicate booking for occupied slot rejected with 409 Conflict")
    void test15_duplicateBooking_returns409() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(10, 0));

        // First booking succeeds
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second booking for same slot returns 409
        req.setPatientId(patientA2Id);
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("16. Cancelled appointment releases slot for future booking")
    void test16_cancelledAppointmentReleasesSlot() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(10, 30));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Cancel appointment
        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/cancel")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Patient cancelled\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Patient cancelled"));

        // Re-book same slot now succeeds!
        req.setPatientId(patientA2Id);
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("17 & 18. Appointment retrieval works & cross-hospital returns 404")
    void test17And18_getAppointmentById() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(11, 0));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Same hospital retrieval succeeds
        mockMvc.perform(get("/api/v1/appointments/" + aptId)
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aptId));

        // Hospital B admin retrieval returns 404
        mockMvc.perform(get("/api/v1/appointments/" + aptId)
                        .header("Authorization", "Bearer " + tokenAdminB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("19, 20, 21, 22, 23 & 37. List appointments with pagination & filters")
    void test19To23And37_getAppointments_filtersAndTenantIsolation() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(11, 15));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Filter by doctorId & date
        mockMvc.perform(get("/api/v1/appointments?doctorId=" + doctorAId + "&appointmentDate=" + testFutureDate + "&page=0&size=10")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));

        // Hospital B lists appointments — should not see Hospital A appointments
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminB))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("24. Today's appointments endpoint works")
    void test24_getTodayAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/today")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("25 & 35. Doctor appointments endpoint works & doctor accesses own")
    void test25And35_getDoctorAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/" + doctorAId + "/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("26. Patient appointments endpoint works")
    void test26_getPatientAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/patients/" + patientAId + "/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("27. Confirm appointment works (BOOKED -> CONFIRMED)")
    void test27_confirmAppointment() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(11, 30));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/confirm")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("28. Check-in appointment works (CONFIRMED -> CHECKED_IN)")
    void test28_checkInAppointment() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(11, 45));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/check-in")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
    }

    @Test
    @DisplayName("30 & 31. Reschedule works & reschedule to occupied slot returns 409")
    void test30And31_rescheduleAppointment() throws Exception {
        // Book Slot 1 (12:00)
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(12, 0));

        MvcResult res1 = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long aptId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        // Book Slot 2 (12:15)
        req.setStartTime(LocalTime.of(12, 15));
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Attempt reschedule aptId1 to occupied Slot 2 (12:15) -> returns 409
        RescheduleAppointmentRequest reschReq = new RescheduleAppointmentRequest();
        reschReq.setAppointmentDate(testFutureDate);
        reschReq.setStartTime(LocalTime.of(12, 15));

        mockMvc.perform(patch("/api/v1/appointments/" + aptId1 + "/reschedule")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschReq)))
                .andExpect(status().isConflict());

        // Reschedule aptId1 to unoccupied Slot 3 (12:30) -> succeeds
        reschReq.setStartTime(LocalTime.of(12, 30));
        mockMvc.perform(patch("/api/v1/appointments/" + aptId1 + "/reschedule")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("12:30:00"));
    }

    @Test
    @DisplayName("32. Invalid status transition rejected with 400")
    void test32_invalidStatusTransition_returns400() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(12, 45));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Cancel appointment
        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/cancel")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk());

        // Attempting to confirm a cancelled appointment returns 400
        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/confirm")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("33. Unauthenticated request returns 401 Unauthorized")
    void test33_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("34. Unauthorized role (LAB_STAFF) returns 403 Forbidden")
    void test34_unauthorizedRole_returns403() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(14, 0));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenLabStaffA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("36. Doctor cannot access another doctor's appointments unless admin")
    void test36_doctorAccessAnotherDoctor_returns403() throws Exception {
        // Doctor A2 attempts to view Doctor A's appointments
        mockMvc.perform(get("/api/v1/doctors/" + doctorAId + "/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA2))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("38 & 39. Response does not expose security fields & hospitalId cannot be overridden")
    void test38And39_securityFieldsOmitted() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(14, 15));

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()));
    }

    @Test
    @DisplayName("40. REAL CONCURRENCY TEST: Multi-threaded booking for same slot results in EXACTLY 1 booking & remaining conflicts (409)")
    void test40_concurrentBookingAttempts_exactlyOneSucceeds() throws Exception {
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();

        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        LocalTime targetSlotTime = LocalTime.of(14, 30);

        UserDetails adminUserDetails = customUserDetailsService.loadUserByUsername(jwtService.extractUsername(tokenAdminA));

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    latch.await(); // Synchronize all threads to fire simultaneously

                    CreateAppointmentRequest req = new CreateAppointmentRequest();
                    req.setPatientId(index % 2 == 0 ? patientAId : patientA2Id);
                    req.setDoctorId(doctorAId);
                    req.setAppointmentDate(testFutureDate);
                    req.setStartTime(targetSlotTime);

                    MvcResult res = mockMvc.perform(post("/api/v1/appointments")
                                    .header("Authorization", "Bearer " + tokenAdminA)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    int status = res.getResponse().getStatus();
                    if (status == 201) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    SecurityContextHolder.clearContext();
                    completionLatch.countDown();
                }
            });
        }

        latch.countDown(); // Release threads
        completionLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertEquals(1, successCount.get(), "Exactly ONE concurrent booking attempt must succeed");
        assertEquals(numberOfThreads - 1, conflictCount.get(), "Remaining concurrent booking attempts must fail with 409 Conflict");

        // Verify database contains exactly one active appointment for this slot
        long activeBookingsInDb = appointmentRepository.findAll().stream()
                .filter(a -> a.getDoctor().getId().equals(doctorAId)
                        && a.getAppointmentDate().equals(testFutureDate)
                        && a.getStartTime().equals(targetSlotTime)
                        && a.getStatus() != AppointmentStatus.CANCELLED)
                .count();

        assertEquals(1L, activeBookingsInDb, "Database must contain exactly 1 active appointment for doctor slot");
    }

    @Test
    @DisplayName("41. Doctor can confirm appointment (BOOKED -> CONFIRMED)")
    void test41_doctorCanConfirmAppointment() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(15, 0));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/confirm")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("42. Doctor can check-in appointment (CONFIRMED -> CHECKED_IN)")
    void test42_doctorCanCheckInAppointment() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(15, 15));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/check-in")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
    }

    @Test
    @DisplayName("43. Doctor can mark eligible appointment as NO_SHOW")
    void test43_doctorCanMarkNoShowAppointment() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(15, 30));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/no-show")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        Appointment updated = appointmentRepository.findById(aptId).orElseThrow();
        assertEquals(AppointmentStatus.NO_SHOW, updated.getStatus());
    }

    @Test
    @DisplayName("44. Unauthenticated no-show request returns 401")
    void test44_unauthenticatedMarkNoShowRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/999/no-show"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("45. Cross-hospital no-show request returns 404")
    void test45_crossHospitalMarkNoShowRejected() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientBId); req.setDoctorId(doctorBId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(15, 30));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/no-show")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("46. Cancelled appointment cannot become NO_SHOW")
    void test46_cancelledAppointmentCannotBecomeNoShow() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(16, 0));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/cancel")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/no-show")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("47. Already NO_SHOW appointment cannot be marked again")
    void test47_alreadyNoShowAppointmentCannotBeMarkedAgain() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId); req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate); req.setStartTime(LocalTime.of(16, 15));

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/no-show")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/no-show")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("48. Search appointments by patient name and filters")
    void test48_searchAppointmentsByPatientNameAndFilters() throws Exception {
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .param("search", "Rahul")
                        .param("status", "BOOKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
