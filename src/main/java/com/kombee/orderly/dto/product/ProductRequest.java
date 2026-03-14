package com.kombee.orderly.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(min = 1, max = 50)
    private String sku;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @DecimalMin(value = "0.0", message = "Price must be >= 0")
    private BigDecimal price;

    private Integer stockQuantity;
}
