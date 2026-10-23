package com.healthcare.backend.patientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDashboardSummaryResponse {
    private Long patientId;
    private String patientCode;
    private String firstName;
    private String lastName;
    private String bloodGroup;
    private String gender;
    private LocalDate dateOfBirth;
    private Long hospitalId;
    private String hospitalName;

    private long upcomingAppointmentsCount;
    private long completedConsultationsCount;
    private long activePrescriptionsCount;
    private long labOrdersCount;

    private PatientAppointmentSummary nextAppointment;
    private List<PatientAppointmentSummary> recentAppointments;
}
