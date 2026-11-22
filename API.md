# VMS API — Vehicles

Base URL: `http://localhost:8080/api/v1` · JSON in and out · Full contract:
[`contracts/openapi/vms-api.yaml`](contracts/openapi/vms-api.yaml) · Live docs:
`http://localhost:8080/swagger-ui.html`

## Authentication

Every endpoint except login needs `Authorization: Bearer <token>`.

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "email": "manager@datavvy.com", "password": "Manager@12345" }
```
→ `200 { "accessToken": "eyJ…", "tokenType": "Bearer", "expiresIn": 28800, "user": { … } }`

Roles: **ADMIN** and **FLEET_MANAGER** can create/update/delete; **VIEWER** can only read.

## Vehicle schema

| Field (course) | JSON | DB column | Type | Rules |
|---|---|---|---|---|
| — | `id` | `id` | BIGINT AUTO_INCREMENT, PK | Generated; read-only |
| Vehicle Number | `vehicleNumber` | `vehicle_number` (+ `vehicle_number_key`) | VARCHAR(32) | Required. BD format `DHA-METRO-GA 11-4582`; loose input like `dha metro ga 11 4582` is accepted and stored canonically. **Unique** ignoring case, spaces and hyphens (service rule + `UNIQUE (vehicle_number_key)`) |
| Company | `company` | `company` | VARCHAR(100) | Required |
| Type | `type` | `type` | VARCHAR(60) | Required; must be a vehicle type from `GET /catalog/vehicle-types` (case-insensitive) |
| Brand | `brand` | `brand` | VARCHAR(60) | Required |
| Model | `model` | `model` | VARCHAR(60) | Required |
| Status | `status` | `status` | VARCHAR(20) + CHECK | Required: `ACTIVE`, `INACTIVE` or `MAINTENANCE` |
| — | `createdAt`, `updatedAt` | `created_at`, `updated_at` | DATETIME(6), UTC | Read-only |
| — | `version` | `version` | BIGINT | Read-only; send it back on update |

## Endpoints

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/vehicles` | `201 Created` + `Location` + vehicle | 400, 401, 403, **409 `DUPLICATE_VEHICLE_NUMBER`** |
| GET | `/vehicles` | `200` page of vehicles | 401 |
| GET | `/vehicles/{id}` | `200` vehicle | 401, 404 |
| PUT | `/vehicles/{id}` | `200` vehicle | 400, 401, 403, 404, **409 `DUPLICATE_VEHICLE_NUMBER`**, 409 `STALE_VERSION` |
| DELETE | `/vehicles/{id}` | `204 No Content` | 401, 403, 404 |

UI helpers (read-only): `GET /vehicles/facets` (status counts, companies, brands,
dates with data) and `GET /vehicles/number-check?number=…&excludeId=…`.

### Create

```http
POST /api/v1/vehicles
Authorization: Bearer <token>
Content-Type: application/json

{
  "vehicleNumber": "dha metro gha 22 1234",
  "company": "Datavvy Logistics",
  "type": "Microbus",
  "brand": "Toyota",
  "model": "Hiace",
  "status": "ACTIVE"
}
```
→ `201 Created`, `Location: /api/v1/vehicles/9`
```json
{
  "id": 9,
  "vehicleNumber": "DHA-METRO-GHA 22-1234",
  "company": "Datavvy Logistics",
  "type": "Microbus",
  "brand": "Toyota",
  "model": "Hiace",
  "status": "ACTIVE",
  "createdAt": "2026-11-21T16:05:12.345Z",
  "updatedAt": "2026-11-21T16:05:12.345Z",
  "version": 0
}
```

### List

`GET /api/v1/vehicles?status=ACTIVE&q=hiace&type=Microbus&company=Datavvy%20Logistics&brand=Toyota&page=0&size=20&sort=updatedAt,desc`

All parameters are optional; `type`, `company` and `brand` may repeat.
Sort fields: `vehicleNumber, company, type, brand, model, status, createdAt, updatedAt`.

```json
{ "content": [ { "id": 1, "vehicleNumber": "DHA-METRO-GA 11-4582", "...": "..." } ],
  "page": 0, "size": 20, "totalElements": 8, "totalPages": 1 }
```

### Get one

`GET /api/v1/vehicles/9` → `200` with the vehicle (same shape as create).

### Update

```http
PUT /api/v1/vehicles/9
Content-Type: application/json

{ "vehicleNumber": "DHA-METRO-GHA 22-1234", "company": "Datavvy Logistics", "type": "Microbus",
  "brand": "Toyota", "model": "Hiace GL", "status": "MAINTENANCE", "version": 0 }
```
→ `200` with the updated vehicle (`version` incremented). All fields are sent
(full replacement). `version` must equal the one you loaded.

### Delete

`DELETE /api/v1/vehicles/9` → `204 No Content`. A second `GET /vehicles/9` → `404`.

## Errors

Every error is an RFC 9457 problem document. Branch on `code`, not on text.

```json
{ "type": "about:blank", "title": "Conflict", "status": 409,
  "detail": "A vehicle with number DHA-METRO-GA 11-4582 already exists.",
  "code": "DUPLICATE_VEHICLE_NUMBER",
  "details": { "field": "vehicleNumber", "vehicleNumber": "DHA-METRO-GA 11-4582", "existingId": 1 } }
```

| Status | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Missing/blank fields, bad vehicle number format, unknown type, bad status value. `fieldErrors: [{ "field": "...", "message": "..." }]` |
| 400 | `MALFORMED_REQUEST` | Body isn't valid JSON |
| 401 | — | No/expired token (login: `INVALID_CREDENTIALS`) |
| 403 | `FORBIDDEN` | VIEWER tried to create/update/delete |
| 404 | `VEHICLE_NOT_FOUND` | No vehicle with that id |
| 409 | `DUPLICATE_VEHICLE_NUMBER` | Vehicle Number already used by another vehicle (create or update) |
| 409 | `STALE_VERSION` | Someone changed the vehicle after you loaded it |
| 500 | `INTERNAL_ERROR` | Unexpected; details are logged server-side, never returned |

Validation example:
```json
{ "status": 400, "code": "VALIDATION_FAILED", "detail": "Some fields need attention.",
  "fieldErrors": [
    { "field": "vehicleNumber", "message": "Enter the full vehicle number, like DHA-METRO-GA 11-4582." },
    { "field": "type", "message": "Choose a vehicle type from the list, like Microbus." } ] }
```

## The duplicate Vehicle Number rule

1. **Business layer** — `VehicleService.rejectDuplicate()` runs on create and
   update before saving (update ignores the vehicle's own number) and throws
   `DuplicateVehicleNumberException` → 409.
2. **Database** — `UNIQUE (vehicle_number_key)` on the normalised number. If two
   requests race past step 1, the constraint fires and the service maps it to
   the same 409, so the client never sees a SQL error.

Tests: `VehicleServiceTest` (unit, Mockito) and `VehicleApiIntegrationTest`
(HTTP + H2), including `createWithAnExistingVehicleNumberIsRejectedWith409`,
`updateToAnotherVehiclesNumberIsRejectedWith409` and
`databaseUniqueConstraintBacksTheRule`.

## Try it

- Postman: import `postman/vms-vehicles.postman_collection.json`
  (runs login → create → list → get → update → duplicate → delete).
- curl: `scripts/api-smoke.sh` (needs the backend running on :8080 with the `local` profile).
