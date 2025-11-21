# FullStackAuth System

A robust Spring Boot authentication system with JWT, Email Verification, and Role-Based Access Control.

## 🚀 Quick Setup

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

## 📂 Project Structure

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

## 🏗️ Architecture

**Flow**: `Request` → `Controller` → `Service` → `Repository` → `Database`

1.  **Controller**: Handles HTTP requests & responses.
2.  **Service**: Contains business logic (e.g., sending emails, hashing passwords).
3.  **Repository**: Interacts with MySQL database.
4.  **Security**: `JwtRequestFilter` intercepts requests to validate tokens before reaching Controllers.

---

## 🔄 System Flow

### 1. Registration & Verification
`User` → **Sign Up** → `DB` (Account Created, Unverified) → `Email Service` (Sends Token) → `User` (Clicks Link) → `API` (Verifies Token) → `DB` (Account Verified)

### 2. Authentication
`User` → **Login** → `API` (Validates Creds) → `JWT Service` (Generates Token) → `User` (Receives Token & Cookie)

---

## 🔮 Future Roadmap

- [ ] **Refresh Tokens**: For long-lived sessions without re-login.
- [ ] **OAuth2 Login**: Sign in with Google/GitHub.
- [ ] **Two-Factor Auth (2FA)**: Extra security layer.
- [ ] **Rate Limiting**: Prevent abuse of APIs.
- [ ] **Audit Logs**: Track important security events.

---

## 🧪 Testing with Postman

**Step 1:** Locate the file `Auth rest apis.postman_collection.json` in the project root.
**Step 2:** Import this file into Postman.
**Step 3:** Start testing the endpoints!

---

## 📡 API Endpoints

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
| `GET` | `/test/whoami` | Check your current role & name |

### Admin (Protected - Admin Only)
*Requires `ADMIN` role*

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/admin/test` | Verify admin access |

---

## 🔑 Role Configuration
New users are `USER` by default. To make an admin:
1. Register a user.
2. Run SQL: `INSERT INTO tbl_user_roles (user_id, role_id) VALUES (1, 2);` (Adjust IDs as needed).
