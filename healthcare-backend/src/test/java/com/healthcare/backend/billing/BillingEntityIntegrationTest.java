package com.healthcare.backend.billing;

import com.healthcare.backend.HealthcareBackendApplication;
import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = HealthcareBackendApplication.class)
@ActiveProfiles("test")
@Transactional
public class BillingEntityIntegrationTest {

    private static final AtomicLong CODE_COUNTER = new AtomicLong(98000);

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
    private PasswordEncoder passwordEncoder;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private Doctor doctorA;
    private Doctor doctorB;
    private Patient patientA;
    private Patient patientB;
    private Appointment appointmentA;
    private Consultation consultationA;

    @BeforeEach
    public void setUp() {
        long ts = System.currentTimeMillis() % 10000000L;

        // Create Hospital A
        hospitalA = new Hospital();
        hospitalA.setName("Billing Test Hospital A");
        hospitalA.setCode("HOSP_BILL_A_" + ts);
        hospitalA.setEmail("admin.bill.a." + ts + "@hospital.com");
        hospitalA.setPhone("9876543210");
        hospitalA.setAddress("123 Health Ave");
        hospitalA.setCity("MedCity");
        hospitalA.setState("State");
        hospitalA.setPincode("560001");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        // Create Hospital B
        hospitalB = new Hospital();
        hospitalB.setName("Billing Test Hospital B");
        hospitalB.setCode("HOSP_BILL_B_" + ts);
        hospitalB.setEmail("admin.bill.b." + ts + "@hospital.com");
        hospitalB.setPhone("9876543211");
        hospitalB.setAddress("456 Care St");
        hospitalB.setCity("MetroCity");
        hospitalB.setState("State");
        hospitalB.setPincode("560002");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        // Doctor Users
        User docUserA = createUserHelper("Dr. John", "Doe", "doc.bill.a." + ts + "@test.com", "91" + (ts % 100000000L), hospitalA, RoleName.DOCTOR);
        User docUserB = createUserHelper("Dr. Jane", "Smith", "doc.bill.b." + ts + "@test.com", "92" + (ts % 100000000L), hospitalB, RoleName.DOCTOR);

        doctorA = createDoctorHelper(docUserA, "Cardiology", "LIC_BILL_A_" + ts);
        doctorB = createDoctorHelper(docUserB, "Neurology", "LIC_BILL_B_" + ts);

        // Patients
        patientA = createPatientHelper(hospitalA, "Alice", "Brown", "alice.bill." + ts + "@test.com", "93" + (ts % 100000000L));
        patientB = createPatientHelper(hospitalB, "Bob", "Green", "bob.bill." + ts + "@test.com", "94" + (ts % 100000000L));

        // Appointment for Hospital A
        appointmentA = new Appointment();
        appointmentA.setHospital(hospitalA);
        appointmentA.setPatient(patientA);
        appointmentA.setDoctor(doctorA);
        appointmentA.setAppointmentDate(LocalDate.now());
        appointmentA.setStartTime(LocalTime.of(10, 0));
        appointmentA.setEndTime(LocalTime.of(10, 30));
        appointmentA.setAppointmentNumber("APT-BILL-" + ts);
        appointmentA = appointmentRepository.save(appointmentA);

        // Consultation for Hospital A
        consultationA = new Consultation();
        consultationA.setHospital(hospitalA);
        consultationA.setDoctor(doctorA);
        consultationA.setPatient(patientA);
        consultationA.setAppointment(appointmentA);
        consultationA.setConsultationDate(LocalDateTime.now());
        consultationA.setStatus(ConsultationStatus.COMPLETED);
        consultationA = consultationRepository.save(consultationA);
    }

    private User createUserHelper(String f, String l, String e, String m, Hospital h, RoleName r) {
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
        p.setDateOfBirth(LocalDate.of(1995, 5, 15));
        p.setGender(Gender.FEMALE);
        p.setStatus(PatientStatus.ACTIVE);
        return patientRepository.save(p);
    }

