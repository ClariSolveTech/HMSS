package com.healthcare.backend.teleconsult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.appointment.dto.CreateAppointmentRequest;
import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.appointment.entity.BookingSource;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.config.DatabaseConstraintInitializer;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.schedule.entity.DoctorSchedule;
import com.healthcare.backend.schedule.entity.ScheduleStatus;
import com.healthcare.backend.schedule.repository.DoctorScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class TeleconsultationIntegrationTest {

    private static final AtomicLong COUNTER = new AtomicLong(20000);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DatabaseConstraintInitializer databaseConstraintInitializer;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    private String tokenDoctorA;
    private String tokenDoctorB;
    private String tokenRecepA;

    private Long doctorAId;
    private Long doctorBId;
    private Long patientAId;

    private LocalDate testFutureDate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        long ts = System.currentTimeMillis() + COUNTER.incrementAndGet();

        // 1. Hospitals
        Hospital hospitalA = new Hospital();
        hospitalA.setName("Teleconsult Hospital Alpha " + ts);
        hospitalA.setCode("THA" + ts % 100000L);
        hospitalA.setEmail("admin.tha." + ts + "@hospital.com");
        hospitalA.setPhone("+91910000" + (ts % 10000L));
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        Hospital hospitalB = new Hospital();
        hospitalB.setName("Teleconsult Hospital Beta " + ts);
        hospitalB.setCode("THB" + ts % 100000L);
        hospitalB.setEmail("admin.thb." + ts + "@hospital.com");
        hospitalB.setPhone("+91920000" + (ts % 10000L));
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        // 2. Users & Tokens
        User userDocA = createUserDirect("Arun", "Kumar", "doc.arun." + ts + "@tha.com", "91" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.DOCTOR);
        tokenDoctorA = jwtService.generateToken(userDocA);

        User userRecepA = createUserDirect("Riya", "Patel", "recep.riya." + ts + "@tha.com", "92" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.RECEPTIONIST);
        tokenRecepA = jwtService.generateToken(userRecepA);

        User userDocB = createUserDirect("Bhavna", "Joshi", "doc.bhavna." + ts + "@thb.com", "93" + (ts % 100000000L), "Pass@1234", hospitalB, RoleName.DOCTOR);
        tokenDoctorB = jwtService.generateToken(userDocB);

        // 3. Doctors
        doctorAId = createDoctorHelper(userDocA, "Telemedicine", "LIC-TC-A");
        doctorBId = createDoctorHelper(userDocB, "Cardiology", "LIC-TC-B");

        // 4. Patients
        patientAId = createPatientHelper(hospitalA, "Rohan", "Verma", "rohan." + ts + "@test.com", "981" + (ts % 10000000L));

        // 5. Doctor Schedule
        testFutureDate = LocalDate.now().plusDays(2);
        DayOfWeek testDow = testFutureDate.getDayOfWeek();
        createScheduleHelper(doctorAId, testDow, LocalTime.of(9, 0), LocalTime.of(17, 0), 15, LocalTime.of(13, 0), LocalTime.of(14, 0));
    }

    private User createUserDirect(String f, String l, String e, String m, String p, Hospital h, RoleName r) {
        Role role = roleRepository.findByName(r).orElseGet(() -> roleRepository.save(new Role(r)));
        User u = new User();
        u.setFirstName(f);
        u.setLastName(l);
        u.setEmail(e);
        u.setMobileNumber(m);
        u.setPasswordHash(passwordEncoder.encode(p));
        u.setHospital(h);
        u.setStatus(UserStatus.ACTIVE);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    private Long createDoctorHelper(User user, String spec, String lic) {
        Doctor d = new Doctor();
        d.setUser(user);
        d.setHospital(user.getHospital());
        long seq = COUNTER.incrementAndGet();
        d.setDoctorCode(user.getHospital().getCode() + "-D" + seq);
        d.setSpecialization(spec);
        d.setQualification("MD");
        d.setLicenseNumber(lic + "-" + seq);
        d.setConsultationFee(new BigDecimal("600.00"));
        d.setStatus(DoctorStatus.ACTIVE);
        return doctorRepository.save(d).getId();
    }

    private Long createPatientHelper(Hospital hospital, String f, String l, String e, String m) {
        Patient p = new Patient();
        p.setHospital(hospital);
        long seq = COUNTER.incrementAndGet();
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
        DoctorSchedule s = new DoctorSchedule();
        s.setDoctor(doctor);
        s.setHospital(doctor.getHospital());
        s.setDayOfWeek(dow);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setSlotDurationMinutes(dur);
        s.setBreakStartTime(bStart);
        s.setBreakEndTime(bEnd);
        s.setStatus(ScheduleStatus.ACTIVE);
        doctorScheduleRepository.save(s);
    }

    @Test
    @DisplayName("1. Provision teleconsultation appointment with mock provider")
    void testCreateTeleconsultation_provisionsMeetingWithMockProvider() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(10, 0));
        req.setAppointmentType(AppointmentType.TELECONSULTATION);
        req.setBookingSource(BookingSource.RECEPTION);
        req.setReason("Online remote consultation");

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenRecepA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentType").value("TELECONSULTATION"))
                .andExpect(jsonPath("$.meetUrl", startsWith("https://meet.mock.internal/teleconsult/simulated-")))
                .andExpect(jsonPath("$.teleconsultationSpaceId", startsWith("spaces/mock-simulated-")))
                .andExpect(jsonPath("$.isTeleconsultation").value(true))
                .andReturn();

        Long appointmentId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Verify retrieval matches persisted meeting metadata
        mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId))
                .andExpect(jsonPath("$.appointmentType").value("TELECONSULTATION"))
                .andExpect(jsonPath("$.meetUrl", startsWith("https://meet.mock.internal/teleconsult/simulated-")))
                .andExpect(jsonPath("$.teleconsultationSpaceId", startsWith("spaces/mock-simulated-")))
                .andExpect(jsonPath("$.isTeleconsultation").value(true));
    }

    @Test
    @DisplayName("2. Standard appointments do NOT trigger meeting creation")
    void testStandardAppointment_doesNotProvisionMeeting() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(10, 15));
        req.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        req.setBookingSource(BookingSource.RECEPTION);
        req.setReason("In-person consultation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenRecepA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentType").value("NEW_CONSULTATION"))
                .andExpect(jsonPath("$.meetUrl").doesNotExist())
                .andExpect(jsonPath("$.teleconsultationSpaceId").doesNotExist())
                .andExpect(jsonPath("$.isTeleconsultation").value(false));
    }

    @Test
    @DisplayName("3. Idempotency: Confirming teleconsultation with existing meeting preserves meeting details")
    void testConfirmAppointment_isIdempotent_doesNotDuplicateMeeting() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(10, 30));
        req.setAppointmentType(AppointmentType.TELECONSULTATION);
        req.setBookingSource(BookingSource.RECEPTION);

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String originalMeetUrl = objectMapper.readTree(result.getResponse().getContentAsString()).get("meetUrl").asText();
        String originalSpaceId = objectMapper.readTree(result.getResponse().getContentAsString()).get("teleconsultationSpaceId").asText();
        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Confirm appointment
        mockMvc.perform(patch("/api/v1/appointments/" + aptId + "/confirm")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.meetUrl").value(originalMeetUrl))
                .andExpect(jsonPath("$.teleconsultationSpaceId").value(originalSpaceId));
    }

    @Test
    @DisplayName("4. Tenant isolation: doctor from another hospital cannot access teleconsultation appointment")
    void testTenantIsolation_unauthorizedDoctorCannotViewMeeting() throws Exception {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(patientAId);
        req.setDoctorId(doctorAId);
        req.setAppointmentDate(testFutureDate);
        req.setStartTime(LocalTime.of(11, 0));
        req.setAppointmentType(AppointmentType.TELECONSULTATION);

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long aptId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Doctor from hospital B attempts to view Doctor A's appointment
        mockMvc.perform(get("/api/v1/appointments/" + aptId)
                        .header("Authorization", "Bearer " + tokenDoctorB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. Database constraint initializer runs safely and idempotently")
    void testDatabaseConstraintInitializer_runsSafely() {
        assertDoesNotThrow(() -> databaseConstraintInitializer.run(new DefaultApplicationArguments()));
    }
}
