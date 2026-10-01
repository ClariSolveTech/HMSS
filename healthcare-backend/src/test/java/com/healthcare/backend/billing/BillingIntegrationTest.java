package com.healthcare.backend.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.HealthcareBackendApplication;
import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.billing.dto.*;
import com.healthcare.backend.billing.entity.*;
import com.healthcare.backend.billing.repository.InvoiceItemRepository;
import com.healthcare.backend.billing.repository.InvoiceRepository;
import com.healthcare.backend.billing.repository.PaymentRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HealthcareBackendApplication.class)
@ActiveProfiles("test")
@Transactional
public class BillingIntegrationTest {

    private static final AtomicLong CODE_COUNTER = new AtomicLong(95000);

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
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private User docUserA1;
    private User docUserA2;
    private User docUserB1;

    private Doctor doctorA1;
    private Doctor doctorA2;
    private Doctor doctorB1;

    private Patient patientA1;
    private Patient patientB1;

    private String tokenDocA1;
    private String tokenDocA2;
    private String tokenDocB1;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        long ts = System.currentTimeMillis() % 10000000L;

        // Hospital A
        hospitalA = new Hospital();
        hospitalA.setName("General Hospital A");
        hospitalA.setCode("HOSP_A_" + ts);
        hospitalA.setEmail("admin.a." + ts + "@hospa.com");
        hospitalA.setPhone("9876543210");
        hospitalA.setAddress("Main Road A");
        hospitalA.setCity("CityA");
        hospitalA.setState("StateA");
        hospitalA.setPincode("100001");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        // Hospital B
        hospitalB = new Hospital();
        hospitalB.setName("Metro Care B");
        hospitalB.setCode("HOSP_B_" + ts);
        hospitalB.setEmail("admin.b." + ts + "@hospb.com");
        hospitalB.setPhone("9876543211");
        hospitalB.setAddress("Cross Road B");
        hospitalB.setCity("CityB");
        hospitalB.setState("StateB");
        hospitalB.setPincode("100002");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        // Users & Roles
        docUserA1 = createUserDirect("Dr. Aarav", "Sharma", "doc.a1." + ts + "@test.com", "91" + (ts % 100000000L), hospitalA, RoleName.DOCTOR);
        docUserA2 = createUserDirect("Dr. Vikram", "Singh", "doc.a2." + ts + "@test.com", "92" + (ts % 100000000L), hospitalA, RoleName.DOCTOR);
        docUserB1 = createUserDirect("Dr. Neha", "Gupta", "doc.b1." + ts + "@test.com", "93" + (ts % 100000000L), hospitalB, RoleName.DOCTOR);

        tokenDocA1 = jwtService.generateToken(docUserA1);
        tokenDocA2 = jwtService.generateToken(docUserA2);
        tokenDocB1 = jwtService.generateToken(docUserB1);

        doctorA1 = createDoctorHelper(docUserA1, "Cardiology", "LIC_A1_" + ts);
        doctorA2 = createDoctorHelper(docUserA2, "Neurology", "LIC_A2_" + ts);
        doctorB1 = createDoctorHelper(docUserB1, "Pediatrics", "LIC_B1_" + ts);

