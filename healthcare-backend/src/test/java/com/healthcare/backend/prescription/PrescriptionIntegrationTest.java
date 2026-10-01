package com.healthcare.backend.prescription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.healthcare.backend.prescription.dto.CreatePrescriptionRequest;
import com.healthcare.backend.prescription.dto.PrescriptionItemRequest;
import com.healthcare.backend.prescription.dto.UpdatePrescriptionRequest;
import com.healthcare.backend.prescription.repository.PrescriptionItemRepository;
import com.healthcare.backend.prescription.repository.PrescriptionRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PrescriptionIntegrationTest {

    private static final AtomicLong CODE_COUNTER = new AtomicLong(98000);

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
    private ConsultationRepository consultationRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private String hospADoctorToken;
    private String hospADoctor2Token;
    private String hospBDoctorToken;

    private Doctor doctorA1;
    private Doctor doctorA2;
    private Doctor doctorB1;

    private Patient patientA1;
    private Patient patientB1;

    private Consultation consultationA1;
    private Consultation consultationA2;
    private Consultation consultationA3;
    private Consultation consultationB1;

    @Autowired
    private com.healthcare.backend.billing.repository.PaymentRepository paymentRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.InvoiceRepository invoiceRepository;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        prescriptionItemRepository.deleteAll();
        prescriptionRepository.deleteAll();
        paymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        consultationRepository.deleteAll();

        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }
        }

        long ts = System.currentTimeMillis();

        hospitalA = new Hospital();
        hospitalA.setName("City Care Hospital");
        hospitalA.setCode("HOSP_RX2_A_" + ts);
        hospitalA.setEmail("admin.rx2.a." + ts + "@citycare.com");
        hospitalA.setPhone("9993334441");
        hospitalA.setAddress("East Boulevard");
        hospitalA.setCity("Metropolis");
        hospitalA.setState("State");
        hospitalA.setPincode("100003");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        hospitalB = new Hospital();
        hospitalB.setName("Central Metro Hospital");
        hospitalB.setCode("HOSP_RX2_B_" + ts);
        hospitalB.setEmail("admin.rx2.b." + ts + "@centralmetro.com");
        hospitalB.setPhone("9993334442");
        hospitalB.setAddress("West Avenue");
        hospitalB.setCity("Metropolis");
        hospitalB.setState("State");
        hospitalB.setPincode("100004");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        User docUserA1 = createUserDirect("Doctor", "Aarav", "doc.rx2.aarav." + ts + "@test.com", "91" + (ts % 100000000L), hospitalA, RoleName.DOCTOR);
        hospADoctorToken = jwtService.generateToken(docUserA1);
        doctorA1 = createDoctorHelper(docUserA1, "Internal Medicine", "LIC_RX2_A1_" + ts);

        User docUserA2 = createUserDirect("Doctor", "Vikram", "doc.rx2.vikram." + ts + "@test.com", "92" + (ts % 100000000L), hospitalA, RoleName.DOCTOR);
        hospADoctor2Token = jwtService.generateToken(docUserA2);
        doctorA2 = createDoctorHelper(docUserA2, "Orthopedics", "LIC_RX2_A2_" + ts);

        User docUserB1 = createUserDirect("Doctor", "Priya", "doc.rx2.priya." + ts + "@test.com", "93" + (ts % 100000000L), hospitalB, RoleName.DOCTOR);
        hospBDoctorToken = jwtService.generateToken(docUserB1);
        doctorB1 = createDoctorHelper(docUserB1, "Pediatrics", "LIC_RX2_B1_" + ts);

        patientA1 = createPatientHelper(hospitalA, "Kiran", "Bedis", "kiran.rx2." + ts + "@test.com", "97" + (ts % 100000000L));
        patientB1 = createPatientHelper(hospitalB, "Rohit", "Verma", "rohit.rx2." + ts + "@test.com", "95" + (ts % 100000000L));

        consultationA1 = createConsultationHelper(hospitalA, doctorA1, patientA1, "Fever and cough");
        consultationA2 = createConsultationHelper(hospitalA, doctorA1, patientA1, "Follow up checkup");
        consultationA3 = createConsultationHelper(hospitalA, doctorA2, patientA1, "Joint pain consultation");
        consultationB1 = createConsultationHelper(hospitalB, doctorB1, patientB1, "Pediatric assessment");
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
        p.setDateOfBirth(LocalDate.of(1995, 3, 20));
        p.setGender(Gender.FEMALE);
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p);
    }

    private Consultation createConsultationHelper(Hospital h, Doctor d, Patient p, String complaint) {
        Consultation c = new Consultation();
        c.setHospital(h);
        c.setDoctor(d);
        c.setPatient(p);
        c.setConsultationDate(LocalDateTime.now());
        c.setChiefComplaint(complaint);
        c.setStatus(ConsultationStatus.IN_PROGRESS);
        return consultationRepository.save(c);
    }

    @Test
    @DisplayName("1-4: Doctor creates prescription, persisted with items & response verified")
    public void test1to4_createPrescription_persisted_responseCorrect() throws Exception {
        PrescriptionItemRequest item1 = PrescriptionItemRequest.builder()
                .medicineName("Azithromycin 500mg")
                .dosage("1 tablet")
                .frequency("OD")
                .duration("3 days")
                .route("ORAL")
                .instructions("Before breakfast")
                .build();

        CreatePrescriptionRequest req = CreatePrescriptionRequest.builder()
                .consultationId(consultationA1.getId())
                .notes("Complete 3-day course of antibiotics")
                .items(List.of(item1))
                .build();

        mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.prescriptionCode").value(startsWith("RX-")))
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()))
                .andExpect(jsonPath("$.doctorId").value(doctorA1.getId()))
                .andExpect(jsonPath("$.patientId").value(patientA1.getId()))
                .andExpect(jsonPath("$.consultationId").value(consultationA1.getId()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.notes").value("Complete 3-day course of antibiotics"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].medicineName").value("Azithromycin 500mg"));
    }

    @Test
    @DisplayName("5-6: Doctor retrieves own prescription & list of prescriptions")
    public void test5to6_getDoctorOwnPrescription_and_List() throws Exception {
        CreatePrescriptionRequest req = CreatePrescriptionRequest.builder()
                .consultationId(consultationA1.getId())
                .notes("Doctor own test")
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Cetrizen 10mg").dosage("1 tab").frequency("HS").duration("5 days").build()))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long rxId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // 5. Get by ID
        mockMvc.perform(get("/api/v1/prescriptions/" + rxId)
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rxId));

        // 6. Get doctor me prescriptions
        mockMvc.perform(get("/api/v1/doctors/me/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(rxId));
    }

    @Test
    @DisplayName("7: Patient prescription history works")
    public void test7_getPatientPrescriptionHistory() throws Exception {
        CreatePrescriptionRequest req = CreatePrescriptionRequest.builder()
                .consultationId(consultationA1.getId())
                .notes("Patient history test")
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Vitamin C 500mg").dosage("1 tab").frequency("OD").duration("10 days").build()))
                .build();

        mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/patients/" + patientA1.getId() + "/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].patientId").value(patientA1.getId()));
    }

    @Test
    @DisplayName("8 & 19: Draft prescription updated & items correctly replaced")
    public void test8and19_updateDraftPrescription_itemsReplaced() throws Exception {
        CreatePrescriptionRequest createReq = CreatePrescriptionRequest.builder()
                .consultationId(consultationA1.getId())
                .notes("Original notes")
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Old Med").dosage("1").frequency("1").duration("1").build()))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long rxId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        UpdatePrescriptionRequest updateReq = UpdatePrescriptionRequest.builder()
                .notes("Updated notes")
                .items(List.of(
                        PrescriptionItemRequest.builder().medicineName("New Med 1").dosage("1").frequency("BD").duration("5d").build(),
                        PrescriptionItemRequest.builder().medicineName("New Med 2").dosage("2").frequency("OD").duration("7d").build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/prescriptions/" + rxId)
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated notes"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].medicineName").value("New Med 1"))
                .andExpect(jsonPath("$.items[1].medicineName").value("New Med 2"));
    }

    @Test
    @DisplayName("9: Draft prescription can be issued")
    public void test9_issueDraftPrescription() throws Exception {
        CreatePrescriptionRequest req = CreatePrescriptionRequest.builder()
                .consultationId(consultationA1.getId())
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Med").dosage("1").frequency("1").duration("1").build()))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long rxId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/prescriptions/" + rxId + "/issue")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.issuedAt").isNotEmpty());
    }

    @Test
    @DisplayName("10: Draft prescription can be cancelled")
    public void test10_cancelDraftPrescription() throws Exception {
        CreatePrescriptionRequest req = CreatePrescriptionRequest.builder()
                .consultationId(consultationA1.getId())
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Med").dosage("1").frequency("1").duration("1").build()))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long rxId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/prescriptions/" + rxId + "/cancel")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("11-14: Invalid state transitions return 409 Conflict")
    public void test11to14_invalidStateTransitions_returns409() throws Exception {
        CreatePrescriptionRequest req1 = CreatePrescriptionRequest.builder()
                .consultationId(consultationA1.getId())
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Med").dosage("1").frequency("1").duration("1").build()))
                .build();

        MvcResult createRes1 = mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        Long rxId1 = objectMapper.readTree(createRes1.getResponse().getContentAsString()).get("id").asLong();

        // Issue rx1
        mockMvc.perform(patch("/api/v1/prescriptions/" + rxId1 + "/issue")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk());

        // 11. Issued prescription cannot be edited -> 409
        UpdatePrescriptionRequest updateReq = UpdatePrescriptionRequest.builder().notes("Illegal update").build();
        mockMvc.perform(put("/api/v1/prescriptions/" + rxId1)
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict());

        // 13. Issued prescription cannot be cancelled -> 409
        mockMvc.perform(patch("/api/v1/prescriptions/" + rxId1 + "/cancel")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isConflict());

        // Create rx2 and cancel it
        CreatePrescriptionRequest req2 = CreatePrescriptionRequest.builder()
                .consultationId(consultationA2.getId())
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Med").dosage("1").frequency("1").duration("1").build()))
                .build();

        MvcResult createRes2 = mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        Long rxId2 = objectMapper.readTree(createRes2.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/prescriptions/" + rxId2 + "/cancel")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isOk());

        // 12. Cancelled prescription cannot be edited -> 409
        mockMvc.perform(put("/api/v1/prescriptions/" + rxId2)
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict());

        // 14. Cancelled prescription cannot be issued -> 409
        mockMvc.perform(patch("/api/v1/prescriptions/" + rxId2 + "/issue")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("15: Duplicate prescription for same consultation returns 409 Conflict")
    public void test15_duplicatePrescriptionForConsultation_returns409() throws Exception {
        CreatePrescriptionRequest req = CreatePrescriptionRequest.builder()
                .consultationId(consultationA1.getId())
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Med").dosage("1").frequency("1").duration("1").build()))
                .build();

        mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second prescription for same consultation -> 409
        mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("16: Doctor cannot prescribe against another doctor's consultation (403 Forbidden)")
    public void test16_prescribeAnotherDoctorConsultation_returns403() throws Exception {
        CreatePrescriptionRequest req = CreatePrescriptionRequest.builder()
                .consultationId(consultationA3.getId()) // Belongs to Doctor A2
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Med").dosage("1").frequency("1").duration("1").build()))
                .build();

        // Doctor A1 trying to create prescription for Doctor A2's consultation
        mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("17 & 18: Cross-hospital access is rejected (404 Not Found)")
    public void test17and18_crossHospitalAccess_returns404() throws Exception {
        CreatePrescriptionRequest req = CreatePrescriptionRequest.builder()
                .consultationId(consultationB1.getId()) // Consultation in Hospital B
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("Med").dosage("1").frequency("1").duration("1").build()))
                .build();

        // Doctor A1 trying to prescribe for Hospital B consultation -> 404
        mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        // Patient from Hospital B accessing Hospital A prescriptions -> 404
        mockMvc.perform(get("/api/v1/patients/" + patientB1.getId() + "/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("20: Bean validation failure returns 400 Bad Request")
    public void test20_beanValidationFailure_returns400() throws Exception {
        CreatePrescriptionRequest req = CreatePrescriptionRequest.builder()
                .consultationId(consultationA1.getId())
                .items(List.of(PrescriptionItemRequest.builder()
                        .medicineName("") // Empty medicine name -> @NotBlank fails
                        .dosage("1")
                        .frequency("1")
                        .duration("1")
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + hospADoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
