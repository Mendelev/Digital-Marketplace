package com.marketplace.user.dto.address;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing address.
 */
@Schema(description = "Request to update an existing address")
public record UpdateAddressRequest(
        @NotBlank(message = "Label is required")
        @Size(max = 50, message = "Label must not exceed 50 characters")
        @Schema(description = "Address label (e.g., Home, Work)", example = "Home", requiredMode = Schema.RequiredMode.REQUIRED)
        String label,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        @Schema(description = "Country", example = "United States", requiredMode = Schema.RequiredMode.REQUIRED)
        String country,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must not exceed 100 characters")
        @Schema(description = "State/Province", example = "California", requiredMode = Schema.RequiredMode.REQUIRED)
        String state,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        @Schema(description = "City", example = "San Francisco", requiredMode = Schema.RequiredMode.REQUIRED)
        String city,

        @NotBlank(message = "ZIP code is required")
        @Size(max = 20, message = "ZIP code must not exceed 20 characters")
        @Schema(description = "ZIP/Postal code", example = "94102", requiredMode = Schema.RequiredMode.REQUIRED)
        String zip,

        @NotBlank(message = "Street is required")
        @Size(max = 200, message = "Street must not exceed 200 characters")
        @Schema(description = "Street name", example = "Market Street", requiredMode = Schema.RequiredMode.REQUIRED)
        String street,

        @NotBlank(message = "Number is required")
        @Size(max = 20, message = "Number must not exceed 20 characters")
        @Schema(description = "Street number", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        String number,

        @Size(max = 200, message = "Complement must not exceed 200 characters")
        @Schema(description = "Complement (apartment, suite, etc.)", example = "Apt 4B")
        String complement
) {
}
