# 📓 NexusPay Developer Diary

## 📑 Table of Contents
1. [Project Port Directory](#-project-port-directory)
2. [Phase 1: Infrastructure & Discovery](#phase-1-infrastructure--discovery)
3. [Phase 2: Security & Routing](#phase-2-security--routing)
4. [Phase 3: The Money Flow](#phase-3-the-money-flow)
5. [🛠️ Common Fixes & Troubleshooting Runbook](#%EF%B8%8F-common-fixes--troubleshooting-runbook)

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