package com.healthcare.backend.patient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.auth.dto.RegisterRequest;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.service.AuthService;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.patient.dto.CreatePatientRequest;
import com.healthcare.backend.patient.dto.UpdatePatientRequest;
import com.healthcare.backend.patient.entity.BloodGroup;
import com.healthcare.backend.patient.entity.Gender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class PatientIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthService authService;

    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private String tokenDoctorA;
    private String tokenDoctorB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Ensure standard roles exist
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
            }
        }

        // Create Hospital A
        hospitalA = new Hospital();
        hospitalA.setName("Hospital Alpha");
        hospitalA.setCode("ALPHA" + System.currentTimeMillis() % 10000);
        hospitalA.setPhone("+919111111111");
        hospitalA.setEmail("alpha@hospital.com");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        // Create Hospital B
        hospitalB = new Hospital();
        hospitalB.setName("Hospital Beta");
        hospitalB.setCode("BETA" + System.currentTimeMillis() % 10000);
        hospitalB.setPhone("+919222222222");
        hospitalB.setEmail("beta@hospital.com");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        // Register Doctor in Hospital A
        RegisterRequest regA = new RegisterRequest();
        regA.setFirstName("Doctor");
        regA.setLastName("Alpha");
        regA.setEmail("doc.alpha." + System.currentTimeMillis() + "@hospital.com");
        regA.setMobileNumber("91" + (System.currentTimeMillis() % 100000000L));
        regA.setPassword("Pass@1234");
        regA.setHospitalId(hospitalA.getId());
        regA.setRole(RoleName.DOCTOR);
        tokenDoctorA = authService.register(regA).getAccessToken();

        // Register Doctor in Hospital B
        RegisterRequest regB = new RegisterRequest();
        regB.setFirstName("Doctor");
        regB.setLastName("Beta");
        regB.setEmail("doc.beta." + System.currentTimeMillis() + "@hospital.com");
        regB.setMobileNumber("92" + (System.currentTimeMillis() % 100000000L));
        regB.setPassword("Pass@1234");
        regB.setHospitalId(hospitalB.getId());
        regB.setRole(RoleName.DOCTOR);
        tokenDoctorB = authService.register(regB).getAccessToken();
    }

    @Test
    @DisplayName("1 & 2. Authenticated user can create patient associated with user's hospital")
    void test1And2_createPatient_success() throws Exception {
        CreatePatientRequest request = new CreatePatientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setGender(Gender.MALE);
        request.setMobileNumber("9876543210");
        request.setBloodGroup(BloodGroup.A_POSITIVE);

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.patientCode").value(startsWith(hospitalA.getCode())))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()));
    }

    @Test
    @DisplayName("3. Patient can be retrieved by ID")
    void test3_getPatientById_success() throws Exception {
        Long patientId = createTestPatient(tokenDoctorA, "Jane", "Doe", "9876543211");

        mockMvc.perform(get("/api/v1/patients/" + patientId)
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("4. Patient list supports pagination")
    void test4_getPatients_pagination() throws Exception {
        createTestPatient(tokenDoctorA, "Patient One", "Test", "9876543212");
        createTestPatient(tokenDoctorA, "Patient Two", "Test", "9876543213");

        mockMvc.perform(get("/api/v1/patients?page=0&size=10&sort=createdAt,desc")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("5. Patient can be updated")
    void test5_updatePatient_success() throws Exception {
        Long patientId = createTestPatient(tokenDoctorA, "Alice", "Smith", "9876543214");

        UpdatePatientRequest updateRequest = new UpdatePatientRequest();
        updateRequest.setAddress("123 Updated Street");
        updateRequest.setBloodGroup(BloodGroup.O_POSITIVE);

        mockMvc.perform(put("/api/v1/patients/" + patientId)
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId))
                .andExpect(jsonPath("$.address").value("123 Updated Street"))
                .andExpect(jsonPath("$.bloodGroup").value("O_POSITIVE"));
    }

    @Test
    @DisplayName("6. Duplicate mobile number within the same hospital returns 409")
    void test6_duplicateMobile_returns409() throws Exception {
        createTestPatient(tokenDoctorA, "First", "Patient", "9998887770");

        CreatePatientRequest duplicateRequest = new CreatePatientRequest();
        duplicateRequest.setFirstName("Second");
        duplicateRequest.setLastName("Patient");
        duplicateRequest.setDateOfBirth(LocalDate.of(1995, 5, 5));
        duplicateRequest.setGender(Gender.FEMALE);
        duplicateRequest.setMobileNumber("9998887770"); // Duplicate in Hospital A

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @DisplayName("7. Non-existent patient returns 404")
    void test7_nonExistentPatient_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/patients/999999")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("8. Unauthenticated request returns 401")
    void test8_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("9. User from Hospital A cannot retrieve a Hospital B patient (404)")
    void test9_crossHospital_getPatient_returns404() throws Exception {
        Long patientBId = createTestPatient(tokenDoctorB, "Hospital B", "Patient", "8887776660");

        // Doctor A attempts to access Hospital B's patient
        mockMvc.perform(get("/api/v1/patients/" + patientBId)
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("10. User from Hospital A cannot update a Hospital B patient (404)")
    void test10_crossHospital_updatePatient_returns404() throws Exception {
        Long patientBId = createTestPatient(tokenDoctorB, "Hospital B", "Patient Two", "8887776661");

        UpdatePatientRequest updateRequest = new UpdatePatientRequest();
        updateRequest.setAddress("Hack Address");

        // Doctor A attempts to update Hospital B's patient
        mockMvc.perform(put("/api/v1/patients/" + patientBId)
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("11. Hospital ID cannot be overridden through request parameters or body")
    void test11_hospitalIdCannotBeOverridden() throws Exception {
        CreatePatientRequest request = new CreatePatientRequest();
        request.setFirstName("Secure");
        request.setLastName("Patient");
        request.setDateOfBirth(LocalDate.of(1988, 8, 8));
        request.setGender(Gender.MALE);
        request.setMobileNumber("7776665550");

        // Authenticated as Doctor A (Hospital A)
        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .param("hospitalId", hospitalB.getId().toString()) // Attempt parameter override
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId())); // Must stay Hospital A
    }

    @Test
    @DisplayName("12. Patient response does not expose password, internal security fields, or entity internals")
    void test12_patientResponse_noInternalExposure() throws Exception {
        Long patientId = createTestPatient(tokenDoctorA, "Clean", "DTO", "6665554440");

        MvcResult result = mockMvc.perform(get("/api/v1/patients/" + patientId)
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.hospital.users").doesNotExist())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Assertions.assertFalse(responseBody.contains("password"));
        Assertions.assertFalse(responseBody.contains("passwordHash"));
    }

    private Long createTestPatient(String token, String firstName, String lastName, String mobile) throws Exception {
        CreatePatientRequest request = new CreatePatientRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setGender(Gender.MALE);
        request.setMobileNumber(mobile);
        request.setBloodGroup(BloodGroup.O_POSITIVE);

        MvcResult result = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }
}
