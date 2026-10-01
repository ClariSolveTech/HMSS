package com.healthcare.backend.lab.service;

import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.lab.dto.CreateLabTestRequest;
import com.healthcare.backend.lab.dto.LabTestResponse;
import com.healthcare.backend.lab.dto.UpdateLabTestRequest;
import com.healthcare.backend.lab.entity.LabTest;
import com.healthcare.backend.lab.repository.LabTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabTestService {

    private final LabTestRepository labTestRepository;
    private final HospitalRepository hospitalRepository;

    public List<LabTestResponse> getLabTests(Boolean activeOnly) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        List<LabTest> tests;
        if (Boolean.TRUE.equals(activeOnly)) {
            tests = labTestRepository.findByHospitalIdAndActive(hospitalId, true);
        } else {
            tests = labTestRepository.findByHospitalId(hospitalId);
        }
        return tests.stream().map(LabTestResponse::fromEntity).collect(Collectors.toList());
    }

    public Page<LabTestResponse> getLabTestsPage(Boolean activeOnly, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Page<LabTest> tests = labTestRepository.findByHospitalId(hospitalId, pageable);
        return tests.map(LabTestResponse::fromEntity);
    }

    public LabTestResponse getLabTestById(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LabTest test = labTestRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab test not found in your hospital with ID: " + id));
        return LabTestResponse.fromEntity(test);
    }

    @Transactional
    public LabTestResponse createLabTest(CreateLabTestRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Hospital not found with ID: " + hospitalId));

        if (labTestRepository.existsByTestCodeAndHospitalId(request.getTestCode(), hospitalId)) {
            throw new IllegalStateException("Lab test with code '" + request.getTestCode() + "' already exists in your hospital");
        }

        LabTest test = new LabTest();
        test.setHospital(hospital);
        test.setTestCode(request.getTestCode());
        test.setTestName(request.getTestName());
        test.setDescription(request.getDescription());
        test.setCategory(request.getCategory());
        test.setSampleType(request.getSampleType());
        test.setUnit(request.getUnit());
        test.setReferenceRange(request.getReferenceRange());
        test.setActive(true);

        LabTest saved = labTestRepository.save(test);
        return LabTestResponse.fromEntity(saved);
    }

    @Transactional
    public LabTestResponse updateLabTest(Long id, UpdateLabTestRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LabTest test = labTestRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab test not found in your hospital with ID: " + id));

        test.setTestName(request.getTestName());
        test.setDescription(request.getDescription());
        test.setCategory(request.getCategory());
        test.setSampleType(request.getSampleType());
        test.setUnit(request.getUnit());
        test.setReferenceRange(request.getReferenceRange());
        if (request.getActive() != null) {
            test.setActive(request.getActive());
        }

        LabTest saved = labTestRepository.save(test);
        return LabTestResponse.fromEntity(saved);
    }

    @Transactional
    public LabTestResponse toggleLabTestStatus(Long id, boolean active) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LabTest test = labTestRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Lab test not found in your hospital with ID: " + id));

        test.setActive(active);
        LabTest saved = labTestRepository.save(test);
        return LabTestResponse.fromEntity(saved);
    }
}
