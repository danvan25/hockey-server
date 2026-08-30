![Java](https://img.shields.io/badge/Java-24-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-green)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Status](https://img.shields.io/badge/status-under_development-yellow)

# Hockey Server

A Spring Boot backend for the Android Air Hockey game. It currently provides user registration, login, validation, secure password storage, persistence, and structured API errors. JWT authentication, server-backed lobbies, and real-time multiplayer are planned next.

## Current status

### Implemented

- Spring Boot REST API foundation
- MySQL persistence through Spring Data JPA
- Dedicated application database account
- Database password loaded from the `DB_PASSWORD` environment variable
- User entity with role, creation time, wins, and losses
- Unique usernames and email addresses
- BCrypt password hashing
- `USER` and `ADMIN` roles
- Registration through `POST /api/auth/register`
- Login through `POST /api/auth/login`
- Jakarta request validation
- Structured `400 Bad Request`, `401 Unauthorized`, and `409 Conflict` responses
- Global handling for validation, duplicate-account, invalid-credential, and unexpected errors
- Public health, registration, and login endpoints
- Entity, service, and controller tests using JUnit, Mockito, and MockMvc
- Verified registration and login against the development MySQL database

### Not implemented yet

- JWT access tokens and stateless authentication
- Refresh tokens
- Authenticated user profile endpoints
- Friends, lobby, matchmaking, and match persistence
- WebSocket-based real-time multiplayer
- Controlled Flyway migrations
- Docker, CI/CD, monitoring, and production deployment

## API

| Method | Endpoint | Authentication | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/health` | Public | Server health check |
| `POST` | `/api/auth/register` | Public | Create a user account |
| `POST` | `/api/auth/login` | Public | Validate credentials and return user data |

The login endpoint does not issue a token yet. Protected application endpoints will be introduced with JWT authentication.

## Architecture

```text
Android Client
      |
      | HTTP / JSON
      v
Spring Boot Controllers
      |
      v
Service Layer
      |
      v
Spring Data JPA Repository
      |
      v
MySQL Database
```

## Technology

- Java 24
- Spring Boot 4.1
- Spring Web
- Spring Security
- Spring Data JPA and Hibernate
- Jakarta Validation
- MySQL 8
- Maven Wrapper
- JUnit 5, Mockito, and MockMvc

## Configuration

The development database is configured as:

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/hockey_db
spring.datasource.username=hockey_app
spring.datasource.password=${DB_PASSWORD}
```

Define `DB_PASSWORD` before starting the application. Do not commit database credentials to the repository.

## Project structure

```text
src
├── main
│   ├── java/com/example/hockeyserver
│   │   ├── config
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── exception
│   │   ├── repository
│   │   └── service
│   └── resources
└── test/java/com/example/hockeyserver
    ├── controller
    ├── entity
    └── service
```

## Agreed development order

1. Connect Android registration to the existing registration endpoint.
2. Add JWT generation, validation, filtering, and stateless security.
3. Store and attach the JWT securely in the Android client.
4. Add authenticated profile and statistics endpoints.
5. Implement server-backed lobby creation and joining.
6. Add authenticated WebSocket communication and game-state synchronization.
7. Persist match results and update player statistics.
8. Add production tooling, deployment, and security hardening.

See [ROADMAP.md](ROADMAP.md) for the complete milestone plan.

## Development approach

The backend is developed incrementally. Each feature should be implemented, tested, documented, and committed before moving to the next major stage. The project is intended both as a learning exercise and as a portfolio-quality multiplayer backend.

## Author

Developed by **Daniel Magyar**.