    @Test
    @DisplayName("1. Invoice can be persisted")
    public void test1_invoiceCanBePersisted() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-001");
        invoice.setHospital(hospitalA);
        invoice.setPatient(patientA);
        invoice.setDoctor(doctorA);
        invoice.setSubtotal(new BigDecimal("1000.00"));
        invoice.setDiscount(new BigDecimal("100.00"));
        invoice.setTax(new BigDecimal("50.00"));
        invoice.setTotalAmount(new BigDecimal("950.00"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueAmount(new BigDecimal("950.00"));
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssuedAt(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);
        assertNotNull(saved.getId());
        assertEquals("INV-001", saved.getInvoiceNumber());
        assertEquals(InvoiceStatus.DRAFT, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("2. InvoiceItem can be persisted")
    public void test2_invoiceItemCanBePersisted() {
        Invoice invoice = createSampleInvoice("INV-002");

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setItemType(InvoiceItemType.CONSULTATION);
        item.setDescription("General Consultation");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("500.00"));
        item.setDiscount(BigDecimal.ZERO);
        item.setTax(BigDecimal.ZERO);
        item.setLineTotal(new BigDecimal("500.00"));

        InvoiceItem savedItem = invoiceItemRepository.save(item);
        assertNotNull(savedItem.getId());
        assertEquals("General Consultation", savedItem.getDescription());
        assertEquals(InvoiceItemType.CONSULTATION, savedItem.getItemType());
    }

    @Test
    @DisplayName("3. Payment can be persisted")
    public void test3_paymentCanBePersisted() {
        Invoice invoice = createSampleInvoice("INV-003");

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setHospital(hospitalA);
        payment.setPaymentReference("PAY-001");
        payment.setAmount(new BigDecimal("500.00"));
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        assertNotNull(saved.getId());
        assertEquals("PAY-001", saved.getPaymentReference());
        assertEquals(PaymentMethod.CASH, saved.getPaymentMethod());
        assertEquals(PaymentStatus.SUCCESS, saved.getStatus());
    }

    @Test
    @DisplayName("4. Invoice references correct hospital")
    public void test4_invoiceReferencesCorrectHospital() {
        Invoice invoice = createSampleInvoice("INV-004");
        assertEquals(hospitalA.getId(), invoice.getHospital().getId());
    }

    @Test
    @DisplayName("5. Invoice references correct patient")
    public void test5_invoiceReferencesCorrectPatient() {
        Invoice invoice = createSampleInvoice("INV-005");
        assertEquals(patientA.getId(), invoice.getPatient().getId());
    }

    @Test
    @DisplayName("6. Invoice references correct doctor")
    public void test6_invoiceReferencesCorrectDoctor() {
        Invoice invoice = createSampleInvoice("INV-006");
        assertEquals(doctorA.getId(), invoice.getDoctor().getId());
    }

    @Test
    @DisplayName("7. Invoice references correct appointment")
    public void test7_invoiceReferencesCorrectAppointment() {
        Invoice invoice = createSampleInvoice("INV-007");
        invoice.setAppointment(appointmentA);
        Invoice saved = invoiceRepository.save(invoice);

        assertNotNull(saved.getAppointment());
        assertEquals(appointmentA.getId(), saved.getAppointment().getId());
    }

    @Test
    @DisplayName("8. Invoice references correct consultation")
    public void test8_invoiceReferencesCorrectConsultation() {
        Invoice invoice = createSampleInvoice("INV-008");
        invoice.setConsultation(consultationA);
        Invoice saved = invoiceRepository.save(invoice);

        assertNotNull(saved.getConsultation());
        assertEquals(consultationA.getId(), saved.getConsultation().getId());
    }

    @Test
    @DisplayName("9. Invoice can contain multiple InvoiceItems")
    public void test9_invoiceCanContainMultipleItems() {
        Invoice invoice = createSampleInvoice("INV-009");

        InvoiceItem item1 = new InvoiceItem();
        item1.setItemType(InvoiceItemType.CONSULTATION);
        item1.setDescription("Doctor Consultation Fee");
        item1.setQuantity(1);
        item1.setUnitPrice(new BigDecimal("500.00"));
        item1.setLineTotal(new BigDecimal("500.00"));

        InvoiceItem item2 = new InvoiceItem();
        item2.setItemType(InvoiceItemType.MEDICINE);
        item2.setDescription("Paracetamol 500mg");
        item2.setQuantity(2);
        item2.setUnitPrice(new BigDecimal("50.00"));
        item2.setLineTotal(new BigDecimal("100.00"));

        invoice.addItem(item1);
        invoice.addItem(item2);

        Invoice saved = invoiceRepository.save(invoice);
        assertEquals(2, saved.getItems().size());
    }

    @Test
    @DisplayName("10. Invoice can contain multiple Payments")
    public void test10_invoiceCanContainMultiplePayments() {
        Invoice invoice = createSampleInvoice("INV-010");

        Payment payment1 = new Payment();
        payment1.setInvoice(invoice);
        payment1.setHospital(hospitalA);
        payment1.setPaymentReference("PAY-010-A");
        payment1.setAmount(new BigDecimal("300.00"));
        payment1.setPaymentMethod(PaymentMethod.CASH);
        payment1.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment1);

        Payment payment2 = new Payment();
        payment2.setInvoice(invoice);
        payment2.setHospital(hospitalA);
        payment2.setPaymentReference("PAY-010-B");
        payment2.setAmount(new BigDecimal("200.00"));
        payment2.setPaymentMethod(PaymentMethod.UPI);
        payment2.setTransactionReference("UPI_TXN_123456");
        payment2.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment2);

        List<Payment> payments = paymentRepository.findByInvoiceIdAndHospitalId(invoice.getId(), hospitalA.getId());
        assertEquals(2, payments.size());
    }

