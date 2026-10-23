package com.healthcare.backend.config;

import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.queue.entity.DoctorQueueStatus;
import com.healthcare.backend.queue.entity.QueueEntry;
import com.healthcare.backend.queue.entity.QueuePriority;
import com.healthcare.backend.queue.entity.QueueStatus;
import com.healthcare.backend.queue.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Seed Roles
        Role doctorRole = seedRole(RoleName.DOCTOR);
        seedRole(RoleName.HOSPITAL_ADMIN);
        seedRole(RoleName.PLATFORM_ADMIN);
        Role receptionistRole = seedRole(RoleName.RECEPTIONIST);
        seedRole(RoleName.LAB_STAFF);
        seedRole(RoleName.PHARMACY_STAFF);
        seedRole(RoleName.PATIENT);

        // Seed Hospital
        Hospital hospital = hospitalRepository.findByCode("CCH001")
                .orElseGet(() -> {
                    Hospital h = new Hospital();
                    h.setName("City Central Hospital");
                    h.setCode("CCH001");
                    h.setPhone("080-12345678");
                    h.setEmail("contact@citycentral.com");
                    h.setAddress("123 Healthcare Boulevard, Tech City");
                    h.setStatus(HospitalStatus.ACTIVE);
                    return hospitalRepository.save(h);
                });

        // Seed Doctor User & Ensure Development Password Hash
        String doctorEmail = "dr.aarav@citycentral.com";
        User doctorUser = userRepository.findByEmail(doctorEmail)
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("Aarav");
                    u.setLastName("Rao");
                    u.setEmail(doctorEmail);
                    u.setMobileNumber("9876543210");
                    u.setHospital(hospital);
                    u.setRoles(Set.of(doctorRole));
                    u.setStatus(UserStatus.ACTIVE);
                    return u;
                });
        doctorUser.setPasswordHash(passwordEncoder.encode("Doctor@123"));
        User savedUser = userRepository.save(doctorUser);

        // Seed Receptionist User & Ensure Development Password Hash
        String recepEmail = "receptionist@citycentral.com";
        User recepUser = userRepository.findByEmail(recepEmail)
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("Maya");
                    u.setLastName("Sharma");
                    u.setEmail(recepEmail);
                    u.setMobileNumber("9876543220");
                    u.setHospital(hospital);
                    u.setRoles(Set.of(receptionistRole));
                    u.setStatus(UserStatus.ACTIVE);
                    return u;
                });
        recepUser.setPasswordHash(passwordEncoder.encode("Recep@123"));
        userRepository.save(recepUser);

        // Seed Doctor Profile & Synchronize User / Hospital Relationship
        Doctor doctor = doctorRepository.findByUserId(savedUser.getId())
                .orElseGet(() -> {
                    Doctor d = new Doctor();
                    d.setDoctorCode("DOC-AARAV-01");
                    d.setSpecialization("General Medicine");
                    d.setQualification("MBBS, MD");
                    d.setLicenseNumber("MED-LIC-998877");
                    d.setConsultationFee(BigDecimal.valueOf(500.0));
                    d.setStatus(DoctorStatus.ACTIVE);
                    d.setQueueStatus(DoctorQueueStatus.ARRIVED);
                    return d;
                });
        doctor.setUser(savedUser);
        doctor.setHospital(savedUser.getHospital());
        Doctor savedDoctor = doctorRepository.save(doctor);

        // Seed Patients
        Patient p1 = seedPatient("Ramesh", "Kumar", "PAT-101", "9000000001", hospital);
        Patient p2 = seedPatient("Priya", "Sharma", "PAT-102", "9000000002", hospital);
        Patient p3 = seedPatient("Amit", "Patel", "PAT-103", "9000000003", hospital);

        // Seed Queue Entries for today if none exist
        LocalDate today = LocalDate.now();
        if (queueEntryRepository.count() == 0) {
            seedQueueEntry(hospital, savedDoctor, p1, 1, QueuePriority.NORMAL, QueueStatus.WAITING, today);
            seedQueueEntry(hospital, savedDoctor, p2, 2, QueuePriority.URGENT, QueueStatus.WAITING, today);
            seedQueueEntry(hospital, savedDoctor, p3, 3, QueuePriority.EMERGENCY, QueueStatus.WAITING, today);
            log.info("Successfully seeded queue entries for Dr. Aarav Rao");
        }
    }

    private Role seedRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private Patient seedPatient(String firstName, String lastName, String code, String mobile, Hospital hospital) {
        return patientRepository.findByPatientCodeAndHospitalId(code, hospital.getId())
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setFirstName(firstName);
                    p.setLastName(lastName);
                    p.setPatientCode(code);
                    p.setMobileNumber(mobile);
                    p.setDateOfBirth(LocalDate.of(1988, 5, 15));
                    p.setGender(Gender.MALE);
                    p.setHospital(hospital);
                    return patientRepository.save(p);
                });
    }

    private void seedQueueEntry(Hospital h, Doctor d, Patient p, int token, QueuePriority priority, QueueStatus status, LocalDate date) {
        QueueEntry entry = new QueueEntry();
        entry.setHospital(h);
        entry.setDoctor(d);
        entry.setPatient(p);
        entry.setTokenNumber(token);
        entry.setQueueDate(date);
        entry.setPriority(priority);
        entry.setStatus(status);
        entry.setCheckedInAt(LocalDateTime.now().minusMinutes(30 - (token * 10)));
        entry.setEstimatedWaitMinutes(token * 10);
        entry.setActiveQueueKey(QueueEntry.buildActiveQueueKey(h.getId(), d.getId(), p.getId(), date));
        queueEntryRepository.save(entry);
    }
}
