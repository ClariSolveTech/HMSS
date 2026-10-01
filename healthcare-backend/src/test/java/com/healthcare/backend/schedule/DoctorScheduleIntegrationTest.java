package com.healthcare.backend.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.auth.service.AuthService;
import com.healthcare.backend.doctor.dto.CreateDoctorRequest;
import com.healthcare.backend.doctor.dto.DoctorResponse;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.doctor.service.DoctorService;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.schedule.dto.CreateDoctorScheduleRequest;
import com.healthcare.backend.schedule.dto.UpdateDoctorScheduleRequest;
import com.healthcare.backend.schedule.entity.DoctorSchedule;
import com.healthcare.backend.schedule.entity.ScheduleStatus;
import com.healthcare.backend.schedule.repository.DoctorScheduleRepository;
import com.healthcare.backend.schedule.service.SlotGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DoctorScheduleIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private SlotGenerationService slotGenerationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private String tokenAdminA;
    private Doctor doctorA;

    private String tokenAdminB;
    private Doctor doctorB;

    private String tokenDoctorA;
    private String tokenDoctorB;
    private String tokenReceptionistA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Ensure roles exist
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
            }
        }

        long ts = System.currentTimeMillis();

        // Create Hospital A
        hospitalA = new Hospital();
        hospitalA.setName("Alpha Medical Center");
        hospitalA.setCode("AMC" + (ts % 10000));
        hospitalA.setPhone("+919800000001");
        hospitalA.setEmail("alpha." + ts + "@med.com");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        // Create Hospital B
        hospitalB = new Hospital();
        hospitalB.setName("Beta General Hospital");
        hospitalB.setCode("BGH" + (ts % 10000));
        hospitalB.setPhone("+919800000002");
        hospitalB.setEmail("beta." + ts + "@med.com");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        // Admin A (Hospital A)
        User adminUserA = createUserDirect("Admin", "Alpha", "admin.a." + ts + "@med.com", "98" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.HOSPITAL_ADMIN);
        tokenAdminA = jwtService.generateToken(adminUserA);

        // Admin B (Hospital B)
        User adminUserB = createUserDirect("Admin", "Beta", "admin.b." + ts + "@med.com", "96" + (ts % 100000000L), "Pass@1234", hospitalB, RoleName.HOSPITAL_ADMIN);
        tokenAdminB = jwtService.generateToken(adminUserB);

        // Doctor User A (Hospital A)
        User docUserA = createUserDirect("Rajesh", "Sharma", "doc.a." + ts + "@med.com", "97" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.DOCTOR);
        tokenDoctorA = jwtService.generateToken(docUserA);

        // Create Doctor Profile A
        Doctor docEntityA = new Doctor();
        docEntityA.setUser(docUserA);
        docEntityA.setHospital(hospitalA);
        docEntityA.setDoctorCode(hospitalA.getCode() + "-D00001");
        docEntityA.setSpecialization("Cardiology");
        docEntityA.setQualification("MD");
        docEntityA.setLicenseNumber("LIC-" + ts);
        docEntityA.setConsultationFee(new BigDecimal("500.00"));
        doctorA = doctorRepository.save(docEntityA);

        // Doctor User B (Hospital B)
        User docUserB = createUserDirect("Priya", "Patel", "doc.b." + ts + "@med.com", "95" + (ts % 100000000L), "Pass@1234", hospitalB, RoleName.DOCTOR);
        tokenDoctorB = jwtService.generateToken(docUserB);

        // Create Doctor Profile B
        Doctor docEntityB = new Doctor();
        docEntityB.setUser(docUserB);
        docEntityB.setHospital(hospitalB);
        docEntityB.setDoctorCode(hospitalB.getCode() + "-D00001");
        docEntityB.setSpecialization("Neurology");
        docEntityB.setQualification("DM");
        docEntityB.setLicenseNumber("LIC-B-" + ts);
        docEntityB.setConsultationFee(new BigDecimal("700.00"));
        doctorB = doctorRepository.save(docEntityB);

        // Receptionist A (Hospital A)
        User recepUserA = createUserDirect("Sam", "Recep", "recep.a." + ts + "@med.com", "94" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.RECEPTIONIST);
        tokenReceptionistA = jwtService.generateToken(recepUserA);
    }

    private User createUserDirect(String firstName, String lastName, String email, String mobile, String rawPassword, Hospital hospital, RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setHospital(hospital);
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("1, 2 & 3. Hospital admin creates schedule belonging to doctor and hospital")
    void test1To3_adminCreatesSchedule_success() throws Exception {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest();
        request.setDoctorId(doctorA.getId());
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(13, 0));
        request.setSlotDurationMinutes(15);
        request.setBreakStartTime(LocalTime.of(11, 0));
        request.setBreakEndTime(LocalTime.of(11, 30));

        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.doctorId").value(doctorA.getId()))
                .andExpect(jsonPath("$.doctorName").value("Rajesh Sharma"))
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()))
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.endTime").value("13:00:00"))
                .andExpect(jsonPath("$.slotDurationMinutes").value(15))
                .andExpect(jsonPath("$.breakStartTime").value("11:00:00"))
                .andExpect(jsonPath("$.breakEndTime").value("11:30:00"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("4. Invalid time range rejected (startTime >= endTime)")
    void test4_invalidTimeRangeRejected() throws Exception {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest();
        request.setDoctorId(doctorA.getId());
        request.setDayOfWeek(DayOfWeek.TUESDAY);
        request.setStartTime(LocalTime.of(14, 0));
        request.setEndTime(LocalTime.of(10, 0)); // invalid
        request.setSlotDurationMinutes(15);

        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5. Invalid break range rejected")
    void test5_invalidBreakRejected() throws Exception {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest();
        request.setDoctorId(doctorA.getId());
        request.setDayOfWeek(DayOfWeek.WEDNESDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(17, 0));
        request.setSlotDurationMinutes(30);
        request.setBreakStartTime(LocalTime.of(14, 0));
        request.setBreakEndTime(LocalTime.of(13, 0)); // breakStart > breakEnd

        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. Invalid slot duration rejected (slotDuration <= 0)")
    void test6_invalidSlotDurationRejected() throws Exception {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest();
        request.setDoctorId(doctorA.getId());
        request.setDayOfWeek(DayOfWeek.THURSDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(12, 0));
        request.setSlotDurationMinutes(0); // invalid

        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Overlapping schedule rejected with 409 Conflict")
    void test7_overlappingScheduleRejected() throws Exception {
        // Create initial schedule 09:00 - 13:00
        CreateDoctorScheduleRequest req1 = new CreateDoctorScheduleRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setDayOfWeek(DayOfWeek.FRIDAY);
        req1.setStartTime(LocalTime.of(9, 0));
        req1.setEndTime(LocalTime.of(13, 0));
        req1.setSlotDurationMinutes(20);

        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Attempt overlapping schedule 12:00 - 15:00 on FRIDAY
        CreateDoctorScheduleRequest req2 = new CreateDoctorScheduleRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setDayOfWeek(DayOfWeek.FRIDAY);
        req2.setStartTime(LocalTime.of(12, 0));
        req2.setEndTime(LocalTime.of(15, 0));
        req2.setSlotDurationMinutes(20);

        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("8. Non-overlapping split schedule accepted")
    void test8_nonOverlappingSplitScheduleAccepted() throws Exception {
        // Shift 1: 09:00 - 13:00
        CreateDoctorScheduleRequest req1 = new CreateDoctorScheduleRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setDayOfWeek(DayOfWeek.MONDAY);
        req1.setStartTime(LocalTime.of(9, 0));
        req1.setEndTime(LocalTime.of(13, 0));
        req1.setSlotDurationMinutes(30);

        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Shift 2: 14:00 - 18:00
        CreateDoctorScheduleRequest req2 = new CreateDoctorScheduleRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setDayOfWeek(DayOfWeek.MONDAY);
        req2.setStartTime(LocalTime.of(14, 0));
        req2.setEndTime(LocalTime.of(18, 0));
        req2.setSlotDurationMinutes(30);

        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("9 & 10. Same-hospital retrieval works, Cross-hospital retrieval returns 404")
    void test9And10_tenantIsolationRetrieval() throws Exception {
        // Admin A creates schedule for Doctor A
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest();
        request.setDoctorId(doctorA.getId());
        request.setDayOfWeek(DayOfWeek.TUESDAY);
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(14, 0));
        request.setSlotDurationMinutes(30);

        String responseContent = mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long scheduleId = objectMapper.readTree(responseContent).get("id").asLong();

        // Admin A (Hospital A) retrieves schedule -> 200 OK
        mockMvc.perform(get("/api/v1/doctor-schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scheduleId));

        // Admin B (Hospital B) attempts retrieval -> 404 Not Found
        mockMvc.perform(get("/api/v1/doctor-schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + tokenAdminB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("11. Hospital A cannot create schedule for Hospital B doctor")
    void test11_hospitalACannotCreateScheduleForHospitalBDoctor() throws Exception {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest();
        request.setDoctorId(doctorB.getId()); // Doctor B belongs to Hospital B
        request.setDayOfWeek(DayOfWeek.WEDNESDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(12, 0));
        request.setSlotDurationMinutes(15);

        // Admin A (Hospital A) attempts creation for Doctor B -> 404 Not Found
        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("12 & 13. Unauthenticated returns 401, Unauthorized role returns 403")
    void test12And13_securityAccessControl() throws Exception {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest();
        request.setDoctorId(doctorA.getId());
        request.setDayOfWeek(DayOfWeek.THURSDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(12, 0));
        request.setSlotDurationMinutes(15);

        // 12. Unauthenticated -> 401 Unauthorized
        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // 13. Receptionist role -> 403 Forbidden
        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenReceptionistA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14 & 15. Doctor can retrieve own schedules, Doctor cannot retrieve another hospital's schedule")
    void test14And15_doctorSelfSchedules() throws Exception {
        // Admin A creates schedule for Doctor A
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest();
        request.setDoctorId(doctorA.getId());
        request.setDayOfWeek(DayOfWeek.FRIDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(12, 0));
        request.setSlotDurationMinutes(30);

        String responseContent = mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long scheduleIdA = objectMapper.readTree(responseContent).get("id").asLong();

        // Doctor A calls /doctors/me/schedules -> returns list containing scheduleIdA
        mockMvc.perform(get("/api/v1/doctors/me/schedules")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].doctorId").value(doctorA.getId()));

        // Doctor B tries to access Doctor A's schedule directly -> 404 Not Found
        mockMvc.perform(get("/api/v1/doctor-schedules/" + scheduleIdA)
                        .header("Authorization", "Bearer " + tokenDoctorB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("16, 17 & 18. Pagination, day filtering, and status filtering")
    void test16To18_scheduleListingFilters() throws Exception {
        // Create 2 schedules for Doctor A
        CreateDoctorScheduleRequest req1 = new CreateDoctorScheduleRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setDayOfWeek(DayOfWeek.MONDAY);
        req1.setStartTime(LocalTime.of(9, 0));
        req1.setEndTime(LocalTime.of(12, 0));
        req1.setSlotDurationMinutes(30);
        mockMvc.perform(post("/api/v1/doctor-schedules")
                .header("Authorization", "Bearer " + tokenAdminA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        CreateDoctorScheduleRequest req2 = new CreateDoctorScheduleRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setDayOfWeek(DayOfWeek.TUESDAY);
        req2.setStartTime(LocalTime.of(14, 0));
        req2.setEndTime(LocalTime.of(17, 0));
        req2.setSlotDurationMinutes(30);
        mockMvc.perform(post("/api/v1/doctor-schedules")
                .header("Authorization", "Bearer " + tokenAdminA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        // Test Pagination & Filtering by dayOfWeek = MONDAY
        mockMvc.perform(get("/api/v1/doctor-schedules?dayOfWeek=MONDAY&page=0&size=10")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].dayOfWeek").value("MONDAY"));

        // Test Filtering by status = ACTIVE
        mockMvc.perform(get("/api/v1/doctor-schedules?status=ACTIVE&page=0&size=10")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("19 & 20. Theoretical slot generation & break interval exclusion")
    void test19And20_slotGenerationWithBreak() {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(10, 0));
        schedule.setSlotDurationMinutes(15);
        schedule.setBreakStartTime(LocalTime.of(9, 30));
        schedule.setBreakEndTime(LocalTime.of(9, 45));

        var slots = slotGenerationService.generateSlots(schedule);

        // Expected slots: 09:00-09:15, 09:15-09:30, 09:45-10:00 (3 slots, break 09:30-09:45 excluded)
        assertEquals(3, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).getStartTime());
        assertEquals(LocalTime.of(9, 15), slots.get(0).getEndTime());

        assertEquals(LocalTime.of(9, 15), slots.get(1).getStartTime());
        assertEquals(LocalTime.of(9, 30), slots.get(1).getEndTime());

        assertEquals(LocalTime.of(9, 45), slots.get(2).getStartTime());
        assertEquals(LocalTime.of(10, 0), slots.get(2).getEndTime());
    }

    @Test
    @DisplayName("21. Availability API returns correct day-specific slots")
    void test21_availabilityEndpoint() throws Exception {
        // Create schedule on MONDAY for Doctor A: 09:00-10:00, 15-min slots, no break
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest();
        request.setDoctorId(doctorA.getId());
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setSlotDurationMinutes(15);

        mockMvc.perform(post("/api/v1/doctor-schedules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2026-08-10 is a MONDAY
        LocalDate mondayDate = LocalDate.of(2026, 8, 10);

        mockMvc.perform(get("/api/v1/doctors/" + doctorA.getId() + "/availability?date=" + mondayDate)
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(doctorA.getId()))
                .andExpect(jsonPath("$.date").value("2026-08-10"))
                .andExpect(jsonPath("$.slots", hasSize(4)))
                .andExpect(jsonPath("$.slots[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("09:15:00"))
                .andExpect(jsonPath("$.slots[0].available").value(true));
    }
}
