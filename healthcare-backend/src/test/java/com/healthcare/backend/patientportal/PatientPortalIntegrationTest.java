package com.healthcare.backend.patientportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.appointment.entity.BookingSource;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.dto.AuthResponse;
import com.healthcare.backend.auth.dto.PatientRegisterRequest;
import com.healthcare.backend.auth.dto.RegisterRequest;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.auth.service.AuthService;
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
import com.healthcare.backend.lab.entity.LabOrderItem;
import com.healthcare.backend.lab.entity.LabOrderStatus;
import com.healthcare.backend.lab.entity.LabOrderPriority;
import com.healthcare.backend.lab.entity.LabTest;
import com.healthcare.backend.lab.repository.LabOrderRepository;
import com.healthcare.backend.lab.repository.LabTestRepository;
import com.healthcare.backend.patient.entity.BloodGroup;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.patientportal.dto.UpdatePatientProfileRequest;
import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.entity.PrescriptionItem;
import com.healthcare.backend.prescription.entity.PrescriptionStatus;
import com.healthcare.backend.prescription.repository.PrescriptionRepository;
import org.junit.jupiter.api.Assertions;
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
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class PatientPortalIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private LabTestRepository labTestRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private Doctor doctorA;

    private User patientUserA;
    private Patient patientA;
    private String tokenPatientA;

    private User patientUserB;
    private Patient patientB;
    private String tokenPatientB;

    private User doctorUser;
    private String tokenDoctor;

    private User unlinkedUser;
    private String tokenUnlinkedUser;

    private static final java.util.concurrent.atomic.AtomicLong PHONE_COUNTER = new java.util.concurrent.atomic.AtomicLong(9800000000L);

    private String nextPhone() {
        return String.valueOf(PHONE_COUNTER.incrementAndGet());
    }

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 6);

        hospitalA = new Hospital();
        hospitalA.setName("Portal Hospital A " + suffix);
        hospitalA.setCode("PHA" + suffix);
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        hospitalB = new Hospital();
        hospitalB.setName("Portal Hospital B " + suffix);
        hospitalB.setCode("PHB" + suffix);
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        Role patientRole = roleRepository.findByName(RoleName.PATIENT)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.PATIENT)));
        Role doctorRole = roleRepository.findByName(RoleName.DOCTOR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.DOCTOR)));

        // Doctor User and Doctor
        doctorUser = new User();
        doctorUser.setFirstName("DrAarav");
        doctorUser.setLastName("Sharma");
        doctorUser.setEmail("dr.aarav." + suffix + "@hospital.com");
        doctorUser.setMobileNumber(nextPhone());
        doctorUser.setPasswordHash(passwordEncoder.encode("Doctor@123"));
        doctorUser.setHospital(hospitalA);
        doctorUser.setStatus(UserStatus.ACTIVE);
        doctorUser.getRoles().add(doctorRole);
        doctorUser = userRepository.save(doctorUser);
        tokenDoctor = jwtService.generateToken(doctorUser);

        doctorA = new Doctor();
        doctorA.setUser(doctorUser);
        doctorA.setHospital(hospitalA);
        doctorA.setDoctorCode("DOC-" + suffix);
        doctorA.setSpecialization("Cardiology");
        doctorA.setQualification("MD");
        doctorA.setLicenseNumber("LIC-" + suffix);
        doctorA.setConsultationFee(new BigDecimal("500.00"));
        doctorA.setStatus(DoctorStatus.ACTIVE);
        doctorA = doctorRepository.save(doctorA);

        // Register Patient A in Hospital A
        PatientRegisterRequest regA = new PatientRegisterRequest();
        regA.setFirstName("Arjun");
        regA.setLastName("Kapoor");
        regA.setDateOfBirth(LocalDate.of(1990, 1, 10));
        regA.setGender(Gender.MALE);
        regA.setBloodGroup(BloodGroup.O_POSITIVE);
        regA.setMobileNumber(nextPhone());
        regA.setEmail("arjun." + suffix + "@patient.com");
        regA.setPassword("Patient@123");
        regA.setHospitalId(hospitalA.getId());
        regA.setCity("Bengaluru");
        AuthResponse resA = authService.registerPatient(regA);
        tokenPatientA = resA.getAccessToken();
        patientUserA = userRepository.findById(resA.getUserId()).orElseThrow();
        patientA = patientRepository.findByUserId(patientUserA.getId()).orElseThrow();

        // Register Patient B in Hospital B
        PatientRegisterRequest regB = new PatientRegisterRequest();
        regB.setFirstName("Kavita");
        regB.setLastName("Rao");
        regB.setDateOfBirth(LocalDate.of(1992, 4, 15));
        regB.setGender(Gender.FEMALE);
        regB.setBloodGroup(BloodGroup.B_POSITIVE);
        regB.setMobileNumber(nextPhone());
        regB.setEmail("kavita." + suffix + "@patient.com");
        regB.setPassword("Patient@123");
        regB.setHospitalId(hospitalB.getId());
        AuthResponse resB = authService.registerPatient(regB);
        tokenPatientB = resB.getAccessToken();
        patientUserB = userRepository.findById(resB.getUserId()).orElseThrow();
        patientB = patientRepository.findByUserId(patientUserB.getId()).orElseThrow();

        // Create an unlinked patient user (has ROLE_PATIENT but no row in patients table)
        unlinkedUser = new User();
        unlinkedUser.setFirstName("Unlinked");
        unlinkedUser.setLastName("User");
        unlinkedUser.setEmail("unlinked." + suffix + "@patient.com");
        unlinkedUser.setMobileNumber(nextPhone());
        unlinkedUser.setPasswordHash(passwordEncoder.encode("Unlinked@123"));
        unlinkedUser.setHospital(hospitalA);
        unlinkedUser.setStatus(UserStatus.ACTIVE);
        unlinkedUser.getRoles().add(patientRole);
        unlinkedUser = userRepository.save(unlinkedUser);
        tokenUnlinkedUser = jwtService.generateToken(unlinkedUser);
    }

    @Test
    @DisplayName("1. Patient can view own profile and update contact details")
    public void testPatientProfileViewAndUpdate() throws Exception {
        // View profile
        mockMvc.perform(get("/api/v1/patient-portal/profile")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(patientA.getId().intValue())))
                .andExpect(jsonPath("$.firstName", is("Arjun")))
                .andExpect(jsonPath("$.lastName", is("Kapoor")))
                .andExpect(jsonPath("$.patientCode", notNullValue()))
                .andExpect(jsonPath("$.hospitalId", is(hospitalA.getId().intValue())));

        // Update profile (safe contact fields only)
        UpdatePatientProfileRequest updateReq = new UpdatePatientProfileRequest();
        updateReq.setAddress("42 Palm Grove Road");
        updateReq.setCity("Bengaluru");
        updateReq.setState("Karnataka");
        updateReq.setPincode("560001");
        updateReq.setEmergencyContactName("Rahul Kapoor");
        updateReq.setEmergencyContactNumber("9888877777");

        mockMvc.perform(put("/api/v1/patient-portal/profile")
                        .header("Authorization", "Bearer " + tokenPatientA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address", is("42 Palm Grove Road")))
                .andExpect(jsonPath("$.emergencyContactName", is("Rahul Kapoor")))
                .andExpect(jsonPath("$.emergencyContactNumber", is("9888877777")));

        Patient updated = patientRepository.findById(patientA.getId()).orElseThrow();
        Assertions.assertEquals("42 Palm Grove Road", updated.getAddress());
        Assertions.assertEquals("Rahul Kapoor", updated.getEmergencyContactName());
    }

    @Test
    @DisplayName("2. Patient can view own appointment list and appointment detail, but NOT another patient's appointment (IDOR protection)")
    public void testAppointmentAccessAndIdorProtection() throws Exception {
        // Create an appointment for Patient A
        Appointment aptA = new Appointment();
        aptA.setAppointmentNumber("APT-A-101");
        aptA.setHospital(hospitalA);
        aptA.setPatient(patientA);
        aptA.setDoctor(doctorA);
        aptA.setAppointmentDate(LocalDate.now());
        aptA.setStartTime(LocalTime.of(14, 0));
        aptA.setEndTime(LocalTime.of(14, 30));
        aptA.setStatus(AppointmentStatus.CONFIRMED);
        aptA.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        aptA.setBookingSource(BookingSource.PATIENT_APP);
        aptA.setReason("Routine Checkup");
        aptA = appointmentRepository.save(aptA);

        // Create an appointment for Patient B in Hospital B
        Appointment aptB = new Appointment();
        aptB.setAppointmentNumber("APT-B-201");
        aptB.setHospital(hospitalB);
        aptB.setPatient(patientB);
        aptB.setDoctor(doctorA);
        aptB.setAppointmentDate(LocalDate.now());
        aptB.setStartTime(LocalTime.of(15, 0));
        aptB.setEndTime(LocalTime.of(15, 30));
        aptB.setStatus(AppointmentStatus.CONFIRMED);
        aptB.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        aptB.setBookingSource(BookingSource.PATIENT_APP);
        aptB = appointmentRepository.save(aptB);

        // Patient A can view own appointments list
        mockMvc.perform(get("/api/v1/patient-portal/appointments")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].appointmentNumber", is("APT-A-101")));

        // Patient A can view own appointment detail
        mockMvc.perform(get("/api/v1/patient-portal/appointments/" + aptA.getId())
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentNumber", is("APT-A-101")))
                .andExpect(jsonPath("$.isTeleconsultation", is(false)));

        // IDOR Attack: Patient A tries to view Patient B's appointment detail -> Must return 404 or 403
        mockMvc.perform(get("/api/v1/patient-portal/appointments/" + aptB.getId())
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("3. Patient cannot access staff-only APIs (403 Forbidden)")
    public void testPatientBlockedFromStaffApis() throws Exception {
        // Staff endpoint: GET /api/v1/appointments
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isForbidden());

        // Staff endpoint: GET /api/v1/patients
        mockMvc.perform(get("/api/v1/patients")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isForbidden());

        // Staff endpoint: GET /api/v1/doctors/me/prescriptions
        mockMvc.perform(get("/api/v1/doctors/me/prescriptions")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Staff cannot access patient portal APIs (403 Forbidden)")
    public void testStaffBlockedFromPatientPortal() throws Exception {
        mockMvc.perform(get("/api/v1/patient-portal/dashboard")
                        .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/patient-portal/appointments")
                        .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Unauthenticated requests are rejected (401 Unauthorized)")
    public void testUnauthenticatedRequestsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/patient-portal/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/patient-portal/appointments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/patient-portal/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6. Unlinked legacy account is handled safely (clear 403 error without data exposure)")
    public void testUnlinkedAccountHandledSafely() throws Exception {
        mockMvc.perform(get("/api/v1/patient-portal/dashboard")
                        .header("Authorization", "Bearer " + tokenUnlinkedUser))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("No patient clinical profile linked")));
    }

    @Test
    @DisplayName("7. Teleconsultation access rules: permits join only when eligible, within window, and never leaks URL when ineligible")
    public void testTeleconsultationJoinRules() throws Exception {
        LocalTime now = LocalTime.now();
        String validMeetUrl = "https://meet.google.com/abc-defg-hij";

        // Case A: IN_PERSON consultation -> Ineligible
        Appointment inPersonApt = new Appointment();
        inPersonApt.setAppointmentNumber("APT-TELE-INPERSON");
        inPersonApt.setHospital(hospitalA);
        inPersonApt.setPatient(patientA);
        inPersonApt.setDoctor(doctorA);
        inPersonApt.setAppointmentDate(LocalDate.now());
        inPersonApt.setStartTime(now);
        inPersonApt.setEndTime(now.plusMinutes(30));
        inPersonApt.setStatus(AppointmentStatus.CONFIRMED);
        inPersonApt.setAppointmentType(AppointmentType.NEW_CONSULTATION);
        inPersonApt.setMeetUrl(validMeetUrl);
        inPersonApt = appointmentRepository.save(inPersonApt);

        mockMvc.perform(get("/api/v1/patient-portal/appointments/" + inPersonApt.getId() + "/teleconsult-access")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canJoin", is(false)))
                .andExpect(jsonPath("$.joinUrl", nullValue()))
                .andExpect(jsonPath("$.reason", containsString("in-person consultation")));

        // Case B: TELECONSULTATION with CANCELLED status -> Ineligible
        Appointment cancelledApt = new Appointment();
        cancelledApt.setAppointmentNumber("APT-TELE-CANCELLED");
        cancelledApt.setHospital(hospitalA);
        cancelledApt.setPatient(patientA);
        cancelledApt.setDoctor(doctorA);
        cancelledApt.setAppointmentDate(LocalDate.now());
        cancelledApt.setStartTime(now);
        cancelledApt.setEndTime(now.plusMinutes(30));
        cancelledApt.setStatus(AppointmentStatus.CANCELLED);
        cancelledApt.setAppointmentType(AppointmentType.TELECONSULTATION);
        cancelledApt.setMeetUrl(validMeetUrl);
        cancelledApt = appointmentRepository.save(cancelledApt);

        mockMvc.perform(get("/api/v1/patient-portal/appointments/" + cancelledApt.getId() + "/teleconsult-access")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canJoin", is(false)))
                .andExpect(jsonPath("$.joinUrl", nullValue()))
                .andExpect(jsonPath("$.reason", containsString("cancelled")));

        // Case C: TELECONSULTATION scheduled tomorrow -> Ineligible
        Appointment futureApt = new Appointment();
        futureApt.setAppointmentNumber("APT-TELE-FUTURE");
        futureApt.setHospital(hospitalA);
        futureApt.setPatient(patientA);
        futureApt.setDoctor(doctorA);
        futureApt.setAppointmentDate(LocalDate.now().plusDays(1));
        futureApt.setStartTime(LocalTime.of(10, 0));
        futureApt.setEndTime(LocalTime.of(10, 30));
        futureApt.setStatus(AppointmentStatus.CONFIRMED);
        futureApt.setAppointmentType(AppointmentType.TELECONSULTATION);
        futureApt.setMeetUrl(validMeetUrl);
        futureApt = appointmentRepository.save(futureApt);

        mockMvc.perform(get("/api/v1/patient-portal/appointments/" + futureApt.getId() + "/teleconsult-access")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canJoin", is(false)))
                .andExpect(jsonPath("$.joinUrl", nullValue()))
                .andExpect(jsonPath("$.reason", containsString("scheduled for")));

        // Case D: TELECONSULTATION with status IN_PROGRESS -> Eligible (can join immediately)
        Appointment inProgressApt = new Appointment();
        inProgressApt.setAppointmentNumber("APT-TELE-INPROGRESS");
        inProgressApt.setHospital(hospitalA);
        inProgressApt.setPatient(patientA);
        inProgressApt.setDoctor(doctorA);
        inProgressApt.setAppointmentDate(LocalDate.now());
        inProgressApt.setStartTime(now.minusMinutes(10));
        inProgressApt.setEndTime(now.plusMinutes(20));
        inProgressApt.setStatus(AppointmentStatus.IN_PROGRESS);
        inProgressApt.setAppointmentType(AppointmentType.TELECONSULTATION);
        inProgressApt.setMeetUrl(validMeetUrl);
        inProgressApt = appointmentRepository.save(inProgressApt);

        mockMvc.perform(get("/api/v1/patient-portal/appointments/" + inProgressApt.getId() + "/teleconsult-access")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canJoin", is(true)))
                .andExpect(jsonPath("$.joinUrl", is(validMeetUrl)))
                .andExpect(jsonPath("$.reason", containsString("in progress")));

        // Case E: TELECONSULTATION with CONFIRMED and within active window [now - 5m to now + 25m] -> Eligible
        Appointment validApt = new Appointment();
        validApt.setAppointmentNumber("APT-TELE-ELIGIBLE");
        validApt.setHospital(hospitalA);
        validApt.setPatient(patientA);
        validApt.setDoctor(doctorA);
        validApt.setAppointmentDate(LocalDate.now());
        validApt.setStartTime(now.minusMinutes(5));
        validApt.setEndTime(now.plusMinutes(25));
        validApt.setStatus(AppointmentStatus.CONFIRMED);
        validApt.setAppointmentType(AppointmentType.TELECONSULTATION);
        validApt.setMeetUrl(validMeetUrl);
        validApt = appointmentRepository.save(validApt);

        mockMvc.perform(get("/api/v1/patient-portal/appointments/" + validApt.getId() + "/teleconsult-access")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canJoin", is(true)))
                .andExpect(jsonPath("$.joinUrl", is(validMeetUrl)));
    }

    @Test
    @DisplayName("8. Patient can view dashboard summary, prescriptions, consultations, and lab orders")
    public void testPatientClinicalRecordsAccess() throws Exception {
        // Create consultation
        Consultation consultation = new Consultation();
        consultation.setHospital(hospitalA);
        consultation.setDoctor(doctorA);
        consultation.setPatient(patientA);
        consultation.setConsultationDate(LocalDateTime.now().minusDays(1));
        consultation.setStatus(ConsultationStatus.COMPLETED);
        consultation.setChiefComplaint("Chest pain on exertion");
        consultation.setDiagnosis("Mild hypertension");
        consultation.setTreatmentAdvice("Low sodium diet and exercise");
        consultation = consultationRepository.save(consultation);

        // Create prescription with 1 item
        Prescription prescription = new Prescription();
        prescription.setPrescriptionCode("RX-" + UUID.randomUUID().toString().substring(0, 8));
        prescription.setHospital(hospitalA);
        prescription.setDoctor(doctorA);
        prescription.setPatient(patientA);
        prescription.setConsultation(consultation);
        prescription.setStatus(PrescriptionStatus.ISSUED);
        prescription.setNotes("Take after meals");

        PrescriptionItem item = new PrescriptionItem();
        item.setPrescription(prescription);
        item.setMedicineName("Amlodipine");
        item.setDosage("5mg");
        item.setFrequency("Once daily");
        item.setDuration("30 days");
        item.setRoute("Oral");
        prescription.getItems().add(item);
        prescription = prescriptionRepository.save(prescription);

        // Create lab order with 1 item
        LabTest labTest = new LabTest();
        labTest.setHospital(hospitalA);
        labTest.setTestCode("LIPID-" + UUID.randomUUID().toString().substring(0, 6));
        labTest.setTestName("Lipid Profile");
        labTest.setCategory("Biochemistry");
        labTest = labTestRepository.save(labTest);

        LabOrder labOrder = new LabOrder();
        labOrder.setOrderNumber("LAB-" + UUID.randomUUID().toString().substring(0, 8));
        labOrder.setHospital(hospitalA);
        labOrder.setPatient(patientA);
        labOrder.setDoctor(doctorA);
        labOrder.setConsultation(consultation);
        labOrder.setStatus(LabOrderStatus.ORDERED);
        labOrder.setPriority(LabOrderPriority.NORMAL);
        labOrder.setOrderedAt(LocalDateTime.now());

        LabOrderItem orderItem = new LabOrderItem();
        orderItem.setLabOrder(labOrder);
        orderItem.setLabTest(labTest);
        orderItem.setTestNameSnapshot("Lipid Profile");
        orderItem.setTestCodeSnapshot(labTest.getTestCode());
        orderItem.setStatus(LabOrderStatus.ORDERED);
        labOrder.getItems().add(orderItem);
        labOrder = labOrderRepository.save(labOrder);

        // 1. Dashboard summary
        mockMvc.perform(get("/api/v1/patient-portal/dashboard")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientCode", is(patientA.getPatientCode())))
                .andExpect(jsonPath("$.completedConsultationsCount", is(1)))
                .andExpect(jsonPath("$.activePrescriptionsCount", is(1)))
                .andExpect(jsonPath("$.labOrdersCount", is(1)));

        // 2. Prescriptions list & detail
        mockMvc.perform(get("/api/v1/patient-portal/prescriptions")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].prescriptionCode", is(prescription.getPrescriptionCode())));

        mockMvc.perform(get("/api/v1/patient-portal/prescriptions/" + prescription.getId())
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionCode", is(prescription.getPrescriptionCode())))
                .andExpect(jsonPath("$.diagnosis", is("Mild hypertension")))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].medicineName", is("Amlodipine")));

        // 3. Consultations list
        mockMvc.perform(get("/api/v1/patient-portal/consultations")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].diagnosis", is("Mild hypertension")));

        // 4. Lab orders list
        mockMvc.perform(get("/api/v1/patient-portal/lab-orders")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber", is(labOrder.getOrderNumber())))
                .andExpect(jsonPath("$.content[0].testNames", contains("Lipid Profile")));
    }
}
