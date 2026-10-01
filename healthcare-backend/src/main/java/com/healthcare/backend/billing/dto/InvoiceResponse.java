package com.healthcare.backend.billing.dto;

import com.healthcare.backend.billing.entity.Invoice;
import com.healthcare.backend.billing.entity.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long hospitalId;
    private Long patientId;
    private Long doctorId;
    private Long appointmentId;
    private Long consultationId;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private InvoiceStatus status;
    private String notes;
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<InvoiceItemResponse> items = new ArrayList<>();

    @Builder.Default
    private List<PaymentResponse> payments = new ArrayList<>();

    public static InvoiceResponse fromEntity(Invoice invoice) {
        if (invoice == null) return null;

        List<InvoiceItemResponse> itemResponses = invoice.getItems() != null
                ? invoice.getItems().stream().map(InvoiceItemResponse::fromEntity).collect(Collectors.toList())
                : new ArrayList<>();

        List<PaymentResponse> paymentResponses = invoice.getPayments() != null
                ? invoice.getPayments().stream().map(PaymentResponse::fromEntity).collect(Collectors.toList())
                : new ArrayList<>();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .hospitalId(invoice.getHospital() != null ? invoice.getHospital().getId() : null)
                .patientId(invoice.getPatient() != null ? invoice.getPatient().getId() : null)
                .doctorId(invoice.getDoctor() != null ? invoice.getDoctor().getId() : null)
                .appointmentId(invoice.getAppointment() != null ? invoice.getAppointment().getId() : null)
                .consultationId(invoice.getConsultation() != null ? invoice.getConsultation().getId() : null)
                .subtotal(invoice.getSubtotal())
                .discount(invoice.getDiscount())
                .tax(invoice.getTax())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .dueAmount(invoice.getDueAmount())
                .status(invoice.getStatus())
                .notes(invoice.getNotes())
                .issuedAt(invoice.getIssuedAt())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(itemResponses)
                .payments(paymentResponses)
                .build();
    }
}
