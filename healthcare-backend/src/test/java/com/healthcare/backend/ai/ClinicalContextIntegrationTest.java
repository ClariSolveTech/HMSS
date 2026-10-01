package com.healthcare.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.HealthcareBackendApplication;
import com.healthcare.backend.ai.context.ClinicalContext;
import com.healthcare.backend.ai.context.ClinicalContextService;
import com.healthcare.backend.ai.dto.ClinicalAssistantAction;
import com.healthcare.backend.ai.dto.ClinicalAssistantRequest;
import com.healthcare.backend.ai.prompt.ClinicalPromptBuilder;
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
import com.healthcare.backend.lab.entity.*;
import com.healthcare.backend.lab.repository.LabOrderItemRepository;
import com.healthcare.backend.lab.repository.LabOrderRepository;
import com.healthcare.backend.lab.repository.LabResultRepository;
import com.healthcare.backend.lab.repository.LabTestRepository;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.entity.PrescriptionItem;
import com.healthcare.backend.prescription.entity.PrescriptionStatus;
import com.healthcare.backend.prescription.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HealthcareBackendApplication.class)
@Transactional
public class ClinicalContextIntegrationTest {

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
    private LabTestRepository labTestRepository;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private LabOrderItemRepository labOrderItemRepository;

    @Autowired
    private LabResultRepository labResultRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private ClinicalContextService clinicalContextService;

    @Autowired
    private ClinicalPromptBuilder clinicalPromptBuilder;

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
    private Doctor doctor1b; // Doctor in hospital 1, but different from doctor 1

    private Patient patient1;
    private Patient patient2;

    private Consultation consult1;
    private Consultation consult2;
    private Consultation consult1b;

    private LabResult labResult1;
    private Prescription prescription1;

    private String doctor1Token;

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

        // 2. Hospitals
        hospital1 = new Hospital();
        hospital1.setName("Ctx Hosp 1 " + suffix);
        hospital1.setCode("CH1" + (suffix % 10000));
        hospital1.setPhone("9876543210");
        hospital1.setEmail("ctxhosp1_" + suffix + "@ai.com");
        hospital1.setStatus(HospitalStatus.ACTIVE);
        hospital1 = hospitalRepository.save(hospital1);

        hospital2 = new Hospital();
        hospital2.setName("Ctx Hosp 2 " + suffix);
        hospital2.setCode("CH2" + (suffix % 10000));
        hospital2.setPhone("9876543211");
        hospital2.setEmail("ctxhosp2_" + suffix + "@ai.com");
        hospital2.setStatus(HospitalStatus.ACTIVE);
        hospital2 = hospitalRepository.save(hospital2);

        // 3. Doctors
        User user1 = new User();
        user1.setEmail("ctxdoc1_" + suffix + "@ai.com");
        user1.setPasswordHash(passwordEncoder.encode("Password@123"));
        user1.setFirstName("Aarav");
        user1.setLastName("Sharma");
        user1.setMobileNumber("992200" + (suffix % 10000));
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

        User user1b = new User();
        user1b.setEmail("ctxdoc1b_" + suffix + "@ai.com");
        user1b.setPasswordHash(passwordEncoder.encode("Password@123"));
        user1b.setFirstName("Raj");
        user1b.setLastName("Verma");
        user1b.setMobileNumber("992201" + (suffix % 10000));
        user1b.setHospital(hospital1);
        user1b.setStatus(UserStatus.ACTIVE);
        user1b.setRoles(Set.of(docRole));
        user1b = userRepository.save(user1b);

        doctor1b = new Doctor();
        doctor1b.setHospital(hospital1);
        doctor1b.setUser(user1b);
        doctor1b.setDoctorCode("DOC1B_" + suffix);
        doctor1b.setLicenseNumber("LIC1B_" + suffix);
        doctor1b.setSpecialization("Neurology");
        doctor1b.setQualification("MD");
        doctor1b.setConsultationFee(new BigDecimal("700.00"));
        doctor1b.setStatus(DoctorStatus.ACTIVE);
        doctor1b = doctorRepository.save(doctor1b);

