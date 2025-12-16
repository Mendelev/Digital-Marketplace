# Digital Marketplace Platform — Functional Requirements, Development Plan, and Data Models

This document is a **functional-first** specification for a **Digital Marketplace** built as independent microservices, plus a **development plan** and **suggested data models** (high-level, not code-level).

Assumptions:
- Each service owns its database (**no shared DB**).
- Services work independently, but integrate via **API calls + events**.
- Payments are mocked but behave like a real provider (authorize/capture/refund).

---

## Platform scope

### Personas
- **Guest**: browse catalog, search, view product details.
- **Customer**: cart, checkout, orders, returns/cancellations, notifications.
- **Seller**: manage product listings & inventory (optional for MVP; can start as “admin catalog”).
- **Admin**: manage users, view reports, moderate products.

### Core user journeys
1. Browse/search products → product detail  
2. Add to cart → checkout → place order  
3. Payment authorized/captured → inventory reserved/committed  
4. Shipment created → tracking updates  
5. Customer receives notifications & sees order status  

---

## Microservices

Recommended services for MVP+:
1. Auth Service  
2. User Service  
3. Catalog Service  
4. Search Service  
5. Cart Service  
6. Order Service  
7. Payment Service  
8. Inventory Service  
9. Shipping Service  
10. Notification Service  
11. Audit/Event Service *(optional early; useful for tracing and compliance)*  
12. API Gateway *(edge routing; can be minimal early)*  

Start MVP with: **Auth, User, Catalog, Search (optional), Cart, Order, Payment (mock), Inventory, Notification**.

---

# Functional requirements (by service)

## 1) Auth Service
### Responsibilities
- User authentication and authorization.
- Token issuing/validation.
- Role management: `CUSTOMER`, `SELLER`, `ADMIN`.

### Functional requirements
- Register/login with email + password.
- Issue **JWT access token** (short-lived) + optional refresh token.
- Password reset flow (email token).
- Support service-to-service auth (client credentials / mTLS later, but define as a requirement).

### Data model (Auth DB)
- **Credential**
  - `credentialId`
  - `userId` (external reference to User Service)
  - `email` (unique)
  - `passwordHash`
  - `status` (ACTIVE/LOCKED)
  - `failedLoginCount`
  - `createdAt`, `updatedAt`
- **RefreshToken** *(if used)*
  - `tokenId`, `userId`, `tokenHash`, `expiresAt`, `revokedAt`

---

## 2) User Service
### Responsibilities
- Customer profile and addresses.
- Preferences (notification opts).

### Functional requirements
- Create/update user profile.
- Manage addresses (shipping/billing).
- View own profile.
- Admin can view/search users (basic).

### Data model
- **User**
  - `userId`
  - `email` (copy for convenience; source of truth still Auth)
  - `name`
  - `phone` (optional)
  - `roles` (CUSTOMER/SELLER/ADMIN)
  - `createdAt`, `updatedAt`
- **Address**
  - `addressId`
  - `userId`
  - `label` (Home/Work)
  - `country`, `state`, `city`, `zip`, `street`, `number`, `complement`
  - `isDefaultShipping`, `isDefaultBilling`

---

## 3) Catalog Service
### Responsibilities
- Product information: description, images, pricing, categories, attributes.
- Acts as the “source of truth” for product content.

### Functional requirements
- CRUD products (admin/seller).
- Product status lifecycle: `DRAFT`, `ACTIVE`, `INACTIVE`, `ARCHIVED`.
- Manage categories.
- Expose product listing + product detail.
- Price changes tracked (at least `updatedAt`; full history optional).

### Data model
- **Product**
  - `productId`
  - `sellerId` (optional for MVP)
  - `sku` (unique)
  - `title`
  - `description`
  - `categoryId`
  - `brand`
  - `status`
  - `priceAmount`, `currency`
  - `images[]` (URLs)
  - `attributes` (key/value: size, color, etc.)
  - `createdAt`, `updatedAt`
- **Category**
  - `categoryId`, `name`, `parentCategoryId?`
- **ProductPriceHistory** *(optional)*
  - `productId`, `priceAmount`, `currency`, `changedAt`

**Events emitted**
- `ProductCreated`, `ProductUpdated`, `ProductActivated`, `ProductDeactivated`, `PriceChanged`

---

## 4) Search Service
### Responsibilities
- Provide fast search / filtering / sorting.
- Built from Catalog events (event-driven indexing).

### Functional requirements
- Search by keyword across title/description.
- Filter by category, price range, attributes.
- Sort by relevance / price / newest.
- Return product IDs + summary fields.

### Data model
- Search index document (not relational):
  - `productId`, `title`, `description`, `category`, `price`, `attributes`, `status`

**Consumes**
- Catalog events to update index.

---

## 5) Cart Service
### Responsibilities
- Customer shopping cart (temporary, mutable).
- Calculates preliminary totals (final totals confirmed at Order).

