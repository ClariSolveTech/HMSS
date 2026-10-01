package com.healthcare.backend.lab;

import com.healthcare.backend.HealthcareBackendApplication;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.lab.entity.*;
import com.healthcare.backend.lab.repository.*;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = HealthcareBackendApplication.class)
@ActiveProfiles("test")
@Transactional
public class LabEntityIntegrationTest {

    private static final AtomicLong CODE_COUNTER = new AtomicLong(88000);

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

    private Hospital hospitalA;
    private Hospital hospitalB;
    private Doctor doctorA;
    private Doctor doctorB;
    private Patient patientA;
    private Patient patientB;
    private Consultation consultationA;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        // Setup Hospital A
        hospitalA = new Hospital();
        hospitalA.setName("Lab Hospital Alpha " + ts);
        hospitalA.setCode("LHA" + (ts % 10000));
        hospitalA.setPhone("+919800000001");
        hospitalA.setEmail("alpha." + ts + "@lab.com");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        // Setup Hospital B
        hospitalB = new Hospital();
        hospitalB.setName("Lab Hospital Beta " + ts);
        hospitalB.setCode("LHB" + (ts % 10000));
        hospitalB.setPhone("+919800000002");
        hospitalB.setEmail("beta." + ts + "@lab.com");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        // Users & Doctors
        User docUserA = createUser("DocA", "Alpha", "doca." + ts + "@lab.com", hospitalA);
        doctorA = createDoctor(docUserA, "Cardiology", "LIC-LA1");

        User docUserB = createUser("DocB", "Beta", "docb." + ts + "@lab.com", hospitalB);
        doctorB = createDoctor(docUserB, "Neurology", "LIC-LB1");

        // Patients
        patientA = createPatient(hospitalA, "John", "Doe", "john." + ts + "@patient.com");
        patientB = createPatient(hospitalB, "Jane", "Smith", "jane." + ts + "@patient.com");

