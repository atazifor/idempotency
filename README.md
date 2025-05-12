# Idempotency and Retry Logic in Reactive Spring Boot

This is part of a structured learning series focused on building robust, production-ready APIs. In this day, we tackle the challenges of **duplicate request handling** using `Idempotency-Key` support in a wallet top-up use case.

---

## 📌 Problem We're Solving

When building APIs — especially for money movement — retries can cause **serious issues**:

- A mobile app user taps “Top Up” and their request times out...
- The app retries — **was it already processed?**
- If not handled, the user may be **charged twice** for the same request.

We solve this using the concept of an **idempotent API**:
> A request with the same `Idempotency-Key` should always return the same result.

---

## ✅ What This Project Includes

- A `POST /api/wallet/topup` API (Reactive / WebFlux)
- Accepts a custom `Idempotency-Key` header
- Caches responses per key in memory
- Returns same response for duplicate keys
- Demonstrates client responsibilities and best practices

---

## ⚙️ Tech Stack

| Tool              | Purpose                         |
|------------------|---------------------------------|
| Java 17+         | Programming language            |
| Spring Boot 3.4.x | App framework                   |
| Spring WebFlux    | Reactive REST API               |
| ConcurrentHashMap | Simulated in-memory key store  |
| Postman           | Manual API testing              |

---

## 🛠️ How to Run Locally

```bash
# Clone this repo
git clone https://github.com/atazifor/idempotency.git
cd idempotency

# Run the app
./mvnw spring-boot:run