### Functional requirements
- One active cart per user.
- Add/remove/update item quantity.
- Validate product is ACTIVE (via Catalog read or cached).
- Compute subtotal; taxes/shipping can be “estimated” or deferred.

### Data model
- **Cart**
  - `cartId`
  - `userId`
  - `status` (ACTIVE, CHECKED_OUT, ABANDONED)
  - `currency`
  - `createdAt`, `updatedAt`
- **CartItem**
  - `cartItemId`
  - `cartId`
  - `productId`
  - `sku`
  - `titleSnapshot`
  - `unitPriceSnapshot`, `currency`
  - `quantity`

---

## 6) Order Service
### Responsibilities
- Order lifecycle orchestrator.
- Owns the canonical “order state machine”.
- Coordinates payment, inventory, and shipping.

### Functional requirements
- Create order from cart:
  - freeze snapshots (product, price, address)
  - compute totals (subtotal, shipping, discounts, taxes if any)
- Order states (example):
  - `PENDING_PAYMENT`
  - `PAYMENT_AUTHORIZED`
  - `PAYMENT_FAILED`
  - `INVENTORY_RESERVED`
  - `CONFIRMED`
  - `SHIPPED`
  - `DELIVERED`
  - `CANCELLED`
  - `REFUNDED`
- Cancel order rules:
  - allowed before shipping; triggers inventory release + refund/void
- View orders (customer) + list orders (admin)

### Data model
- **Order**
  - `orderId`
  - `userId`
  - `status`
  - `currency`
  - `subtotalAmount`
  - `shippingAmount`
  - `taxAmount` *(optional)*
  - `discountAmount` *(optional)*
  - `totalAmount`
  - `paymentId` (from Payment Service)
  - `shippingAddressSnapshot` (embedded object)
  - `billingAddressSnapshot` (embedded object)
  - `createdAt`, `updatedAt`
- **OrderItem**
  - `orderItemId`
  - `orderId`
  - `productId`
  - `sku`
  - `titleSnapshot`
  - `unitPriceSnapshot`
  - `quantity`
  - `lineTotalAmount`

**Events emitted**
- `OrderCreated`, `OrderCancelled`, `OrderConfirmed`, `OrderShipped`, `OrderDelivered`, `OrderRefunded`

---

## 7) Payment Service (Mock gateway)
### Responsibilities
- Payment authorization/capture/refund simulation.
- Stores payment transactions and status.

### Functional requirements
- Create a payment intent for an order.
- Authorize payment (simulate success/failure, configurable).
- Capture payment.
- Refund payment (full/partial optional).
- Expose payment status query.

### Data model
- **Payment**
  - `paymentId`
  - `orderId`
  - `userId`
  - `status` (INITIATED, AUTHORIZED, CAPTURED, FAILED, REFUNDED, VOIDED)
  - `amount`, `currency`
  - `provider` (MOCK)
  - `createdAt`, `updatedAt`
- **PaymentTransaction**
  - `transactionId`
  - `paymentId`
  - `type` (AUTHORIZE, CAPTURE, REFUND, VOID)
  - `status`
  - `providerReference`
  - `createdAt`

**Events emitted**
- `PaymentAuthorized`, `PaymentFailed`, `PaymentCaptured`, `PaymentRefunded`

---

## 8) Inventory Service
### Responsibilities
- Stock management per SKU/product.
- Reservation to prevent overselling.

### Functional requirements
- Maintain available quantity per SKU.
- Reserve stock for an order (with TTL / expiration).
- Confirm reservation on successful capture.
- Release reservation on cancellation/payment failure.

### Data model
- **StockItem**
  - `sku`
  - `productId`
  - `availableQty`
  - `reservedQty`
  - `updatedAt`
- **Reservation**
  - `reservationId`
  - `orderId`
  - `status` (ACTIVE, CONFIRMED, RELEASED, EXPIRED)
  - `expiresAt`
  - `createdAt`
- **ReservationLine**
  - `reservationLineId`
  - `reservationId`
  - `sku`
  - `quantity`

**Events**
- `StockReserved`, `StockReservationFailed`, `StockConfirmed`, `StockReleased`, `StockExpired`

---

## 9) Shipping Service
### Responsibilities
- Shipment creation and tracking updates.

### Functional requirements
- Create shipment when order confirmed.
- Update shipment status: `CREATED`, `IN_TRANSIT`, `DELIVERED`, `RETURNED`.
- Provide tracking info to Order Service.
- Shipping fee calculation can be basic (flat rate) initially.

### Data model
- **Shipment**
  - `shipmentId`
  - `orderId`
  - `status`
  - `carrier` (MOCK)
  - `trackingNumber`
  - `shippingAddressSnapshot`
  - `createdAt`, `updatedAt`
- **ShipmentEvent**
  - `shipmentEventId`
  - `shipmentId`
  - `status`
  - `message`
  - `occurredAt`

