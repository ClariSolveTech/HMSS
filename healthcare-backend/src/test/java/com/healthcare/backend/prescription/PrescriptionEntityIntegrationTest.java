package com.healthcare.backend.prescription;

import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
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
import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.entity.PrescriptionItem;
import com.healthcare.backend.prescription.entity.PrescriptionStatus;
import com.healthcare.backend.prescription.repository.PrescriptionItemRepository;
import com.healthcare.backend.prescription.repository.PrescriptionRepository;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PrescriptionEntityIntegrationTest {

    private static final AtomicLong CODE_COUNTER = new AtomicLong(99000);

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
    private PasswordEncoder passwordEncoder;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private Doctor doctorA;
    private Patient patientA;
    private Consultation consultationA1;
    private Consultation consultationA2;

    @Autowired
    private com.healthcare.backend.billing.repository.PaymentRepository paymentRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private com.healthcare.backend.billing.repository.InvoiceRepository invoiceRepository;

    @BeforeEach
    public void setup() {
        prescriptionItemRepository.deleteAll();
        prescriptionRepository.deleteAll();
        paymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        consultationRepository.deleteAll();

        long ts = System.currentTimeMillis();

        hospitalA = new Hospital();
        hospitalA.setName("City General Hospital");
        hospitalA.setCode("HOSP_RX_A_" + ts);
        hospitalA.setEmail("admin.rx.a." + ts + "@citygeneral.com");
        hospitalA.setPhone("9991112221");
        hospitalA.setAddress("Main Street");
        hospitalA.setCity("Metropolis");
        hospitalA.setState("State");
        hospitalA.setPincode("100001");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        hospitalB = new Hospital();
        hospitalB.setName("MetroCare Hospital");
        hospitalB.setCode("HOSP_RX_B_" + ts);
        hospitalB.setEmail("admin.rx.b." + ts + "@metrocare.com");
        hospitalB.setPhone("9991112222");
        hospitalB.setAddress("North Avenue");
        hospitalB.setCity("Metropolis");
        hospitalB.setState("State");
        hospitalB.setPincode("100002");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        User docUserA = createUserHelper("Doctor", "Rohan", "doc.rx.rohan." + ts + "@test.com", "92" + (ts % 100000000L), hospitalA, RoleName.DOCTOR);
        doctorA = createDoctorHelper(docUserA, "Cardiology", "LIC_RX_A_" + ts);

        patientA = createPatientHelper(hospitalA, "Suresh", "Raina", "suresh.rx." + ts + "@test.com", "97" + (ts % 100000000L));

        consultationA1 = createConsultationHelper(hospitalA, doctorA, patientA, "Hypertension review");
        consultationA2 = createConsultationHelper(hospitalA, doctorA, patientA, "Chest tightness review");
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
        d.setConsultationFee(new BigDecimal("600.00"));
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
        p.setDateOfBirth(LocalDate.of(1988, 6, 15));
        p.setGender(Gender.MALE);
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
    @DisplayName("1. Verify Prescription and PrescriptionItem persistence with relationships")
    public void test1_prescriptionAndItemPersistence() {
        Prescription rx = new Prescription();
        rx.setPrescriptionCode("RX-1001");
        rx.setHospital(hospitalA);
        rx.setDoctor(doctorA);
        rx.setPatient(patientA);
        rx.setConsultation(consultationA1);
        rx.setStatus(PrescriptionStatus.DRAFT);
        rx.setNotes("Take medications with water after food");

        PrescriptionItem item1 = new PrescriptionItem();
        item1.setMedicineName("Amoxicillin 500mg");
        item1.setDosage("1 capsule");
        item1.setFrequency("TDS");
        item1.setDuration("5 days");
        item1.setRoute("Oral");
        item1.setInstructions("After meals");

        PrescriptionItem item2 = new PrescriptionItem();
        item2.setMedicineName("Paracetamol 650mg");
        item2.setDosage("1 tablet");
        item2.setFrequency("BD");
        item2.setDuration("3 days");
        item2.setRoute("Oral");
        item2.setInstructions("As needed for fever");

        rx.addItem(item1);
        rx.addItem(item2);

        Prescription savedRx = prescriptionRepository.save(rx);
        assertNotNull(savedRx.getId());
        assertNotNull(savedRx.getCreatedAt());
        assertNotNull(savedRx.getUpdatedAt());
        assertEquals(2, savedRx.getItems().size());

        Prescription fetchedRx = prescriptionRepository.findById(savedRx.getId()).orElseThrow();
        assertEquals("RX-1001", fetchedRx.getPrescriptionCode());
        assertEquals(hospitalA.getId(), fetchedRx.getHospital().getId());
        assertEquals(doctorA.getId(), fetchedRx.getDoctor().getId());
        assertEquals(patientA.getId(), fetchedRx.getPatient().getId());
        assertEquals(consultationA1.getId(), fetchedRx.getConsultation().getId());
        assertEquals(PrescriptionStatus.DRAFT, fetchedRx.getStatus());

        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(savedRx.getId());
        assertEquals(2, items.size());
    }

    @Test
    @DisplayName("2. Verify prescription_code + hospital_id uniqueness constraint")
    public void test2_prescriptionCodeHospitalUniqueness() {
        Prescription rx1 = new Prescription();
        rx1.setPrescriptionCode("RX-DUP-01");
        rx1.setHospital(hospitalA);
        rx1.setDoctor(doctorA);
        rx1.setPatient(patientA);
        rx1.setConsultation(consultationA1);
        rx1.setStatus(PrescriptionStatus.DRAFT);
        prescriptionRepository.saveAndFlush(rx1);

        Prescription rx2 = new Prescription();
        rx2.setPrescriptionCode("RX-DUP-01"); // Duplicate code in Hospital A
        rx2.setHospital(hospitalA);
        rx2.setDoctor(doctorA);
        rx2.setPatient(patientA);
        rx2.setConsultation(consultationA2);
        rx2.setStatus(PrescriptionStatus.DRAFT);

        assertThrows(DataIntegrityViolationException.class, () -> {
            prescriptionRepository.saveAndFlush(rx2);
        });
    }

    @Test
    @DisplayName("3. Verify consultation_id uniqueness constraint")
    public void test3_consultationUniqueness() {
        Prescription rx1 = new Prescription();
        rx1.setPrescriptionCode("RX-CONS-01");
        rx1.setHospital(hospitalA);
        rx1.setDoctor(doctorA);
        rx1.setPatient(patientA);
        rx1.setConsultation(consultationA1);
        rx1.setStatus(PrescriptionStatus.DRAFT);
        prescriptionRepository.saveAndFlush(rx1);

        Prescription rx2 = new Prescription();
        rx2.setPrescriptionCode("RX-CONS-02");
        rx2.setHospital(hospitalA);
        rx2.setDoctor(doctorA);
        rx2.setPatient(patientA);
        rx2.setConsultation(consultationA1); // Duplicate consultation reference
        rx2.setStatus(PrescriptionStatus.DRAFT);

        assertThrows(DataIntegrityViolationException.class, () -> {
            prescriptionRepository.saveAndFlush(rx2);
        });
    }

    @Test
    @DisplayName("4. Verify repository multi-tenant helper queries")
    public void test4_repositoryMultiTenantQueries() {
        Prescription rx1 = new Prescription();
        rx1.setPrescriptionCode("RX-QUERY-01");
        rx1.setHospital(hospitalA);
        rx1.setDoctor(doctorA);
        rx1.setPatient(patientA);
        rx1.setConsultation(consultationA1);
        rx1.setStatus(PrescriptionStatus.DRAFT);
        prescriptionRepository.save(rx1);

        assertTrue(prescriptionRepository.findByIdAndHospitalId(rx1.getId(), hospitalA.getId()).isPresent());
        assertFalse(prescriptionRepository.findByIdAndHospitalId(rx1.getId(), hospitalB.getId()).isPresent());

        assertTrue(prescriptionRepository.findByConsultationIdAndHospitalId(consultationA1.getId(), hospitalA.getId()).isPresent());
        assertTrue(prescriptionRepository.existsByConsultationIdAndHospitalId(consultationA1.getId(), hospitalA.getId()));
        assertEquals(1, prescriptionRepository.countByHospitalId(hospitalA.getId()));
    }
}
