# FullStackAuth System
# [Frontend Url](https://github.com/arpondark/auth-front)

A robust Spring Boot authentication system with JWT, Email Verification, and Role-Based Access Control.

## Quick Setup

### 1. Database
Create a MySQL database named `springdb`:
```sql
CREATE DATABASE springdb;
```

### 2. Environment Variables
Create a `.env` file in the root directory or set these in your IDE/System:
```properties
EMAIL_USER=your-email@gmail.com
EMAIL_PASS=your-app-password
```
*(Note: Use a Gmail App Password, not your regular password)*

### 3. Run Application
```bash
./mvnw spring-boot:run
```
Server starts at: `http://localhost:8080`

---

## Project Structure

```
src/main/java/com/arpon007/FullStackAuth
├── config/          # Security & App Config (SecurityConfig, Cors)
├── Controller/      # API Endpoints (AuthController, ProfileController)
├── Service/         # Business Logic (EmailService, ProfileService)
├── Entity/          # Database Models (UserEntity, RoleEntity)
├── repository/      # Database Access (UserRepository)
├── Filter/          # JWT Request Filter
├── Util/            # Utilities (JwtUtil)
└── Io/              # DTOs (Request/Response objects)
```

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot Application                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              HTTP Requests (REST API)                     │   │
│  └────────────────┬─────────────────────────────────────────┘   │
│                   │                                              │
│  ┌────────────────▼────────────┐     ┌──────────────────────┐   │
│  │   AuthController            │     │  ProfileController   │   │
│  │  ├─ POST /auth/login        │     │  ├─ POST /auth/register│ │
│  │  ├─ GET /auth/isAuthenticated│     │  ├─ GET /profile    │   │
│  │  ├─ GET /auth/verify        │     │  └─ PUT /profile    │   │
│  │  ├─ POST /auth/request-      │     │                      │   │
│  │  │      password-reset       │     └──────────────────────┘   │
│  │  └─ POST /auth/reset-password│                                 │
│  └────────────────┬────────────────────────┘                    │
│                   │                                              │
│  ┌────────────────▼─────────────────────────────────────────┐   │
│  │            Security Filter Chain (JwtRequestFilter)       │   │
│  │  1. Extract JWT from Authorization header or cookie       │   │
│  │  2. Validate token signature                              │   │
│  │  3. Check token expiration                                │   │
│  │  4. Extract user email from token claims                  │   │
│  │  5. Set SecurityContext with authentication               │   │
│  └────────────────┬─────────────────────────────────────────┘   │
│                   │                                              │
│  ┌────────────────▼─────────────────────────────────────────┐   │
│  │         Service Layer (Business Logic)                    │   │
│  │  ├─ AppUserDetailsService (Load user credentials)         │   │
│  │  ├─ ProfileService (User profile operations)              │   │
│  │  ├─ EmailService (Send verification/reset emails)         │   │
│  │  └─ JwtUtil (Generate & validate JWT tokens)              │   │
│  └────────────────┬─────────────────────────────────────────┘   │
│                   │                                              │
│  ┌────────────────▼─────────────────────────────────────────┐   │
│  │           Repository Layer (Data Access)                  │   │
│  │  └─ UserRepository (Database queries)                     │   │
│  └────────────────┬─────────────────────────────────────────┘   │
│                   │                                              │
│  ┌────────────────▼─────────────────────────────────────────┐   │
│  │           Database (MySQL)                                │   │
│  │  └─ UserEntity (User data with security fields)           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                   │                                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## System Flow

### 1. Login Flow (Step-by-Step)