        User user2 = new User();
        user2.setEmail("ctxdoc2_" + suffix + "@ai.com");
        user2.setPasswordHash(passwordEncoder.encode("Password@123"));
        user2.setFirstName("Sana");
        user2.setLastName("Iqbal");
        user2.setMobileNumber("992202" + (suffix % 10000));
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
        consult1.setChiefComplaint("Severe headache");
        consult1.setSymptoms("Nausea, photophobia");
        consult1.setClinicalNotes("Migraine suspected");
        consult1.setDiagnosis("Acute Migraine");
        consult1.setStatus(ConsultationStatus.COMPLETED);
        consult1 = consultationRepository.save(consult1);

        consult1b = new Consultation();
        consult1b.setHospital(hospital1);
        consult1b.setDoctor(doctor1b);
        consult1b.setPatient(patient1);
        consult1b.setConsultationDate(LocalDateTime.now());
        consult1b.setChiefComplaint("Back pain");
        consult1b.setStatus(ConsultationStatus.COMPLETED);
        consult1b = consultationRepository.save(consult1b);

        consult2 = new Consultation();
        consult2.setHospital(hospital2);
        consult2.setDoctor(doctor2);
        consult2.setPatient(patient2);
        consult2.setConsultationDate(LocalDateTime.now());
        consult2.setChiefComplaint("Fever");
        consult2.setStatus(ConsultationStatus.COMPLETED);
        consult2 = consultationRepository.save(consult2);

        // 6. Lab Results for Patient 1
        LabTest labTest = new LabTest();
        labTest.setHospital(hospital1);
        labTest.setTestCode("CBC_" + suffix);
        labTest.setTestName("Complete Blood Count");
        labTest.setActive(true);
        labTest = labTestRepository.save(labTest);

        LabOrder labOrder = new LabOrder();
        labOrder.setHospital(hospital1);
        labOrder.setDoctor(doctor1);
        labOrder.setPatient(patient1);
        labOrder.setConsultation(consult1);
        labOrder.setOrderNumber("LO_" + suffix);
        labOrder.setStatus(LabOrderStatus.COMPLETED);
        labOrder.setPriority(LabOrderPriority.NORMAL);
        labOrder.setOrderedAt(LocalDateTime.now());
        labOrder = labOrderRepository.save(labOrder);

        LabOrderItem labItem = new LabOrderItem();
        labItem.setLabOrder(labOrder);
        labItem.setLabTest(labTest);
        labItem.setTestNameSnapshot("Complete Blood Count");
        labItem.setTestCodeSnapshot("CBC_" + suffix);
        labItem.setStatus(LabOrderStatus.COMPLETED);
        labItem = labOrderItemRepository.save(labItem);

        labResult1 = new LabResult();
        labResult1.setHospital(hospital1);
        labResult1.setDoctor(doctor1);
        labResult1.setPatient(patient1);
        labResult1.setLabOrder(labOrder);
        labResult1.setLabOrderItem(labItem);
        labResult1.setResultValue("14.5");
        labResult1.setResultUnit("g/dL");
        labResult1.setReferenceRange("12.0-16.0");
        labResult1.setAbnormalFlag(false);
        labResult1.setResultStatus(LabResultStatus.COMPLETED);
        labResult1 = labResultRepository.save(labResult1);

        // 7. Prescription for Patient 1
        prescription1 = new Prescription();
        prescription1.setHospital(hospital1);
        prescription1.setDoctor(doctor1);
        prescription1.setPatient(patient1);
        prescription1.setConsultation(consult1);
        prescription1.setPrescriptionCode("RX_" + suffix);
        prescription1.setStatus(PrescriptionStatus.ISSUED);

