#!/usr/bin/env python3
import json
import os
import sys
import time
import uuid
import urllib.error
import urllib.request


DEFAULTS = {
    "AUTH_BASE_URL": "http://localhost:8080",
    "USER_BASE_URL": "http://localhost:8081",
    "CATALOG_BASE_URL": "http://localhost:8082",
    "CART_BASE_URL": "http://localhost:8083",
    "ORDER_BASE_URL": "http://localhost:8086",
    "PAYMENT_BASE_URL": "http://localhost:8087",
    "INVENTORY_BASE_URL": "http://localhost:8088",
    "SHIPPING_BASE_URL": "http://localhost:8089",
    "SERVICE_SECRET": "dev-secret-change-in-production",
}


class RequestError(Exception):
    pass


class TestFailure(Exception):
    pass


class SkipTest(Exception):
    pass


class TestRunner:
    def __init__(self):
        self.results = []

    def run(self, name, func):
        try:
            detail = func() or ""
            self.results.append(("ok", name, detail))
        except SkipTest as exc:
            self.results.append(("skip", name, str(exc)))
        except TestFailure as exc:
            self.results.append(("fail", name, str(exc)))
        except Exception as exc:
            self.results.append(("fail", name, f"unexpected error: {exc}"))

    def summary(self):
        counts = {"ok": 0, "fail": 0, "skip": 0}
        for status, _, _ in self.results:
            counts[status] += 1
        return counts

    def print_results(self):
        for status, name, detail in self.results:
            if detail:
                print(f"[{status.upper()}] {name}: {detail}")
            else:
                print(f"[{status.upper()}] {name}")
        counts = self.summary()
        print(
            f"\nTotals: {counts['ok']} ok, {counts['fail']} failed, {counts['skip']} skipped"
        )


def env(name):
    return os.environ.get(name, DEFAULTS.get(name, ""))


def build_headers(extra=None):
    headers = {"Accept": "application/json"}
    if extra:
        headers.update(extra)
    return headers


def parse_body(body, headers):
    if not body:
        return None
    content_type = headers.get("Content-Type", "")
    if "application/json" in content_type or body.startswith("{") or body.startswith("["):
        try:
            return json.loads(body)
        except json.JSONDecodeError:
            return body
    return body


