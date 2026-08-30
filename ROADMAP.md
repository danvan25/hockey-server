# 🏒 Hockey Server Roadmap

This roadmap tracks the planned development of the backend for the multiplayer Air Hockey Android game.

The project is developed incrementally. Each milestone is implemented, tested, documented and committed before moving to the next major feature.

---

## ✅ Milestone 1 — Project Foundation

- [x] Create Spring Boot project
- [x] Configure Maven
- [x] Create GitHub repository
- [x] Define layered package structure
- [x] Add Spring Web
- [x] Add Spring Security
- [x] Add Spring Data JPA
- [x] Add Jakarta Validation
- [x] Add MySQL Driver
- [x] Create health endpoint
- [x] Add health controller test

---

## ✅ Milestone 2 — User Registration

- [x] Create `User` entity
- [x] Add username and email uniqueness constraints
- [x] Store password as a BCrypt hash
- [x] Add win and loss statistics
- [x] Add account creation timestamp
- [x] Create `UserRepository`
- [x] Create registration request and response DTOs
- [x] Validate registration requests
- [x] Detect duplicate usernames
- [x] Detect duplicate email addresses
- [x] Create registration service
- [x] Create `POST /api/auth/register`
- [x] Add global exception handling
- [x] Return structured API error responses
- [x] Add unit and controller tests
- [x] Connect the backend to MySQL
- [x] Verify real user registration through the HTTP client

---

## ✅ Milestone 3 — User Roles

- [x] Create `Role` enum
- [x] Add `USER` and `ADMIN` roles
- [x] Add role field to the `User` entity
- [x] Store roles as readable strings in MySQL
- [x] Assign the `USER` role during registration
- [x] Extend unit tests for role assignment

---

## ✅ Milestone 4 — Login

- [x] Create login request DTO
- [x] Create login response DTO
- [x] Create `InvalidCredentialsException`
- [x] Find users by username
- [x] Verify passwords with BCrypt
- [x] Implement login service logic
- [x] Add login service unit tests
- [x] Add `POST /api/auth/login`
- [x] Permit the login endpoint in Spring Security
- [x] Return `401 Unauthorized` for invalid credentials
- [x] Add login controller tests
- [x] Add login request to `api-tests.http`
- [x] Verify login against the real MySQL database

---

## Next Implementation Order

The immediate development sequence agreed for the Android client and backend is:

1. Connect the Android registration screen to `POST /api/auth/register`.
2. Add Android request and response DTOs plus Retrofit registration support.
3. Display backend validation and duplicate-account errors in the Android client.
4. Add JWT generation and validation to the backend.
5. Store and attach the JWT securely in the Android client.
6. Implement authenticated profile and statistics features.
7. Build the server-backed lobby system.
8. Add authenticated real-time multiplayer communication.

---

## 🚧 Milestone 5 — Android Registration Integration

- [x] Configure LAN access for the Spring Boot server
- [x] Add Android network permission
- [x] Configure development HTTP access
- [x] Add Retrofit and Gson
- [x] Connect the Android login screen to the API
- [x] Handle login failures and connection errors
- [ ] Create Android registration request and response DTOs
- [ ] Add the registration method to the Retrofit API
- [ ] Connect the Android registration screen to the API
- [ ] Display backend validation errors
- [ ] Display duplicate username and email errors
- [ ] Return to login after successful registration
- [ ] Move the development server URL out of the Retrofit client source
- [ ] Add meaningful Android authentication tests

---

## ⬜ Milestone 6 — JWT Authentication

- [ ] Generate JWT tokens after successful login
- [ ] Include user identity and role in the token
- [ ] Create JWT validation service
- [ ] Create authentication filter
- [ ] Configure stateless Spring Security
- [ ] Reject missing, invalid and expired tokens
- [ ] Protect private API endpoints
- [ ] Add authentication tests
- [ ] Define token expiration strategy
- [ ] Evaluate refresh-token support

---

## ⬜ Milestone 7 — Android JWT Integration

- [ ] Accept the JWT returned by login
- [ ] Store the JWT securely on the Android device
- [ ] Add the JWT to protected requests
- [ ] Restore authenticated state when the application restarts
- [ ] Clear authentication state during logout
- [ ] Handle missing and expired tokens
- [ ] Handle connection failures and timeouts

---

## ⬜ Milestone 8 — User Profile and Statistics

- [ ] Create `GET /api/users/me`
- [ ] Return authenticated user data
- [ ] Add profile update endpoint
- [ ] Add password change endpoint
- [ ] Track wins and losses
- [ ] Calculate total matches
- [ ] Calculate win rate
- [ ] Add match history
- [ ] Add player rating or ELO
- [ ] Add leaderboard endpoint

---

## ⬜ Milestone 9 — Friends System

- [ ] Search users by username
- [ ] Send friend requests
- [ ] Accept or reject friend requests
- [ ] List friends
- [ ] Remove friends
- [ ] Prevent duplicate requests
- [ ] Prevent users from adding themselves
- [ ] Add friend-system tests
- [ ] Display online status later

---

## ⬜ Milestone 10 — Lobby System

- [ ] Create a game lobby
- [ ] Generate a unique room code
- [ ] Join a lobby by room code
- [ ] Leave a lobby
- [ ] Track host and guest
- [ ] Add ready states
- [ ] Start the match when both players are ready
- [ ] Handle disconnected players
- [ ] Expire abandoned lobbies

