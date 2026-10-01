package com.healthcare.backend.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.HealthcareBackendApplication;
import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.billing.entity.Invoice;
import com.healthcare.backend.billing.entity.InvoiceStatus;
import com.healthcare.backend.billing.repository.InvoiceRepository;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.entity.ConsultationStatus;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.lab.entity.LabOrder;
import com.healthcare.backend.lab.entity.LabOrderPriority;
import com.healthcare.backend.lab.entity.LabOrderStatus;
import com.healthcare.backend.lab.repository.LabOrderRepository;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.entity.PrescriptionStatus;
import com.healthcare.backend.prescription.repository.PrescriptionRepository;
import com.healthcare.backend.queue.entity.QueueEntry;
import com.healthcare.backend.queue.entity.QueuePriority;
import com.healthcare.backend.queue.entity.QueueStatus;
import com.healthcare.backend.queue.repository.QueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HealthcareBackendApplication.class)
@Transactional
public class AnalyticsIntegrationTest {

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
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    private Hospital hospital1;
    private Hospital hospital2;

    private Doctor doctor1;
    private Doctor doctor2;

    private Patient patient1;
    private Patient patient2;

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

        // 1. Setup Roles
        Role docRole = roleRepository.findByName(RoleName.DOCTOR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.DOCTOR)));
        Role recepRole = roleRepository.findByName(RoleName.RECEPTIONIST)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.RECEPTIONIST)));

        // 2. Setup Hospitals
        hospital1 = new Hospital();
        hospital1.setName("Analytics Hospital 1 " + suffix);
        hospital1.setCode("AH1" + (suffix % 10000));
        hospital1.setPhone("9876543210");
        hospital1.setEmail("hosp1_" + suffix + "@analytics.com");
        hospital1.setStatus(HospitalStatus.ACTIVE);
        hospital1 = hospitalRepository.save(hospital1);

        hospital2 = new Hospital();
        hospital2.setName("Analytics Hospital 2 " + suffix);
        hospital2.setCode("AH2" + (suffix % 10000));
        hospital2.setPhone("9876543211");
        hospital2.setEmail("hosp2_" + suffix + "@analytics.com");
        hospital2.setStatus(HospitalStatus.ACTIVE);
        hospital2 = hospitalRepository.save(hospital2);

        // 3. Setup Users & Doctors
        User user1 = new User();
        user1.setEmail("doc1_" + suffix + "@analytics.com");
        user1.setPasswordHash(passwordEncoder.encode("Password@123"));
        user1.setFirstName("Aarav");
        user1.setLastName("Sharma");
        user1.setMobileNumber("990011" + (suffix % 10000));
        user1.setHospital(hospital1);
        user1.setStatus(UserStatus.ACTIVE);
        user1.setRoles(Set.of(docRole));
        user1 = userRepository.save(user1);

        doctor1 = new Doctor();
        doctor1.setHospital(hospital1);
        doctor1.setUser(user1);
        doctor1.setDoctorCode("DOC1_" + suffix);
        doctor1.setLicenseNumber("LIC1_" + suffix);
        doctor1.setSpecialization("General Cardiology");
        doctor1.setQualification("MD");
        doctor1.setConsultationFee(new BigDecimal("500.00"));
        doctor1.setStatus(DoctorStatus.ACTIVE);
        doctor1 = doctorRepository.save(doctor1);

        doctor1Token = jwtService.generateToken(user1);

        User user2 = new User();
        user2.setEmail("doc2_" + suffix + "@analytics.com");
        user2.setPasswordHash(passwordEncoder.encode("Password@123"));
        user2.setFirstName("Sana");
        user2.setLastName("Iqbal");
        user2.setMobileNumber("990012" + (suffix % 10000));
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
        recepUser.setEmail("recep_" + suffix + "@analytics.com");
        recepUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        recepUser.setFirstName("Recep");
        recepUser.setLastName("User");
        recepUser.setMobileNumber("990013" + (suffix % 10000));
        recepUser.setHospital(hospital1);
        recepUser.setStatus(UserStatus.ACTIVE);
        recepUser.setRoles(Set.of(recepRole));
        recepUser = userRepository.save(recepUser);

        receptionistToken = jwtService.generateToken(recepUser);

        // 4. Setup Patients
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

        // 5. Seed Test Domain Data for Doctor 1 (Hospital 1)
        LocalDate today = LocalDate.now();

        // Appointments
        Appointment appt1 = new Appointment();
        appt1.setHospital(hospital1);
        appt1.setDoctor(doctor1);
        appt1.setPatient(patient1);
        appt1.setAppointmentDate(today);
        appt1.setStartTime(LocalTime.of(10, 0));
        appt1.setEndTime(LocalTime.of(10, 30));
        appt1.setAppointmentNumber("APP1_" + suffix);
        appt1.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        appt1.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appt1);

        Appointment appt2 = new Appointment();
        appt2.setHospital(hospital1);
        appt2.setDoctor(doctor1);
        appt2.setPatient(patient1);
        appt2.setAppointmentDate(today);
        appt2.setStartTime(LocalTime.of(11, 0));
        appt2.setEndTime(LocalTime.of(11, 30));
        appt2.setAppointmentNumber("APP2_" + suffix);
        appt2.setAppointmentType(AppointmentType.FOLLOW_UP);
        appt2.setStatus(AppointmentStatus.NO_SHOW);
        appointmentRepository.save(appt2);

        Appointment appt3 = new Appointment();
        appt3.setHospital(hospital1);
        appt3.setDoctor(doctor1);
        appt3.setPatient(patient1);
        appt3.setAppointmentDate(today);
        appt3.setStartTime(LocalTime.of(12, 0));
        appt3.setEndTime(LocalTime.of(12, 30));
        appt3.setAppointmentNumber("APP3_" + suffix);
        appt3.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        appt3.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt3);

        // Queue
        QueueEntry queue = new QueueEntry();
        queue.setHospital(hospital1);
        queue.setDoctor(doctor1);
        queue.setPatient(patient1);
        queue.setQueueDate(today);
        queue.setTokenNumber(1);
        queue.setPriority(QueuePriority.NORMAL);
        queue.setStatus(QueueStatus.WAITING);
        queue.setCheckedInAt(LocalDateTime.now());
        queueEntryRepository.save(queue);

        // Consultation
        Consultation consult1 = new Consultation();
        consult1.setHospital(hospital1);
        consult1.setDoctor(doctor1);
        consult1.setPatient(patient1);
        consult1.setConsultationDate(LocalDateTime.now());
        consult1.setChiefComplaint("Fever and cough");
        consult1.setStatus(ConsultationStatus.COMPLETED);
        consult1 = consultationRepository.save(consult1);

        // Prescription
        Prescription rx = new Prescription();
        rx.setHospital(hospital1);
        rx.setDoctor(doctor1);
        rx.setPatient(patient1);
        rx.setConsultation(consult1);
        rx.setPrescriptionCode("RX1_" + suffix);
        rx.setStatus(PrescriptionStatus.ISSUED);
        prescriptionRepository.save(rx);

        // Lab Order
        LabOrder labOrder = new LabOrder();
        labOrder.setHospital(hospital1);
        labOrder.setDoctor(doctor1);
        labOrder.setPatient(patient1);
        labOrder.setConsultation(consult1);
        labOrder.setOrderNumber("LAB1_" + suffix);
        labOrder.setPriority(LabOrderPriority.NORMAL);
        labOrder.setStatus(LabOrderStatus.COMPLETED);
        labOrder.setOrderedAt(LocalDateTime.now());
        labOrderRepository.save(labOrder);

        // Invoice
        Invoice invoice = new Invoice();
        invoice.setHospital(hospital1);
        invoice.setDoctor(doctor1);
        invoice.setPatient(patient1);
        invoice.setConsultation(consult1);
        invoice.setInvoiceNumber("INV1_" + suffix);
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setPaidAmount(new BigDecimal("1000.00"));
        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);
    }

    @Test
    @DisplayName("1. Authenticated doctor can retrieve summary")
    void test1_getDoctorDashboardSummary_Success() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/me/analytics/summary")
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayAppointmentsCount", is(3)))
                .andExpect(jsonPath("$.completedConsultationsCount", is(1)))
                .andExpect(jsonPath("$.noShowAppointmentsCount", is(1)))
                .andExpect(jsonPath("$.cancelledAppointmentsCount", is(1)))
                .andExpect(jsonPath("$.waitingQueueCount", is(1)))
                .andExpect(jsonPath("$.prescriptionsIssuedCount", is(1)))
                .andExpect(jsonPath("$.labOrdersCount", is(1)))
                .andExpect(jsonPath("$.completedLabOrdersCount", is(1)))
                .andExpect(jsonPath("$.totalRevenueBilled", is(1000.00)))
                .andExpect(jsonPath("$.totalRevenueCollected", is(1000.00)));
    }

    @Test
    @DisplayName("2. Unauthenticated request returns 401 Unauthorized")
    void test2_getSummary_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/me/analytics/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Non-doctor role is rejected with 403 Forbidden")
    void test3_getSummary_NonDoctorRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/me/analytics/summary")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Doctor cannot see another hospital's analytics")
    void test4_getSummary_HospitalIsolation() throws Exception {
        // Doctor 2 from Hospital 2 queries summary — should see 0 metrics for Hospital 1
        mockMvc.perform(get("/api/v1/doctors/me/analytics/summary")
                        .header("Authorization", "Bearer " + doctor2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayAppointmentsCount", is(0)))
                .andExpect(jsonPath("$.completedConsultationsCount", is(0)))
                .andExpect(jsonPath("$.totalRevenueCollected", is(0)));
    }

    @Test
    @DisplayName("5 & 6. Doctor cannot override hospitalId or doctorId via query params")
    void test5_getSummary_QueryOverride_Ignored() throws Exception {
        // Even if client passes doctorId=2 or hospitalId=2 as query params, server resolves doctor 1's context
        mockMvc.perform(get("/api/v1/doctors/me/analytics/summary")
                        .param("hospitalId", "999")
                        .param("doctorId", "999")
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayAppointmentsCount", is(3)));
    }

    @Test
    @DisplayName("14 & 15. Trends return correct dates and daily aggregation")
    void test14_getAnalyticsTrends_Success() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);

        mockMvc.perform(get("/api/v1/doctors/me/analytics/trends")
                        .param("from", from.toString())
                        .param("to", today.toString())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[6].date", is(today.toString())))
                .andExpect(jsonPath("$[6].appointments", is(3)))
                .andExpect(jsonPath("$[6].consultations", is(1)))
                .andExpect(jsonPath("$[6].revenue", is(1000.00)));
    }

    @Test
    @DisplayName("16. Invalid date range returns 400 Bad Request")
    void test16_getAnalyticsTrends_InvalidDateRange() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(5);

        // from > to
        mockMvc.perform(get("/api/v1/doctors/me/analytics/trends")
                        .param("from", future.toString())
                        .param("to", today.toString())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("from date cannot be after to date")));
    }

    @Test
    @DisplayName("17. Default date range returns valid trend points")
    void test17_getAnalyticsTrends_DefaultDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/me/analytics/trends")
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(14)));
    }
}