        patientA1 = createPatientHelper(hospitalA, "Rahul", "Verma", "rahul.a1." + ts + "@test.com", "94" + (ts % 100000000L));
        patientB1 = createPatientHelper(hospitalB, "Priya", "Roy", "priya.b1." + ts + "@test.com", "95" + (ts % 100000000L));
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
        p.setDateOfBirth(LocalDate.of(1992, 8, 20));
        p.setGender(Gender.MALE);
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p);
    }

    private CreateInvoiceRequest createSampleInvoiceRequest(Long patientId, Long doctorId) {
        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setPatientId(patientId);
        req.setDoctorId(doctorId);
        req.setNotes("Test Invoice Notes");

        InvoiceItemRequest item1 = new InvoiceItemRequest();
        item1.setItemType(InvoiceItemType.CONSULTATION);
        item1.setDescription("Cardiology Consultation Fee");
        item1.setQuantity(1);
        item1.setUnitPrice(new BigDecimal("500.00"));
        item1.setDiscount(new BigDecimal("50.00"));
        item1.setTax(new BigDecimal("25.00"));

        InvoiceItemRequest item2 = new InvoiceItemRequest();
        item2.setItemType(InvoiceItemType.MEDICINE);
        item2.setDescription("Aspirin 75mg");
        item2.setQuantity(2);
        item2.setUnitPrice(new BigDecimal("20.00"));
        item2.setDiscount(BigDecimal.ZERO);
        item2.setTax(new BigDecimal("2.00"));

        req.setItems(List.of(item1, item2));
        return req;
    }

    @Test
    @DisplayName("1-4. Doctor creates invoice with generated number, persisted items, and correct amounts")
    public void test1_to_4_createInvoice_success() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult res = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.invoiceNumber").value(startsWith("INV-")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.subtotal").value(540.00))
                .andExpect(jsonPath("$.discount").value(50.00))
                .andExpect(jsonPath("$.tax").value(27.00))
                .andExpect(jsonPath("$.totalAmount").value(517.00))
                .andExpect(jsonPath("$.paidAmount").value(0.00))
                .andExpect(jsonPath("$.dueAmount").value(517.00))
                .andReturn();

        InvoiceResponse response = objectMapper.readValue(res.getResponse().getContentAsString(), InvoiceResponse.class);
        assertNotNull(response.getId());
        assertTrue(invoiceRepository.existsById(response.getId()));
    }

    @Test
    @DisplayName("5. Draft invoice retrieved")
    public void test5_getDraftInvoice_success() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/invoices/" + id)
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("6. Draft invoice updated")
    public void test6_updateDraftInvoice_success() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        UpdateInvoiceRequest updateReq = new UpdateInvoiceRequest();
        updateReq.setNotes("Updated Notes");

        InvoiceItemRequest newItem = new InvoiceItemRequest();
        newItem.setItemType(InvoiceItemType.LAB);
        newItem.setDescription("ECG Test");
        newItem.setQuantity(1);
        newItem.setUnitPrice(new BigDecimal("300.00"));
        newItem.setDiscount(BigDecimal.ZERO);
        newItem.setTax(BigDecimal.ZERO);

        updateReq.setItems(List.of(newItem));

        mockMvc.perform(put("/api/v1/invoices/" + id)
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated Notes"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalAmount").value(300.00));
    }

    @Test
    @DisplayName("7-8. Draft invoice issued and becomes immutable")
    public void test7_8_issueInvoiceAndVerifyImmutability() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Issue invoice
        mockMvc.perform(patch("/api/v1/invoices/" + id + "/issue")
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.issuedAt").exists());

        // Attempting to update issued invoice returns 409 Conflict
        UpdateInvoiceRequest updateReq = new UpdateInvoiceRequest();
        updateReq.setNotes("Should fail");
        InvoiceItemRequest item = new InvoiceItemRequest();
        item.setItemType(InvoiceItemType.OTHER);
        item.setDescription("Service");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("100.00"));
        updateReq.setItems(List.of(item));

        mockMvc.perform(put("/api/v1/invoices/" + id)
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("9-10. Draft invoice cancelled and cannot be edited")
    public void test9_10_cancelInvoiceAndVerifyImmutability() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Cancel invoice
        mockMvc.perform(patch("/api/v1/invoices/" + id + "/cancel")
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Attempting to update cancelled invoice returns 409
        UpdateInvoiceRequest updateReq = new UpdateInvoiceRequest();
        InvoiceItemRequest item = new InvoiceItemRequest();
        item.setItemType(InvoiceItemType.OTHER);
        item.setDescription("Test");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("10.00"));
        updateReq.setItems(List.of(item));

        mockMvc.perform(put("/api/v1/invoices/" + id)
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("11-15. Payment lifecycle: Issued invoice receives payments, transitions PARTIALLY_PAID -> PAID")
    public void test11_to_15_paymentLifecycle_partialToFull() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();
        BigDecimal totalAmount = new BigDecimal(objectMapper.readTree(createRes.getResponse().getContentAsString()).get("totalAmount").asText());

        // Issue invoice
        mockMvc.perform(patch("/api/v1/invoices/" + id + "/issue")
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk());

        // Payment 1: Partial payment of 200.00
        CreatePaymentRequest payReq1 = new CreatePaymentRequest();
        payReq1.setAmount(new BigDecimal("200.00"));
        payReq1.setPaymentMethod(PaymentMethod.CASH);
        payReq1.setNotes("Advance cash payment");

        mockMvc.perform(post("/api/v1/invoices/" + id + "/payments")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify invoice is now PARTIALLY_PAID
        mockMvc.perform(get("/api/v1/invoices/" + id)
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.paidAmount").value(200.00))
                .andExpect(jsonPath("$.dueAmount").value(317.00));

        // Payment 2: Remaining payment of 317.00
        CreatePaymentRequest payReq2 = new CreatePaymentRequest();
        payReq2.setAmount(new BigDecimal("317.00"));
        payReq2.setPaymentMethod(PaymentMethod.UPI);
        payReq2.setTransactionReference("UPI987654321");

        mockMvc.perform(post("/api/v1/invoices/" + id + "/payments")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq2)))
                .andExpect(status().isCreated());

        // Verify invoice is now PAID
        mockMvc.perform(get("/api/v1/invoices/" + id)
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAmount").value(517.00))
                .andExpect(jsonPath("$.dueAmount").value(0.00));
    }

    @Test
    @DisplayName("16. Paid invoice rejects additional payment")
    public void test16_paidInvoiceRejectsAdditionalPayment() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Issue invoice
        mockMvc.perform(patch("/api/v1/invoices/" + id + "/issue")
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk());

        // Pay full amount (517.00)
        CreatePaymentRequest fullPay = new CreatePaymentRequest();
        fullPay.setAmount(new BigDecimal("517.00"));
        fullPay.setPaymentMethod(PaymentMethod.CARD);

        mockMvc.perform(post("/api/v1/invoices/" + id + "/payments")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullPay)))
                .andExpect(status().isCreated());

        // Extra payment on paid invoice returns 409 Conflict
        CreatePaymentRequest extraPay = new CreatePaymentRequest();
        extraPay.setAmount(new BigDecimal("10.00"));
        extraPay.setPaymentMethod(PaymentMethod.CASH);

        mockMvc.perform(post("/api/v1/invoices/" + id + "/payments")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraPay)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("17. Payment cannot exceed due amount")
    public void test17_paymentCannotExceedDueAmount() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Issue invoice
        mockMvc.perform(patch("/api/v1/invoices/" + id + "/issue")
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk());

        // Overpayment (due is 517.00, paying 600.00)
        CreatePaymentRequest overPay = new CreatePaymentRequest();
        overPay.setAmount(new BigDecimal("600.00"));
        overPay.setPaymentMethod(PaymentMethod.CASH);

        mockMvc.perform(post("/api/v1/invoices/" + id + "/payments")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overPay)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("18. Cancelled invoice rejects payment")
    public void test18_cancelledInvoiceRejectsPayment() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Cancel invoice
        mockMvc.perform(patch("/api/v1/invoices/" + id + "/cancel")
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk());

        // Payment on cancelled invoice returns 409 Conflict
        CreatePaymentRequest payReq = new CreatePaymentRequest();
        payReq.setAmount(new BigDecimal("100.00"));
        payReq.setPaymentMethod(PaymentMethod.CASH);

        mockMvc.perform(post("/api/v1/invoices/" + id + "/payments")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("19-20. Cross-hospital invoice and payment lookup returns 404")
    public void test19_20_crossHospitalIsolation_returns404() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long invId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Doctor from Hospital B trying to access Hospital A's invoice returns 404
        mockMvc.perform(get("/api/v1/invoices/" + invId)
                        .header("Authorization", "Bearer " + tokenDocB1))
                .andExpect(status().isNotFound());

        // Doctor from Hospital B trying to access Hospital A's invoice payments returns 404
        mockMvc.perform(get("/api/v1/invoices/" + invId + "/payments")
                        .header("Authorization", "Bearer " + tokenDocB1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("21-22. Doctor cannot create or modify invoice for another doctor profile (403 Forbidden)")
    public void test21_22_doctorAuthorizationRestrictions() throws Exception {
        // Doctor A1 trying to create invoice for Doctor A2 returns 403 Forbidden
        CreateInvoiceRequest reqForOtherDoc = createSampleInvoiceRequest(patientA1.getId(), doctorA2.getId());

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqForOtherDoc)))
                .andExpect(status().isForbidden());

        // Create valid invoice under Doctor A2
        CreateInvoiceRequest reqA2 = createSampleInvoiceRequest(patientA1.getId(), doctorA2.getId());
        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA2)))
                .andExpect(status().isCreated())
                .andReturn();

        Long invIdA2 = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Doctor A1 trying to update Doctor A2's invoice returns 403 Forbidden
        UpdateInvoiceRequest updateReq = new UpdateInvoiceRequest();
        updateReq.setNotes("Tampered by Doctor A1");
        InvoiceItemRequest item = new InvoiceItemRequest();
        item.setItemType(InvoiceItemType.CONSULTATION);
        item.setDescription("Fee");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("100.00"));
        updateReq.setItems(List.of(item));

        mockMvc.perform(put("/api/v1/invoices/" + invIdA2)
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("23. Patient invoice history works")
    public void test23_patientInvoiceHistory_success() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/patients/" + patientA1.getId() + "/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("24. Doctor invoice history works")
    public void test24_doctorInvoiceHistory_success() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/doctors/me/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("25. Unauthenticated access returns 401")
    public void test25_unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("26. Invalid invoice data returns 400")
    public void test26_invalidInvoiceDataReturns400() throws Exception {
        CreateInvoiceRequest badReq = new CreateInvoiceRequest();
        // Missing patientId, doctorId, items

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("27. Invalid payment amount returns 400")
    public void test27_invalidPaymentAmountReturns400() throws Exception {
        CreateInvoiceRequest req = createSampleInvoiceRequest(patientA1.getId(), doctorA1.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Issue invoice
        mockMvc.perform(patch("/api/v1/invoices/" + id + "/issue")
                        .header("Authorization", "Bearer " + tokenDocA1))
                .andExpect(status().isOk());

        // Zero payment amount returns 400
        CreatePaymentRequest zeroPay = new CreatePaymentRequest();
        zeroPay.setAmount(BigDecimal.ZERO);
        zeroPay.setPaymentMethod(PaymentMethod.CASH);

        mockMvc.perform(post("/api/v1/invoices/" + id + "/payments")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroPay)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("28-29. Security: Doctor A cannot create invoice for Hospital B patient")
    public void test28_29_doctorCannotCreateInvoiceForOtherHospitalPatient() throws Exception {
        // Doctor A1 trying to create invoice for Hospital B's patientB1 returns 404 (or 400)
        CreateInvoiceRequest crossHospReq = createSampleInvoiceRequest(patientB1.getId(), doctorA1.getId());

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenDocA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossHospReq)))
                .andExpect(status().isNotFound());
    }
}
