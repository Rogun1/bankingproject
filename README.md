# Banking Project

A backend application simulating a digital banking system, built with Java and Spring Boot.
Covers real-world concepts like JWT authentication, atomic money transfers, 
role-based authorization, and transaction limit enforcement.

## Tech Stack

- Java 21 + Spring Boot 3
- Spring Security + JWT
- PostgreSQL + Spring Data JPA / Hibernate
- Docker - PostgreSQL instance
- JUnit 5 + Mockito + Testcontainers
- Postman

## Features

- User registration and login with JWT authentication and BCrypt password hashing
- Role-based authorization (CUSTOMER, ADMIN, MANAGER)
- Account and card management with IBAN generation and status control
- Atomic money transfers with deadlock prevention
- Transaction limit enforcement (daily limit, per-transaction limit, max daily count)
- Custom exception handling with appropriate HTTP status codes
- Multi-profile configuration (local / production) with HTTPS enforced in production
- Unit and integration tests with a real PostgreSQL instance via Testcontainers

## Prerequisites

- Java 21
- Maven
- Docker (for PostgreSQL)

## Getting Started

1. Start the PostgreSQL container
```bash
docker run --name bankingproject-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  -e POSTGRES_DB=bankingproject \
  -p 5002:5432 \
  -d postgres:16
```

2. Clone the repository
```bash
git clone https://github.com/Rogun1/bankingproject.git
cd bankingproject
```

3. Run the application
```bash
mvn spring-boot:run
```

## API Documentation

Postman Utilization

## Environment Variables

All variables have defaults and work out of the box for local development.

| Variable | Default | Description |
|---|---|---|
| DATABASE_HOST | localhost | PostgreSQL host |
| DATABASE_PORT | 5002 | PostgreSQL port |
| DATABASE_NAME | bankingproject | Database name |
| DATABASE_USERNAME | postgres | Database user |
| DATABASE_PASSWORD | root | Database password |
| DDL_UPDATE | update | Hibernate DDL mode |
| JPA_SHOW_SQL | true | Log SQL queries |

## Running Tests

```bash
mvn test
```

Integration tests spin up a real PostgreSQL instance automatically via Testcontainers.
No manual setup required.