        PrescriptionItem item = new PrescriptionItem();
        item.setMedicineName("Sumatriptan");
        item.setDosage("50mg");
        item.setFrequency("As needed");
        item.setDuration("5 days");
        prescription1.addItem(item);
        prescription1 = prescriptionRepository.save(prescription1);
    }

    @Test
    @DisplayName("1. Same-hospital patient context succeeds")
    void test1_sameHospitalPatientContextSucceeds() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_PATIENT)
                .build();

        ClinicalContext ctx = clinicalContextService.buildContext(req, doctor1, hospital1.getId());

        assertNotNull(ctx.getPatient());
        assertEquals(patient1.getId(), ctx.getPatient().getId());
        assertEquals(patient1.getPatientCode(), ctx.getPatient().getPatientCode());
    }

    @Test
    @DisplayName("2. Same-doctor consultation context succeeds")
    void test2_sameDoctorConsultationContextSucceeds() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .consultationId(consult1.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_CONSULTATION)
                .build();

        ClinicalContext ctx = clinicalContextService.buildContext(req, doctor1, hospital1.getId());

        assertNotNull(ctx.getConsultation());
        assertEquals(consult1.getId(), ctx.getConsultation().getId());
        assertEquals(patient1.getId(), ctx.getPatient().getId());
    }

    @Test
    @DisplayName("3. Cross-hospital patient is rejected with 404")
    void test3_crossHospitalPatientRejected() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient2.getId()) // Hospital 2 patient
                .action(ClinicalAssistantAction.SUMMARIZE_PATIENT)
                .build();

        assertThrows(NoSuchElementException.class, () ->
                clinicalContextService.buildContext(req, doctor1, hospital1.getId())
        );
    }

    @Test
    @DisplayName("4. Cross-hospital consultation is rejected with 404")
    void test4_crossHospitalConsultationRejected() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .consultationId(consult2.getId()) // Hospital 2 consultation
                .action(ClinicalAssistantAction.SUMMARIZE_CONSULTATION)
                .build();

        assertThrows(NoSuchElementException.class, () ->
                clinicalContextService.buildContext(req, doctor1, hospital1.getId())
        );
    }

    @Test
    @DisplayName("5. Consultation belonging to another doctor is rejected with 403")
    void test5_crossDoctorConsultationRejected() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .consultationId(consult1b.getId()) // Belong to Doctor 1b in same hospital
                .action(ClinicalAssistantAction.SUMMARIZE_CONSULTATION)
                .build();

        assertThrows(AccessDeniedException.class, () ->
                clinicalContextService.buildContext(req, doctor1, hospital1.getId())
        );
    }

    @Test
    @DisplayName("6. Patient/consultation mismatch is rejected with 400")
    void test6_patientConsultationMismatchRejected() {
        // Create another patient in hospital 1
        Patient patient3 = new Patient();
        patient3.setHospital(hospital1);
        patient3.setPatientCode("PAT3_" + System.currentTimeMillis());
        patient3.setFirstName("Anita");
        patient3.setLastName("Deshmukh");
        patient3.setDateOfBirth(LocalDate.of(1988, 3, 3));
        patient3.setGender(Gender.FEMALE);
        patient3.setMobileNumber("9811223377");
        patient3.setStatus(PatientStatus.ACTIVE);
        patient3 = patientRepository.save(patient3);

        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient3.getId())
                .consultationId(consult1.getId()) // Consult 1 belongs to Patient 1
                .action(ClinicalAssistantAction.SUMMARIZE_CONSULTATION)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                clinicalContextService.buildContext(req, doctor1, hospital1.getId())
        );
    }

    @Test
    @DisplayName("7. Lab results are restricted to correct patient and hospital")
    void test7_labResultsRestrictedToPatientAndHospital() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.ANALYZE_LAB_RESULTS)
                .build();

        ClinicalContext ctx = clinicalContextService.buildContext(req, doctor1, hospital1.getId());

        assertNotNull(ctx.getRecentLabResults());
        assertFalse(ctx.getRecentLabResults().isEmpty());
        ctx.getRecentLabResults().forEach(r ->
                assertEquals(patient1.getId(), r.getPatient().getId())
        );
    }

    @Test
    @DisplayName("8. Prescription history is restricted to correct patient and hospital")
    void test8_prescriptionHistoryRestrictedToPatientAndHospital() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_PATIENT)
                .build();

        ClinicalContext ctx = clinicalContextService.buildContext(req, doctor1, hospital1.getId());

        assertNotNull(ctx.getRecentPrescriptions());
        assertFalse(ctx.getRecentPrescriptions().isEmpty());
        ctx.getRecentPrescriptions().forEach(p ->
                assertEquals(patient1.getId(), p.getPatient().getId())
        );
    }

    @Test
    @DisplayName("9. Previous consultations are hospital isolated")
    void test9_previousConsultationsHospitalIsolated() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.EXPLAIN_PATIENT_HISTORY)
                .build();

        ClinicalContext ctx = clinicalContextService.buildContext(req, doctor1, hospital1.getId());

        assertNotNull(ctx.getRecentConsultations());
        ctx.getRecentConsultations().forEach(c ->
                assertEquals(hospital1.getId(), c.getHospital().getId())
        );
    }

    @Test
    @DisplayName("10 & 11. Client hospitalId and doctorId cannot override security context")
    void test10_11_clientCannotOverrideContext() throws Exception {
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
    @DisplayName("12. Action-specific context does not include unrelated data")
    void test12_actionSpecificContextExcludesUnrelatedData() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .action(ClinicalAssistantAction.ANALYZE_LAB_RESULTS)
                .build();

        ClinicalContext ctx = clinicalContextService.buildContext(req, doctor1, hospital1.getId());
        String formattedContext = ctx.formatForAction(ClinicalAssistantAction.ANALYZE_LAB_RESULTS);

        assertTrue(formattedContext.contains("LABORATORY RESULTS"));
        assertFalse(formattedContext.contains("PRESCRIPTION HISTORY"), "Prescriptions should be excluded from lab action format");
    }

    @Test
    @DisplayName("13. No password hash, JWT, or secret included in prompt")
    void test13_noSensitiveDataInPrompt() {
        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(patient1.getId())
                .consultationId(consult1.getId())
                .action(ClinicalAssistantAction.DRAFT_CLINICAL_NOTE)
                .build();

        ClinicalContext ctx = clinicalContextService.buildContext(req, doctor1, hospital1.getId());
        String prompt = clinicalPromptBuilder.buildPrompt(ClinicalAssistantAction.DRAFT_CLINICAL_NOTE, ctx);

        assertFalse(prompt.contains("passwordHash"));
        assertFalse(prompt.contains("Password"));
        assertFalse(prompt.contains("eyJ"));
        assertFalse(prompt.contains("secret"));
    }

    @Test
    @DisplayName("14 & 15. Empty clinical history and missing optional fields handled safely")
    void test14_15_emptyClinicalHistoryHandledSafely() {
        Patient newPatient = new Patient();
        newPatient.setHospital(hospital1);
        newPatient.setPatientCode("NEWPAT_" + System.currentTimeMillis());
        newPatient.setFirstName("Fresh");
        newPatient.setLastName("Patient");
        newPatient.setDateOfBirth(LocalDate.of(2000, 1, 1));
        newPatient.setGender(Gender.OTHER);
        newPatient.setMobileNumber("9811223388");
        newPatient.setStatus(PatientStatus.ACTIVE);
        newPatient = patientRepository.save(newPatient);

        ClinicalAssistantRequest req = ClinicalAssistantRequest.builder()
                .patientId(newPatient.getId())
                .action(ClinicalAssistantAction.SUMMARIZE_PATIENT)
                .build();

        ClinicalContext ctx = clinicalContextService.buildContext(req, doctor1, hospital1.getId());
        String formatted = ctx.formatForAction(ClinicalAssistantAction.SUMMARIZE_PATIENT);

        assertNotNull(formatted);
        assertTrue(formatted.contains(newPatient.getPatientCode()));
    }
}
