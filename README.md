# 📓 NexusPay Developer Diary

## 📑 Table of Contents
1. [Project Port Directory](#-project-port-directory)
2. [Phase 1: Infrastructure & Discovery](#phase-1-infrastructure--discovery)
3. [Phase 2: Security & Routing](#phase-2-security--routing)
4. [Phase 3: The Money Flow](#phase-3-the-money-flow)
5. [Phase 4: Security Lockdown, Zero Trust & Central Config](#phase-4-security-lockdown--zero-trust)
6. [🛠️ Common Fixes & Troubleshooting Runbook](#%EF%B8%8F-common-fixes--troubleshooting-runbook)

---

## 🧭 Project Port Directory
*Quick reference for all running services in our cluster.*

| Service | Port | Description |
| :--- | :--- | :--- |
| **PostgreSQL** | `5432` | Main relational database (Docker) |
| **Redis** | `6379` | In-memory cache (Docker) |
| **Discovery Server** | `8761` | Eureka Registry ("The Phonebook") |
| **Config Server** | `8888` | Centralized settings ("The Vault") |
| **API Gateway** | `8080` | Front Door / Netty Router ("The Bouncer") |
| **Identity Service** | `8081` | Authentication & JWT Generation |
| **Payment Service** | `8082` | Core Business Logic & Stripe API |

---

## Phase 1: Infrastructure & Discovery

### 1. The Docker Environment (PostgreSQL & Redis)
**The Goal:** Run our database (Postgres) and cache (Redis) without permanently installing them on our local machine.
**The "Why":** We use Docker so that our environment is 100% reproducible. If a new developer joins the team, they don't spend 3 days installing databases; they just run one command.

**Key Commands:**
* `docker-compose up -d` : Starts the containers in the background ("detached" mode).
* `docker-compose down` : Stops and removes the containers.

### 2. The Eureka Discovery Server
**The Goal:** Create a centralized registry so microservices can find each other dynamically.
**The "Why":** In a cloud environment, IP addresses change constantly. If the `Payment-Service` needs to talk to the `Identity-Service`, it shouldn't use a hardcoded IP (like `192.168.1.5`). Instead, it asks Eureka: *"Hey, where does the Identity Service live today?"*

**Key Code / Configuration:**
* `@EnableEurekaServer`: This single annotation transforms a basic Spring Boot app into a registry server.
* `eureka.client.register-with-eureka=false`: Tells the server not to register itself as a client in its own registry.
* `eureka.client.fetch-registry=false`: Prevents the server from trying to download registry data from peer servers. Required for "standalone" mode.

### 3. Config Server
**The Goal:** Centralize all environment variables, database credentials, and application properties into a single service.
**The "Why":** Adheres to the "12-Factor App" methodology (Rule 3: Store config in the environment). It prevents hardcoding secrets into individual microservices and allows us to change settings without redeploying code.

**Key Code / Configuration:**
* `@EnableConfigServer`: Activates the Spring Cloud Config capabilities.
* `spring.profiles.active=native`: Bypasses the default requirement for a Git repository. Tells the server to read config files locally from `src/main/resources/config/`.

---

## Phase 2: Security & Routing

### 1. API Gateway
**The Goal:** Create a single entry point for all frontend client requests.
**The "Why":** Hides internal microservice IP addresses and ports from the outside world. It provides a centralized place to handle cross-cutting concerns like Rate Limiting, JWT Authentication, and CORS.

**Key Configuration:**
* `lb://<service-name>`: Used in our YAML routes instead of hardcoded HTTP addresses. This activates the Spring Cloud LoadBalancer. The Gateway dynamically fetches real IPs from Eureka and routes traffic in a round-robin fashion.

### 2. Identity Service Core
**The Goal:** Create a microservice responsible for Authentication (Auth-N) and Authorization (Auth-Z).
**The "Why":** Security should be isolated. If the Payment Service goes down, users should still be able to log in. By separating Identity, we can scale it independently.

**Key Code / Concepts:**
* `@RestController`: Tells Spring Boot this class handles HTTP requests and returns data (JSON/Text).
* `@RequestMapping("/api/auth")`: Sets the base URL for the entire controller.
* **The Gateway Routing Test:** When a request hits the Gateway (`:8080/api/auth/**`), the Gateway looks up the IP in Eureka and forwards it to Port `8081` silently. The user never knows Port `8081` exists.

---
## Phase 3: The Money Flow

### 1. Payment Service Foundation & Database
**The Goal:** Create the core business microservice responsible for orchestrating payments and saving transaction states to PostgreSQL.
**The "Why":** By isolating the payment logic into its own service, we ensure that high CPU tasks (like encrypting payloads for Stripe) don't slow down the Identity or Notification services.

**Key Technologies:**
* **Spring Data JPA:** Acts as an ORM (Object-Relational Mapper). It allows us to interact with the database using Java objects instead of writing raw SQL strings.
* **`ddl-auto: update`:** A crucial property for early development. It tells Hibernate to look at our Java code and automatically create/update the PostgreSQL tables. *(Note: Turn off in production in favor of Flyway/Liquibase).*

### 2. Stripe API Integration
**The Goal:** Securely process credit card transactions over the internet without touching sensitive card data directly.
**The "Why":** Handling raw credit card numbers requires intense legal and security compliance (PCI-DSS). By using Stripe, we only pass non-sensitive metadata (amount, currency), and Stripe handles the highly regulated banking layer.

**The Transaction Flow:**
1. **API Gateway:** Receives the POST request with JSON payment details and routes it to the Payment Service (`/api/payments/charge`).
2. **Database (Pending):** The Payment Service saves a `PENDING` record in PostgreSQL via JPA.
3. **Stripe API Call:** The backend uses the `stripe-java` SDK to create a `PaymentIntent`. (Note: Stripe expects all monetary amounts in cents, so $25.50 is sent as 2550).
4. **Database (Resolution):**
  * If Stripe succeeds, the database record is updated to `SUCCESS` and securely stores the Stripe `pi_...` Transaction ID.
  * If the network fails or the API key is rejected, the `catch` block safely updates the database row to `FAILED`.
---

## Phase 4: Security Lockdown, Zero Trust & Central Config

### Part 1: The Identity Service (The Mint)
**The Goal:** Create a dedicated, secure microservice responsible for user registration, password hashing, and generating JSON Web Tokens (JWTs).
**The "Why":** We are moving towards a Zero-Trust architecture. Before the API Gateway lets anyone access the core Payment Service, users must present a valid, cryptographically signed "VIP Wristband" (JWT).

**Key Technologies:**
* **Spring Security & BCrypt:** We do not use Spring Security to lock down our endpoints (the Gateway will do that later). Instead, we use its `BCryptPasswordEncoder` to securely hash user passwords before storing them in the database. Never store plain-text passwords!
* **JJWT Library:** An industry-standard Java library used to construct, sign, and compact JSON Web Tokens securely.
* **Java Records:** We utilized Java 14+ Records (`AuthRequest`, `AuthResponse`, `ErrorResponse`) as lightweight, immutable Data Transfer Objects (DTOs) to ensure clean, structured JSON communication.
* **`@RestControllerAdvice`:** Implemented a Global Exception Handler to catch runtime errors (like "Invalid Credentials") and automatically map them to professional HTTP status codes (`401 Unauthorized`, `409 Conflict`) with structured JSON error bodies.

**The Authentication Flow:**
1. **Registration (`/api/auth/register`):**
  * Accepts a username and password.
  * Checks PostgreSQL via JPA to ensure the username is unique.
  * Hashes the password using BCrypt and saves the `UserCredential` to the database.
2. **Login (`/api/auth/login`):**
  * Retrieves the user from the database.
  * Compares the provided password against the stored BCrypt hash.
  * If valid, the `JwtService` uses an HMAC-SHA algorithm and a secret key (injected via environment variables) to mint a JWT valid for 30 minutes.
  * Returns the JWT in a structured `200 OK` JSON response.

### Part 2: The API Gateway Bouncer (Edge Security)

**The Goal:** Intercept every incoming request to the Payment Service and verify the user's JWT "Wristband" before allowing the traffic to pass.

**The "Why":** Internal microservices (like the Payment Service) should not be exposed to the public internet or forced to handle authentication logic. By moving security to the "Edge" (the Gateway), we ensure a Zero-Trust environment where only cryptographically verified requests reach our core business logic.

#### The Reactive Pipeline (Netty & WebFlux)
Unlike our other services that use Tomcat (one thread per request), the API Gateway uses **Netty**. This is a non-blocking, event-driven engine that handles thousands of concurrent requests with a tiny memory footprint.

*   **ServerWebExchange:** Replaces the traditional `HttpServletRequest`. It allows us to stream request data and inspect headers asynchronously.
*   **AbstractGatewayFilterFactory:** The base class used to build our custom `AuthenticationFilter`.

#### The Authentication Flow
1.  **Header Extraction:** The filter looks for the `Authorization` header using `.getFirst(HttpHeaders.AUTHORIZATION)`.
2.  **Validation:** If a token is found, it strips the `Bearer ` prefix and hands the raw JWT to the `JwtUtil`.
3.  **Cryptographic Check:** `JwtUtil` uses our shared `jwt.secret` key to verify the mathematical signature. If the token was tampered with or has expired, it throws an exception.
4.  **The "Bounce":** If validation fails, the filter returns a `401 Unauthorized` response immediately, terminating the connection before it ever touches the Payment Service.
5.  **The "Pass":** If valid, `chain.filter(exchange)` is called, and the request is proxied to the downstream microservice.

#### Key Configuration
*   **AuthenticationFilter:** Applied specifically to the `payment-service` route in `application.yml`.
*   **Public Routes:** The `identity-service` routes (login/register) remain bypassable so new users can actually get their first token.

### Part 3: Centralized Configuration (The Brain)
**The Goal:** Decouple environment-specific settings (database URLs, ports, Stripe keys) from the individual microservice source code and manage them in a central location.
**The "Why":** Adheres to the "12-Factor App" methodology. It prevents hardcoding secrets into individual services and allows the fleet to dynamically pull configuration on startup.

**The "Handshake" Flow:**
1. **Config Server Boots:** Uses `@EnableConfigServer` and reads configuration files (e.g., `gateway-service.yml`, `payment-service.yml`) from its local classpath (`src/main/resources/config/`).
2. **Microservice Boots:** A client service (like the Gateway) starts up with a completely hollowed-out local `application.yml` containing only its name and a pointer to the Config Server.
3. **The Import:** The client uses `spring.config.import: "optional:configserver:http://localhost:8888"` to call the Config Server.
4. **Configuration Applied:** The Config Server returns the exact settings for that service in JSON format, which the client service dynamically applies to its environment.

#### Key Rule: **Eureka (The Phonebook) and the Config Server (The Brain) eep their own properties locally so they can bootstrap the environment. All other microservices pull their settings dynamically.**

---

## 🛠️ Common Fixes & Troubleshooting Runbook

### 🚨 Error: `ResourceAccessException: Connection refused`
* **The Error:** `I/O error on POST request for "http://localhost:8761/eureka/...: Connection refused"`
* **The Diagnosis:** A microservice is trying to register with the Eureka server, but Eureka is turned off or still booting up.
* **The Fix:** Order of operations matters. **Always start the `discovery-server` first**, wait for it to fully initialize, and then start the dependent microservices. Use the IntelliJ **Services** tab to manage this easily.

### 🚨 Gotcha: Spring Boot 4 & Spring Cloud 2025 Gateway Changes
* **The Problem:** Following older tutorials for Spring Cloud Gateway results in `404 Not Found` errors and missing dependencies.
* **The Cause:** The Spring team completely split the gateway engine. The old `spring-cloud-starter-gateway` dependency no longer exists.
* **The Fix:**
    1. **Dependency:** You must explicitly choose the reactive server in the `pom.xml`: `<artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>`.
    2. **YAML Config:** You can no longer use `spring.cloud.gateway.routes`. You must nest them one level deeper: `spring.cloud.gateway.server.webflux.routes`.

### 🚨 Gotcha: Database Startup Failures
* **The Cause:** Usually a port collision.
* **The Fix:** Check if you already have a local, native version of PostgreSQL running and hogging port `5432` on your machine. Also, ensure the Docker Desktop app is actually open before running `docker-compose up -d`.

### 🚨 Gotcha: 404 on Direct Service Hit
* **The Cause:** Controller is in the wrong folder.
* **The Fix:** Spring Boot only scans for `@RestController` classes if they are in the exact same folder (or a sub-folder) as the main `@SpringBootApplication` class. Move the file into the correct package.

### 🚨 Error: Config Server Returns 404 (Whitelabel Error Page)
* **The Error:** Hitting `http://localhost:8888/gateway-service/default` returns a Whitelabel 404.
* **The Cause:** The `@EnableConfigServer` annotation is missing from the main class, or the server cannot find the specified `native` search location.
* **The Fix:** Ensure `@EnableConfigServer` is present in your main application class and verify the `search-locations` path matches exactly (e.g., `classpath:/config/`).

### ⚠️ Gotcha: Config Server Properties Are "Invisible" or Stale
* **The Problem:** You added a new `payment-service.yml` to the Config Server, but it won't show up in the browser, and the Payment Service crashes without its properties.
* **The Cause:** When using the `classpath:/config/` approach, the Config Server only sees the files that were compiled into the `target/classes` folder during the last build.
* **The Fix:** Stop the Config Server, run `mvn clean install` on the `config-server` module to move the newly created YAML files into the target directory, and restart it.

### ⚠️ Gotcha: "Empty Nest" Client Service Failure
* **The Problem:** After hollowing out a service's `application.yml` (like the Gateway or Payment service), it starts up on the default port `8080` with zero properties and missing routes.
* **The Cause:** The microservice either lacks the `spring-cloud-starter-config` dependency to perform the handshake, or its local `application.yml` is missing the import instructions.
* **The Fix:**
  1. Add the `spring-cloud-starter-config` dependency to the microservice's `pom.xml`.
  2. Ensure the local `application.yml` looks exactly like this:
     ```yaml
     spring:
       application:
         name: [service-name] # Must match the filename in the config server
       config:
         import: "optional:configserver:http://localhost:8888"
     ```