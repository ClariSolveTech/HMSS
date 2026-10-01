package com.healthcare.backend.billing.dto;

import com.healthcare.backend.billing.entity.InvoiceItem;
import com.healthcare.backend.billing.entity.InvoiceItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemResponse {

    private Long id;
    private InvoiceItemType itemType;
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal lineTotal;

    public static InvoiceItemResponse fromEntity(InvoiceItem item) {
        if (item == null) return null;
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discount(item.getDiscount())
                .tax(item.getTax())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
