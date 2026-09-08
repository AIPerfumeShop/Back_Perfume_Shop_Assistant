# Customer Management API — Implementation Plan

## Overview

Implement full Customer Management API: list, search, filter, view details (with order history), update customer info, and activate/deactivate accounts. The `User` model with `Role.CUSTOMER` serves as the customer entity — no new model needed.

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/customers` | List customers with search, filter, pagination |
| GET | `/api/customers/{id}` | Customer detail (profile + order history) |
| PUT | `/api/customers/{id}` | Update customer info (name, email, phone, role) |
| PATCH | `/api/customers/{id}/activate` | Activate customer account |
| PATCH | `/api/customers/{id}/deactivate` | Deactivate customer account |

---

## Files to Create (8 new files)

### 1. `dto/request/customer/CustomerFilterRequest.java`
- Fields: `search` (String), `role` (Role), `isActive` (Boolean), `page`, `size`, `sort`, `direction`
- `toPageRequest()` method with sortable fields whitelist (id, name, email, role, createdAt, updatedAt)
- Follows `BrandFilterRequest` pattern exactly

### 2. `dto/response/customer/CustomerSummaryResponse.java`
- Fields: `id`, `name`, `email`, `phone`, `role`, `isActive`, `orderCount`, `totalSpent`, `createdAt`
- Used for list/search results (lightweight)

### 3. `dto/response/customer/CustomerDetailResponse.java`
- Extends profile fields: `id`, `name`, `email`, `phone`, `role`, `isActive`, `createdAt`, `updatedAt`
- Order stats: `totalOrders`, `totalSpent`
- `List<RecentOrderResponse>` — last 5 orders (id, totalAmount, status, createdAt)
- Used for the single-customer detail endpoint

### 4. `repository/specification/CustomerSpecification.java`
- Static `fromFilter(CustomerFilterRequest)` method
- Filters: search (name LIKE, email LIKE), role, isActive
- Follows `BrandSpecification` pattern

### 5. `mapper/CustomerMapper.java`
- `toSummaryResponse(User user, Long orderCount, BigDecimal totalSpent)`
- `toDetailResponse(User user, Long totalOrders, BigDecimal totalSpent, List<Order> recentOrders)`
- `updateEntity(CustomerUpdateRequest request, User user)` — applies name/email/phone/role changes
- Follows `BrandMapper` manual mapping pattern

### 6. `service/CustomerService.java`
- Interface methods matching the 5 endpoints above

### 7. `service/impl/CustomerServiceImpl.java`
- `@Service @Transactional`, constructor injection
- `getAllCustomers()` — Specification + pagination, batch-fetch order counts
- `getCustomerById()` — user + order stats + recent orders
- `updateCustomer()` — validate email uniqueness, update fields
- `activateCustomer()` / `deactivateCustomer()` — toggle isActive with idempotency check
- Throws `ResourceNotFoundException`, `BadRequestException`, `ConflictException` per existing pattern

### 8. `controller/CustomerController.java`
- `@RestController @RequestMapping("/api/customers") @RequiredArgsConstructor @Validated`
- Swagger `@Operation` / `@ApiResponses` on every endpoint
- Follows `BrandController` pattern exactly

---

## Files to Modify (2 existing files)

### 9. `repository/UserRepository.java`
Add queries:
```java
boolean existsByEmailAndIdNot(String email, Long id);

@Query("select count(u) from User u where u.role = :role")
long countByRole(@Param("role") Role role);  // already exists, reuse

@Query("select u from User u where u.id = :id")
Optional<User> findById(@Param("id") Long id);  // already inherited from JpaRepository
```
Only new method needed: `existsByEmailAndIdNot(String email, Long id)`

### 10. `config/SecurityConfig.java`
Add permit rule:
```java
.requestMatchers("/api/customers/**").permitAll()
```

---

## Implementation Order

1. `CustomerFilterRequest` → `CustomerSummaryResponse` → `CustomerDetailResponse` (DTOs)
2. `CustomerSpecification` (repository layer)
3. Update `UserRepository` (add `existsByEmailAndIdNot`)
4. `CustomerMapper` (mapping layer)
5. `CustomerService` interface → `CustomerServiceImpl` (business logic)
6. `CustomerController` (HTTP layer)
7. Update `SecurityConfig` (add permitAll)
8. Compile & verify

---

## Conventions Followed

- Layered architecture: controller → service → repository
- DTOs never expose JPA entities directly
- `@RequiredArgsConstructor` for constructor injection (no `@Autowired`)
- Jakarta validation on request DTOs
- Custom exceptions thrown from service, handled by `GlobalExceptionHandler`
- `@Transactional(readOnly = true)` on read operations
- `PagedResponse<T>` for paginated results
- Swagger annotations on all endpoints
- `@ModelAttribute` for filter params on GET endpoints
