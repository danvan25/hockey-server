![Java](https://img.shields.io/badge/Java-23-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-green)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Status](https://img.shields.io/badge/status-under_development-yellow)

# 🏒 Hockey Server

A Spring Boot backend server for a multiplayer Air Hockey game.

This project is the backend component of the Hockey Game ecosystem, providing user management, authentication, persistence, and future multiplayer networking capabilities.

The server is currently under active development and follows a test-driven and incremental development approach.

---

# 📖 Project Overview

The Hockey Server is responsible for:

- User registration
- User authentication
- Secure password storage
- Database management
- Multiplayer game session management (planned)
- Player statistics
- Match history
- REST API for the Android client

The Android application communicates exclusively with this backend through HTTP requests.

---

# 🏗 Current Architecture

```
Android Client
        │
        │ HTTP / JSON
        ▼
Spring Boot REST API
        │
        ▼
Service Layer
        │
        ▼
Repository (Spring Data JPA)
        │
        ▼
MySQL Database
```

---

# 🚀 Technologies

### Backend

- Java 24
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Spring Security
- Hibernate
- MySQL

### Testing

- JUnit 5
- Spring Boot Test
- MockMvc

### Tools

- IntelliJ IDEA
- Git
- GitHub
- MySQL Workbench

---

# ✅ Features Implemented

## User Management

- User entity
- User repository
- User service
- Registration validation
- Duplicate username detection
- Duplicate email detection

---

## REST API

- Authentication controller
- Registration endpoint
- JSON request handling
- Validation support

---

## Exception Handling

Global exception handling using:

- GlobalExceptionHandler
- Custom exception classes

Including:

- UsernameAlreadyExistsException
- EmailAlreadyExistsException

---

## Database

- MySQL integration
- Dedicated application user
- Hibernate automatic schema generation
- UTF-8 database configuration

Current database:

```
hockey_db
```

---

## Security

Current progress:

- Spring Security configured
- Basic security filter chain
- Protected endpoints

---

## Testing

The project is developed together with automated tests.

Implemented:

- Application startup test
- UserService unit tests
- Repository tests
- Controller tests
- Exception handler tests

---

# 📂 Project Structure

```
src
 ├── main
 │   ├── config
 │   ├── controller
 │   ├── dto
 │   ├── entity
 │   ├── exception
 │   ├── repository
 │   ├── security
 │   ├── service
 │   └── util
 │
 └── test
     ├── controller
     ├── entity
     ├── integration
     ├── repository
     └── service
```

---

# 🛣 Development Roadmap

## Authentication

- Login endpoint
- BCrypt password hashing
- JWT authentication
- Refresh tokens

---

## Multiplayer

- LAN multiplayer
- Online matchmaking
- Lobby system
- Game session management

---

## Statistics

- Player profile
- Match history
- Win/Loss statistics
- Leaderboards

---

## Friends System

- Friend requests
- Friends list
- Online status

---

## Future Improvements

- Docker support
- CI/CD pipeline
- Flyway database migrations
- OpenAPI / Swagger documentation
- Logging
- Monitoring
- Rate limiting

---

# 🎯 Development Philosophy

This project is intentionally built step by step.

The primary goals are:

- Learn modern backend development
- Understand Spring Boot architecture
- Practice clean architecture
- Build reliable automated tests
- Apply secure coding practices
- Develop production-ready software

Rather than rushing to implement features, every component is designed to be understood before moving forward.

---

# 📌 Current Status

✔ Spring Boot backend

✔ REST API foundation

✔ MySQL integration

✔ Automated testing

✔ Registration module

🚧 Login system

🚧 JWT authentication

🚧 Android integration

🚧 Multiplayer server

---

# 👨‍💻 Author

Developed by **Daniel Magyar**

Project created for learning modern backend development, software architecture and multiplayer server design.
