package com.healthcare.backend.hospital.service;

import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    @Transactional
    public Hospital createHospital(Hospital hospital) {
        if (hospitalRepository.existsByCode(hospital.getCode())) {
            throw new IllegalStateException("Hospital with code '" + hospital.getCode() + "' already exists");
        }
        return hospitalRepository.save(hospital);
    }

    @Transactional(readOnly = true)
    public Hospital getHospitalById(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Hospital not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }
}