    @Test
    @DisplayName("11. Invoice number uniqueness within hospital works")
    public void test11_invoiceNumberUniquenessWithinHospital() {
        createSampleInvoice("INV-UNIQUE-01");

        // Same invoice number in same hospital should fail
        Invoice duplicate = new Invoice();
        duplicate.setInvoiceNumber("INV-UNIQUE-01");
        duplicate.setHospital(hospitalA);
        duplicate.setPatient(patientA);
        duplicate.setDoctor(doctorA);
        duplicate.setSubtotal(new BigDecimal("100.00"));
        duplicate.setTotalAmount(new BigDecimal("100.00"));
        duplicate.setDueAmount(new BigDecimal("100.00"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            invoiceRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @DisplayName("11b. Same invoice number in different hospitals is allowed")
    public void test11b_sameInvoiceNumberDifferentHospitalsAllowed() {
        createSampleInvoice("INV-SHARED-01"); // In hospitalA

        Invoice invoiceB = new Invoice();
        invoiceB.setInvoiceNumber("INV-SHARED-01");
        invoiceB.setHospital(hospitalB);
        invoiceB.setPatient(patientB);
        invoiceB.setDoctor(doctorB);
        invoiceB.setSubtotal(new BigDecimal("100.00"));
        invoiceB.setTotalAmount(new BigDecimal("100.00"));
        invoiceB.setDueAmount(new BigDecimal("100.00"));

        Invoice saved = invoiceRepository.saveAndFlush(invoiceB);
        assertNotNull(saved.getId());
    }

    @Test
    @DisplayName("12. Payment reference uniqueness within hospital works")
    public void test12_paymentReferenceUniquenessWithinHospital() {
        Invoice invoice = createSampleInvoice("INV-012");

        Payment p1 = new Payment();
        p1.setInvoice(invoice);
        p1.setHospital(hospitalA);
        p1.setPaymentReference("PAY-REF-UNIQUE");
        p1.setAmount(new BigDecimal("100.00"));
        p1.setPaymentMethod(PaymentMethod.CASH);
        p1.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.saveAndFlush(p1);

        Payment p2 = new Payment();
        p2.setInvoice(invoice);
        p2.setHospital(hospitalA);
        p2.setPaymentReference("PAY-REF-UNIQUE");
        p2.setAmount(new BigDecimal("100.00"));
        p2.setPaymentMethod(PaymentMethod.CARD);
        p2.setStatus(PaymentStatus.SUCCESS);

        assertThrows(DataIntegrityViolationException.class, () -> {
            paymentRepository.saveAndFlush(p2);
        });
    }

    @Test
    @DisplayName("13. Cross-hospital invoice lookup returns empty")
    public void test13_crossHospitalInvoiceLookupReturnsEmpty() {
        Invoice invoiceA = createSampleInvoice("INV-CROSS-A");

        Optional<Invoice> lookupFromB = invoiceRepository.findByIdAndHospitalId(invoiceA.getId(), hospitalB.getId());
        assertTrue(lookupFromB.isEmpty());

        Optional<Invoice> lookupNumberFromB = invoiceRepository.findByInvoiceNumberAndHospitalId("INV-CROSS-A", hospitalB.getId());
        assertTrue(lookupNumberFromB.isEmpty());
    }

    @Test
    @DisplayName("14. Cross-hospital payment lookup returns empty")
    public void test14_crossHospitalPaymentLookupReturnsEmpty() {
        Invoice invoiceA = createSampleInvoice("INV-CROSS-PAY");

        Payment p = new Payment();
        p.setInvoice(invoiceA);
        p.setHospital(hospitalA);
        p.setPaymentReference("PAY-CROSS-01");
        p.setAmount(new BigDecimal("200.00"));
        p.setPaymentMethod(PaymentMethod.UPI);
        p.setStatus(PaymentStatus.SUCCESS);
        Payment saved = paymentRepository.save(p);

        Optional<Payment> lookupFromB = paymentRepository.findByIdAndHospitalId(saved.getId(), hospitalB.getId());
        assertTrue(lookupFromB.isEmpty());

        Optional<Payment> lookupRefFromB = paymentRepository.findByPaymentReferenceAndHospitalId("PAY-CROSS-01", hospitalB.getId());
        assertTrue(lookupRefFromB.isEmpty());
    }

    @Test
    @DisplayName("15. Monetary values persist correctly as BigDecimal")
    public void test15_monetaryValuesPersistCorrectly() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-MONEY-01");
        invoice.setHospital(hospitalA);
        invoice.setPatient(patientA);
        invoice.setDoctor(doctorA);
        invoice.setSubtotal(new BigDecimal("1234.56"));
        invoice.setDiscount(new BigDecimal("100.25"));
        invoice.setTax(new BigDecimal("204.17"));
        invoice.setTotalAmount(new BigDecimal("1338.48"));
        invoice.setPaidAmount(new BigDecimal("1000.00"));
        invoice.setDueAmount(new BigDecimal("338.48"));

        Invoice saved = invoiceRepository.save(invoice);
        assertEquals(0, new BigDecimal("1234.56").compareTo(saved.getSubtotal()));
        assertEquals(0, new BigDecimal("100.25").compareTo(saved.getDiscount()));
        assertEquals(0, new BigDecimal("204.17").compareTo(saved.getTax()));
        assertEquals(0, new BigDecimal("1338.48").compareTo(saved.getTotalAmount()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(saved.getPaidAmount()));
        assertEquals(0, new BigDecimal("338.48").compareTo(saved.getDueAmount()));
    }

    @Test
    @DisplayName("16. Invoice item line values persist correctly")
    public void test16_invoiceItemLineValuesPersistCorrectly() {
        Invoice invoice = createSampleInvoice("INV-ITEM-VAL");

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setItemType(InvoiceItemType.LAB);
        item.setDescription("Blood Test - CBC");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("250.00"));
        item.setDiscount(new BigDecimal("50.00"));
        item.setTax(new BigDecimal("45.00"));
        item.setLineTotal(new BigDecimal("445.00"));

        InvoiceItem saved = invoiceItemRepository.save(item);
        assertEquals(0, new BigDecimal("250.00").compareTo(saved.getUnitPrice()));
        assertEquals(0, new BigDecimal("50.00").compareTo(saved.getDiscount()));
        assertEquals(0, new BigDecimal("45.00").compareTo(saved.getTax()));
        assertEquals(0, new BigDecimal("445.00").compareTo(saved.getLineTotal()));
    }

    @Test
    @DisplayName("17. Payment transaction reference can be nullable")
    public void test17_paymentTransactionReferenceCanBeNullable() {
        Invoice invoice = createSampleInvoice("INV-NULL-TXN");

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setHospital(hospitalA);
        payment.setPaymentReference("PAY-CASH-NO-TXN");
        payment.setAmount(new BigDecimal("500.00"));
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionReference(null); // Explicitly null for CASH

        Payment saved = paymentRepository.save(payment);
        assertNull(saved.getTransactionReference());
    }

    @Test
    @DisplayName("18. Appointment and Consultation are optional (nullable)")
    public void test18_appointmentAndConsultationAreOptional() {
        Invoice walkInInvoice = new Invoice();
        walkInInvoice.setInvoiceNumber("INV-WALKIN-01");
        walkInInvoice.setHospital(hospitalA);
        walkInInvoice.setPatient(patientA);
        walkInInvoice.setDoctor(doctorA);
        walkInInvoice.setAppointment(null); // Nullable for walk-in
        walkInInvoice.setConsultation(null); // Nullable for direct billing
        walkInInvoice.setSubtotal(new BigDecimal("500.00"));
        walkInInvoice.setTotalAmount(new BigDecimal("500.00"));
        walkInInvoice.setDueAmount(new BigDecimal("500.00"));

        Invoice saved = invoiceRepository.save(walkInInvoice);
        assertNotNull(saved.getId());
        assertNull(saved.getAppointment());
        assertNull(saved.getConsultation());
    }

    private Invoice createSampleInvoice(String number) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(number);
        invoice.setHospital(hospitalA);
        invoice.setPatient(patientA);
        invoice.setDoctor(doctorA);
        invoice.setSubtotal(new BigDecimal("500.00"));
        invoice.setTotalAmount(new BigDecimal("500.00"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueAmount(new BigDecimal("500.00"));
        invoice.setStatus(InvoiceStatus.DRAFT);
        return invoiceRepository.save(invoice);
    }
}
