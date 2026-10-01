package com.healthcare.backend.consultation;

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
import com.healthcare.backend.consultation.dto.CreateConsultationRequest;
import com.healthcare.backend.consultation.dto.UpdateConsultationRequest;
import com.healthcare.backend.consultation.entity.ConsultationStatus;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
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
import com.healthcare.backend.queue.entity.QueueEntry;
import com.healthcare.backend.queue.entity.QueuePriority;
import com.healthcare.backend.queue.entity.QueueStatus;
import com.healthcare.backend.queue.repository.QueueEntryRepository;
import com.healthcare.backend.queue.service.QueueService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ConsultationIntegrationTest {

    private static final AtomicLong CODE_COUNTER = new AtomicLong(90000);

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
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private com.healthcare.backend.prescription.repository.PrescriptionRepository prescriptionRepository;

    @Autowired
    private com.healthcare.backend.prescription.repository.PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.InvoiceRepository invoiceRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.PaymentRepository paymentRepository;

    @Autowired
    private QueueService queueService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private String hospAAdminToken;
    private String hospADoctorToken;
    private String hospALabToken;
    private String hospBDoctorToken;

    private Doctor doctorA;
    private Doctor doctorB;
    private Patient patientA1;
    private Patient patientB1;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        paymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        prescriptionItemRepository.deleteAll();
        prescriptionRepository.deleteAll();
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

        hospitalA = new Hospital();
        hospitalA.setName("City Central Hospital");
        hospitalA.setCode("HOSP_CONS_A_" + ts);
        hospitalA.setEmail("admin.cons.a." + ts + "@citycentral.com");
        hospitalA.setPhone("9998887771");
        hospitalA.setAddress("Main St");
        hospitalA.setCity("Metropolis");
        hospitalA.setState("State");
        hospitalA.setPincode("100001");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        hospitalB = new Hospital();
        hospitalB.setName("Metro Care Hospital");
        hospitalB.setCode("HOSP_CONS_B_" + ts);
        hospitalB.setEmail("admin.cons.b." + ts + "@metrocare.com");
        hospitalB.setPhone("9998887772");
        hospitalB.setAddress("North St");
        hospitalB.setCity("Metropolis");
        hospitalB.setState("State");
        hospitalB.setPincode("100002");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        User adminA = createUserDirect("Admin", "Alpha", "admin.cons.a." + ts + "@test.com", "91" + (ts % 100000000L), hospitalA, RoleName.HOSPITAL_ADMIN);
        hospAAdminToken = jwtService.generateToken(adminA);

        User docUserA = createUserDirect("Doctor", "Aarav", "doc.cons.aarav." + ts + "@test.com", "93" + (ts % 100000000L), hospitalA, RoleName.DOCTOR);
        hospADoctorToken = jwtService.generateToken(docUserA);

        User labA = createUserDirect("Lab", "Staff", "lab.cons.staff." + ts + "@test.com", "94" + (ts % 100000000L), hospitalA, RoleName.LAB_STAFF);
        hospALabToken = jwtService.generateToken(labA);

        User docUserB = createUserDirect("Doctor", "Neha", "doc.cons.neha." + ts + "@test.com", "96" + (ts % 100000000L), hospitalB, RoleName.DOCTOR);
        hospBDoctorToken = jwtService.generateToken(docUserB);

        doctorA = createDoctorHelper(docUserA, "Cardiology", "LIC_CONS_A_" + ts);
        doctorB = createDoctorHelper(docUserB, "Pediatrics", "LIC_CONS_B_" + ts);

        patientA1 = createPatientHelper(hospitalA, "Ramesh", "Kumar", "ramesh.cons." + ts + "@test.com", "98" + (ts % 100000000L));
        patientB1 = createPatientHelper(hospitalB, "Anita", "Sharma", "anita.cons." + ts + "@test.com", "96" + (ts % 100000000L));
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

    @Test
    @DisplayName("Test 1: Create consultation success")
    public void test1_createConsultation_success() throws Exception {
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA1.getId());
        req.setChiefComplaint("Chest pain and fatigue");
        req.setSymptoms("Shortness of breath");
        req.setClinicalNotes("BP 130/85, HR 78 bpm");
        req.setDiagnosis("Hypertension Stage 1");
        req.setTreatmentAdvice("Low sodium diet, Amlodipine 5mg QD");

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.doctorId").value(doctorA.getId()))
                .andExpect(jsonPath("$.patientId").value(patientA1.getId()))
                .andExpect(jsonPath("$.chiefComplaint").value("Chest pain and fatigue"))
                .andExpect(jsonPath("$.diagnosis").value("Hypertension Stage 1"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Test 2: Get consultation by ID success")
    public void test2_getConsultationById_success() throws Exception {
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA1.getId());
        req.setChiefComplaint("Mild fever");

        MvcResult createRes = mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long consId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/consultations/" + consId)
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(consId))
                .andExpect(jsonPath("$.chiefComplaint").value("Mild fever"));
    }

    @Test
    @DisplayName("Test 3: Cross-hospital consultation access returns 404")
    public void test3_crossHospital_returns404() throws Exception {
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA1.getId());
        req.setChiefComplaint("Hospital A record");

        MvcResult createRes = mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long consId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Doctor from Hospital B trying to access Hospital A's consultation
        mockMvc.perform(get("/api/v1/consultations/" + consId)
                        .header("Authorization", "Bearer " + hospBDoctorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test 4: Creating consultation with cross-hospital patient returns 404")
    public void test4_createConsultation_crossHospitalPatient_returns404() throws Exception {
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientB1.getId()); // Patient from Hospital B
        req.setChiefComplaint("Invalid access");

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test 5: Update in-progress consultation success")
    public void test5_updateConsultation_success() throws Exception {
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA1.getId());
        req.setChiefComplaint("Initial complaint");

        MvcResult createRes = mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long consId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        UpdateConsultationRequest updateReq = new UpdateConsultationRequest();
        updateReq.setDiagnosis("Updated Diagnosis: Viral Fever");
        updateReq.setTreatmentAdvice("Paracetamol 650mg TDS");

        mockMvc.perform(put("/api/v1/consultations/" + consId)
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Updated Diagnosis: Viral Fever"))
                .andExpect(jsonPath("$.treatmentAdvice").value("Paracetamol 650mg TDS"));
    }

    @Test
    @DisplayName("Test 6: Complete consultation success and auto-completes queue entry")
    public void test6_completeConsultation_success() throws Exception {
        // Create Queue Entry first
        CreateQueueEntryRequest queueReq = new CreateQueueEntryRequest();
        queueReq.setDoctorId(doctorA.getId());
        queueReq.setPatientId(patientA1.getId());
        queueReq.setPriority(QueuePriority.NORMAL);

        MvcResult queueRes = mockMvc.perform(post("/api/v1/queue")
                        .header("Authorization", "Bearer " + hospAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queueReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long queueId = objectMapper.readTree(queueRes.getResponse().getContentAsString()).get("id").asLong();

        // Start consultation for queue entry
        mockMvc.perform(patch("/api/v1/queue/" + queueId + "/start-consultation")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk());

        // Create consultation linked to queue entry
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA1.getId());
        req.setQueueEntryId(queueId);
        req.setChiefComplaint("Routine checkup");

        MvcResult createRes = mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long consId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Complete consultation
        mockMvc.perform(patch("/api/v1/consultations/" + consId + "/complete")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Verify associated queue entry is also completed
        QueueEntry updatedQueue = queueEntryRepository.findById(queueId).orElseThrow();
        assertEquals(QueueStatus.COMPLETED, updatedQueue.getStatus());
    }

    @Test
    @DisplayName("Test 7: Updating or completing a COMPLETED consultation returns 409 Conflict")
    public void test7_completedConsultationModification_returns409() throws Exception {
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA1.getId());
        req.setChiefComplaint("Temporary consultation");

        MvcResult createRes = mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long consId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Complete consultation
        mockMvc.perform(patch("/api/v1/consultations/" + consId + "/complete")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk());

        // Try updating completed consultation -> 409
        UpdateConsultationRequest updateReq = new UpdateConsultationRequest();
        updateReq.setDiagnosis("New Diagnosis");

        mockMvc.perform(put("/api/v1/consultations/" + consId)
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict());

        // Try completing again -> 409
        mockMvc.perform(patch("/api/v1/consultations/" + consId + "/complete")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Test 8: GET /api/v1/doctors/me/consultations returns logged in doctor's consultations")
    public void test8_getDoctorOwnConsultations() throws Exception {
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA1.getId());
        req.setChiefComplaint("Doctor A consultation");

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/doctors/me/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].doctorId").value(doctorA.getId()))
                .andExpect(jsonPath("$.content[0].chiefComplaint").value("Doctor A consultation"));
    }

    @Test
    @DisplayName("Test 9: GET /api/v1/patients/{patientId}/consultations returns patient history")
    public void test9_getPatientConsultationHistory() throws Exception {
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA1.getId());
        req.setChiefComplaint("Patient history test");

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/patients/" + patientA1.getId() + "/consultations")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientId").value(patientA1.getId()))
                .andExpect(jsonPath("$.content[0].chiefComplaint").value("Patient history test"));
    }

    @Test
    @DisplayName("Test 10: Unauthenticated request returns 401")
    public void test10_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/consultations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test 11: Unauthorized role (LAB_STAFF) returns 403 on create")
    public void test11_unauthorizedRole_returns403() throws Exception {
        CreateConsultationRequest req = new CreateConsultationRequest();
        req.setPatientId(patientA1.getId());
        req.setChiefComplaint("Unauthorized attempt");

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + hospALabToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
