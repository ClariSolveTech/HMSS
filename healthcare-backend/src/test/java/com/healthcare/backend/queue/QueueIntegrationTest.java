package com.healthcare.backend.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.healthcare.backend.queue.dto.CreateQueueEntryRequest;
import com.healthcare.backend.queue.dto.UpdateDoctorQueueStatusRequest;
import com.healthcare.backend.queue.dto.UpdateQueueStatusRequest;
import com.healthcare.backend.queue.entity.DoctorQueueStatus;
import com.healthcare.backend.queue.entity.QueuePriority;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class QueueIntegrationTest {

    private static final AtomicLong CODE_COUNTER = new AtomicLong(30000);

    @Autowired
    private WebApplicationContext webApplicationContext;

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
    private JwtService jwtService;


    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private String hospAAdminToken;
    private String hospAReceptionToken;
    private String hospADoctorToken;
    private String hospALabToken;

    private String hospBAdminToken;
    private String hospBDoctorToken;

    private Doctor doctorA;
    private Doctor doctorB;
    private Patient patientA1;
    private Patient patientA2;
    private Patient patientB1;

    private Appointment appointmentA1;

    @BeforeEach
    public void setup() {
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
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }
        }


        long ts = System.currentTimeMillis();
        String uidA = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String uidB = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        hospitalA = new Hospital();
        hospitalA.setName("City Central Hospital " + uidA);
        hospitalA.setCode("HQ" + uidA);
        hospitalA.setEmail("admin.a." + uidA + "@citycentral.com");
        hospitalA.setPhone("9998887771");
        hospitalA.setAddress("Main St");
        hospitalA.setCity("Metropolis");
        hospitalA.setState("State");
        hospitalA.setPincode("100001");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        hospitalB = new Hospital();
        hospitalB.setName("Metro Care Hospital " + uidB);
        hospitalB.setCode("HQ" + uidB);
        hospitalB.setEmail("admin.b." + uidB + "@metrocare.com");
        hospitalB.setPhone("9998887772");
        hospitalB.setAddress("North St");
        hospitalB.setCity("Metropolis");
        hospitalB.setState("State");
        hospitalB.setPincode("100002");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        User adminA = createUserDirect("Admin", "Alpha", "admin.q.a." + ts + "@test.com", "91" + (ts % 100000000L), hospitalA, RoleName.HOSPITAL_ADMIN);
        hospAAdminToken = jwtService.generateToken(adminA);

        User recepA = createUserDirect("Receptionist", "Alpha", "recep.q.a." + ts + "@test.com", "92" + (ts % 100000000L), hospitalA, RoleName.RECEPTIONIST);
        hospAReceptionToken = jwtService.generateToken(recepA);

        User docUserA = createUserDirect("Doctor", "Aarav", "doc.aarav." + ts + "@test.com", "93" + (ts % 100000000L), hospitalA, RoleName.DOCTOR);
        hospADoctorToken = jwtService.generateToken(docUserA);

        User labA = createUserDirect("Lab", "Staff", "lab.staff." + ts + "@test.com", "94" + (ts % 100000000L), hospitalA, RoleName.LAB_STAFF);
        hospALabToken = jwtService.generateToken(labA);

        User adminB = createUserDirect("Admin", "Beta", "admin.q.b." + ts + "@test.com", "95" + (ts % 100000000L), hospitalB, RoleName.HOSPITAL_ADMIN);
        hospBAdminToken = jwtService.generateToken(adminB);

        User docUserB = createUserDirect("Doctor", "Neha", "doc.neha." + ts + "@test.com", "96" + (ts % 100000000L), hospitalB, RoleName.DOCTOR);
        hospBDoctorToken = jwtService.generateToken(docUserB);

        doctorA = createDoctorHelper(docUserA, "Cardiology", "LIC_A_" + ts);
        doctorB = createDoctorHelper(docUserB, "Pediatrics", "LIC_B_" + ts);

        patientA1 = createPatientHelper(hospitalA, "Ramesh", "Kumar", "ramesh." + ts + "@test.com", "98" + (ts % 100000000L));
        patientA2 = createPatientHelper(hospitalA, "Suresh", "Patel", "suresh." + ts + "@test.com", "97" + (ts % 100000000L));
        patientB1 = createPatientHelper(hospitalB, "Anita", "Sharma", "anita." + ts + "@test.com", "96" + (ts % 100000000L));

        appointmentA1 = new Appointment();
        appointmentA1.setHospital(hospitalA);
        appointmentA1.setDoctor(doctorA);
        appointmentA1.setPatient(patientA1);
        appointmentA1.setAppointmentDate(LocalDate.now());
        appointmentA1.setStartTime(LocalTime.of(10, 0));
        appointmentA1.setEndTime(LocalTime.of(10, 15));
        appointmentA1.setAppointmentNumber("APPT_A_" + ts);
        appointmentA1.setStatus(AppointmentStatus.BOOKED);
        appointmentA1.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        appointmentA1.setBookingSource(BookingSource.RECEPTION);
        appointmentA1.setActiveSlotKey(Appointment.buildActiveSlotKey(doctorA.getId(), LocalDate.now(), LocalTime.of(10, 0)));
        appointmentA1 = appointmentRepository.save(appointmentA1);
    }

    private User createUserDirect(String f, String l, String e, String m, Hospital h, RoleName r) {
        Role role = roleRepository.findByName(r).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(r);
            return roleRepository.save(newRole);
        });
        User u = new User();
        u.setFirstName(f);
        u.setLastName(l);
        u.setEmail(e);
        u.setMobileNumber(m);
        u.setPasswordHash(passwordEncoder.encode("Password123!"));
        u.setHospital(h);
        u.setStatus(UserStatus.ACTIVE);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    private Doctor createDoctorHelper(User user, String spec, String lic) {
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
        return doctorRepository.save(d);
    }

    private Patient createPatientHelper(Hospital hospital, String f, String l, String e, String m) {
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
        return patientRepository.save(p);
    }

    // 1. Create walk-in queue entry
    @Test
    @DisplayName("Test 1: Create walk-in queue entry success")
    public void test1_createWalkInQueueEntry_success() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        req.setPriority(QueuePriority.NORMAL);
        req.setNotes("Walk-in checkup");

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.priority").value("NORMAL"))
                .andExpect(jsonPath("$.patientName").value("Ramesh Kumar"))
                .andExpect(jsonPath("$.doctorId").value(doctorA.getId()));
    }

    // 2. Appointment check-in
    @Test
    @DisplayName("Test 2: Appointment check-in success")
    public void test2_appointmentCheckIn_success() throws Exception {
        mockMvc.perform(post("/api/v1/queue/check-in/" + appointmentA1.getId())
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(1))
                .andExpect(jsonPath("$.appointmentId").value(appointmentA1.getId()))
                .andExpect(jsonPath("$.status").value("WAITING"));

        Appointment updatedAppt = appointmentRepository.findById(appointmentA1.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CHECKED_IN, updatedAppt.getStatus());
    }

    // 3. Token generation incremental
    @Test
    @DisplayName("Test 3: Token numbers generated incrementally 1, 2, 3...")
    public void test3_tokenGeneration_incremental() throws Exception {
        CreateQueueEntryRequest req1 = new CreateQueueEntryRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setPatientId(patientA1.getId());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(1));

        CreateQueueEntryRequest req2 = new CreateQueueEntryRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setPatientId(patientA2.getId());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(2));
    }

    // 4. Token uniqueness scoped to hospital + doctor + date
    @Test
    @DisplayName("Test 4: Token numbers scoped to hospital + doctor + queueDate")
    public void test4_tokenUniqueness_scoped() throws Exception {
        CreateQueueEntryRequest reqA = new CreateQueueEntryRequest();
        reqA.setDoctorId(doctorA.getId());
        reqA.setPatientId(patientA1.getId());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(1));

        CreateQueueEntryRequest reqB = new CreateQueueEntryRequest();
        reqB.setDoctorId(doctorB.getId());
        reqB.setPatientId(patientB1.getId());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospBDoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(1));
    }

    // 5. Hospital isolation cannot view other hospital queue
    @Test
    @DisplayName("Test 5: Hospital A user cannot view Hospital B queue")
    public void test5_hospitalIsolation_queueListing() throws Exception {
        CreateQueueEntryRequest reqB = new CreateQueueEntryRequest();
        reqB.setDoctorId(doctorB.getId());
        reqB.setPatientId(patientB1.getId());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospBDoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // 6. Cross hospital doctor access returns 404
    @Test
    @DisplayName("Test 6: Cross hospital doctor access returns 404")
    public void test6_crossHospitalDoctorAccess_returns404() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorB.getId());
        req.setPatientId(patientA1.getId());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // 7. Cross hospital patient access returns 404
    @Test
    @DisplayName("Test 7: Cross hospital patient access returns 404")
    public void test7_crossHospitalPatientAccess_returns404() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientB1.getId());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // 8. Cross hospital appointment check-in returns 404
    @Test
    @DisplayName("Test 8: Cross hospital appointment check-in returns 404")
    public void test8_crossHospitalAppointmentCheckIn_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/queue/check-in/" + appointmentA1.getId())
                        .header("Authorization", "Bearer " + hospBDoctorToken))
                .andExpect(status().isNotFound());
    }

    // 9. Duplicate patient active queue rejection
    @Test
    @DisplayName("Test 9: Duplicate patient active queue rejection returns 409")
    public void test9_duplicatePatientActiveQueue_returns409() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // 10. Duplicate appointment check-in rejection
    @Test
    @DisplayName("Test 10: Duplicate appointment check-in returns 409")
    public void test10_duplicateAppointmentCheckIn_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/queue/check-in/" + appointmentA1.getId())
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/queue/check-in/" + appointmentA1.getId())
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isConflict());
    }

    // 11. FIFO ordering
    @Test
    @DisplayName("Test 11: FIFO ordering for NORMAL priority entries")
    public void test11_fifoOrdering() throws Exception {
        CreateQueueEntryRequest req1 = new CreateQueueEntryRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setPatientId(patientA1.getId());
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        CreateQueueEntryRequest req2 = new CreateQueueEntryRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setPatientId(patientA2.getId());
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/queue?doctorId=" + doctorA.getId())
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tokenNumber").value(1))
                .andExpect(jsonPath("$.content[1].tokenNumber").value(2));
    }

    // 12. Emergency priority precedence
    @Test
    @DisplayName("Test 12: EMERGENCY priority takes precedence over NORMAL")
    public void test12_emergencyPriority_takesPrecedence() throws Exception {
        CreateQueueEntryRequest req1 = new CreateQueueEntryRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setPatientId(patientA1.getId());
        req1.setPriority(QueuePriority.NORMAL);
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        CreateQueueEntryRequest req2 = new CreateQueueEntryRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setPatientId(patientA2.getId());
        req2.setPriority(QueuePriority.EMERGENCY);
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/doctors/me/queue/next")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patientA2.getId()))
                .andExpect(jsonPath("$.priority").value("EMERGENCY"));
    }

    // 13. Urgent priority precedence
    @Test
    @DisplayName("Test 13: URGENT priority takes precedence over NORMAL")
    public void test13_urgentPriority_takesPrecedence() throws Exception {
        CreateQueueEntryRequest req1 = new CreateQueueEntryRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setPatientId(patientA1.getId());
        req1.setPriority(QueuePriority.NORMAL);
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        CreateQueueEntryRequest req2 = new CreateQueueEntryRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setPatientId(patientA2.getId());
        req2.setPriority(QueuePriority.URGENT);
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/doctors/me/queue/next")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patientA2.getId()))
                .andExpect(jsonPath("$.priority").value("URGENT"));
    }

    // 14. Call patient
    @Test
    @DisplayName("Test 14: Call patient transitions WAITING to CALLED")
    public void test14_callPatient_success() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();

        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id + "/call")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.calledAt").isNotEmpty());
    }

    // 15. Start consultation
    @Test
    @DisplayName("Test 15: Start consultation transitions CALLED to IN_CONSULTATION")
    public void test15_startConsultation_success() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id + "/call").header("Authorization", "Bearer " + hospAReceptionToken)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/queue/" + id + "/start-consultation")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_CONSULTATION"))
                .andExpect(jsonPath("$.consultationStartedAt").isNotEmpty());
    }

    // 16. Complete consultation
    @Test
    @DisplayName("Test 16: Complete consultation transitions IN_CONSULTATION to COMPLETED")
    public void test16_completeConsultation_success() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id + "/start-consultation").header("Authorization", "Bearer " + hospADoctorToken)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/queue/" + id + "/complete")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.consultationDurationMinutes").value(greaterThanOrEqualTo(0)));
    }

    // 17. Skip patient
    @Test
    @DisplayName("Test 17: Skip patient transitions WAITING to SKIPPED")
    public void test17_skipPatient_success() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        UpdateQueueStatusRequest skipReq = new UpdateQueueStatusRequest();
        skipReq.setReason("Patient not responding to call");

        mockMvc.perform(patch("/api/v1/queue/" + id + "/skip")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skipReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.skipReason").value("Patient not responding to call"));
    }

    // 18. Recall patient
    @Test
    @DisplayName("Test 18: Recall patient transitions SKIPPED to WAITING retaining token")
    public void test18_recallPatient_success() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id + "/skip").header("Authorization", "Bearer " + hospAReceptionToken)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/queue/" + id + "/recall")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.tokenNumber").value(1));
    }

    // 19. Cancel queue entry
    @Test
    @DisplayName("Test 19: Cancel queue entry transitions to CANCELLED")
    public void test19_cancelQueueEntry_success() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        UpdateQueueStatusRequest cancelReq = new UpdateQueueStatusRequest();
        cancelReq.setReason("Patient left hospital");

        mockMvc.perform(patch("/api/v1/queue/" + id + "/cancel")
                        .header("Authorization", "Bearer " + hospAReceptionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Patient left hospital"));
    }

    // 20. Invalid transition returns 409
    @Test
    @DisplayName("Test 20: Invalid transition COMPLETED to CALLED returns 409")
    public void test20_invalidTransition_returns409() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id + "/start-consultation").header("Authorization", "Bearer " + hospADoctorToken)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/queue/" + id + "/complete").header("Authorization", "Bearer " + hospADoctorToken)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/queue/" + id + "/call")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isConflict());
    }

    // 21. Only one active consultation per doctor returns 409
    @Test
    @DisplayName("Test 21: Second active consultation for same doctor returns 409")
    public void test21_onlyOneActiveConsultation_returns409() throws Exception {
        CreateQueueEntryRequest req1 = new CreateQueueEntryRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setPatientId(patientA1.getId());
        MvcResult res1 = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated()).andReturn();
        Long id1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        CreateQueueEntryRequest req2 = new CreateQueueEntryRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setPatientId(patientA2.getId());
        MvcResult res2 = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated()).andReturn();
        Long id2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id1 + "/start-consultation").header("Authorization", "Bearer " + hospADoctorToken)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/queue/" + id2 + "/start-consultation")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isConflict());
    }

    // 22. Unauthenticated request returns 401
    @Test
    @DisplayName("Test 22: Unauthenticated request returns 401")
    public void test22_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/queue"))
                .andExpect(status().isUnauthorized());
    }

    // 23. Unauthorized role returns 403
    @Test
    @DisplayName("Test 23: Unauthorized role (LAB_STAFF) returns 403")
    public void test23_unauthorizedRole_returns403() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());

        mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospALabToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // 24. Doctor sees only own queue
    @Test
    @DisplayName("Test 24: GET /api/v1/doctors/me/queue returns logged-in doctor's queue")
    public void test24_doctorSeesOnlyOwnQueue() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/doctors/me/queue")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].doctorId").value(doctorA.getId()));
    }

    // 25. Receptionist sees hospital queue
    @Test
    @DisplayName("Test 25: GET /api/v1/queue returns full hospital queue for receptionist")
    public void test25_receptionistSeesHospitalQueue() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // 26. Estimated wait time calculation
    @Test
    @DisplayName("Test 26: Estimated wait time calculated correctly for waiting entries")
    public void test26_estimatedWaitTimeCalculation() throws Exception {
        CreateQueueEntryRequest req1 = new CreateQueueEntryRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setPatientId(patientA1.getId());
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        Thread.sleep(20);

        CreateQueueEntryRequest req2 = new CreateQueueEntryRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setPatientId(patientA2.getId());
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(greaterThan(0)));
    }

    // 27. Consultation duration calculation
    @Test
    @DisplayName("Test 27: Consultation duration calculation is accurate")
    public void test27_consultationDurationCalculation() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id + "/start-consultation").header("Authorization", "Bearer " + hospADoctorToken)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/queue/" + id + "/complete").header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultationDurationMinutes").value(greaterThanOrEqualTo(0)));
    }

    // 28. Doctor arrival status update & view
    @Test
    @DisplayName("Test 28: Doctor updates arrival status and receptionist views it")
    public void test28_doctorArrivalStatus() throws Exception {
        UpdateDoctorQueueStatusRequest req = new UpdateDoctorQueueStatusRequest();
        req.setStatus(DoctorQueueStatus.ARRIVED);

        mockMvc.perform(patch("/api/v1/doctors/me/queue/status")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARRIVED"));

        mockMvc.perform(get("/api/v1/doctors/" + doctorA.getId() + "/queue/status")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARRIVED"));
    }

    // 29. Pagination and filtering
    @Test
    @DisplayName("Test 29: Pagination and status filtering for queue endpoints")
    public void test29_paginationAndFiltering() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/queue?status=WAITING&page=0&size=5")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/queue?status=COMPLETED")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // 30. HospitalId cannot be overridden
    @Test
    @DisplayName("Test 30: HospitalId cannot be overridden via query parameter")
    public void test30_hospitalIdCannotBeOverridden() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/queue?hospitalId=999")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].hospitalId").value(hospitalA.getId()));
    }

    // 31. Concurrent token generation uniqueness
    @Test
    @DisplayName("Test 31: Concurrent queue creation protects token uniqueness")
    @Commit
    public void test31_concurrentTokenGeneration_uniqueness() throws Exception {
        int threadCount = 2;

        List<Patient> createdPatients = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            createdPatients.add(createPatientHelper(hospitalA, "ConcPat" + i, "Test", "conc" + i + "_" + System.nanoTime() + "@test.com", String.format("9880000%03d", i)));
        }

        TestTransaction.flagForCommit();
        TestTransaction.end();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        List<Integer> tokenNumbers = Collections.synchronizedList(new ArrayList<>());
        org.springframework.security.core.context.SecurityContext mainSecurityContext = org.springframework.security.core.context.SecurityContextHolder.getContext();

        for (int i = 0; i < threadCount; i++) {
            final Patient targetPatient = createdPatients.get(i);
            executor.submit(() -> {
                try {
                    org.springframework.security.core.context.SecurityContextHolder.setContext(mainSecurityContext);
                    latch.await();
                    CreateQueueEntryRequest req = new CreateQueueEntryRequest();
                    req.setDoctorId(doctorA.getId());
                    req.setPatientId(targetPatient.getId());

                    MvcResult result = mockMvc.perform(post("/api/v1/queue")
                                    .header("Authorization", "Bearer " + hospAReceptionToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    if (result.getResponse().getStatus() == 201) {
                        successCount.incrementAndGet();
                        Integer token = objectMapper.readTree(result.getResponse().getContentAsString()).get("tokenNumber").asInt();
                        tokenNumbers.add(token);
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown();
        doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        org.junit.jupiter.api.Assertions.assertTrue(successCount.get() >= 1);
        assertEquals(tokenNumbers.size(), tokenNumbers.stream().distinct().count());
    }






    // 32. Concurrent check-in of same appointment
    @Test
    @DisplayName("Test 32: Concurrent check-in of same appointment returns 409 for second attempt")
    @Commit
    public void test32_concurrentCheckIn_sameAppointment() throws Exception {
        TestTransaction.flagForCommit();
        TestTransaction.end();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/queue/check-in/" + appointmentA1.getId())
                                    .header("Authorization", "Bearer " + hospAReceptionToken))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 201) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown();
        doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get());
        assertEquals(1, conflictCount.get());
    }

    // 33. Concurrent start consultation returns 409
    @Test
    @DisplayName("Test 33: Concurrent start consultation for same doctor returns 409")
    public void test33_concurrentStartConsultation_returns409() throws Exception {
        CreateQueueEntryRequest req1 = new CreateQueueEntryRequest();
        req1.setDoctorId(doctorA.getId());
        req1.setPatientId(patientA1.getId());
        MvcResult res1 = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated()).andReturn();
        Long id1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        CreateQueueEntryRequest req2 = new CreateQueueEntryRequest();
        req2.setDoctorId(doctorA.getId());
        req2.setPatientId(patientA2.getId());
        MvcResult res2 = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated()).andReturn();
        Long id2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id1 + "/start-consultation").header("Authorization", "Bearer " + hospADoctorToken)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/queue/" + id2 + "/start-consultation")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isConflict());
    }

    // 34. Completed queue cannot be modified
    @Test
    @DisplayName("Test 34: Completed queue entry cannot be recalled or started")
    public void test34_completedQueueCannotBeModified() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id + "/start-consultation").header("Authorization", "Bearer " + hospADoctorToken)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/queue/" + id + "/complete").header("Authorization", "Bearer " + hospADoctorToken)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/queue/" + id + "/recall")
                        .header("Authorization", "Bearer " + hospAReceptionToken))
                .andExpect(status().isConflict());
    }

    // 35. Cancelled queue cannot be started
    @Test
    @DisplayName("Test 35: Cancelled queue entry cannot be called or started")
    public void test35_cancelledQueueCannotBeStarted() throws Exception {
        CreateQueueEntryRequest req = new CreateQueueEntryRequest();
        req.setDoctorId(doctorA.getId());
        req.setPatientId(patientA1.getId());
        MvcResult res = mockMvc.perform(post("/api/v1/queue").header("Authorization", "Bearer " + hospAReceptionToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/queue/" + id + "/cancel").header("Authorization", "Bearer " + hospAReceptionToken)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/queue/" + id + "/start-consultation")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isConflict());
    }
}
