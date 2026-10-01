package com.healthcare.backend.doctor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.auth.dto.RegisterRequest;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.service.AuthService;
import com.healthcare.backend.doctor.dto.CreateDoctorRequest;
import com.healthcare.backend.doctor.dto.UpdateDoctorRequest;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class DoctorIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.healthcare.backend.auth.security.JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc mockMvc;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private String tokenAdminA;
    private Long doctorUserAId;
    private String tokenDoctorA;

    private String tokenAdminB;
    private Long doctorUserBId;
    private String tokenDoctorB;

    private String tokenReceptionistA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Ensure roles exist
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
            }
        }

        // Create Hospital A
        hospitalA = new Hospital();
        hospitalA.setName("Alpha Medical Center");
        hospitalA.setCode("AMC" + (System.currentTimeMillis() % 10000));
        hospitalA.setPhone("+919800000001");
        hospitalA.setEmail("alpha@med.com");
        hospitalA.setStatus(HospitalStatus.ACTIVE);
        hospitalA = hospitalRepository.save(hospitalA);

        // Create Hospital B
        hospitalB = new Hospital();
        hospitalB.setName("Beta General Hospital");
        hospitalB.setCode("BGH" + (System.currentTimeMillis() % 10000));
        hospitalB.setPhone("+919800000002");
        hospitalB.setEmail("beta@med.com");
        hospitalB.setStatus(HospitalStatus.ACTIVE);
        hospitalB = hospitalRepository.save(hospitalB);

        long ts = System.currentTimeMillis();

        // Admin A (Hospital A) — created directly to bypass public registration restrictions
        com.healthcare.backend.auth.entity.User adminUserA = createUserDirect(
                "Admin", "Alpha", "admin.alpha." + ts + "@hospital.com", "98" + (ts % 100000000L), "Pass@1234", hospitalA, RoleName.HOSPITAL_ADMIN
        );
        tokenAdminA = jwtService.generateToken(adminUserA);

        // Doctor User A (Hospital A)
        RegisterRequest regDocA = new RegisterRequest();
        regDocA.setFirstName("Rajesh");
        regDocA.setLastName("Sharma");
        regDocA.setEmail("doc.rajesh." + ts + "@hospital.com");
        regDocA.setMobileNumber("97" + (ts % 100000000L));
        regDocA.setPassword("Pass@1234");
        regDocA.setHospitalId(hospitalA.getId());
        regDocA.setRole(RoleName.DOCTOR);
        var resDocA = authService.register(regDocA);
        tokenDoctorA = resDocA.getAccessToken();
        doctorUserAId = resDocA.getUserId();

        // Admin B (Hospital B)
        com.healthcare.backend.auth.entity.User adminUserB = createUserDirect(
                "Admin", "Beta", "admin.beta." + ts + "@hospital.com", "96" + (ts % 100000000L), "Pass@1234", hospitalB, RoleName.HOSPITAL_ADMIN
        );
        tokenAdminB = jwtService.generateToken(adminUserB);

        // Doctor User B (Hospital B)
        RegisterRequest regDocB = new RegisterRequest();
        regDocB.setFirstName("Priya");
        regDocB.setLastName("Patel");
        regDocB.setEmail("doc.priya." + ts + "@hospital.com");
        regDocB.setMobileNumber("95" + (ts % 100000000L));
        regDocB.setPassword("Pass@1234");
        regDocB.setHospitalId(hospitalB.getId());
        regDocB.setRole(RoleName.DOCTOR);
        var resDocB = authService.register(regDocB);
        tokenDoctorB = resDocB.getAccessToken();
        doctorUserBId = resDocB.getUserId();

        // Receptionist A (Hospital A)
        RegisterRequest regRecepA = new RegisterRequest();
        regRecepA.setFirstName("Sam");
        regRecepA.setLastName("Recep");
        regRecepA.setEmail("recep.sam." + ts + "@hospital.com");
        regRecepA.setMobileNumber("94" + (ts % 100000000L));
        regRecepA.setPassword("Pass@1234");
        regRecepA.setHospitalId(hospitalA.getId());
        regRecepA.setRole(RoleName.RECEPTIONIST);
        tokenReceptionistA = authService.register(regRecepA).getAccessToken();
    }

    private com.healthcare.backend.auth.entity.User createUserDirect(
            String firstName, String lastName, String email, String mobile, String rawPassword, Hospital hospital, RoleName roleName
    ) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
        com.healthcare.backend.auth.entity.User user = new com.healthcare.backend.auth.entity.User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setHospital(hospital);
        user.setStatus(com.healthcare.backend.auth.entity.UserStatus.ACTIVE);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("1 & 2. Hospital admin creates doctor profile linked to DOCTOR user")
    void test1And2_createDoctorProfile_success() throws Exception {
        CreateDoctorRequest request = new CreateDoctorRequest();
        request.setUserId(doctorUserAId);
        request.setSpecialization("Cardiology");
        request.setQualification("MD, DM");
        request.setLicenseNumber("LIC-12345");
        request.setConsultationFee(new BigDecimal("800.00"));

        mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.doctorCode").value(startsWith(hospitalA.getCode())))
                .andExpect(jsonPath("$.userId").value(doctorUserAId))
                .andExpect(jsonPath("$.firstName").value("Rajesh"))
                .andExpect(jsonPath("$.lastName").value("Sharma"))
                .andExpect(jsonPath("$.specialization").value("Cardiology"))
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("3. Duplicate doctor profile is rejected (409 Conflict)")
    void test3_duplicateDoctorProfile_returns409() throws Exception {
        createDoctorProfileHelper(tokenAdminA, doctorUserAId, "Neurology", "LIC-33333");

        CreateDoctorRequest duplicateRequest = new CreateDoctorRequest();
        duplicateRequest.setUserId(doctorUserAId);
        duplicateRequest.setSpecialization("Pediatrics");
        duplicateRequest.setQualification("MBBS");
        duplicateRequest.setLicenseNumber("LIC-44444");
        duplicateRequest.setConsultationFee(new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("4 & 15. Doctor can retrieve own profile via /doctors/me")
    void test4And15_getDoctorMe_success() throws Exception {
        createDoctorProfileHelper(tokenAdminA, doctorUserAId, "Orthopedics", "LIC-55555");

        mockMvc.perform(get("/api/v1/doctors/me")
                        .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(doctorUserAId))
                .andExpect(jsonPath("$.specialization").value("Orthopedics"))
                .andExpect(jsonPath("$.firstName").value("Rajesh"));
    }

    @Test
    @DisplayName("5. Doctor / authorized user can access same-hospital doctor")
    void test5_getSameHospitalDoctor_success() throws Exception {
        Long docId = createDoctorProfileHelper(tokenAdminA, doctorUserAId, "Dermatology", "LIC-66666");

        mockMvc.perform(get("/api/v1/doctors/" + docId)
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId))
                .andExpect(jsonPath("$.specialization").value("Dermatology"));
    }

    @Test
    @DisplayName("6. Cross-hospital doctor access returns 404")
    void test6_crossHospitalDoctorAccess_returns404() throws Exception {
        Long docBId = createDoctorProfileHelper(tokenAdminB, doctorUserBId, "Oncology", "LIC-77777");

        // Admin A (Hospital A) attempts to access Doctor B (Hospital B)
        mockMvc.perform(get("/api/v1/doctors/" + docBId)
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("7. Hospital admin cannot assign another hospital's user")
    void test7_assignCrossHospitalUser_returns404() throws Exception {
        // Admin A attempts to create doctor profile using Doctor User B (belongs to Hospital B)
        CreateDoctorRequest request = new CreateDoctorRequest();
        request.setUserId(doctorUserBId);
        request.setSpecialization("Gynecology");
        request.setQualification("MS");
        request.setLicenseNumber("LIC-88888");
        request.setConsultationFee(new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8. Unauthenticated request returns 401 Unauthorized")
    void test8_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("9. Unauthorized role receives 403 Forbidden on doctor creation")
    void test9_unauthorizedRole_returns403() throws Exception {
        CreateDoctorRequest request = new CreateDoctorRequest();
        request.setUserId(doctorUserAId);
        request.setSpecialization("ENT");
        request.setQualification("MS");
        request.setLicenseNumber("LIC-99999");
        request.setConsultationFee(new BigDecimal("600.00"));

        // Receptionist attempts doctor creation
        mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", "Bearer " + tokenReceptionistA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Doctor listing supports pagination")
    void test10_getDoctors_pagination() throws Exception {
        createDoctorProfileHelper(tokenAdminA, doctorUserAId, "General Physician", "LIC-10001");

        mockMvc.perform(get("/api/v1/doctors?page=0&size=10&sort=createdAt,desc")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("11. Specialization filtering works")
    void test11_getDoctors_specializationFilter() throws Exception {
        createDoctorProfileHelper(tokenAdminA, doctorUserAId, "Pediatric Surgery", "LIC-11001");

        mockMvc.perform(get("/api/v1/doctors?specialization=Pediatric")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].specialization").value("Pediatric Surgery"));

        mockMvc.perform(get("/api/v1/doctors?specialization=NonExistentSpec")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("12. Doctor update works")
    void test12_updateDoctor_success() throws Exception {
        Long docId = createDoctorProfileHelper(tokenAdminA, doctorUserAId, "Urology", "LIC-12001");

        UpdateDoctorRequest updateRequest = new UpdateDoctorRequest();
        updateRequest.setSpecialization("Advanced Urology");
        updateRequest.setConsultationFee(new BigDecimal("1200.00"));
        updateRequest.setStatus(DoctorStatus.ON_LEAVE);

        mockMvc.perform(put("/api/v1/doctors/" + docId)
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId))
                .andExpect(jsonPath("$.specialization").value("Advanced Urology"))
                .andExpect(jsonPath("$.consultationFee").value(1200.00))
                .andExpect(jsonPath("$.status").value("ON_LEAVE"));
    }

    @Test
    @DisplayName("13 & 14. Doctor cannot change hospital or user identity through update")
    void test13And14_cannotChangeHospitalOrUser() throws Exception {
        Long docId = createDoctorProfileHelper(tokenAdminA, doctorUserAId, "Ophthalmology", "LIC-13001");

        UpdateDoctorRequest updateRequest = new UpdateDoctorRequest();
        updateRequest.setSpecialization("Retina Specialist");

        // Send parameters attempting to override hospitalId and userId
        mockMvc.perform(put("/api/v1/doctors/" + docId)
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .param("hospitalId", hospitalB.getId().toString())
                        .param("userId", doctorUserBId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()))
                .andExpect(jsonPath("$.userId").value(doctorUserAId));
    }

    private Long createDoctorProfileHelper(String adminToken, Long userId, String spec, String lic) throws Exception {
        CreateDoctorRequest request = new CreateDoctorRequest();
        request.setUserId(userId);
        request.setSpecialization(spec);
        request.setQualification("MBBS, MD");
        request.setLicenseNumber(lic);
        request.setConsultationFee(new BigDecimal("750.00"));

        MvcResult result = mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }
}
