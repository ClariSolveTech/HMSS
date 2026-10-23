package com.healthcare.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.auth.dto.AuthResponse;
import com.healthcare.backend.auth.dto.LoginRequest;
import com.healthcare.backend.auth.dto.PatientRegisterRequest;
import com.healthcare.backend.auth.dto.RegisterRequest;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.service.AuthService;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.patient.entity.BloodGroup;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class PatientAuthenticationIntegrationTest {

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
    private PatientRepository patientRepository;

    @Autowired
    private AuthService authService;

    private MockMvc mockMvc;

    private Hospital activeHospital;
    private Hospital inactiveHospital;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 6);

        activeHospital = new Hospital();
        activeHospital.setName("Active Test Hospital " + suffix);
        activeHospital.setCode("ATH" + suffix);
        activeHospital.setStatus(HospitalStatus.ACTIVE);
        activeHospital = hospitalRepository.save(activeHospital);

        inactiveHospital = new Hospital();
        inactiveHospital.setName("Inactive Hospital " + suffix);
        inactiveHospital.setCode("INH" + suffix);
        inactiveHospital.setStatus(HospitalStatus.INACTIVE);
        inactiveHospital = hospitalRepository.save(inactiveHospital);

        roleRepository.findByName(RoleName.PATIENT)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.PATIENT)));
        roleRepository.findByName(RoleName.RECEPTIONIST)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.RECEPTIONIST)));
        roleRepository.findByName(RoleName.DOCTOR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.DOCTOR)));
    }

    @Test
    @DisplayName("1. Successfully registers a new patient with user and patient entity linked atomically")
    public void testRegisterNewPatientSuccess() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        PatientRegisterRequest request = new PatientRegisterRequest();
        request.setFirstName("Rohan");
        request.setLastName("Verma");
        request.setDateOfBirth(LocalDate.of(1992, 5, 14));
        request.setGender(Gender.MALE);
        request.setMobileNumber("98" + uniqueSuffix.replaceAll("[^0-9]", "1").substring(0, 8));
        request.setEmail("rohan." + uniqueSuffix + "@patientportal.com");
        request.setPassword("SecretPass@123");
        request.setHospitalId(activeHospital.getId());
        request.setBloodGroup(BloodGroup.O_POSITIVE);
        request.setCity("Bengaluru");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.roles", contains("PATIENT")))
                .andExpect(jsonPath("$.hospitalId", is(activeHospital.getId().intValue())))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        Long userId = authResponse.getUserId();

        // Verify Patient record exists and is linked to the User
        Optional<Patient> linkedPatient = patientRepository.findByUserId(userId);
        Assertions.assertTrue(linkedPatient.isPresent(), "Patient record should be linked to created user");
        Assertions.assertEquals("Rohan", linkedPatient.get().getFirstName());
        Assertions.assertEquals("Verma", linkedPatient.get().getLastName());
        Assertions.assertEquals(userId, linkedPatient.get().getUser().getId());
        Assertions.assertEquals(activeHospital.getId(), linkedPatient.get().getHospital().getId());
        Assertions.assertTrue(linkedPatient.get().getPatientCode().contains("ATH"));

        // Verify repository query methods
        Assertions.assertTrue(patientRepository.existsByUserId(userId));
        Assertions.assertTrue(patientRepository.findByUserIdAndHospitalId(userId, activeHospital.getId()).isPresent());
        Assertions.assertFalse(patientRepository.findByUserIdAndHospitalId(userId, 999999L).isPresent());
    }

    @Test
    @DisplayName("2. Existing unlinked patients have null user_id and can coexist without conflict")
    public void testMultipleUnlinkedPatientsAllowedWithNullUser() {
        String code1 = "UNLINKED-001-" + UUID.randomUUID();
        String code2 = "UNLINKED-002-" + UUID.randomUUID();

        Patient p1 = new Patient();
        p1.setPatientCode(code1);
        p1.setFirstName("WalkIn");
        p1.setLastName("PatientOne");
        p1.setDateOfBirth(LocalDate.of(1985, 3, 10));
        p1.setGender(Gender.FEMALE);
        p1.setMobileNumber("9100000001");
        p1.setStatus(PatientStatus.ACTIVE);
        p1.setHospital(activeHospital);
        p1.setUser(null); // Unlinked
        p1 = patientRepository.save(p1);

        Patient p2 = new Patient();
        p2.setPatientCode(code2);
        p2.setFirstName("WalkIn");
        p2.setLastName("PatientTwo");
        p2.setDateOfBirth(LocalDate.of(1987, 8, 20));
        p2.setGender(Gender.MALE);
        p2.setMobileNumber("9100000002");
        p2.setStatus(PatientStatus.ACTIVE);
        p2.setHospital(activeHospital);
        p2.setUser(null); // Unlinked
        p2 = patientRepository.save(p2);

        Assertions.assertNull(p1.getUser());
        Assertions.assertNull(p2.getUser());
        Assertions.assertNotEquals(p1.getId(), p2.getId());

        // Verify finding by null user returns empty
        Assertions.assertFalse(patientRepository.existsByUserId(99999L));
    }

    @Test
    @DisplayName("3. Enforces 1-to-1 uniqueness: one User cannot be linked to multiple patients")
    public void testOneUserCannotBeLinkedToMultiplePatients() {
        // Create user
        User user = new User();
        user.setFirstName("Single");
        user.setLastName("User");
        user.setMobileNumber("9200000001");
        user.setEmail("single.user@example.com");
        user.setPasswordHash("$2a$10$dummyHashVal");
        user.setHospital(activeHospital);
        user.setStatus(com.healthcare.backend.auth.entity.UserStatus.ACTIVE);
        user = userRepository.save(user);

        // First patient linked to user
        Patient p1 = new Patient();
        p1.setPatientCode("CODE-1-" + UUID.randomUUID());
        p1.setFirstName("Patient");
        p1.setLastName("One");
        p1.setDateOfBirth(LocalDate.of(1990, 1, 1));
        p1.setGender(Gender.MALE);
        p1.setMobileNumber("9200000001");
        p1.setStatus(PatientStatus.ACTIVE);
        p1.setHospital(activeHospital);
        p1.setUser(user);
        patientRepository.saveAndFlush(p1);

        // Second patient attempting to link to SAME user should violate unique constraint
        Patient p2 = new Patient();
        p2.setPatientCode("CODE-2-" + UUID.randomUUID());
        p2.setFirstName("Patient");
        p2.setLastName("Two");
        p2.setDateOfBirth(LocalDate.of(1991, 2, 2));
        p2.setGender(Gender.FEMALE);
        p2.setMobileNumber("9200000002");
        p2.setStatus(PatientStatus.ACTIVE);
        p2.setHospital(activeHospital);
        p2.setUser(user);

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            patientRepository.saveAndFlush(p2);
        });
    }

    @Test
    @DisplayName("4. Patient registration rejects duplicate mobile or email in users table")
    public void testRejectsDuplicateMobileOrEmailInUsers() throws Exception {
        String mobile = "9300000001";
        String email = "duplicate.test@patientportal.com";

        PatientRegisterRequest first = new PatientRegisterRequest();
        first.setFirstName("Original");
        first.setLastName("Patient");
        first.setDateOfBirth(LocalDate.of(1995, 4, 12));
        first.setGender(Gender.FEMALE);
        first.setMobileNumber(mobile);
        first.setEmail(email);
        first.setPassword("Password@123");
        first.setHospitalId(activeHospital.getId());

        mockMvc.perform(post("/api/v1/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Duplicate mobile attempt
        PatientRegisterRequest dupMobile = new PatientRegisterRequest();
        dupMobile.setFirstName("Duplicate");
        dupMobile.setLastName("Mobile");
        dupMobile.setDateOfBirth(LocalDate.of(1996, 6, 18));
        dupMobile.setGender(Gender.MALE);
        dupMobile.setMobileNumber(mobile); // Same mobile
        dupMobile.setEmail("different.email@patientportal.com");
        dupMobile.setPassword("Password@123");
        dupMobile.setHospitalId(activeHospital.getId());

        mockMvc.perform(post("/api/v1/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupMobile)))
                .andExpect(status().is4xxClientError());

        // Duplicate email attempt
        PatientRegisterRequest dupEmail = new PatientRegisterRequest();
        dupEmail.setFirstName("Duplicate");
        dupEmail.setLastName("Email");
        dupEmail.setDateOfBirth(LocalDate.of(1997, 7, 22));
        dupEmail.setGender(Gender.MALE);
        dupEmail.setMobileNumber("9300000099"); // Different mobile
        dupEmail.setEmail(email); // Same email
        dupEmail.setPassword("Password@123");
        dupEmail.setHospitalId(activeHospital.getId());

        mockMvc.perform(post("/api/v1/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupEmail)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("5. Rejects patient registration for non-existent or inactive hospital")
    public void testRejectsInvalidOrInactiveHospital() throws Exception {
        PatientRegisterRequest reqNonExistent = new PatientRegisterRequest();
        reqNonExistent.setFirstName("Ghost");
        reqNonExistent.setLastName("Patient");
        reqNonExistent.setDateOfBirth(LocalDate.of(1993, 11, 5));
        reqNonExistent.setGender(Gender.OTHER);
        reqNonExistent.setMobileNumber("9400000001");
        reqNonExistent.setPassword("Password@123");
        reqNonExistent.setHospitalId(999999L); // Non-existent hospital

        mockMvc.perform(post("/api/v1/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqNonExistent)))
                .andExpect(status().isNotFound());

        PatientRegisterRequest reqInactive = new PatientRegisterRequest();
        reqInactive.setFirstName("Inactive");
        reqInactive.setLastName("Patient");
        reqInactive.setDateOfBirth(LocalDate.of(1994, 12, 6));
        reqInactive.setGender(Gender.FEMALE);
        reqInactive.setMobileNumber("9400000002");
        reqInactive.setPassword("Password@123");
        reqInactive.setHospitalId(inactiveHospital.getId()); // Inactive hospital

        mockMvc.perform(post("/api/v1/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInactive)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("6. Public staff registration endpoint prohibits assigning PATIENT role")
    public void testStaffRegistrationRejectsPatientRole() throws Exception {
        RegisterRequest staffReq = new RegisterRequest();
        staffReq.setFirstName("Sneaky");
        staffReq.setLastName("Patient");
        staffReq.setEmail("sneaky@example.com");
        staffReq.setMobileNumber("9500000001");
        staffReq.setPassword("Password@123");
        staffReq.setHospitalId(activeHospital.getId());
        staffReq.setRole(RoleName.PATIENT); // Attempting to use staff registration for patient

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Patient login works via standard login endpoint and returns ROLE_PATIENT authority")
    public void testRegisteredPatientCanLoginSuccessfully() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String mobile = "96" + uniqueSuffix.replaceAll("[^0-9]", "2").substring(0, 8);
        String email = "login.test." + uniqueSuffix + "@patientportal.com";
        String password = "LoginPass@123";

        PatientRegisterRequest regReq = new PatientRegisterRequest();
        regReq.setFirstName("Meera");
        regReq.setLastName("Nair");
        regReq.setDateOfBirth(LocalDate.of(1990, 8, 15));
        regReq.setGender(Gender.FEMALE);
        regReq.setMobileNumber(mobile);
        regReq.setEmail(email);
        regReq.setPassword(password);
        regReq.setHospitalId(activeHospital.getId());

        mockMvc.perform(post("/api/v1/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        // Login via email
        LoginRequest emailLogin = new LoginRequest(email, password);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.roles", contains("PATIENT")));

        // Login via mobile
        LoginRequest mobileLogin = new LoginRequest(mobile, password);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mobileLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.roles", contains("PATIENT")));
    }

    @Test
    @DisplayName("8. Staff registration regression: RECEPTIONIST and DOCTOR registrations work as before")
    public void testStaffRegistrationRegression() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);

        RegisterRequest recepReq = new RegisterRequest();
        recepReq.setFirstName("Staff");
        recepReq.setLastName("Recep");
        recepReq.setEmail("recep." + suffix + "@hospital.com");
        String randomDigits = String.format("%07d", Math.abs(UUID.randomUUID().hashCode() % 10000000));
        recepReq.setMobileNumber("970" + randomDigits);
        recepReq.setPassword("StaffPass@123");
        recepReq.setHospitalId(activeHospital.getId());
        recepReq.setRole(RoleName.RECEPTIONIST);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recepReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles", contains("RECEPTIONIST")));
    }

    @Test
    @DisplayName("9. Secure patient activation: Walk-in patient account activation workflow")
    public void testWalkInPatientActivationWorkflow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String randomDigits = String.format("%07d", Math.abs(UUID.randomUUID().hashCode() % 10000000));
        String mobile = "960" + randomDigits;

        // 1. Create a walk-in patient (user is null)
        Patient walkIn = new Patient();
        walkIn.setPatientCode("WALK-" + suffix);
        walkIn.setFirstName("WalkIn");
        walkIn.setLastName("Patient");
        walkIn.setDateOfBirth(LocalDate.of(1992, 4, 15));
        walkIn.setGender(Gender.MALE);
        walkIn.setMobileNumber(mobile);
        walkIn.setHospital(activeHospital);
        walkIn.setUser(null);
        walkIn = patientRepository.save(walkIn);

        Assertions.assertNull(walkIn.getUser());
        long patientCountBefore = patientRepository.count();

        // 2. Receptionist generates one-time activation code
        RegisterRequest recepReq = new RegisterRequest();
        recepReq.setFirstName("Staff");
        recepReq.setLastName("Recep");
        recepReq.setEmail("recep." + suffix + "@hospital.com");
        recepReq.setMobileNumber("975" + randomDigits);
        recepReq.setPassword("StaffPass@123");
        recepReq.setHospitalId(activeHospital.getId());
        recepReq.setRole(RoleName.RECEPTIONIST);
        AuthResponse recepAuth = authService.register(recepReq);
        String recepToken = recepAuth.getAccessToken();

        MvcResult actResult = mockMvc.perform(post("/api/v1/patients/" + walkIn.getId() + "/generate-activation-code")
                        .header("Authorization", "Bearer " + recepToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activationCode", notNullValue()))
                .andExpect(jsonPath("$.patientId", is(walkIn.getId().intValue())))
                .andReturn();

        String rawCode = objectMapper.readTree(actResult.getResponse().getContentAsString()).path("activationCode").asText();
        Assertions.assertTrue(rawCode.startsWith("ACT-"));

        // 3. Attempt activation with invalid code fails
        com.healthcare.backend.auth.dto.PatientActivateRequest invalidReq =
                new com.healthcare.backend.auth.dto.PatientActivateRequest("ACT-INVALID-CODE", "NewSecurePass@123", null);
        mockMvc.perform(post("/api/v1/auth/patient/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isUnauthorized());

        // 4. Valid patient activation establishes own password and logs in
        com.healthcare.backend.auth.dto.PatientActivateRequest validReq =
                new com.healthcare.backend.auth.dto.PatientActivateRequest(rawCode, "NewSecurePass@123", "patient." + suffix + "@gmail.com");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/patient/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.roles", contains("PATIENT")))
                .andExpect(jsonPath("$.hospitalId", is(activeHospital.getId().intValue())))
                .andReturn();

        String patientToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("accessToken").asText();

        // 5. Verify patient record is now linked to the newly created user (no duplicate patient row)
        Patient linkedPatient = patientRepository.findById(walkIn.getId()).orElseThrow();
        Assertions.assertNotNull(linkedPatient.getUser());
        Assertions.assertEquals(mobile, linkedPatient.getUser().getMobileNumber());
        Assertions.assertEquals("patient." + suffix + "@gmail.com", linkedPatient.getEmail());
        Assertions.assertEquals(patientCountBefore, patientRepository.count(), "Activation must not create duplicate patient records");

        // 6. Code cannot be reused
        mockMvc.perform(post("/api/v1/auth/patient/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isUnauthorized());

        // 7. Patient can now query patient portal using their JWT token
        mockMvc.perform(get("/api/v1/patient-portal/profile")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("WalkIn")))
                .andExpect(jsonPath("$.patientCode", is("WALK-" + suffix)));
    }
}