```
┌──────────────────────────────────────────────────────────────────┐
│ Client                                   Server                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│ 1. Submit login form                                             │
│    POST /api/v1/auth/login                                       │
│    {                                                             │
│      "email": "user@example.com",                               │
│      "password": "secret123"                                    │
│    }                   ──────────────────────────────►          │
│                                                                   │
│                                        ┌─ Verify credentials    │
│                                        │  (AuthenticationManager)
│                                        │                        │
│                                        ├─ Check if account     │
│                                        │  is verified          │
│                                        │  (MUST be verified!)  │
│                                        │                        │
│                                        ├─ Load user details    │
│                                        │  (AppUserDetailsService)
│                                        │                        │
│                                        ├─ Generate JWT token   │
│                                        │  (JwtUtil)            │
│                                        │                        │
│                                        ├─ Create HTTP-only     │
│                                        │  cookie               │
│                                        │                        │
│ 2. Receive token & cookie (OR ERROR if unverified)             │
│    ◄──────────────────────────────────                          │
│    Success:                                                      │
│    {                                                             │
│      "email": "user@example.com",                               │
│      "token": "eyJhbGc..."                                      │
│    }                                                             │
│    Set-Cookie: jwt=eyJhbGc...; HttpOnly; Path=/                │
│                                                                   │
│ 3. Store token & cookie (if successful)                         │
│    - Token: localStorage or sessionStorage (for manual sending) │
│    - Cookie: Automatically managed by browser                   │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 2. Registration & Verification Flow

```
┌─────────────────────────────────────────────────────────┐
│ 1. User registers (POST /api/v1/auth/register)          │
│    { name, email, password }                            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 2. User account created                                  │
│    isAccountVerified = false                            │
│    Verification token (UUID) generated automatically   │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 3. Verification email sent automatically                │
│    EmailService.sendVerificationLinkEmail()            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 4. User clicks verification link                        │
│    GET /api/v1/auth/verify?token=abc-123-def...        │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 5. Mark account as verified                            │
│    isAccountVerified = true                            │
└─────────────────────────────────────────────────────────┘
```

---

## Future Roadmap

- [ ] **Refresh Tokens**: For long-lived sessions without re-login.
- [x] **OAuth2 Login**: Sign in with Google.
- [ ] **Two-Factor Auth (2FA)**: Extra security layer.
- [ ] **Rate Limiting**: Prevent abuse of APIs.
- [ ] **Audit Logs**: Track important security events.

---

## Google OAuth2 Setup

1.  **Create Google Cloud Project**: Go to [Google Cloud Console](https://console.cloud.google.com/).
2.  **Enable APIs**: Enable "Google People API" or just "Google+ API" (legacy) - actually just "Google Identity" setup.
3.  **Create Credentials**:
    *   Create OAuth Client ID.
    *   Application Type: Web Application.
    *   Authorized Redirect URIs: `http://localhost:8080/api/v1/login/oauth2/code/google`
4.  **Update .env**:
    ```properties
    GOOGLE_CLIENT_ID=your-client-id
    GOOGLE_CLIENT_SECRET=your-client-secret
    ```

### OAuth2 Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/oauth2/authorization/google` | Initiate Google Login (Browser) |
| `GET` | `/api/v1/login/oauth2/code/google` | Callback URL (Handled by Spring Security) |


---

## Testing with Postman

**Step 1:** Locate the file `Auth rest apis.postman_collection.json` in the project root.
**Step 2:** Import this file into Postman.
**Step 3:** Start testing the endpoints!

---

## API Endpoints

All endpoints are prefixed with `/api/v1`

### Authentication (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/login` | Login & get JWT token |
| `POST` | `/auth/register` | Signup & receive verification email |
| `GET` | `/auth/verify` | Verify email (via link/token) |
| `POST` | `/auth/resend-verification` | Resend verification email |
| `POST` | `/auth/request-password-reset` | Request password reset link |
| `POST` | `/auth/reset-password` | Reset password with token |

### User Profile (Protected)
*Requires `Authorization: Bearer <token>` header*

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/profile/me` | Get current user details |
| `PUT` | `/profile/me` | Update profile |
| `POST` | `/profile/change-password/init` | Initiate password change (sends OTP) |
| `POST` | `/profile/change-password/verify` | Verify OTP & update password |
| `POST` | `/profile/change-email/init` | Initiate email change (sends OTP to new email) |
| `POST` | `/profile/change-email/verify` | Verify OTP & update email |

### Admin Operations (Protected)
*Requires `Authorization: Bearer <token>` with `ADMIN` role*

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/admin/users` | Create new Admin or User account |

---

## Role Configuration
New users are `USER` by default. To make an admin:
1. Register a user.
2. Run SQL: `INSERT INTO tbl_user_roles (user_id, role_id) VALUES (1, 2);` (Adjust IDs as needed).