        // Consultation
        consultationA = new Consultation();
        consultationA.setHospital(hospitalA);
        consultationA.setDoctor(doctorA);
        consultationA.setPatient(patientA);
        consultationA.setConsultationDate(LocalDateTime.now());
        consultationA = consultationRepository.save(consultationA);
    }

    private User createUser(String f, String l, String email, Hospital h) {
        Role role = roleRepository.findByName(RoleName.DOCTOR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.DOCTOR)));
        User u = new User();
        u.setFirstName(f);
        u.setLastName(l);
        u.setEmail(email);
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

    private Patient createPatient(Hospital h, String f, String l, String email) {
        long seq = CODE_COUNTER.incrementAndGet();
        Patient p = new Patient();
        p.setHospital(h);
        p.setPatientCode(h.getCode() + "-PAT" + seq);
        p.setFirstName(f);
        p.setLastName(l);
        p.setEmail(email);
        p.setMobileNumber("98" + (seq % 10000000L));
        p.setGender(Gender.MALE);
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p);
    }

    @Test
    @DisplayName("1. Create hospital-specific LabTest")
    void test1_createHospitalSpecificLabTest() {
        LabTest test = new LabTest();
        test.setHospital(hospitalA);
        test.setTestCode("LAB-CBC");
        test.setTestName("Complete Blood Count");
        test.setDescription("Measures overall health and detects disorders");
        test.setCategory("HEMATOLOGY");
        test.setSampleType("Whole Blood");
        test.setUnit("cells/mcL");
        test.setReferenceRange("4.5-11.0");
        test.setActive(true);

        LabTest saved = labTestRepository.save(test);

        assertNotNull(saved.getId());
        assertEquals("LAB-CBC", saved.getTestCode());
        assertEquals(hospitalA.getId(), saved.getHospital().getId());
        assertTrue(saved.isActive());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("2. Create LabOrder with Patient, Doctor, Consultation, and OrderItems")
    void test2_createLabOrderWithPatientDoctorConsultationAndItems() {
        LabTest test1 = new LabTest();
        test1.setHospital(hospitalA);
        test1.setTestCode("LAB-CBC");
        test1.setTestName("Complete Blood Count");
        test1 = labTestRepository.save(test1);

        LabTest test2 = new LabTest();
        test2.setHospital(hospitalA);
        test2.setTestCode("LAB-LIPID");
        test2.setTestName("Lipid Profile");
        test2 = labTestRepository.save(test2);

        LabOrder order = new LabOrder();
        order.setOrderNumber("ORD-ALPHA-001");
        order.setHospital(hospitalA);
        order.setPatient(patientA);
        order.setDoctor(doctorA);
        order.setConsultation(consultationA);
        order.setStatus(LabOrderStatus.ORDERED);
        order.setPriority(LabOrderPriority.URGENT);
        order.setClinicalNotes("Fasting sample required");
        order.setOrderedAt(LocalDateTime.now());

        LabOrderItem item1 = new LabOrderItem();
        item1.setLabOrder(order);
        item1.setLabTest(test1);
        item1.setTestNameSnapshot(test1.getTestName());
        item1.setTestCodeSnapshot(test1.getTestCode());
        item1.setStatus(LabOrderStatus.ORDERED);

        LabOrderItem item2 = new LabOrderItem();
        item2.setLabOrder(order);
        item2.setLabTest(test2);
        item2.setTestNameSnapshot(test2.getTestName());
        item2.setTestCodeSnapshot(test2.getTestCode());
        item2.setStatus(LabOrderStatus.ORDERED);

        order.getItems().add(item1);
        order.getItems().add(item2);

        LabOrder saved = labOrderRepository.save(order);

        assertNotNull(saved.getId());
        assertEquals("ORD-ALPHA-001", saved.getOrderNumber());
        assertEquals(2, saved.getItems().size());
        assertEquals(patientA.getId(), saved.getPatient().getId());
        assertEquals(doctorA.getId(), saved.getDoctor().getId());
        assertEquals(consultationA.getId(), saved.getConsultation().getId());
        assertEquals(LabOrderPriority.URGENT, saved.getPriority());
    }

    @Test
    @DisplayName("3. Create LabResult and link to LabOrderItem")
    void test3_createLabResultAndLinkToItem() {
        LabTest test = new LabTest();
        test.setHospital(hospitalA);
        test.setTestCode("LAB-GLU");
        test.setTestName("Fasting Plasma Glucose");
        test = labTestRepository.save(test);

        LabOrder order = new LabOrder();
        order.setOrderNumber("ORD-ALPHA-002");
        order.setHospital(hospitalA);
        order.setPatient(patientA);
        order.setDoctor(doctorA);
        order.setOrderedAt(LocalDateTime.now());

        LabOrderItem item = new LabOrderItem();
        item.setLabOrder(order);
        item.setLabTest(test);
        item.setTestNameSnapshot(test.getTestName());
        item.setTestCodeSnapshot(test.getTestCode());

        order.getItems().add(item);
        order = labOrderRepository.save(order);
        LabOrderItem savedItem = order.getItems().get(0);

        LabResult result = new LabResult();
        result.setLabOrderItem(savedItem);
        result.setLabOrder(order);
        result.setPatient(patientA);
        result.setDoctor(doctorA);
        result.setHospital(hospitalA);
        result.setResultValue("110.5");
        result.setResultUnit("mg/dL");
        result.setReferenceRange("70.0 - 99.0");
        result.setAbnormalFlag(true);
        result.setInterpretation("Slightly elevated fasting blood sugar");
        result.setResultStatus(LabResultStatus.COMPLETED);
        result.setResultedAt(LocalDateTime.now());

        LabResult savedResult = labResultRepository.save(result);

        assertNotNull(savedResult.getId());
        assertEquals(savedItem.getId(), savedResult.getLabOrderItem().getId());
        assertEquals("110.5", savedResult.getResultValue());
        assertTrue(savedResult.getAbnormalFlag());
        assertEquals(LabResultStatus.COMPLETED, savedResult.getResultStatus());
    }

    @Test
    @DisplayName("4a. Duplicate test code in same hospital throws exception")
    void test4a_duplicateTestCodeInSameHospitalThrowsException() {
        LabTest t1 = new LabTest();
        t1.setHospital(hospitalA);
        t1.setTestCode("LAB-UNIQUE");
        t1.setTestName("Unique Test A");
        labTestRepository.saveAndFlush(t1);

        LabTest t2 = new LabTest();
        t2.setHospital(hospitalA);
        t2.setTestCode("LAB-UNIQUE");
        t2.setTestName("Duplicate Test Code in Hospital A");
        assertThrows(DataIntegrityViolationException.class, () -> labTestRepository.saveAndFlush(t2));
    }

    @Test
    @DisplayName("4b. Same test code in different hospitals allowed")
    void test4b_sameTestCodeInDifferentHospitalsAllowed() {
        LabTest t1 = new LabTest();
        t1.setHospital(hospitalA);
        t1.setTestCode("LAB-SHARED");
        t1.setTestName("Shared Test Code");
        labTestRepository.saveAndFlush(t1);

        LabTest t2 = new LabTest();
        t2.setHospital(hospitalB);
        t2.setTestCode("LAB-SHARED");
        t2.setTestName("Shared Test Code B");
        LabTest savedB = labTestRepository.saveAndFlush(t2);
        assertNotNull(savedB.getId());
    }

    @Test
    @DisplayName("5a. Duplicate order number in same hospital throws exception")
    void test5a_duplicateOrderNumberInSameHospitalThrowsException() {
        LabOrder o1 = new LabOrder();
        o1.setOrderNumber("ORD-DUP-001");
        o1.setHospital(hospitalA);
        o1.setPatient(patientA);
        o1.setDoctor(doctorA);
        o1.setOrderedAt(LocalDateTime.now());
        labOrderRepository.saveAndFlush(o1);

        LabOrder o2 = new LabOrder();
        o2.setOrderNumber("ORD-DUP-001");
        o2.setHospital(hospitalA);
        o2.setPatient(patientA);
        o2.setDoctor(doctorA);
        o2.setOrderedAt(LocalDateTime.now());
        assertThrows(DataIntegrityViolationException.class, () -> labOrderRepository.saveAndFlush(o2));
    }

    @Test
    @DisplayName("5b. Same order number in different hospitals allowed")
    void test5b_sameOrderNumberInDifferentHospitalsAllowed() {
        LabOrder o1 = new LabOrder();
        o1.setOrderNumber("ORD-MULTI-001");
        o1.setHospital(hospitalA);
        o1.setPatient(patientA);
        o1.setDoctor(doctorA);
        o1.setOrderedAt(LocalDateTime.now());
        labOrderRepository.saveAndFlush(o1);

        LabOrder o2 = new LabOrder();
        o2.setOrderNumber("ORD-MULTI-001");
        o2.setHospital(hospitalB);
        o2.setPatient(patientB);
        o2.setDoctor(doctorB);
        o2.setOrderedAt(LocalDateTime.now());
        LabOrder savedB = labOrderRepository.saveAndFlush(o2);
        assertNotNull(savedB.getId());
    }

    @Test
    @DisplayName("6. Verify historical test name/code snapshot")
    void test6_historicalTestNameAndCodeSnapshot() {
        LabTest test = new LabTest();
        test.setHospital(hospitalA);
        test.setTestCode("LAB-OLD-CODE");
        test.setTestName("Old Test Name");
        test = labTestRepository.save(test);

        LabOrder order = new LabOrder();
        order.setOrderNumber("ORD-SNAP-01");
        order.setHospital(hospitalA);
        order.setPatient(patientA);
        order.setDoctor(doctorA);
        order.setOrderedAt(LocalDateTime.now());

        LabOrderItem item = new LabOrderItem();
        item.setLabOrder(order);
        item.setLabTest(test);
        item.setTestNameSnapshot(test.getTestName());
        item.setTestCodeSnapshot(test.getTestCode());

        order.getItems().add(item);
        order = labOrderRepository.save(order);

        // Update original LabTest catalog entry
        test.setTestName("Renamed Super Test");
        test.setTestCode("LAB-NEW-CODE");
        labTestRepository.save(test);

        // Verify item snapshot remains unchanged
        LabOrderItem retrieved = labOrderItemRepository.findByLabOrderId(order.getId()).get(0);
        assertEquals("Old Test Name", retrieved.getTestNameSnapshot());
        assertEquals("LAB-OLD-CODE", retrieved.getTestCodeSnapshot());
    }

    @Test
    @DisplayName("7. Verify tenant isolation and cross-hospital queries")
    void test7_tenantIsolationAndCrossHospitalQueries() {
        LabTest testA = new LabTest();
        testA.setHospital(hospitalA); testA.setTestCode("T-A"); testA.setTestName("Test A");
        testA = labTestRepository.save(testA);

        LabOrder orderA = new LabOrder();
        orderA.setOrderNumber("ORD-A"); orderA.setHospital(hospitalA);
        orderA.setPatient(patientA); orderA.setDoctor(doctorA);
        orderA.setOrderedAt(LocalDateTime.now());
        orderA = labOrderRepository.save(orderA);

        LabTest testB = new LabTest();
        testB.setHospital(hospitalB); testB.setTestCode("T-B"); testB.setTestName("Test B");
        testB = labTestRepository.save(testB);

        LabOrder orderB = new LabOrder();
        orderB.setOrderNumber("ORD-B"); orderB.setHospital(hospitalB);
        orderB.setPatient(patientB); orderB.setDoctor(doctorB);
        orderB.setOrderedAt(LocalDateTime.now());
        orderB = labOrderRepository.save(orderB);

        // Hospital A queries cannot find Hospital B data
        Optional<LabOrder> crossOrder = labOrderRepository.findByIdAndHospitalId(orderB.getId(), hospitalA.getId());
        assertTrue(crossOrder.isEmpty());

        Optional<LabTest> crossTest = labTestRepository.findByIdAndHospitalId(testB.getId(), hospitalA.getId());
        assertTrue(crossTest.isEmpty());

        Page<LabOrder> pageA = labOrderRepository.findByHospitalId(hospitalA.getId(), PageRequest.of(0, 10));
        assertEquals(1, pageA.getTotalElements());
        assertEquals(orderA.getId(), pageA.getContent().get(0).getId());
    }

    @Test
    @DisplayName("8. Verify enum persistence")
    void test8_enumPersistence() {
        LabOrder order = new LabOrder();
        order.setOrderNumber("ORD-ENUM-01");
        order.setHospital(hospitalA);
        order.setPatient(patientA);
        order.setDoctor(doctorA);
        order.setStatus(LabOrderStatus.PROCESSING);
        order.setPriority(LabOrderPriority.STAT);
        order.setOrderedAt(LocalDateTime.now());
        order = labOrderRepository.save(order);

        LabOrder fetched = labOrderRepository.findById(order.getId()).orElseThrow();
        assertEquals(LabOrderStatus.PROCESSING, fetched.getStatus());
        assertEquals(LabOrderPriority.STAT, fetched.getPriority());
    }

    @Test
    @DisplayName("9. Verify cascade behavior and shared entity protection")
    void test9_cascadeBehaviorAndSharedEntityProtection() {
        LabTest test = new LabTest();
        test.setHospital(hospitalA); test.setTestCode("T-DEL"); test.setTestName("Test Del");
        test = labTestRepository.save(test);

        LabOrder order = new LabOrder();
        order.setOrderNumber("ORD-DEL-01"); order.setHospital(hospitalA);
        order.setPatient(patientA); order.setDoctor(doctorA);
        order.setOrderedAt(LocalDateTime.now());

        LabOrderItem item = new LabOrderItem();
        item.setLabOrder(order); item.setLabTest(test);
        item.setTestNameSnapshot(test.getTestName()); item.setTestCodeSnapshot(test.getTestCode());
        order.getItems().add(item);

        order = labOrderRepository.save(order);
        Long orderId = order.getId();
        Long itemId = order.getItems().get(0).getId();

        // Delete order
        labOrderRepository.delete(order);
        labOrderRepository.flush();

        // Order item is deleted
        assertTrue(labOrderItemRepository.findById(itemId).isEmpty());

        // Shared entities remain intact
        assertTrue(patientRepository.findById(patientA.getId()).isPresent());
        assertTrue(doctorRepository.findById(doctorA.getId()).isPresent());
        assertTrue(hospitalRepository.findById(hospitalA.getId()).isPresent());
        assertTrue(labTestRepository.findById(test.getId()).isPresent());
    }

    @Test
    @DisplayName("10. Verify repository queries and counts")
    void test10_repositoryQueriesAndCounts() {
        LabOrder order = new LabOrder();
        order.setOrderNumber("ORD-CNT-01");
        order.setHospital(hospitalA);
        order.setPatient(patientA);
        order.setDoctor(doctorA);
        order.setStatus(LabOrderStatus.ORDERED);
        order.setOrderedAt(LocalDateTime.now());
        labOrderRepository.save(order);

        assertEquals(1, labOrderRepository.countByHospitalId(hospitalA.getId()));
        assertEquals(1, labOrderRepository.countByHospitalIdAndStatus(hospitalA.getId(), LabOrderStatus.ORDERED));
        assertEquals(0, labOrderRepository.countByHospitalIdAndStatus(hospitalA.getId(), LabOrderStatus.COMPLETED));
    }
}