def http_request(method, url, headers=None, json_body=None, timeout=10):
    data = None
    req_headers = headers or {}
    if json_body is not None:
        data = json.dumps(json_body).encode("utf-8")
        req_headers = dict(req_headers)
        req_headers.setdefault("Content-Type", "application/json")
    req = urllib.request.Request(url, data=data, headers=req_headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8")
            parsed = parse_body(body, resp.headers)
            return resp.status, dict(resp.headers), parsed
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8")
        parsed = parse_body(body, exc.headers)
        return exc.code, dict(exc.headers), parsed
    except urllib.error.URLError as exc:
        raise RequestError(str(exc)) from exc


def expect_status(status, allowed, body=None):
    if status not in allowed:
        detail = ""
        if isinstance(body, dict):
            message = body.get("message") or body.get("error")
            if message:
                detail = f": {message}"
            else:
                detail = f": {body}"
            correlation_id = body.get("correlationId")
            if correlation_id:
                detail = f"{detail} (correlationId: {correlation_id})"
        elif body:
            detail = f": {body}"
        raise TestFailure(f"unexpected status {status}, expected {allowed}{detail}")


def ensure_field(data, field):
    if not isinstance(data, dict) or field not in data:
        raise TestFailure(f"missing field: {field}")
    return data[field]


def extract_list(data):
    if isinstance(data, list):
        return data
    if isinstance(data, dict) and "content" in data and isinstance(data["content"], list):
        return data["content"]
    return []


class Context:
    def __init__(self):
        self.auth_base = env("AUTH_BASE_URL")
        self.user_base = env("USER_BASE_URL")
        self.catalog_base = env("CATALOG_BASE_URL")
        self.cart_base = env("CART_BASE_URL")
        self.order_base = env("ORDER_BASE_URL")
        self.payment_base = env("PAYMENT_BASE_URL")
        self.inventory_base = env("INVENTORY_BASE_URL")
        self.shipping_base = env("SHIPPING_BASE_URL")
        self.service_secret = env("SERVICE_SECRET")
        self.auth_service_secret = env("AUTH_SERVICE_SECRET") or self.service_secret
        self.payment_service_secret = env("PAYMENT_SERVICE_SECRET") or self.service_secret
        self.shipping_service_secret = env("SHIPPING_SERVICE_SECRET") or self.service_secret
        self.admin_email = env("ADMIN_EMAIL") or "admin@example.com"
        self.admin_password = env("ADMIN_PASSWORD") or "Admin123!"
        self.customer_email = env("CUSTOMER_EMAIL") or "e2e.customer@example.com"
        self.customer_password = env("CUSTOMER_PASSWORD") or "Password123!"
        self.catalog_image_url = env("CATALOG_IMAGE_URL") or "https://via.placeholder.com/400x400"
        self.admin_token = None
        self.customer_token = None
        self.customer_refresh = None
        self.admin_user_id = None
        self.customer_user_id = None
        self.shipping_address_id = None
        self.billing_address_id = None
        self.category_id = None
        self.product_id = None
        self.cart_id = None
        self.cart_item_id = None
        self.order_id = None
        self.payment_id = None
        self.reservation_id = None
        self.shipment_id = None
        self.sku = None


def auth_public_key(ctx):
    status, _, _ = http_request(
        "GET", f"{ctx.auth_base}/api/v1/auth/public-key"
    )
    expect_status(status, [200])
    return f"status {status}"


def auth_login_admin(ctx):
    payload = {"email": ctx.admin_email, "password": ctx.admin_password}
    status, _, body = http_request(
        "POST",
        f"{ctx.auth_base}/api/v1/auth/login",
        headers=build_headers(),
        json_body=payload,
    )
    expect_status(status, [200])
    ctx.admin_token = ensure_field(body, "accessToken")
    ctx.admin_user_id = ensure_field(body, "userId")
    return f"userId {ctx.admin_user_id}"


def auth_login_or_register_customer(ctx):
    payload = {"email": ctx.customer_email, "password": ctx.customer_password}
    status, _, body = http_request(
        "POST",
        f"{ctx.auth_base}/api/v1/auth/login",
        headers=build_headers(),
        json_body=payload,
    )
    if status == 200:
        ctx.customer_token = ensure_field(body, "accessToken")
        ctx.customer_refresh = ensure_field(body, "refreshToken")
        ctx.customer_user_id = ensure_field(body, "userId")
        return f"login ok userId {ctx.customer_user_id}"
    if status not in [401, 404]:
        raise TestFailure(f"login failed with status {status}")

    register_payload = {
        "email": ctx.customer_email,
        "password": ctx.customer_password,
        "name": "E2E Customer",
    }
    status, _, body = http_request(
        "POST",
        f"{ctx.auth_base}/api/v1/auth/register",
        headers=build_headers(),
        json_body=register_payload,
    )
    if status == 201:
        ctx.customer_token = ensure_field(body, "accessToken")
        ctx.customer_refresh = ensure_field(body, "refreshToken")
        ctx.customer_user_id = ensure_field(body, "userId")
        return f"registered userId {ctx.customer_user_id}"
    if status == 409:
        status, _, body = http_request(
            "POST",
            f"{ctx.auth_base}/api/v1/auth/login",
            headers=build_headers(),
            json_body=payload,
        )
        expect_status(status, [200])
        ctx.customer_token = ensure_field(body, "accessToken")
        ctx.customer_refresh = ensure_field(body, "refreshToken")
        ctx.customer_user_id = ensure_field(body, "userId")
        return f"login after conflict userId {ctx.customer_user_id}"

    raise TestFailure(f"register failed with status {status}")


def auth_refresh(ctx):
    if not ctx.customer_refresh:
        raise SkipTest("missing refresh token")
    payload = {"refreshToken": ctx.customer_refresh}
    status, _, body = http_request(
        "POST",
        f"{ctx.auth_base}/api/v1/auth/refresh",
        headers=build_headers(),
        json_body=payload,
    )
    expect_status(status, [200], body)
    new_token = ensure_field(body, "accessToken")
    return f"token length {len(new_token)}"


def auth_validate(ctx):
    if not ctx.customer_token:
        raise SkipTest("missing access token")
    payload = {"token": ctx.customer_token}
    status, _, body = http_request(
        "POST",
        f"{ctx.auth_base}/api/v1/auth/validate",
        headers=build_headers(),
        json_body=payload,
    )
    expect_status(status, [200])
    is_valid = ensure_field(body, "valid")
    return f"valid {is_valid}"


def user_internal_get(ctx):
    if not ctx.customer_user_id:
        raise SkipTest("missing customer user id")
    headers = build_headers({"X-Service-Secret": ctx.auth_service_secret})
    status, _, body = http_request(
        "GET",
        f"{ctx.user_base}/api/v1/users/internal/{ctx.customer_user_id}",
        headers=headers,
    )
    expect_status(status, [200])
    ensure_field(body, "userId")
    return "internal user found"


def user_me(ctx):
    if not ctx.customer_token:
        raise SkipTest("missing access token")
    headers = build_headers({"Authorization": f"Bearer {ctx.customer_token}"})
    status, _, body = http_request(
        "GET", f"{ctx.user_base}/api/v1/users/me", headers=headers
    )
    expect_status(status, [200])
    ensure_field(body, "userId")
    return "me ok"


def user_addresses(ctx):
    if not ctx.customer_token:
        raise SkipTest("missing access token")
    headers = build_headers({"Authorization": f"Bearer {ctx.customer_token}"})
    status, _, body = http_request(
        "GET", f"{ctx.user_base}/api/v1/addresses", headers=headers
    )
    expect_status(status, [200])
    addresses = extract_list(body)

    def find_by_label(label):
        for addr in addresses:
            if addr.get("label") == label:
                return addr.get("addressId")
        return None

    ctx.shipping_address_id = find_by_label("E2E Shipping")
    ctx.billing_address_id = find_by_label("E2E Billing")

    if not ctx.shipping_address_id:
        payload = {
            "label": "E2E Shipping",
            "country": "US",
            "state": "CA",
            "city": "San Francisco",
            "zip": "94105",
            "street": "Market Street",
            "number": "100",
            "complement": "Suite 1",
        }
        status, _, body = http_request(
            "POST",
            f"{ctx.user_base}/api/v1/addresses",
            headers=headers,
            json_body=payload,
        )
        expect_status(status, [200, 201])
        ctx.shipping_address_id = ensure_field(body, "addressId")

    if not ctx.billing_address_id:
        payload = {
            "label": "E2E Billing",
            "country": "US",
            "state": "CA",
            "city": "San Francisco",
            "zip": "94105",
            "street": "Market Street",
            "number": "200",
            "complement": "Suite 2",
        }
        status, _, body = http_request(
            "POST",
            f"{ctx.user_base}/api/v1/addresses",
            headers=headers,
            json_body=payload,
        )
        expect_status(status, [200, 201])
        ctx.billing_address_id = ensure_field(body, "addressId")

    if ctx.shipping_address_id:
        http_request(
            "PATCH",
            f"{ctx.user_base}/api/v1/addresses/{ctx.shipping_address_id}/default-shipping",
            headers=headers,
        )
    if ctx.billing_address_id:
        http_request(
            "PATCH",
            f"{ctx.user_base}/api/v1/addresses/{ctx.billing_address_id}/default-billing",
            headers=headers,
        )

    return (
        f"shipping {ctx.shipping_address_id}, "
        f"billing {ctx.billing_address_id}"
    )


def catalog_category(ctx):
    if not ctx.admin_token:
        raise SkipTest("missing admin token")
    status, _, body = http_request(
        "GET", f"{ctx.catalog_base}/api/v1/categories", headers=build_headers()
    )
    expect_status(status, [200])
    categories = extract_list(body)
    for cat in categories:
        if cat.get("name") == "E2E Electronics":
            ctx.category_id = cat.get("id")
            return f"existing id {ctx.category_id}"

    payload = {"name": "E2E Electronics", "description": "E2E test category"}
    headers = build_headers({"Authorization": f"Bearer {ctx.admin_token}"})
    status, _, body = http_request(
        "POST",
        f"{ctx.catalog_base}/api/v1/categories",
        headers=headers,
        json_body=payload,
    )
    expect_status(status, [200, 201])
    ctx.category_id = ensure_field(body, "id")
    return f"created id {ctx.category_id}"


def catalog_product(ctx):
    if not ctx.admin_token or not ctx.category_id:
        raise SkipTest("missing admin token or category")

    status, _, body = http_request(
        "GET",
        f"{ctx.catalog_base}/api/v1/products?categoryId={ctx.category_id}&size=100",
        headers=build_headers(),
    )
    expect_status(status, [200])
    products = extract_list(body)
    for prod in products:
        if prod.get("name") == "E2E Console":
            ctx.product_id = prod.get("id")
            break

    if not ctx.product_id:
        payload = {
            "sellerId": ctx.admin_user_id,
            "name": "E2E Console",
            "description": "E2E test product",
            "basePrice": 499.99,
            "categoryId": ctx.category_id,
            "availableSizes": ["STD"],
            "availableColors": ["Black"],
            "stockPerVariant": {"STD-Black": 10},
            "imageUrls": [ctx.catalog_image_url],
        }
        headers = build_headers({"Authorization": f"Bearer {ctx.admin_token}"})
        status, _, body = http_request(
            "POST",
            f"{ctx.catalog_base}/api/v1/products",
            headers=headers,
            json_body=payload,
        )
        expect_status(status, [200, 201], body)
        ctx.product_id = ensure_field(body, "id")

    ctx.sku = f"PROD-{ctx.product_id[:8].upper()}"
    return f"product {ctx.product_id}"


def cart_get_or_create(ctx):
    if not ctx.product_id or not ctx.customer_user_id:
        raise SkipTest("missing product or user id")
    status, _, body = http_request(
        "GET",
        f"{ctx.cart_base}/api/v1/carts/{ctx.customer_user_id}",
        headers=build_headers(),
    )
    expect_status(status, [200])
    ctx.cart_id = ensure_field(body, "cartId")
    return f"cart {ctx.cart_id}"


def cart_add_or_update(ctx):
    if not ctx.cart_id or not ctx.product_id or not ctx.customer_user_id:
        raise SkipTest("missing cart or product or user id")
    status, _, body = http_request(
        "GET",
        f"{ctx.cart_base}/api/v1/carts/{ctx.customer_user_id}",
        headers=build_headers(),
    )
    expect_status(status, [200])
    items = body.get("items") or []
    for item in items:
        if item.get("productId") == ctx.product_id:
            ctx.cart_item_id = item.get("cartItemId")
            break

    if ctx.cart_item_id:
        payload = {"quantity": 2}
        status, _, _ = http_request(
            "PUT",
            f"{ctx.cart_base}/api/v1/carts/{ctx.customer_user_id}/items/{ctx.cart_item_id}",
            headers=build_headers(),
            json_body=payload,
        )
        expect_status(status, [200])
        return f"updated item {ctx.cart_item_id}"

    payload = {"productId": ctx.product_id, "quantity": 2}
    status, _, body = http_request(
        "POST",
        f"{ctx.cart_base}/api/v1/carts/{ctx.customer_user_id}/items",
        headers=build_headers(),
        json_body=payload,
    )
    expect_status(status, [200])
    items = body.get("items") or []
    for item in items:
        if item.get("productId") == ctx.product_id:
            ctx.cart_item_id = item.get("cartItemId")
            break
    return f"added item {ctx.cart_item_id}"


def order_create(ctx):
    if not ctx.cart_id or not ctx.customer_user_id:
        raise SkipTest("missing cart or user id")
    if not ctx.shipping_address_id or not ctx.billing_address_id:
        raise SkipTest("missing addresses")
    payload = {
        "userId": ctx.customer_user_id,
        "cartId": ctx.cart_id,
        "shippingAddressId": ctx.shipping_address_id,
        "billingAddressId": ctx.billing_address_id,
    }
    attempts = 0
    while attempts < 3:
        attempts += 1
        status, _, body = http_request(
            "POST",
            f"{ctx.order_base}/api/v1/orders",
            headers=build_headers(),
            json_body=payload,
        )
        if status in [200, 201]:
            ctx.order_id = ensure_field(body, "orderId")
            return f"created {ctx.order_id}"
        if status == 402:
            time.sleep(1)
            continue
        raise TestFailure(f"status {status}")

    raise TestFailure("payment failed after retries")


def order_get(ctx):
    if not ctx.order_id:
        raise SkipTest("missing order id")
    status, _, body = http_request(
        "GET",
        f"{ctx.order_base}/api/v1/orders/{ctx.order_id}",
        headers=build_headers(),
    )
    expect_status(status, [200])
    ensure_field(body, "orderId")
    return "order fetched"


def order_list(ctx):
    if not ctx.customer_user_id:
        raise SkipTest("missing user id")
    status, _, _ = http_request(
        "GET",
        f"{ctx.order_base}/api/v1/orders/user/{ctx.customer_user_id}?page=0&size=5",
        headers=build_headers(),
    )
    expect_status(status, [200])
    return "orders listed"


def payment_create(ctx):
    if not ctx.customer_user_id:
        raise SkipTest("missing user id")
    if not ctx.order_id:
        ctx.order_id = str(uuid.uuid4())
    payload = {
        "orderId": ctx.order_id,
        "userId": ctx.customer_user_id,
        "amount": 49.99,
        "currency": "USD",
    }
    headers = build_headers({"X-Service-Secret": ctx.payment_service_secret})
    status, _, body = http_request(
        "POST",
        f"{ctx.payment_base}/api/v1/payments",
        headers=headers,
        json_body=payload,
    )
    expect_status(status, [200, 201])
    ctx.payment_id = ensure_field(body, "paymentId")
    return f"payment {ctx.payment_id}"


def payment_authorize(ctx):
    if not ctx.payment_id:
        raise SkipTest("missing payment id")
    headers = build_headers({"X-Service-Secret": ctx.payment_service_secret})
    payload = {"idempotencyKey": f"auth-{uuid.uuid4()}"}
    attempts = 0
    while attempts < 3:
        attempts += 1
        status, _, _ = http_request(
            "POST",
            f"{ctx.payment_base}/api/v1/payments/{ctx.payment_id}/authorize",
            headers=headers,
            json_body=payload,
        )
        if status == 200:
            return "authorized"
        if status == 402:
            time.sleep(1)
            continue
        raise TestFailure(f"status {status}")
    raise TestFailure("authorization failed after retries")


def payment_capture(ctx):
    if not ctx.payment_id:
        raise SkipTest("missing payment id")
    headers = build_headers({"X-Service-Secret": ctx.payment_service_secret})
    payload = {"amount": 49.99, "idempotencyKey": f"cap-{uuid.uuid4()}"}
    attempts = 0
    while attempts < 3:
        attempts += 1
        status, _, _ = http_request(
            "POST",
            f"{ctx.payment_base}/api/v1/payments/{ctx.payment_id}/capture",
            headers=headers,
            json_body=payload,
        )
        if status == 200:
            return "captured"
        if status == 402:
            time.sleep(1)
            continue
        raise TestFailure(f"status {status}")
    raise TestFailure("capture failed after retries")


def payment_refund(ctx):
    if not ctx.payment_id:
        raise SkipTest("missing payment id")
    headers = build_headers({"X-Service-Secret": ctx.payment_service_secret})
    payload = {
        "amount": 10.00,
        "reason": "E2E test refund",
        "idempotencyKey": f"refund-{uuid.uuid4()}",
    }
    attempts = 0
    while attempts < 3:
        attempts += 1
        status, _, _ = http_request(
            "POST",
            f"{ctx.payment_base}/api/v1/payments/{ctx.payment_id}/refund",
            headers=headers,
            json_body=payload,
        )
        if status == 200:
            return "refunded"
        if status == 402:
            time.sleep(1)
            continue
        raise TestFailure(f"status {status}")
    raise TestFailure("refund failed after retries")


def inventory_stock(ctx):
    if not ctx.product_id or not ctx.sku:
        raise SkipTest("missing product or sku")
    payload = {
        "sku": ctx.sku,
        "productId": ctx.product_id,
        "initialQty": 25,
        "lowStockThreshold": 5,
    }
    status, _, _ = http_request(
        "POST",
        f"{ctx.inventory_base}/api/v1/inventory/admin/stock-items",
        headers=build_headers(),
        json_body=payload,
    )
    if status not in [200, 201, 409]:
        raise TestFailure(f"status {status}")

    adjust_payload = {"availableQtyDelta": 10, "reason": "E2E test top-up"}
    http_request(
        "PUT",
        f"{ctx.inventory_base}/api/v1/inventory/admin/stock-items/{ctx.sku}",
        headers=build_headers(),
        json_body=adjust_payload,
    )

    status, _, _ = http_request(
        "GET",
        f"{ctx.inventory_base}/api/v1/inventory/admin/stock-items/{ctx.sku}",
        headers=build_headers(),
    )
    expect_status(status, [200])
    status, _, _ = http_request(
        "GET",
        f"{ctx.inventory_base}/api/v1/inventory/public/stock/{ctx.sku}",
        headers=build_headers(),
    )
    expect_status(status, [200])
    return f"stock {ctx.sku}"


def inventory_reservation(ctx):
    if not ctx.sku:
        raise SkipTest("missing sku")
    if not ctx.order_id:
        ctx.order_id = str(uuid.uuid4())
    payload = {"orderId": ctx.order_id, "lines": [{"sku": ctx.sku, "quantity": 1}]}
    status, _, body = http_request(
        "POST",
        f"{ctx.inventory_base}/api/v1/inventory/reservations",
        headers=build_headers(),
        json_body=payload,
    )
    expect_status(status, [200, 201])
    ctx.reservation_id = ensure_field(body, "reservationId")
    status, _, _ = http_request(
        "PUT",
        f"{ctx.inventory_base}/api/v1/inventory/reservations/{ctx.reservation_id}/confirm",
        headers=build_headers(),
    )
    expect_status(status, [200])
    return f"reservation {ctx.reservation_id}"


def shipping_create(ctx):
    if not ctx.shipping_address_id or not ctx.customer_user_id:
        raise SkipTest("missing address or user")
    if not ctx.order_id:
        ctx.order_id = str(uuid.uuid4())
    headers = build_headers({"X-Service-Secret": ctx.shipping_service_secret})
    address = {
        "label": "E2E Shipping",
        "country": "US",
        "state": "CA",
        "city": "San Francisco",
        "zip": "94105",
        "street": "Market Street",
        "number": "100",
        "complement": "Suite 1",
    }
    payload = {
        "orderId": ctx.order_id,
        "userId": ctx.customer_user_id,
        "shippingAddress": address,
        "itemCount": 2,
        "packageWeightKg": 2.5,
        "packageDimensions": "30x20x15 cm",
    }
    status, _, body = http_request(
        "POST",
        f"{ctx.shipping_base}/api/v1/shipments",
        headers=headers,
        json_body=payload,
    )
    expect_status(status, [200, 201], body)
    ctx.shipment_id = ensure_field(body, "shipmentId")
    return f"shipment {ctx.shipment_id}"


def shipping_get_by_order(ctx):
    if not ctx.order_id:
        raise SkipTest("missing order id")
    headers = build_headers({"X-Service-Secret": ctx.shipping_service_secret})
    status, _, body = http_request(
        "GET",
        f"{ctx.shipping_base}/api/v1/shipments/order/{ctx.order_id}",
        headers=headers,
    )
    expect_status(status, [200], body)
    return "shipment by order ok"


def shipping_user_get(ctx):
    if not ctx.shipment_id or not ctx.customer_token:
        raise SkipTest("missing shipment or token")
    headers = build_headers({"Authorization": f"Bearer {ctx.customer_token}"})
    status, _, _ = http_request(
        "GET",
        f"{ctx.shipping_base}/api/v1/shipments/{ctx.shipment_id}",
        headers=headers,
    )
    expect_status(status, [200])
    status, _, _ = http_request(
        "GET",
        f"{ctx.shipping_base}/api/v1/shipments/{ctx.shipment_id}/tracking",
        headers=headers,
    )
    expect_status(status, [200])
    status, _, _ = http_request(
        "GET",
        f"{ctx.shipping_base}/api/v1/shipments/user/me",
        headers=headers,
    )
    expect_status(status, [200])
    return "user shipment ok"


def shipping_admin_update(ctx):
    if not ctx.shipment_id or not ctx.admin_token:
        raise SkipTest("missing shipment or admin token")
    headers = build_headers({"Authorization": f"Bearer {ctx.admin_token}"})
    payload = {"status": "IN_TRANSIT", "reason": "E2E test update"}
    status, _, _ = http_request(
        "PATCH",
        f"{ctx.shipping_base}/api/v1/shipments/{ctx.shipment_id}/status",
        headers=headers,
        json_body=payload,
    )
    expect_status(status, [200])
    return "status updated"


def main():
    ctx = Context()
    runner = TestRunner()

    runner.run("Auth public key", lambda: auth_public_key(ctx))
    runner.run("Auth login admin", lambda: auth_login_admin(ctx))
    runner.run("Auth login/register customer", lambda: auth_login_or_register_customer(ctx))
    runner.run("Auth refresh", lambda: auth_refresh(ctx))
    runner.run("Auth validate", lambda: auth_validate(ctx))

    runner.run("User internal get", lambda: user_internal_get(ctx))
    runner.run("User me", lambda: user_me(ctx))
    runner.run("User addresses", lambda: user_addresses(ctx))

    runner.run("Catalog category", lambda: catalog_category(ctx))
    runner.run("Catalog product", lambda: catalog_product(ctx))

    runner.run("Cart get/create", lambda: cart_get_or_create(ctx))
    runner.run("Cart add/update", lambda: cart_add_or_update(ctx))

    runner.run("Order create", lambda: order_create(ctx))
    runner.run("Order get", lambda: order_get(ctx))
    runner.run("Order list", lambda: order_list(ctx))

    runner.run("Payment create", lambda: payment_create(ctx))
    runner.run("Payment authorize", lambda: payment_authorize(ctx))
    runner.run("Payment capture", lambda: payment_capture(ctx))
    runner.run("Payment refund", lambda: payment_refund(ctx))

    runner.run("Inventory stock", lambda: inventory_stock(ctx))
    runner.run("Inventory reservation", lambda: inventory_reservation(ctx))

    runner.run("Shipping create", lambda: shipping_create(ctx))
    runner.run("Shipping get by order", lambda: shipping_get_by_order(ctx))
    runner.run("Shipping user get", lambda: shipping_user_get(ctx))
    runner.run("Shipping admin update", lambda: shipping_admin_update(ctx))

    runner.print_results()
    counts = runner.summary()
    if counts["fail"] > 0:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
