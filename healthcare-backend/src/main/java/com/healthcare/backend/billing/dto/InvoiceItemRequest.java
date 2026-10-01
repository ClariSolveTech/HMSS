package com.healthcare.backend.billing.dto;

import com.healthcare.backend.billing.entity.InvoiceItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceItemRequest {

    @NotNull(message = "Item type is required")
    private InvoiceItemType itemType;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00", message = "Discount must be non-negative")
    private BigDecimal discount = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Tax must be non-negative")
    private BigDecimal tax = BigDecimal.ZERO;
}
