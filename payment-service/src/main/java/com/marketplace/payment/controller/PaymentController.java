package com.marketplace.payment.controller;

import com.marketplace.payment.config.AuthServiceProperties;
import com.marketplace.payment.exception.InvalidTokenException;
import com.marketplace.payment.filter.JwtValidationFilter;
import com.marketplace.payment.security.AuthenticatedUser;
import com.marketplace.payment.service.PaymentService;
import com.marketplace.shared.dto.payment.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for payment operations.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Management", description = "APIs for managing payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final String SERVICE_SECRET_HEADER = "X-Service-Secret";

    private final PaymentService paymentService;
    private final AuthServiceProperties authServiceProperties;

    public PaymentController(PaymentService paymentService, AuthServiceProperties authServiceProperties) {
        this.paymentService = paymentService;
        this.authServiceProperties = authServiceProperties;
    }

    @PostMapping
    @Operation(summary = "Create payment", description = "Create a new payment intent (service-to-service only)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment created successfully"),
        @ApiResponse(responseCode = "403", description = "Invalid shared secret"),
        @ApiResponse(responseCode = "409", description = "Payment already exists for order")
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret,
            @Valid @RequestBody CreatePaymentRequest request) {

        // Service-to-service authentication
        validateServiceSecret(sharedSecret);

        log.info("Creating payment for order: {}", request.orderId());
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{paymentId}/authorize")
    @Operation(summary = "Authorize payment", description = "Authorize payment (service-to-service only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment authorized successfully"),
        @ApiResponse(responseCode = "402", description = "Payment authorization failed"),
        @ApiResponse(responseCode = "403", description = "Invalid shared secret"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "400", description = "Invalid payment state")
    })
    public ResponseEntity<PaymentResponse> authorizePayment(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret,
            @PathVariable UUID paymentId,
            @RequestBody(required = false) AuthorizePaymentRequest request) {

        // Service-to-service authentication
        validateServiceSecret(sharedSecret);

        if (request == null) {
            request = new AuthorizePaymentRequest(null);
        }

        log.info("Authorizing payment: {}", paymentId);
        PaymentResponse response = paymentService.authorizePayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/capture")
    @Operation(summary = "Capture payment", description = "Capture authorized payment (service-to-service only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment captured successfully"),
        @ApiResponse(responseCode = "402", description = "Payment capture failed"),
        @ApiResponse(responseCode = "403", description = "Invalid shared secret"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "400", description = "Invalid payment state or amount")
    })
    public ResponseEntity<PaymentResponse> capturePayment(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret,
            @PathVariable UUID paymentId,
            @Valid @RequestBody CapturePaymentRequest request) {

        // Service-to-service authentication
        validateServiceSecret(sharedSecret);

        log.info("Capturing payment: {} amount: {}", paymentId, request.amount());
        PaymentResponse response = paymentService.capturePayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund payment", description = "Refund captured payment (service-to-service or admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment refunded successfully"),
        @ApiResponse(responseCode = "402", description = "Payment refund failed"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "400", description = "Invalid payment state or amount")
    })
    public ResponseEntity<PaymentResponse> refundPayment(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret,
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundPaymentRequest request,
            HttpServletRequest httpRequest) {

        // Allow service-to-service OR admin users
        if (sharedSecret != null && sharedSecret.equals(authServiceProperties.sharedSecret())) {
            log.info("Service-to-service refund request for payment: {}", paymentId);
        } else {
            AuthenticatedUser user = getAuthenticatedUser(httpRequest);
            if (!user.isAdmin()) {
                throw new InvalidTokenException("Only admin users can refund payments");
            }
            log.info("Admin refund request for payment: {} by user: {}", paymentId, user.getUserId());
        }

        PaymentResponse response = paymentService.refundPayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/void")
    @Operation(summary = "Void payment", description = "Void payment authorization (service-to-service only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment voided successfully"),
        @ApiResponse(responseCode = "402", description = "Payment void failed"),
        @ApiResponse(responseCode = "403", description = "Invalid shared secret"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "400", description = "Invalid payment state")
    })
    public ResponseEntity<PaymentResponse> voidPayment(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret,
            @PathVariable UUID paymentId,
            @RequestBody(required = false) VoidPaymentRequest request) {

        // Service-to-service authentication
        validateServiceSecret(sharedSecret);

        if (request == null) {
            request = new VoidPaymentRequest(null);
        }

        log.info("Voiding payment: {}", paymentId);
        PaymentResponse response = paymentService.voidPayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment", description = "Get payment details by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId,
            HttpServletRequest request) {

        // Require authenticated user
        AuthenticatedUser user = getAuthenticatedUser(request);

        PaymentResponse response = paymentService.getPayment(paymentId);

        // Verify user owns the payment or is admin
        if (!response.userId().equals(user.getUserId()) && !user.isAdmin()) {
            throw new InvalidTokenException("You do not have access to this payment");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID", description = "Get payment for an order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PaymentResponse> getPaymentByOrder(
            @PathVariable UUID orderId,
            HttpServletRequest request) {

        // Require authenticated user
        AuthenticatedUser user = getAuthenticatedUser(request);

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);

        // Verify user owns the payment or is admin
        if (!response.userId().equals(user.getUserId()) && !user.isAdmin()) {
            throw new InvalidTokenException("You do not have access to this payment");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user payments", description = "Get all payments for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payments retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<PaymentResponse>> getUserPayments(
            @PathVariable UUID userId,
            HttpServletRequest request) {

        // Require authenticated user
        AuthenticatedUser user = getAuthenticatedUser(request);

        // Verify user is requesting their own payments or is admin
        if (!userId.equals(user.getUserId()) && !user.isAdmin()) {
            throw new InvalidTokenException("You can only view your own payments");
        }

        List<PaymentResponse> response = paymentService.getPaymentsByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}/transactions")
    @Operation(summary = "Get payment transactions", description = "Get transaction history for a payment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transactions retrieved"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<PaymentTransactionResponse>> getPaymentTransactions(
            @PathVariable UUID paymentId,
            HttpServletRequest request) {

        // Require authenticated user
        AuthenticatedUser user = getAuthenticatedUser(request);

        // First get payment to verify ownership
        PaymentResponse payment = paymentService.getPayment(paymentId);

        // Verify user owns the payment or is admin
        if (!payment.userId().equals(user.getUserId()) && !user.isAdmin()) {
            throw new InvalidTokenException("You do not have access to this payment");
        }

        List<PaymentTransactionResponse> response = paymentService.getPaymentTransactions(paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate service-to-service shared secret.
     */
    private void validateServiceSecret(String sharedSecret) {
        if (sharedSecret == null || !sharedSecret.equals(authServiceProperties.sharedSecret())) {
            log.warn("Invalid or missing shared secret for service-to-service request");
            throw new InvalidTokenException("Invalid service authentication");
        }
    }

    /**
     * Get authenticated user from request or throw exception.
     */
    private AuthenticatedUser getAuthenticatedUser(HttpServletRequest request) {
        AuthenticatedUser user = (AuthenticatedUser) request.getAttribute(
            JwtValidationFilter.AUTHENTICATED_USER_ATTRIBUTE);

        if (user == null) {
            throw new InvalidTokenException("Authentication required");
        }

        return user;
    }
}