**Events**
- `ShipmentCreated`, `ShipmentUpdated`, `ShipmentDelivered`

---

## 10) Notification Service
### Responsibilities
- Send email/SMS/webhook notifications.
- Uses events from other services.

### Functional requirements
- Notify on: registration, order created, payment failure, order confirmed, shipped, delivered, cancelled, refunded.
- Support user preferences (email on/off).
- Store delivery attempts and status.

### Data model
- **Notification**
  - `notificationId`
  - `userId`
  - `type` (ORDER_CONFIRMED, SHIPPED, etc.)
  - `channel` (EMAIL, SMS, WEBHOOK)
  - `payload` (json)
  - `status` (PENDING, SENT, FAILED)
  - `attempts`
  - `createdAt`, `updatedAt`

---

## 11) Audit/Event Service *(optional but valuable)*
### Responsibilities
- Store immutable event log / audit trail.
- Enables replay, debugging, compliance, analytics.

### Functional requirements
- Ingest events from all services.
- Query audit by user/order/time range.
- Correlation IDs for tracing.

### Data model
- **DomainEvent**
  - `eventId`
  - `eventType`
  - `aggregateType` (ORDER, PAYMENT, PRODUCT…)
  - `aggregateId`
  - `payload`
  - `correlationId`
  - `occurredAt`

---

# Cross-service functional contracts

## Events and flow (happy path)
1. Customer checks out → `OrderCreated`
2. Order Service requests:
   - Payment authorize → emits `PaymentAuthorized`
   - Inventory reserve → emits `StockReserved`
3. When both succeed:
   - Payment capture → `PaymentCaptured`
   - Inventory confirm → `StockConfirmed`
   - Create shipment → `ShipmentCreated`
   - Order becomes `CONFIRMED`
4. Shipping updates → Order becomes `SHIPPED` / `DELIVERED`
5. Notifications subscribe to all key events

## Failure scenarios (must be supported)
- Payment fails after order created → order `PAYMENT_FAILED`, release inventory if reserved
- Inventory reservation fails → void payment authorization, order `CANCELLED` (or `FAILED`)
- Shipment creation fails → order stays `CONFIRMED` but “shipping pending”, retry async
- Duplicate events → all consumers must be **idempotent** *(functional requirement)*

---

# Data ownership rules
- Catalog owns product truth; other services store **snapshots** (title/price at purchase time).
- Orders own order truth; Shipping/Payment/Inventory reference `orderId`.
- User profile data in User Service; Auth owns credentials.
- No service updates another service’s DB directly.

---

# Development plan (functional-first, incremental)

## Phase 0 — Foundation
- Define IDs and naming conventions (`userId`, `orderId`, etc.)
- Define event schemas (minimal JSON shape per event)
- Decide MVP state machines (Order/Payment/Shipment)

**Deliverable**: “contracts” document + simple API specs per service.

---

## Phase 1 — Core commerce MVP (end-to-end)
**Services**: Auth, User, Catalog, Cart, Order, Payment(mock), Inventory, Notification (basic)

### Milestones
1. **Auth + User**
   - Register/login
   - Profile + address management

2. **Catalog**
   - Admin can create products
   - Customer can list & view details

3. **Cart**
   - Add/update/remove items
   - Cart shows subtotal

4. **Order + Payment + Inventory**
   - Checkout creates order (snapshots)
   - Payment authorize/capture simulated
   - Inventory reserve/confirm/release
   - Order status updates visible

5. **Notification**
   - Email/log notifications on key events

**Outcome**: Customer can register → add to cart → checkout → see confirmed order.

---

## Phase 2 — Search + Shipping
**Add**: Search, Shipping

### Milestones
- Search indexes products from Catalog events
- Search endpoint used by UI
- Shipment created after order confirmed
- Tracking events update order status

**Outcome**: Discoverability + “delivered” lifecycle.

---

## Phase 3 — Operational realism (functional features that force DevOps practice)
- **Idempotency keys** for:
  - payment operations
  - order creation
  - inventory reservation
- **Retry workflows**:
  - shipping creation retry
  - notification resend
- **Rate limiting** at gateway (login, search)
- **Admin views**:
  - list orders, filter by status
  - inventory dashboard (stock low alerts)

---

## Phase 4 — Marketplace extensions (optional)
- Seller onboarding (Seller role)
- Multi-seller catalog management
- Promotions/discount service
- Returns management
- Reviews/ratings service

---

# Data model map (quick view)

- Auth: Credential, RefreshToken  
- User: User, Address  
- Catalog: Product, Category, PriceHistory  
- Cart: Cart, CartItem (snapshots)  
- Order: Order, OrderItem (snapshots)  
- Payment: Payment, PaymentTransaction  
- Inventory: StockItem, Reservation, ReservationLine  
- Shipping: Shipment, ShipmentEvent  
- Notification: Notification  
- Audit: DomainEvent  

