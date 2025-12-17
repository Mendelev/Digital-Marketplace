package com.marketplace.user.dto.address;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for Address.
 */
@Schema(description = "Address response")
public record AddressResponse(
        @Schema(description = "Address ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID addressId,

        @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID userId,

        @Schema(description = "Address label (e.g., Home, Work)", example = "Home")
        String label,

        @Schema(description = "Country", example = "United States")
        String country,

        @Schema(description = "State/Province", example = "California")
        String state,

        @Schema(description = "City", example = "San Francisco")
        String city,

        @Schema(description = "ZIP/Postal code", example = "94102")
        String zip,

        @Schema(description = "Street name", example = "Market Street")
        String street,

        @Schema(description = "Street number", example = "123")
        String number,

        @Schema(description = "Complement (apartment, suite, etc.)", example = "Apt 4B")
        String complement,

        @Schema(description = "Whether this is the default shipping address")
        Boolean isDefaultShipping,

        @Schema(description = "Whether this is the default billing address")
        Boolean isDefaultBilling,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        @Schema(example = "2025-12-16T18:45:30.123+00:00", description = "Timestamp in UTC with timezone offset")
        OffsetDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        @Schema(example = "2025-12-16T18:45:30.123+00:00", description = "Timestamp in UTC with timezone offset")
        OffsetDateTime updatedAt
) {
}