---

## ⬜ Milestone 11 — Real-Time Multiplayer

- [ ] Add Spring WebSocket support
- [ ] Establish authenticated WebSocket connections
- [ ] Define multiplayer message formats
- [ ] Transfer player input
- [ ] Synchronize mallet positions
- [ ] Synchronize puck position and velocity
- [ ] Synchronize score and countdown
- [ ] Handle latency and interpolation
- [ ] Add reconnect support
- [ ] Decide which game state is authoritative
- [ ] Detect invalid or impossible client input

---

## ⬜ Milestone 12 — Match Persistence

- [ ] Create match entity
- [ ] Store participating players
- [ ] Store final score
- [ ] Store winner and loser
- [ ] Store match start and finish times
- [ ] Update player statistics transactionally
- [ ] Prevent duplicate match results
- [ ] Add repository and integration tests

---

## ⬜ Milestone 13 — Testing Improvements

- [x] Add entity unit tests
- [x] Add service unit tests
- [x] Add controller tests with MockMvc
- [x] Use Mockito mocks and argument captors
- [ ] Add repository tests
- [ ] Add full registration integration test
- [ ] Add full login integration test
- [ ] Add security integration tests
- [ ] Introduce Testcontainers with MySQL
- [ ] Add WebSocket tests
- [ ] Add test coverage reporting

---

## ⬜ Milestone 14 — Database Migrations

- [ ] Replace Hibernate schema updates with controlled migrations
- [ ] Add Flyway
- [ ] Create initial schema migration
- [ ] Add migration for user roles
- [ ] Add migrations for matches, friends and lobbies
- [ ] Document migration workflow
- [ ] Use `validate` instead of `update` in production

---

## ⬜ Milestone 15 — API Documentation

- [ ] Add OpenAPI / Swagger
- [ ] Document request and response DTOs
- [ ] Document authentication requirements
- [ ] Document validation errors
- [ ] Document HTTP status codes
- [ ] Add API usage examples
- [ ] Keep `api-tests.http` as a developer test collection

---

## ⬜ Milestone 16 — Security Hardening

- [x] Avoid storing raw passwords
- [x] Keep database credentials outside Git
- [x] Use a dedicated MySQL application account
- [ ] Add request rate limiting
- [ ] Add login attempt protection
- [ ] Add account locking or cooldown
- [ ] Add secure CORS configuration
- [ ] Add production HTTPS requirements
- [ ] Review sensitive logging
- [ ] Add secure token storage guidance
- [ ] Review authorization on every protected endpoint

---

## ⬜ Milestone 17 — Docker

- [ ] Create Spring Boot Dockerfile
- [ ] Create MySQL container configuration
- [ ] Add Docker Compose
- [ ] Move configuration to environment variables
- [ ] Add persistent database volume
- [ ] Add container health checks
- [ ] Document local container startup

---

## ⬜ Milestone 18 — CI/CD

- [ ] Add GitHub Actions workflow
- [ ] Build the project on every push
- [ ] Run automated tests
- [ ] Reject failing pull requests
- [ ] Add build-status badge
- [ ] Add test-coverage badge
- [ ] Build deployable JAR or container image
- [ ] Prepare automated deployment

---

## ⬜ Milestone 19 — Monitoring and Logging

- [ ] Add Spring Boot Actuator
- [ ] Add health and readiness endpoints
- [ ] Introduce structured logging
- [ ] Add request correlation identifiers
- [ ] Track authentication failures
- [ ] Track active matches and WebSocket connections
- [ ] Add metrics
- [ ] Evaluate Prometheus and Grafana

---

## ⬜ Milestone 20 — Linux Deployment

- [ ] Deploy the backend to AlmaLinux
- [ ] Run the application as a dedicated system user
- [ ] Create a systemd service
- [ ] Deploy MySQL securely
- [ ] Configure firewalld
- [ ] Configure SELinux correctly
- [ ] Add Nginx reverse proxy
- [ ] Configure a dedicated API subdomain
- [ ] Add HTTPS with Let's Encrypt
- [ ] Keep MySQL inaccessible from the public internet
- [ ] Add backup and restore procedures

---

## ⬜ Milestone 21 — Production Readiness

- [ ] Separate development and production profiles
- [ ] Disable development SQL logging
- [ ] Replace automatic schema updates
- [ ] Define production secrets management
- [ ] Add database backups
- [ ] Add error monitoring
- [ ] Add graceful shutdown
- [ ] Add load and performance testing
- [ ] Review privacy and account deletion requirements
- [ ] Publish the Android client configuration for production

---

## Future Ideas

These features are outside the initial scope but may be considered later:

- [ ] Avatars
- [ ] Achievements
- [ ] Seasonal rankings
- [ ] Match replays
- [ ] Spectator mode
- [ ] In-game chat
- [ ] Friend invitations
- [ ] Push notifications
- [ ] AI-controlled opponent
- [ ] Multiple languages
- [ ] Moderator role
- [ ] Administration dashboard

---

## Current Focus

The current development goal is:

> Connect the Android registration screen to the existing registration endpoint, including Retrofit DTOs and clear validation or duplicate-account errors.

After that, development will continue with backend JWT authentication, secure Android token handling, authenticated profile features, and the server-backed lobby system.
