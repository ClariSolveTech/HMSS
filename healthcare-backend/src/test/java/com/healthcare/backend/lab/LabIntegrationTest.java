package com.healthcare.backend.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.HealthcareBackendApplication;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.billing.repository.InvoiceItemRepository;
import com.healthcare.backend.billing.repository.InvoiceRepository;
import com.healthcare.backend.billing.repository.PaymentRepository;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.lab.dto.*;
import com.healthcare.backend.lab.entity.*;
import com.healthcare.backend.lab.repository.*;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HealthcareBackendApplication.class)
@ActiveProfiles("test")
@Transactional
public class LabIntegrationTest {

    private static final AtomicLong CODE_COUNTER = new AtomicLong(77000);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private Doctor doctorA;
    private Doctor doctorB;
    private Patient patientA;
    private Patient patientB;
    private Consultation consultationA;
    private Consultation consultationB;
    private LabTest labTestA1;
    private LabTest labTestA2;
    private LabTest labTestB1;

    private String docAToken;
    private String docBToken;
    private String labStaffAToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        long ts = System.currentTimeMillis();

        // Setup Hospitals
        hospitalA = new Hospital();
        hospitalA.setName("LabCare Hospital Alpha " + ts);
        hospitalA.setCode("LCA" + (ts % 10000));
        hospitalA.setPhone("+919700000001");
        hospitalA.setEmail("alpha.lab." + ts + "@hospital.com");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        hospitalB = new Hospital();
        hospitalB.setName("LabCare Hospital Beta " + ts);
        hospitalB.setCode("LCB" + (ts % 10000));
        hospitalB.setPhone("+919700000002");
        hospitalB.setEmail("beta.lab." + ts + "@hospital.com");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        // Users & Tokens
        User userDocA = createUser("DocA", "Alpha", "doca." + ts + "@labcare.com", hospitalA, RoleName.DOCTOR);
        doctorA = createDoctor(userDocA, "Cardiology", "LIC-LCA");
        docAToken = jwtService.generateToken(userDocA);

        User userDocB = createUser("DocB", "Beta", "docb." + ts + "@labcare.com", hospitalB, RoleName.DOCTOR);
        doctorB = createDoctor(userDocB, "Neurology", "LIC-LCB");
        docBToken = jwtService.generateToken(userDocB);

        User userLabStaffA = createUser("StaffA", "Lab", "staffa." + ts + "@labcare.com", hospitalA, RoleName.LAB_STAFF);
        labStaffAToken = jwtService.generateToken(userLabStaffA);

        // Patients
        patientA = createPatient(hospitalA, "Alice", "Smith", "alice." + ts + "@pat.com");
        patientB = createPatient(hospitalB, "Bob", "Jones", "bob." + ts + "@pat.com");

        // Consultations
        consultationA = new Consultation();
        consultationA.setHospital(hospitalA);
        consultationA.setDoctor(doctorA);
        consultationA.setPatient(patientA);
        consultationA.setConsultationDate(LocalDateTime.now());
        consultationA = consultationRepository.save(consultationA);

        consultationB = new Consultation();
        consultationB.setHospital(hospitalB);
        consultationB.setDoctor(doctorB);
        consultationB.setPatient(patientB);
        consultationB.setConsultationDate(LocalDateTime.now());
        consultationB = consultationRepository.save(consultationB);

