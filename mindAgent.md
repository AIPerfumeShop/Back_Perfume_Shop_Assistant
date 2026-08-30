# MindAgent — Project Mind Map

A quick-reference map of `Back_Perfume_Shop_Assistant` for AI agents. Read `agent_guide_ai.md`
for conventions and this file for ground truth about what actually exists vs. what is scaffolding.

> **⚠️ WARNING — MOST OF THIS PROJECT IS SCAFFOLDING.** Most layers exist as empty stub files
> (class declarations only, no code). Before touching anything, check the **[Status Legend](#status-legend)**.
> Do NOT assume a service/controller is functional just because its file exists.

---

## 1. Identity

```
Product        Perfume/e-commerce shop backend + AI fragrance-assistant chatbot
App            Spring Boot REST API
Base package   com.example.spring_boot_project_api
DB             MySQL (default db `db_Aiperfume` via .env, fallback `spring_boot_project_api`)
Table prefix   tb_  (e.g. tb_users, tb_products, tb_ai_conversations)
Swagger UI     http://localhost:8089/swagger-ui.html
```

---

## 2. Status Legend

| Mark | Meaning |
|---|---|
| ✅ **LIVE** | Implemented, wired, buildable, works end-to-end |
| 🟡 **PARTIAL** | Entity/DTO exist but service/controller missing, OR half a slice is done |
| ⛔ **STUB** | File exists but is empty/no code (skeleton only) |

**Fully LIVE vertical slices:**

- ✅ **AI Chat** + OpenRouter LLM integration (`AIController`, `AIServiceImpl`, `OpenRouterServiceImpl`, `AIMapper`, `AIConversation`, `AIMessage`, `enums/MessageSender`)
- ✅ **Brand CRUD** (`BrandController`, `BrandServiceImpl`, `BrandMapper`, `Brand`) — soft delete via `isActive`
- ✅ **Security foundations** (`SecurityConfig`, `PasswordEncoderConfig`, `CorsConfig`)
- ✅ **Infrastructure** (`SpringBootProjectApiApplication` + .env loader, `GlobalExceptionHandler`, `PaginationUtils`)
- ✅ **Models**: `User`, `Product` (basic relations only), `Category`, `Brand`, `AIConversation`, `AIMessage`

**⛔ STUB — files exist, nothing implemented:** JWT/auth stack, Product/Category/Cart/Order/Review/Wishlist/Settings/User services & controllers, Dashboard, Analytics, Email, Telegram, Payment, AI Analytics & AI Recommendations, `FragranceProfile`, mappers `CartMapper|CategoryMapper|OrderMapper|ProductMapper|ReviewMapper|SettingsMapper|UserMapper|WishlistMapper`, exceptions `ConflictException|InvalidOrderException|OutOfStockException|UnauthorizedException`, enums `OrderStatus|PaymentMethod|Intensity`, `util/Constants|SecurityUtils|ValidationUtils`, `config/OpenApiConfig|EmailConfig|TelegramConfig|JwtAuthenticationFilter`.

---

## 3. Tech Stack (from `pom.xml`)

| Layer | Choice |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.8-SNAPSHOT (spring snapshots repo) |
| Web | `spring-boot-starter-webmvc` (Boot 4 artifact — NOT `starter-web`) |
| Persistence | Spring Data JPA + `mysql-connector-j` |
| Security | `spring-boot-starter-security` (config only, JWT not wired) |
| Validation | `spring-boot-starter-validation` (jakarta) |
| API docs | `springdoc-openapi-starter-webmvc-ui` 3.0.2 |
| AI/LLM | OpenRouter REST (via Spring `RestClient`) — no SDK |
| Telegram | `telegrambots-spring-boot-starter` 6.9.7.1 (dependency present, no code) |
| Env | `dotenv-java` 3.0.0 (loads `.env` in main) |
| Boilerplate | Lombok |
| Build | Maven wrapper `./mvnw` |

---

## 4. Module Map

```
com.example.spring_boot_project_api
├── SpringBootProjectApiApplication   ✅ load .env → System props → run
├── config/          SecurityConfig✅ PasswordEncoderConfig✅ CorsConfig✅
│                    OpenApiConfig⛔ EmailConfig⛔ TelegramConfig⛔ JwtAuthenticationFilter⛔
├── controller/      AI✅ Brand✅ | 11 more ⛔ (Auth, Product, Category, Cart, Order,
│                    Review, Wishlist, Settings, User, Dashboard, Analytics, AIAnalytics)
├── dto/
│   ├── request/     brand✅ ai✅ | auth/product/cart/order/review/wishlist/settings/user ⛔
│   ├── response/    brand✅ ai✅ product(User-names ok but not served) | rest ⛔
│   └── external/openrouter/   OpenRouterRequest✅ Message✅ Response✅ Choice✅
├── enums/           Role✅ customer|admin   MessageSender✅ USER|AI   Gender✅ Men|Women|Unisex
│                    OrderStatus⛔ PaymentMethod⛔ Intensity⛔ (empty)
├── exception/       GlobalExceptionHandler✅ ResourceNotFound✅ Forbidden✅ AIService✅
│                    BadRequest✅(thrown but NOT handled → currently 500)
│                    Conflict⛔ InvalidOrder⛔ OutOfStock⛔ Unauthorized⛔
├── mapper/          AIMapper✅ BrandMapper✅ | 8 others ⛔ (hand-written, NOT MapStruct)
├── model/           User✅ Product🟡 Category✅ Brand✅ AIConversation✅ AIMessage✅
│                    ProductVariant⛔ ProductImage⛔ Cart⛔ CartItem⛔ Order⛔ OrderItem⛔
│                    Payment⛔ Review⛔ Wishlist⛔ WishlistItem⛔ Settings⛔
│                    FragranceProfile⛔ AIRecommendation⛔ AIRecommendationClick⛔
├── repository/      User✅ findByEmail/existsByEmail  Brand✅ existsByNameIgnoreCase(+AndIdNot)
│                    AIConversation✅ AIMessage✅ (query methods) | ~12 more ⛔
├── security/        ALL STUBS ⛔ JwtService CustomUserDetailsService
│                    CustomAccessDeniedHandler SecurityUser
├── service/         AI✅ OpenRouter✅ Brand✅ | ~14 interfaces ⛔
│   └── impl/        AIServiceImpl✅(251L) OpenRouterServiceImpl✅(99L) BrandServiceImpl✅(112L)
│                    | ~14 more ⛔
└── util/            PaginationUtils✅ | Constants⛔ SecurityUtils⛔ ValidationUtils⛔
```

---

## 5. Domain & Relationships

```
User (tb_users)                        fullname, email(uniq), password, phone, address, role
└── 1─N AIConversation (tb_ai_conversations)   user(LAZY ManyToOne), userName, title(200)
     └── 1─N AIMessage (tb_ai_messages)        conversation(LAZY), sender(USER|AI), message(TEXT)
Brand (tb_brands)              name(uniq), description(TEXT), logoUrl, isActive(soft-delete)
Category (tb_categories)       name(uniq), description, imageUrl, isActive
└── 1─N Product (tb_products)  category(N-1), brand(N-1), name, description, isActive
```

Notes:
- All entities use `@PrePersist`/`@PreUpdate` timestamps (created_at, updated_at).
- `AIConversation.messages` cascade ALL + orphanRemoval; `touch()` bumps updatedAt.
- `User.role` is `Role` enum stored as STRING — **lowercase** values `customer` / `admin`.
- Relationships beyond the above (ProductVariant/Image, Cart/Item, Order/Item/Payment,
  Review, Wishlist/Item, Settings) are **NOT implemented**.

---

## 6. LIVE Flows

### 6.1 AI Chat — the centrepiece
```
POST /api/ai/chat?userId={id}  body: { "conversationId"?, "message" }
1. resolve User (404 if missing)
2. conversationId null  → create conversation;  title = first 50 chars of message + "..."
   conversationId set   → load, verify conversation.user.id == userId else ForbiddenException(403)
3. save user msg (MessageSender.USER)
4. load history oldest→newest, cap to last MAX_HISTORY_SIZE=30
5. OpenRouterService.generateResponse(history):
      AIMessage → {role:"user"|"assistant", content}   (sender USER→user else assistant)
      POST openrouter.url  (https://openrouter.ai/api/v1/chat/completions)
      header Authorization: Bearer ${OPENROUTER_API_KEY}
      body OpenRouterRequest(model=openrouter/free, messages)   via Spring RestClient
      parse choices[0].message.content   → failure wrapped in AIServiceException → 502
6. save AI reply (MessageSender.AI), touch() conversation, return AIChatResponse
```
Ownership checks (`conversation.getUser().getId().equals(userId)`) on every conversation
operation are the de-facto authorization layer (no principal yet).

### 6.2 Brand CRUD (soft delete)
```
POST   /api/brands          duplicate name → BadRequestException("Brand name already exists : X")
GET    /api/brands          returns isActive==true only
GET    /api/brands/{id}     404 if not found OR soft-deleted
PUT    /api/brands/{id}     404 if not found OR not active; update via mapper
DELETE /api/brands/{id}     204; isActive=false (soft); 400 if already inactive
```

---

## 7. HTTP Surface (implemented)

### AIController — base `/api/ai` ✅
| Method | Path | Notes |
|---|---|---|
| POST | `/api/ai/chat` | `@RequestParam Long userId` + `@Valid AIChatRequest` → `AIChatResponse` |
| GET | `/api/ai/conversations` | `?userId=` → list ordered by `updatedAt` desc |
| GET | `/api/ai/conversations/{conversationId}/messages` | `?userId=` → oldest→newest |
| PUT | `/api/ai/conversations/{conversationId}` | rename via `RenameConversationRequest.title` |
| DELETE | `/api/ai/conversations/{conversationId}` | `?userId=` → 204 / 404 |

### BrandController — base `/api/brands` ✅
`POST /` `GET /` `GET /{id}` `PUT /{id}` `DELETE /{id}` → 201 / 200 / 200 / 200 / 204

> **Every other controller is an empty stub with no endpoints.**

---

## 8. Security (current reality)

- `SecurityConfig` ✅: STATELESS sessions, CSRF disabled.
- **PermitAll:** `/api/auth/**`, `/api/ai/**`, `/swagger-ui/**`, `/swagger-ui.html`,
  `/v3/api-docs/**`, `/api/brands/**`. **Everything else → `authenticated()`** (Spring default
  → HTTP 403/401, no custom entrypoint).
- `PasswordEncoderConfig` ✅ exposes `BCryptPasswordEncoder` bean.
- `CorsConfig` ✅ allows `http://localhost:5173` (Vite frontend), methods GET/POST/PUT/DELETE/PATCH/OPTIONS, headers Authorization/Content-Type/Accept, credentials, `/**`.
- **JWT is NOT wired.** All of `security/` and `config/JwtAuthenticationFilter` are empty
  stubs. Auth endpoints are public but `AuthController`/`AuthService` are unimplemented.
  `userId` is passed as a **query param** everywhere instead of from a principal.
  Treat JWT/roles/refresh-token as future work.

---

## 9. Exceptions & Error Format

`GlobalExceptionHandler` ✅ — `@RestControllerAdvice`, uniform body:
```json
{ "status": <int>, "message": "...", "timestamp": "ISO-8601" }   // record ErrorResponse
```

| Exception | HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `ForbiddenException` | 403 |
| `AIServiceException` | 502 (gateway) |
| `BadRequestException` | ⚠️ no handler → currently **500** |
| `MethodArgumentNotValidException` (jakarta validation) | ⚠️ no handler → currently **500** |

---

## 10. Configuration

### `application.properties` (env-overridable)
- `server.port=${SERVER:8089}`
- MySQL: `jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:spring_boot_project_api}?createDatabaseIfNotExist=true`, `${DB_USERNAME:root}`, `${DB_PASSWORD:}`
- JPA: `ddl-auto=update`, `show-sql=true`, `MySQLDialect`
- Telegram: `telegram.bot.token` / `telegram.bot.username` (configured, unused)
- OpenRouter: `openrouter.api-key`(`${OPENROUTER_API_KEY}`), `openrouter.url`, `openrouter.model=openrouter/free`

### `.env` loader
`main()` loads `.env` via `Dotenv.configure().ignoreIfMissing()` and copies entries into
System properties so `${...}` placeholders resolve. `.env.example` shows the keys.

---

## 11. Conventions (from `agent_guide_ai.md`, followed by live code)

- Controller → Service → Repository layering; controllers thin, call service interfaces.
- Services: `interface` in `service/`, impl in `service/impl/`.
- Never expose entities; map via `mapper/` (hand-written `@Component`, explicit setters — not MapStruct).
- Request DTOs validated with `jakarta.validation`; exceptions bubble to GlobalExceptionHandler.
- Constructor injection (explicit constructors in live code; `@RequiredArgsConstructor` also accepted).
- New resource = model + repository + request/response DTOs + mapper + service(+impl) + controller.

---

## 12. Gotchas & Known Gaps

1. **~194 main classes, most are empty stubs** — verify before using anything not in the LIVE list.
2. **`BadRequestException` + bean-validation failures return 500**, not 400 (no handler).
3. **No JWT / roles / method security** — `userId` query params and in-service ownership checks only.
4. **`Role` enum is lowercase** (`customer`, `admin`); `Gender`/`MessageSender` are Pascal/UPPER — inconsistent.
5. **`spring-boot-starter-webmvc`** (Boot 4) replaces `starter-web`; don't add the old starter.
6. `springdoc` works out of the box even though `OpenApiConfig` is empty.
7. Telegram bot dependency + props are dead config until `TelegramServiceImpl` is written.
8. `PaginationUtils.createPageable(page,size,sortby,direction)` — page 0 default, size 10 (max 100), default sort `createdAt`, `asc`→ASC else DESC. Unused by live controllers so far.
9. Only test is `SpringBootProjectApiApplicationTests` (`contextLoads`).
10. `.env` is gitignored; never commit keys. DB default (root, no pass) matches XAMPP / docker-compose.

---

## 13. Commands

```bash
./mvnw spring-boot:run   # run on http://localhost:8089 (or $SERVER)
./mvnw test
./mvnw clean package
```

Quick mental model: **AI chat + Brand CRUD are real; everything else is a named, empty slot
waiting for implementation.**