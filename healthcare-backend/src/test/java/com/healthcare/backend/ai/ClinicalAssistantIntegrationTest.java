package com.healthcare.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.HealthcareBackendApplication;
import com.healthcare.backend.ai.dto.ClinicalAssistantAction;
import com.healthcare.backend.ai.dto.ClinicalAssistantRequest;
import com.healthcare.backend.ai.repository.AIRequestAuditRepository;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.consultation.entity.Consultation;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HealthcareBackendApplication.class)
@Transactional
public class ClinicalAssistantIntegrationTest {

    @Autowired
    private WebApplicationContext context;

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
    private ConsultationRepository consultationRepository;

    @Autowired
    private AIRequestAuditRepository auditRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc mockMvc;

    private Hospital hospital1;
    private Hospital hospital2;

    private Doctor doctor1;
    private Doctor doctor2;

    private Patient patient1;
    private Patient patient2;

    private Consultation consult1;
    private Consultation consult2;

    private String doctor1Token;
    private String doctor2Token;
    private String receptionistToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        long suffix = System.currentTimeMillis();

        // 1. Roles
        Role docRole = roleRepository.findByName(RoleName.DOCTOR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.DOCTOR)));
        Role recepRole = roleRepository.findByName(RoleName.RECEPTIONIST)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.RECEPTIONIST)));

        // 2. Hospitals
        hospital1 = new Hospital();
        hospital1.setName("AI Hosp 1 " + suffix);
        hospital1.setCode("AIH1" + (suffix % 10000));
        hospital1.setPhone("9876543210");
        hospital1.setEmail("hosp1_" + suffix + "@ai.com");
        hospital1.setStatus(HospitalStatus.ACTIVE);
        hospital1 = hospitalRepository.save(hospital1);

        hospital2 = new Hospital();
        hospital2.setName("AI Hosp 2 " + suffix);
        hospital2.setCode("AIH2" + (suffix % 10000));
        hospital2.setPhone("9876543211");
        hospital2.setEmail("hosp2_" + suffix + "@ai.com");
        hospital2.setStatus(HospitalStatus.ACTIVE);
        hospital2 = hospitalRepository.save(hospital2);

        // 3. Users & Doctors
        User user1 = new User();
        user1.setEmail("doc1_" + suffix + "@ai.com");
        user1.setPasswordHash(passwordEncoder.encode("Password@123"));
        user1.setFirstName("Aarav");
        user1.setLastName("Sharma");
        user1.setMobileNumber("991100" + (suffix % 10000));
        user1.setHospital(hospital1);
        user1.setStatus(UserStatus.ACTIVE);
        user1.setRoles(Set.of(docRole));
        user1 = userRepository.save(user1);

        doctor1 = new Doctor();
        doctor1.setHospital(hospital1);
        doctor1.setUser(user1);
        doctor1.setDoctorCode("DOC1_" + suffix);
        doctor1.setLicenseNumber("LIC1_" + suffix);
        doctor1.setSpecialization("General Medicine");
        doctor1.setQualification("MD");
        doctor1.setConsultationFee(new BigDecimal("500.00"));
        doctor1.setStatus(DoctorStatus.ACTIVE);
        doctor1 = doctorRepository.save(doctor1);

        doctor1Token = jwtService.generateToken(user1);

        User user2 = new User();
        user2.setEmail("doc2_" + suffix + "@ai.com");
        user2.setPasswordHash(passwordEncoder.encode("Password@123"));
        user2.setFirstName("Sana");
        user2.setLastName("Iqbal");
        user2.setMobileNumber("991101" + (suffix % 10000));
        user2.setHospital(hospital2);
        user2.setStatus(UserStatus.ACTIVE);
        user2.setRoles(Set.of(docRole));
        user2 = userRepository.save(user2);

        doctor2 = new Doctor();
        doctor2.setHospital(hospital2);
        doctor2.setUser(user2);
        doctor2.setDoctorCode("DOC2_" + suffix);
        doctor2.setLicenseNumber("LIC2_" + suffix);
        doctor2.setSpecialization("Pediatrics");
        doctor2.setQualification("MD");
        doctor2.setConsultationFee(new BigDecimal("600.00"));
        doctor2.setStatus(DoctorStatus.ACTIVE);
        doctor2 = doctorRepository.save(doctor2);

        doctor2Token = jwtService.generateToken(user2);

        User recepUser = new User();
        recepUser.setEmail("recep_" + suffix + "@ai.com");
        recepUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        recepUser.setFirstName("Recep");
        recepUser.setLastName("User");
        recepUser.setMobileNumber("991102" + (suffix % 10000));
        recepUser.setHospital(hospital1);
        recepUser.setStatus(UserStatus.ACTIVE);
        recepUser.setRoles(Set.of(recepRole));
        recepUser = userRepository.save(recepUser);

        receptionistToken = jwtService.generateToken(recepUser);

        // 4. Patients
        patient1 = new Patient();
        patient1.setHospital(hospital1);
        patient1.setPatientCode("PAT1_" + suffix);
        patient1.setFirstName("Rohan");
        patient1.setLastName("Mehta");
        patient1.setDateOfBirth(LocalDate.of(1990, 5, 15));
        patient1.setGender(Gender.MALE);
        patient1.setMobileNumber("9811223344");
        patient1.setStatus(PatientStatus.ACTIVE);
        patient1 = patientRepository.save(patient1);

        patient2 = new Patient();
        patient2.setHospital(hospital2);
        patient2.setPatientCode("PAT2_" + suffix);
        patient2.setFirstName("Priya");
        patient2.setLastName("Patel");
        patient2.setDateOfBirth(LocalDate.of(1992, 8, 20));
        patient2.setGender(Gender.FEMALE);
        patient2.setMobileNumber("9811223355");
        patient2.setStatus(PatientStatus.ACTIVE);
        patient2 = patientRepository.save(patient2);

        // 5. Consultations
        consult1 = new Consultation();
        consult1.setHospital(hospital1);
        consult1.setDoctor(doctor1);
        consult1.setPatient(patient1);
        consult1.setConsultationDate(LocalDateTime.now());
        consult1.setChiefComplaint("Persistent fever");
        consult1.setStatus(ConsultationStatus.COMPLETED);
        consult1 = consultationRepository.save(consult1);

        consult2 = new Consultation();
        consult2.setHospital(hospital2);
        consult2.setDoctor(doctor2);
        consult2.setPatient(patient2);
        consult2.setConsultationDate(LocalDateTime.now());
        consult2.setChiefComplaint("Cough and cold");
        consult2.setStatus(ConsultationStatus.COMPLETED);
        consult2 = consultationRepository.save(consult2);
    }

    @Test
    @DisplayName("1. Authenticated doctor can request AI assistance")
    void test1_authenticatedDoctorCanRequestAIAssistance() throws Exception {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_PATIENT)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action", is("SUMMARIZE_PATIENT")))
                .andExpect(jsonPath("$.generatedContent", containsString("[Mock AI Provider - Development Mode]")))
                .andExpect(jsonPath("$.disclaimer", containsString("DISCLAIMER: This output is for clinical decision-support only")));
    }

    @Test
    @DisplayName("2. Unauthenticated request returns 401 Unauthorized")
    void test2_unauthenticatedReturns401() throws Exception {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_PATIENT)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Non-doctor role returns 403 Forbidden")
    void test3_nonDoctorRoleReturns403() throws Exception {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_PATIENT)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Same-hospital patient succeeds")
    void test4_sameHospitalPatientSucceeds() throws Exception {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.EXPLAIN_PATIENT_HISTORY)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action", is("EXPLAIN_PATIENT_HISTORY")));
    }

    @Test
    @DisplayName("5. Cross-hospital patient returns 404 Not Found")
    void test5_crossHospitalPatientReturns404() throws Exception {
        // Doctor 1 (Hospital 1) requests patient 2 (Hospital 2)
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient2.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_PATIENT)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6. Same-doctor consultation succeeds")
    void test6_sameDoctorConsultationSucceeds() throws Exception {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .consultationId(consult1.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_CONSULTATION)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action", is("SUMMARIZE_CONSULTATION")));
    }

    @Test
    @DisplayName("7. Unauthorized consultation returns 404 or 403")
    void test7_unauthorizedConsultationRejected() throws Exception {
        // Doctor 1 requests Doctor 2's consultation (Hospital 2)
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .consultationId(consult2.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_CONSULTATION)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8 & 9. Client cannot override hospitalId or doctorId")
    void test8_9_clientCannotOverrideHospitalOrDoctorId() throws Exception {
        // Raw JSON attempting to pass hospitalId and doctorId override
        String payload = """
                {
                  "hospitalId": 999,
                  "doctorId": 888,
                  "patientId": %d,
                  "action": "SUMMARIZE_PATIENT"
                }
                """.formatted(patient1.getId());

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceSummary", containsString(patient1.getPatientCode())));
    }

    @Test
    @DisplayName("10. Patient/consultation mismatch is rejected with 400")
    void test10_patientConsultationMismatchRejected() throws Exception {
        // Create another patient in hospital 1
        Patient patient3 = new Patient();
        patient3.setHospital(hospital1);
        patient3.setPatientCode("PAT3_" + System.currentTimeMillis());
        patient3.setFirstName("Vikram");
        patient3.setLastName("Singh");
        patient3.setDateOfBirth(LocalDate.of(1985, 1, 1));
        patient3.setGender(Gender.MALE);
        patient3.setMobileNumber("9811223366");
        patient3.setStatus(PatientStatus.ACTIVE);
        patient3 = patientRepository.save(patient3);

        // Doctor 1 passes consult1 (which belongs to patient 1) but specifies patient 3's ID
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient3.getId())
                .consultationId(consult1.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_CONSULTATION)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. Mock AI provider is invoked")
    void test11_mockAIProviderInvoked() throws Exception {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.ANALYZE_LAB_RESULTS)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedContent", containsString("Development AI provider response. No clinical inference was performed.")));
    }

    @Test
    @DisplayName("12. Audit record is persisted in DB")
    void test12_auditRecordPersisted() throws Exception {
        long initialCount = auditRepository.count();

        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.SUGGEST_CLINICAL_CONSIDERATIONS)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertTrue(auditRepository.count() > initialCount, "Audit log count should increase");
    }

    @Test
    @DisplayName("13 & 14. No password hash or JWT appears in response")
    void test13_14_noSensitiveDataInResponse() throws Exception {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.DRAFT_CLINICAL_NOTE)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedContent", not(containsString("password"))))
                .andExpect(jsonPath("$.generatedContent", not(containsString("Password"))))
                .andExpect(jsonPath("$.generatedContent", not(containsString("eyJ"))));
    }

    @Test
    @DisplayName("15. AI disclaimer is present")
    void test15_disclaimerPresent() throws Exception {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_PATIENT)
                .build();

        mockMvc.perform(post("/api/v1/doctors/me/ai/clinical-assistant")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disclaimer", is("DISCLAIMER: This output is for clinical decision-support only and does not constitute an autonomous diagnosis or treatment decision. All clinical recommendations must be reviewed and approved by a qualified clinician.")));
    }
}