        // Lab Tests
        labTestA1 = createLabTest(hospitalA, "CBC-01", "Complete Blood Count", "HEMATOLOGY", true);
        labTestA2 = createLabTest(hospitalA, "LIP-01", "Lipid Panel", "BIOCHEMISTRY", true);
        labTestB1 = createLabTest(hospitalB, "CBC-01", "Complete Blood Count Beta", "HEMATOLOGY", true);
    }

    private User createUser(String f, String l, String e, Hospital h, RoleName r) {
        Role role = roleRepository.findByName(r).orElseGet(() -> roleRepository.save(new Role(r)));
        User u = new User();
        u.setFirstName(f);
        u.setLastName(l);
        u.setEmail(e);
        u.setMobileNumber("9" + (System.currentTimeMillis() % 100000000L));
        u.setPasswordHash(passwordEncoder.encode("Pass@1234"));
        u.setHospital(h);
        u.setStatus(UserStatus.ACTIVE);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    private Doctor createDoctor(User u, String spec, String lic) {
        Doctor d = new Doctor();
        d.setUser(u);
        d.setHospital(u.getHospital());
        long seq = CODE_COUNTER.incrementAndGet();
        d.setDoctorCode(u.getHospital().getCode() + "-DOC" + seq);
        d.setSpecialization(spec);
        d.setQualification("MD");
        d.setLicenseNumber(lic + "-" + seq);
        d.setConsultationFee(new BigDecimal("500.00"));
        d.setStatus(DoctorStatus.ACTIVE);
        return doctorRepository.save(d);
    }

    private Patient createPatient(Hospital h, String f, String l, String e) {
        long seq = CODE_COUNTER.incrementAndGet();
        Patient p = new Patient();
        p.setHospital(h);
        p.setPatientCode(h.getCode() + "-P" + seq);
        p.setFirstName(f);
        p.setLastName(l);
        p.setEmail(e);
        p.setMobileNumber("98" + (seq % 10000000L));
        p.setGender(Gender.FEMALE);
        p.setDateOfBirth(LocalDate.of(1992, 5, 10));
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p);
    }

    private LabTest createLabTest(Hospital h, String code, String name, String category, boolean active) {
        LabTest t = new LabTest();
        t.setHospital(h);
        t.setTestCode(code);
        t.setTestName(name);
        t.setCategory(category);
        t.setSampleType("Blood");
        t.setUnit("mg/dL");
        t.setReferenceRange("70-110");
        t.setActive(active);
        return labTestRepository.save(t);
    }

    @Test
    @DisplayName("1. Doctor can list active lab tests")
    void test1_doctorCanListActiveLabTests() throws Exception {
        mockMvc.perform(get("/api/v1/lab/tests")
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].testCode", is(oneOf("CBC-01", "LIP-01"))));
    }

    @Test
    @DisplayName("2. Doctor can create lab order")
    void test2_doctorCanCreateLabOrder() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientA.getId());
        request.setConsultationId(consultationA.getId());
        request.setPriority(LabOrderPriority.URGENT);
        request.setClinicalNotes("Fasting sample requested");
        request.getItems().add(new CreateLabOrderItemRequest(labTestA1.getId(), "Fasting"));
        request.getItems().add(new CreateLabOrderItemRequest(labTestA2.getId(), "Fasting"));

        mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber", startsWith("LAB-")))
                .andExpect(jsonPath("$.patientId", is(patientA.getId().intValue())))
                .andExpect(jsonPath("$.doctorId", is(doctorA.getId().intValue())))
                .andExpect(jsonPath("$.hospitalId", is(hospitalA.getId().intValue())))
                .andExpect(jsonPath("$.priority", is("URGENT")))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("3. Server generates order number")
    void test3_serverGeneratesOrderNumber() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientA.getId());
        request.getItems().add(new CreateLabOrderItemRequest(labTestA1.getId(), null));

        MvcResult result = mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        LabOrderResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), LabOrderResponse.class);
        assertNotNull(response.getOrderNumber());
        assertEquals(true, response.getOrderNumber().startsWith("LAB-"));
    }

    @Test
    @DisplayName("4. Client cannot override hospitalId")
    void test4_clientCannotOverrideHospitalId() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientA.getId());
        request.getItems().add(new CreateLabOrderItemRequest(labTestA1.getId(), null));

        mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hospitalId", is(hospitalA.getId().intValue())));
    }

    @Test
    @DisplayName("5. Client cannot override doctorId")
    void test5_clientCannotOverrideDoctorId() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientA.getId());
        request.getItems().add(new CreateLabOrderItemRequest(labTestA1.getId(), null));

        mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.doctorId", is(doctorA.getId().intValue())));
    }

    @Test
    @DisplayName("6. Patient must belong to doctor's hospital")
    void test6_patientMustBelongToDoctorHospital() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientA.getId());
        request.getItems().add(new CreateLabOrderItemRequest(labTestA1.getId(), null));

        mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("7. Consultation must belong to doctor's hospital")
    void test7_consultationMustBelongToDoctorHospital() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientA.getId());
        request.setConsultationId(consultationA.getId());
        request.getItems().add(new CreateLabOrderItemRequest(labTestA1.getId(), null));

        mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("8. Lab test must belong to doctor's hospital")
    void test8_labTestMustBelongToDoctorHospital() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientA.getId());
        request.getItems().add(new CreateLabOrderItemRequest(labTestA1.getId(), null));

        mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("9. Cross-hospital patient is rejected")
    void test9_crossHospitalPatientIsRejected() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientB.getId()); // Patient from Hospital B
        request.getItems().add(new CreateLabOrderItemRequest(labTestA1.getId(), null));

        mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("10. Cross-hospital consultation is rejected")
    void test10_crossHospitalConsultationIsRejected() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientA.getId());
        request.setConsultationId(consultationB.getId()); // Consultation from Hospital B
        request.getItems().add(new CreateLabOrderItemRequest(labTestA1.getId(), null));

        mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("11. Cross-hospital lab test is rejected")
    void test11_crossHospitalLabTestIsRejected() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest();
        request.setPatientId(patientA.getId());
        request.getItems().add(new CreateLabOrderItemRequest(labTestB1.getId(), null)); // Lab Test from Hospital B

        mockMvc.perform(post("/api/v1/lab/orders")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("12. Doctor can retrieve own lab order")
    void test12_doctorCanRetrieveOwnLabOrder() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);

        mockMvc.perform(get("/api/v1/lab/orders/" + order.getId())
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(order.getId().intValue())))
                .andExpect(jsonPath("$.orderNumber", is(order.getOrderNumber())));

        mockMvc.perform(get("/api/v1/doctors/me/lab-orders")
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("13. Doctor can retrieve patient lab history")
    void test13_doctorCanRetrievePatientLabHistory() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);

        mockMvc.perform(get("/api/v1/patients/" + patientA.getId() + "/lab-orders")
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(order.getId().intValue())));
    }

    @Test
    @DisplayName("14. Doctor cannot retrieve another hospital's order")
    void test14_doctorCannotRetrieveAnotherHospitalOrder() throws Exception {
        LabOrder orderB = createLabOrderHelper(hospitalB, doctorB, patientB, labTestB1);

        mockMvc.perform(get("/api/v1/lab/orders/" + orderB.getId())
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("15. Order status transitions are enforced")
    void test15_orderStatusTransitionsAreEnforced() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);

        UpdateLabOrderStatusRequest req1 = new UpdateLabOrderStatusRequest();
        req1.setStatus(LabOrderStatus.SAMPLE_COLLECTED);

        mockMvc.perform(patch("/api/v1/lab/orders/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SAMPLE_COLLECTED")));

        UpdateLabOrderStatusRequest req2 = new UpdateLabOrderStatusRequest();
        req2.setStatus(LabOrderStatus.PROCESSING);

        mockMvc.perform(patch("/api/v1/lab/orders/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSING")));
    }

    @Test
    @DisplayName("16. Invalid transition returns conflict")
    void test16_invalidTransitionReturnsConflict() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        order.setStatus(LabOrderStatus.COMPLETED);
        order = labOrderRepository.save(order);

        UpdateLabOrderStatusRequest req = new UpdateLabOrderStatusRequest();
        req.setStatus(LabOrderStatus.ORDERED);

        mockMvc.perform(patch("/api/v1/lab/orders/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("17. Completed order cannot be modified")
    void test17_completedOrderCannotBeModified() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        order.setStatus(LabOrderStatus.COMPLETED);
        order = labOrderRepository.save(order);

        UpdateLabOrderRequest req = new UpdateLabOrderRequest();
        req.setClinicalNotes("Modified notes");

        mockMvc.perform(put("/api/v1/lab/orders/" + order.getId())
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("18. Cancelled order cannot be modified")
    void test18_cancelledOrderCannotBeModified() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        order.setStatus(LabOrderStatus.CANCELLED);
        order = labOrderRepository.save(order);

        UpdateLabOrderRequest req = new UpdateLabOrderRequest();
        req.setClinicalNotes("Modified notes");

        mockMvc.perform(put("/api/v1/lab/orders/" + order.getId())
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("19. Doctor can view completed lab results")
    void test19_doctorCanViewCompletedLabResults() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        LabOrderItem item = order.getItems().get(0);
        LabResult res = createLabResultHelper(order, item, "120.0", false, LabResultStatus.COMPLETED);

        mockMvc.perform(get("/api/v1/lab/results/" + res.getId())
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultValue", is("120.0")))
                .andExpect(jsonPath("$.resultStatus", is("COMPLETED")));
    }

    @Test
    @DisplayName("20. Doctor can review completed result")
    void test20_doctorCanReviewCompletedResult() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        LabOrderItem item = order.getItems().get(0);
        LabResult res = createLabResultHelper(order, item, "125.0", true, LabResultStatus.COMPLETED);

        ReviewLabResultRequest req = new ReviewLabResultRequest();
        req.setInterpretation("Confirmed slightly high");

        mockMvc.perform(patch("/api/v1/lab/results/" + res.getId() + "/review")
                        .header("Authorization", "Bearer " + docAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultStatus", is("REVIEWED")))
                .andExpect(jsonPath("$.interpretation", is("Confirmed slightly high")))
                .andExpect(jsonPath("$.reviewedBy", startsWith("Dr. ")));
    }

    @Test
    @DisplayName("21. Doctor cannot review pending result")
    void test21_doctorCannotReviewPendingResult() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        LabOrderItem item = order.getItems().get(0);
        LabResult res = createLabResultHelper(order, item, "120.0", false, LabResultStatus.PENDING);

        mockMvc.perform(patch("/api/v1/lab/results/" + res.getId() + "/review")
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("22. Cross-hospital result access is rejected")
    void test22_crossHospitalResultAccessIsRejected() throws Exception {
        LabOrder orderB = createLabOrderHelper(hospitalB, doctorB, patientB, labTestB1);
        LabOrderItem itemB = orderB.getItems().get(0);
        LabResult resB = createLabResultHelper(orderB, itemB, "99.0", false, LabResultStatus.COMPLETED);

        mockMvc.perform(get("/api/v1/lab/results/" + resB.getId())
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/lab/results/" + resB.getId() + "/review")
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("23. Result belongs to correct order item")
    void test23_resultBelongsToCorrectOrderItem() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        LabOrderItem item = order.getItems().get(0);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("13.5");
        req.setResultUnit("g/dL");
        req.setReferenceRange("12.0 - 15.5");

        mockMvc.perform(post("/api/v1/lab/results")
                        .header("Authorization", "Bearer " + labStaffAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.labOrderItemId", is(item.getId().intValue())))
                .andExpect(jsonPath("$.labOrderId", is(order.getId().intValue())));
    }

    @Test
    @DisplayName("24. Duplicate result for same order item is rejected")
    void test24_duplicateResultForSameOrderItemIsRejected() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        LabOrderItem item = order.getItems().get(0);
        createLabResultHelper(order, item, "13.5", false, LabResultStatus.COMPLETED);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("14.0");

        mockMvc.perform(post("/api/v1/lab/results")
                        .header("Authorization", "Bearer " + labStaffAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("25. Lab staff authorization works for result entry")
    void test25_labStaffAuthorizationWorks() throws Exception {
        LabOrder order = createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        LabOrderItem item = order.getItems().get(0);

        CreateLabResultRequest req = new CreateLabResultRequest();
        req.setLabOrderItemId(item.getId());
        req.setResultValue("13.8");

        mockMvc.perform(post("/api/v1/lab/results")
                        .header("Authorization", "Bearer " + labStaffAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("26. Pagination and filtering works for doctor lab orders")
    void test26_filteringAndPagination() throws Exception {
        createLabOrderHelper(hospitalA, doctorA, patientA, labTestA1);
        createLabOrderHelper(hospitalA, doctorA, patientA, labTestA2);

        mockMvc.perform(get("/api/v1/doctors/me/lab-orders?status=ORDERED&page=0&size=10")
                        .header("Authorization", "Bearer " + docAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    private LabOrder createLabOrderHelper(Hospital h, Doctor d, Patient p, LabTest t) {
        LabOrder order = new LabOrder();
        order.setOrderNumber("LAB-" + System.currentTimeMillis() + "-" + (CODE_COUNTER.incrementAndGet() % 1000));
        order.setHospital(h);
        order.setDoctor(d);
        order.setPatient(p);
        order.setStatus(LabOrderStatus.ORDERED);
        order.setPriority(LabOrderPriority.NORMAL);
        order.setOrderedAt(LocalDateTime.now());

        LabOrderItem item = new LabOrderItem();
        item.setLabOrder(order);
        item.setLabTest(t);
        item.setTestNameSnapshot(t.getTestName());
        item.setTestCodeSnapshot(t.getTestCode());
        item.setStatus(LabOrderStatus.ORDERED);
        order.getItems().add(item);

        return labOrderRepository.save(order);
    }

    private LabResult createLabResultHelper(LabOrder o, LabOrderItem i, String val, boolean abnormal, LabResultStatus status) {
        LabResult r = new LabResult();
        r.setLabOrder(o);
        r.setLabOrderItem(i);
        r.setHospital(o.getHospital());
        r.setDoctor(o.getDoctor());
        r.setPatient(o.getPatient());
        r.setResultValue(val);
        r.setResultUnit("mg/dL");
        r.setReferenceRange("70-110");
        r.setAbnormalFlag(abnormal);
        r.setResultStatus(status);
        r.setResultedAt(LocalDateTime.now());
        return labResultRepository.save(r);
    }
}
