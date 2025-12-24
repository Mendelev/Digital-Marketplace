package com.marketplace.shared.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for address snapshot in order.
 */
@Schema(description = "Address snapshot frozen at time of order creation")
public record AddressSnapshotResponse(
        @Schema(description = "Address label", example = "Home", nullable = true)
        String label,

        @Schema(description = "Country", example = "Brazil")
        String country,

        @Schema(description = "State/Province", example = "São Paulo")
        String state,

        @Schema(description = "City", example = "São Paulo")
        String city,

        @Schema(description = "Postal/ZIP code", example = "01310-100")
        String zip,

        @Schema(description = "Street name", example = "Avenida Paulista")
        String street,

        @Schema(description = "Street number", example = "1578")
        String number,

        @Schema(description = "Complement (apartment, suite, etc.)", example = "Apt 42", nullable = true)
        String complement
) {
}
