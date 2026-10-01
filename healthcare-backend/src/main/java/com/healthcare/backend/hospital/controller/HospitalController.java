package com.healthcare.backend.hospital.controller;

import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.service.HospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    /**
     * TEMPORARY DEVELOPMENT ENDPOINT:
     * Publicly accessible endpoint to seed hospital data during initial setup.
     */
    @PostMapping
    public ResponseEntity<Hospital> createHospital(@Valid @RequestBody Hospital hospital) {
        Hospital created = hospitalService.createHospital(hospital);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hospital> getHospitalById(@PathVariable Long id) {
        Hospital hospital = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(hospital);
    }

    @GetMapping
    public ResponseEntity<List<Hospital>> getAllHospitals() {
        return ResponseEntity.ok(hospitalService.getAllHospitals());
    }
}
