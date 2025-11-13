# FullStackAuth - Complete Authentication System Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [JWT (JSON Web Token) Explanation](#jwt-explanation)
4. [Authentication Flow](#authentication-flow)
5. [API Endpoints](#api-endpoints)
6. [Security Configuration](#security-configuration)
7. [Email Verification System](#email-verification-system)
8. [Password Reset System](#password-reset-system)
9. [Setup & Configuration](#setup--configuration)
10. [Testing Guide](#testing-guide)
11. [Error Handling](#error-handling)
12. [Security Best Practices](#security-best-practices)

---

## Overview

**FullStackAuth** is a comprehensive authentication and authorization system built with:
- **Backend**: Spring Boot 3.x + Spring Security
- **Database**: MySQL
- **Token Management**: JWT (JSON Web Tokens)
- **Email Service**: SMTP (Gmail integration)
- **Security**: HS256 (HMAC-SHA256) token signing

### Key Features
✅ User registration and login with JWT authentication  
✅ Email-based OTP verification for account activation  
✅ Password reset with OTP validation  
✅ HTTP-only secure cookies for token storage  
✅ JWT token validation via filter  
✅ CORS-enabled for frontend integration  
✅ Comprehensive error handling  
✅ Role-based access control ready (Spring Security)  

---

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
│  │  ├─ POST /auth/login        │     │  ├─ GET /profile    │   │
│  │  ├─ GET /auth/isAuthenticated     │  └─ PUT /profile    │   │
│  │  ├─ POST /auth/send-otp          │                      │   │
│  │  ├─ POST /auth/verify-otp        └──────────────────────┘   │
│  │  ├─ POST /auth/send-reset-otp         │                     │
│  │  └─ POST /auth/reset-password         │                     │
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
└─────────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Purpose |
|-----------|---------|
| **AuthController** | Handles login, OTP verification, password reset endpoints |
| **ProfileController** | Manages user profile operations |
| **JwtRequestFilter** | Intercepts requests and validates JWT tokens |
| **JwtUtil** | Creates and validates JWT tokens |
| **SecurityConfig** | Configures Spring Security (CORS, authentication, authorization) |
| **AppUserDetailsService** | Loads user details from database for authentication |
| **EmailService** | Sends verification and password reset emails |
| **UserEntity** | Database model for user data |
| **UserRepository** | JPA repository for database operations |

---

## JWT Explanation

### What is JWT?

A **JSON Web Token (JWT)** is a self-contained, digitally signed token that proves a user's identity. It's like a tamper-proof ID card issued by your server.

### JWT Structure

A JWT consists of three parts separated by dots (`.`):

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.signature
│                                         │                                                    │
└─ HEADER (Base64URL encoded)    └─ PAYLOAD (Base64URL encoded)        └─ SIGNATURE
```

#### 1. **Header**
Contains token type and signing algorithm:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

#### 2. **Payload**
Contains user claims (data):
```json
{
  "sub": "user@example.com",
  "iat": 1516239022,
  "exp": 1516242622
}
```

Common claims:
- `sub` (subject): User identifier (email)
- `iat` (issued at): Token creation timestamp
- `exp` (expiration): Token expiration timestamp

#### 3. **Signature**
Created using the header, payload, and a secret key:
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### Why Use JWT?

✅ **Stateless**: No server-side session storage needed  
✅ **Self-contained**: All user info is in the token  
✅ **Secure**: Signed with a secret key (tampering detected)  
✅ **Scalable**: Works well with microservices  
✅ **Mobile-friendly**: Can be used by any HTTP client  
✅ **CORS-friendly**: Works across different domains  

### Security Algorithm: HS256

This app uses **HMAC-SHA256** to sign tokens:
- **HMAC**: Hash-based Message Authentication Code
- **SHA256**: Cryptographic hash function (256-bit output)
- **Secret Key**: Kept on the server; prevents token forgery

If someone tries to modify a token without the secret key, the signature becomes invalid.

---

## Authentication Flow

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
│                                        ├─ Load user details    │
│                                        │  (AppUserDetailsService)
│                                        │                        │
│                                        ├─ Generate JWT token   │
│                                        │  (JwtUtil)            │
│                                        │                        │
│                                        ├─ Create HTTP-only     │
│                                        │  cookie               │
│                                        │                        │
│ 2. Receive token & cookie                                       │
│    ◄──────────────────────────────────                          │
│    {                                                             │
│      "email": "user@example.com",                               │
│      "token": "eyJhbGc..."                                      │
│    }                                                             │
│    Set-Cookie: jwt=eyJhbGc...; HttpOnly; Path=/                │
│                                                                   │
│ 3. Store token & cookie                                         │
│    - Token: localStorage or sessionStorage (for manual sending) │
│    - Cookie: Automatically managed by browser                   │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 2. Authenticated Request Flow

```
┌──────────────────────────────────────────────────────────────────┐
│ Client                                   Server                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│ 1. Make authenticated request (with token)                       │
│    GET /api/v1/profile                                           │
│    Authorization: Bearer eyJhbGc...  ──────────────────────►    │
│    (or cookie sent automatically)                               │
│                                                                   │
│                                        ┌─ JwtRequestFilter      │
│                                        │  1. Extract token     │
│                                        │  2. Validate signature│
│                                        │  3. Check expiration  │
│                                        │  4. Extract email     │
│                                        │  5. Set SecurityContext
│                                        │                        │
│                                        ├─ Route to endpoint    │
│                                        │  (ProfileController)   │
│                                        │                        │
│                                        ├─ Execute endpoint     │
│                                        │  (User verified)       │
│                                        │                        │
│ 2. Receive response                                              │
│    ◄──────────────────────────────────                          │
│    {                                                             │
│      "userId": "123-abc",                                       │
│      "name": "John Doe",                                        │
│      "email": "user@example.com",                               │
│      "isAccountVerified": true                                  │
│    }                                                             │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 3. Invalid/Expired Token Handling

```
┌──────────────────────────────────────────────────────────────────┐
│ Client                                   Server                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│ Make request with invalid/expired token                          │
│    GET /api/v1/profile                                           │
│    Authorization: Bearer oldToken...  ──────────────────────►   │
│                                                                   │
│                                        ┌─ JwtRequestFilter      │
│                                        │  Validation fails!     │
│                                        │  (Signature invalid OR │
│                                        │   Token expired)       │
│                                        │                        │
│                                        ├─ Throw exception       │
│                                        │  (JwtAuthenticationEx) │
│                                        │                        │
│                                        ├─ Catch exception       │
│                                        │  (AuthenticationEntry
│                                        │   Point)               │
│                                        │                        │
│ Receive error response                                           │
│    ◄──────────────────────────────────                          │
│    HTTP 401 Unauthorized                                        │
│    {                                                             │
│      "message": "Unauthorized",                                 │
│      "details": "Invalid token or expired"                      │
│    }                                                             │
│                                                                   │
│ Client action: Redirect to login or refresh token               │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## API Endpoints

### Base URL
All endpoints are prefixed with `/api/v1`

### Authentication Endpoints

#### 1. **Login**
```
POST /api/v1/auth/login
```
**Description**: Authenticates a user and returns a JWT token

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

**Success Response (200 OK)**:
```json
{
  "email": "user@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI..."
}
```
Plus HTTP-only cookie: `jwt=eyJhbGc...; HttpOnly; Path=/; SameSite=Strict`

**Error Responses**:
- `400 Bad Request`: Invalid credentials
```json
{
  "error": true,
  "message": "Invalid credentials"
}
```
- `401 Unauthorized`: Account disabled
```json
{
  "error": true,
  "message": "Account is disabled"
}
```

---

#### 2. **Check Authentication Status**
```
GET /api/v1/auth/isAuthenticated
```
**Description**: Checks if the current user is authenticated

**Headers**:
```
Authorization: Bearer <jwt_token>
```
OR Cookie: `jwt=<jwt_token>` (automatic)

**Success Response (200 OK)**:
```json
{
  "authenticated": true,
  "email": "user@example.com"
}
```

**Unauthenticated Response (200 OK)**:
```json
{
  "authenticated": false
}
```

---

#### 3. **Send Verification OTP**
```
POST /api/v1/auth/send-otp
```
**Description**: Sends an OTP to the logged-in user's email for account verification

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Success Response (200 OK)**:
```
Email sent with OTP
```

---

#### 4. **Verify OTP**
```
POST /api/v1/auth/verify-otp
```
**Description**: Verifies the OTP sent to the user's email

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Request Body**:
```json
{
  "otp": "123456"
}
```

**Success Response (200 OK)**:
```
Account verified successfully
```

**Error Response**:
- `400 Bad Request`: OTP is required
```json
{
  "error": true,
  "message": "OTP is required/Invalid"
}
```
- `500 Internal Server Error`: Invalid or expired OTP
```json
{
  "error": true,
  "message": "Invalid OTP or OTP has expired"
}
```

---

#### 5. **Send Password Reset OTP**
```
POST /api/v1/auth/send-reset-otp
```
**Description**: Sends an OTP for password reset (no authentication required)

**Query Parameters**:
```
email=user@example.com
```

**Success Response (200 OK)**:
```
Email sent with reset OTP
```

---

#### 6. **Reset Password**
```
POST /api/v1/auth/reset-password
```
**Description**: Resets password using OTP (no authentication required)

**Request Body**:
```json
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "newSecret123"
}
```

**Success Response (200 OK)**:
```
Password reset successfully
```

---

### Profile Endpoints

#### 1. **Get User Profile**
```
GET /api/v1/profile
```
**Description**: Retrieves the logged-in user's profile

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Success Response (200 OK)**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "user@example.com",
  "isAccountVerified": true
}
```

---

#### 2. **Update User Profile**
```
PUT /api/v1/profile
```
**Description**: Updates the logged-in user's profile

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Request Body**:
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "newPassword123"
}
```

**Success Response (200 OK)**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Jane Doe",
  "email": "jane@example.com",
  "isAccountVerified": true
}
```

---

## Security Configuration

### Spring Security Configuration (`SecurityConfig.java`)

The application uses Spring Security to:
1. **Disable CSRF**: Not needed for stateless JWT authentication
2. **Enable CORS**: Allow requests from frontend on different domain
3. **Configure filters**: Add JWT filter to validate tokens
4. **Set authentication manager**: Use username/password authentication
5. **Define protected endpoints**: Specify which URLs need authentication

### JWT Request Filter (`JwtRequestFilter.java`)

Executes on every HTTP request to:
1. Extract JWT from `Authorization` header or `jwt` cookie
2. Validate token signature using the secret key
3. Check if token has expired
4. Extract user email from token payload
5. Set authentication in `SecurityContext`
6. Pass request to the next filter

**Filter Chain Order**:
```
Request → JwtRequestFilter → AuthenticationFilter → DispatcherServlet → Controller
                    ↓
            Valid JWT?
              ✓ Yes    → Continue
              ✗ No     → Throw JwtAuthenticationException
                        → AuthenticationEntryPoint
                        → 401 Response
```

### HTTP-Only Cookies

Cookies are set with security flags:
```
Set-Cookie: jwt=<token>; 
  HttpOnly;        # JavaScript cannot access (prevents XSS)
  Path=/;          # Sent with all paths
  SameSite=Strict; # Not sent in cross-site requests (prevents CSRF)
  Secure=true;     # HTTPS only (production)
```

### CORS Configuration

Allows requests from different origins:
- **Allowed Origins**: Configured in `SecurityConfig`
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Allowed Headers**: Any
- **Allow Credentials**: Yes (for cookies)
- **Max Age**: 3600 seconds

---

## Email Verification System

### Purpose
- Verify users' email addresses before account activation
- Increase data quality and prevent bot registrations
- Enable account recovery via email

### Flow

```
┌─────────────────────────────────────────────────────────┐
│ 1. User registers (POST /api/v1/auth/register)          │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 2. User account created                                  │
│    isAccountVerified = false                            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 3. User logs in with JWT token                          │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 4. User requests verification OTP                       │
│    POST /api/v1/auth/send-otp                          │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 5. Generate 6-digit OTP                                 │
│    verifyOtp = "123456"                                │
│    verifyOtpExpireAt = now + 15 minutes                │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 6. Send OTP via email (EmailService)                   │
│    emailService.sendVerificationOtpEmail()             │
│                                                         │
│    ┌─────────────────────────────────────┐            │
│    │ Email Template:                      │            │
│    │ Subject: Verification OTP            │            │
│    │ Body: Your OTP: 123456              │            │
│    │       Valid for 15 minutes           │            │
│    └─────────────────────────────────────┘            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 7. User enters OTP                                      │
│    POST /api/v1/auth/verify-otp                        │
│    { "otp": "123456" }                                 │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 8. Verify OTP                                           │
│    ✓ OTP matches                                        │
│    ✓ OTP not expired                                    │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 9. Mark account as verified                            │
│    isAccountVerified = true                            │
│    verifyOtp = null                                    │
│    verifyOtpExpireAt = 0                              │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 10. Success response                                    │
│     Account verified successfully                       │
└─────────────────────────────────────────────────────────┘
```

### Email Service Implementation

**Class**: `EmailService.java`

**Methods**:
- `sendVerificationOtpEmail(String email, String otp)`: Sends verification OTP
- `sendResetOtpEmail(String email, String otp)`: Sends password reset OTP
- `sendWelcomeEmail(String email, String name)`: Sends welcome email

**Email Configuration** (`application.properties`):
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USER}          # From environment variable
spring.mail.password=${EMAIL_PASS}          # From environment variable
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Setup Instructions**:
1. Create a Gmail account (or use existing)
2. Enable 2-Factor Authentication
3. Generate App Password: https://myaccount.google.com/apppasswords
4. Set environment variables:
   ```
   EMAIL_USER=your-email@gmail.com
   EMAIL_PASS=your-app-password
   ```

---

## Password Reset System

### Purpose
- Allow users to reset forgotten passwords
- Use OTP for verification without security questions

### Flow

```
┌─────────────────────────────────────────────────────────┐
│ 1. User requests password reset                         │
│    POST /api/v1/auth/send-reset-otp?email=user@ex.com  │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Find user by email                                   │
│    (User must exist in database)                        │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 3. Generate OTP and expiry                              │
│    resetOtp = "654321"                                 │
│    resetOtpExpireAt = now + 24 hours                  │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Send reset OTP via email                            │
│    emailService.sendResetOtpEmail()                    │
│                                                         │
│    ┌─────────────────────────────────────┐            │
│    │ Email Template:                      │            │
│    │ Subject: Password Reset OTP          │            │
│    │ Body: Your reset OTP: 654321        │            │
│    │       Valid for 24 hours             │            │
│    └─────────────────────────────────────┘            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 5. User submits reset request with OTP & new password  │
│    POST /api/v1/auth/reset-password                    │
│    {                                                    │
│      "email": "user@example.com",                      │
│      "otp": "654321",                                 │
│      "newPassword": "newSecret123"                    │
│    }                                                    │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 6. Validate OTP                                         │
│    ✓ OTP matches stored OTP                            │
│    ✓ OTP not expired                                   │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 7. Update password                                      │
│    newPasswordHash = bcrypt(newPassword)               │
│    password = newPasswordHash                          │
│    resetOtp = null                                     │
│    resetOtpExpireAt = 0                               │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 8. Success response                                     │
│    Password reset successfully                         │
│    User can now login with new password                │
└─────────────────────────────────────────────────────────┘
```

### OTP Expiry Times
- **Verification OTP**: 15 minutes
- **Reset OTP**: 24 hours

---

## Setup & Configuration

### Prerequisites
- Java 17+ (Spring Boot 3.x requires Java 17+)
- Maven 3.6+
- MySQL 8.0+
- Gmail account (for email notifications)

### Step 1: Database Setup

Create MySQL database:
```sql
CREATE DATABASE springdb;
```

### Step 2: Environment Variables

Set these environment variables (Windows PowerShell):
```powershell
$env:EMAIL_USER="your-email@gmail.com"
$env:EMAIL_PASS="your-app-password"
```

Or create a `.env` file:
```
EMAIL_USER=your-email@gmail.com
EMAIL_PASS=your-app-password
```

### Step 3: Configure Application Properties

Update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/springdb
spring.datasource.username=root
spring.datasource.password=your_mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.show-sql=true

# JWT Secret (at least 32 bytes for HS256)
jwt.secret.key=your-secret-key-min-32-chars-long-example12345

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USER}
spring.mail.password=${EMAIL_PASS}
spring.mail.properties.mail.smtp.from=${EMAIL_USER}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# API Base Path
server.servlet.context-path=/api/v1
```

### Step 4: Build & Run

```bash
# Build the project
./mvnw.cmd clean package

# Run the application
./mvnw.cmd spring-boot:run

# Or run the JAR directly
java -jar target/FullStackAuth-0.0.1-SNAPSHOT.jar
```

Application will be available at: `http://localhost:8080/api/v1`

---

## Testing Guide

### Using Postman

#### 1. **Import Collection**

Import the provided Postman collection: `Auth rest apis.postman_collection.json`

#### 2. **Test Login**

```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secret123"
}
```

**Response**:
```json
{
  "email": "user@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 3. **Test Protected Endpoint**

Add token to Authorization header:
```
GET http://localhost:8080/api/v1/auth/isAuthenticated
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 4. **Test Send OTP**

```
POST http://localhost:8080/api/v1/auth/send-otp
Authorization: Bearer <your-jwt-token>
```

#### 5. **Test Verify OTP**

```
POST http://localhost:8080/api/v1/auth/verify-otp
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "otp": "123456"
}
```

#### 6. **Test Password Reset**

```
POST http://localhost:8080/api/v1/auth/send-reset-otp?email=user@example.com
```

Then:
```
POST http://localhost:8080/api/v1/auth/reset-password
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "654321",
  "newPassword": "newSecret123"
}
```

### Using cURL

#### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "secret123"
  }'
```

#### Check Authentication
```bash
curl -X GET http://localhost:8080/api/v1/auth/isAuthenticated \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### Get Profile
```bash
curl -X GET http://localhost:8080/api/v1/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Error Handling

### HTTP Status Codes

| Status | Meaning | Example Scenario |
|--------|---------|------------------|
| `200` | OK | Successful login, profile retrieved |
| `400` | Bad Request | Invalid OTP, missing required fields |
| `401` | Unauthorized | Invalid credentials, expired token |
| `403` | Forbidden | Not enough permissions |
| `404` | Not Found | User not found, endpoint doesn't exist |
| `500` | Server Error | Database error, email sending failed |

### Common Error Responses

#### Invalid Credentials
```json
{
  "error": true,
  "message": "Invalid credentials"
}
```

#### Token Expired
```json
{
  "message": "Unauthorized",
  "details": "Invalid token or expired"
}
```

#### OTP Expired
```json
{
  "error": true,
  "message": "OTP has expired"
}
```

#### Invalid OTP
```json
{
  "error": true,
  "message": "Invalid OTP"
}
```

#### User Not Found
```json
{
  "error": true,
  "message": "User not found: user@example.com"
}
```

---

## Security Best Practices

### 1. **Secret Key Management**
- ✅ Use strong secret key (at least 32 bytes for HS256)
- ✅ Never commit secret key to version control
- ✅ Load from environment variables or secret manager
- ❌ Don't hardcode in application.properties

### 2. **Token Expiration**
- ✅ Set reasonable expiration (10 hours for access, 7 days for refresh)
- ✅ Implement token refresh mechanism for long-lived sessions
- ❌ Never use indefinite token expiration

### 3. **HTTPS/TLS**
- ✅ Use HTTPS in production (Secure flag on cookies)
- ✅ Redirect HTTP to HTTPS
- ❌ Don't send tokens over plain HTTP

### 4. **Password Security**
- ✅ Hash passwords with bcrypt (Spring Security does this)
- ✅ Enforce strong password requirements
- ✅ Use password reset with OTP verification
- ❌ Never store plain-text passwords

### 5. **CORS Configuration**
- ✅ Specify allowed origins explicitly
- ✅ Enable credentials mode for cookies
- ❌ Don't use wildcard origins with credentials

### 6. **OTP Security**
- ✅ Use cryptographically secure random number generator
- ✅ Set short expiration times (15 min for verification, 24h for reset)
- ✅ Limit OTP retry attempts
- ❌ Don't transmit OTP in response body

### 7. **Cookie Security**
- ✅ Set HttpOnly flag (prevents XSS)
- ✅ Set SameSite flag (prevents CSRF)
- ✅ Set Secure flag in production
- ❌ Don't allow JavaScript to read token cookie

### 8. **Logging & Monitoring**
- ✅ Log authentication attempts and failures
- ✅ Monitor for suspicious patterns
- ✅ Alert on repeated failed login attempts
- ❌ Don't log sensitive data (passwords, tokens)

### 9. **Input Validation**
- ✅ Validate email format
- ✅ Validate password requirements
- ✅ Validate OTP format
- ❌ Don't trust client-side validation

### 10. **Rate Limiting**
- ✅ Limit login attempts per IP/email
- ✅ Limit OTP generation requests
- ✅ Implement exponential backoff
- ❌ Don't allow unlimited authentication attempts

---

## Database Schema

### UserEntity Table

```sql
CREATE TABLE user_entity (
  user_id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  is_account_verified BOOLEAN DEFAULT FALSE,
  verify_otp VARCHAR(10),
  verify_otp_expire_at BIGINT DEFAULT 0,
  reset_otp VARCHAR(10),
  reset_otp_expire_at BIGINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Column Descriptions

| Column | Type | Description |
|--------|------|-------------|
| `user_id` | VARCHAR(36) | Unique user identifier (UUID) |
| `name` | VARCHAR(255) | User's full name |
| `email` | VARCHAR(255) | User's email (unique) |
| `password` | VARCHAR(255) | Hashed password |
| `is_account_verified` | BOOLEAN | Whether email is verified |
| `verify_otp` | VARCHAR(10) | Current verification OTP |
| `verify_otp_expire_at` | BIGINT | OTP expiration timestamp (ms) |
| `reset_otp` | VARCHAR(10) | Current reset OTP |
| `reset_otp_expire_at` | BIGINT | Reset OTP expiration timestamp (ms) |
| `created_at` | TIMESTAMP | Account creation time |
| `updated_at` | TIMESTAMP | Last update time |

---

## Troubleshooting

### Issue: "Invalid token" on every request
**Cause**: JWT secret key mismatch or token generation failed
**Solution**: Ensure `jwt.secret.key` in properties is at least 32 bytes

### Issue: Email not sending
**Cause**: Gmail credentials incorrect or 2FA not setup
**Solution**: 
1. Verify EMAIL_USER and EMAIL_PASS in environment variables
2. Enable 2FA on Gmail
3. Generate App Password and use that instead

### Issue: CORS errors
**Cause**: Frontend domain not allowed
**Solution**: Update CORS allowed origins in `SecurityConfig.java`

### Issue: OTP "already expired" immediately
**Cause**: System time difference between server components
**Solution**: Check server system time is correct

### Issue: "User not found" on login
**Cause**: User doesn't exist in database
**Solution**: Verify user email exists; ensure registration endpoint works

---

## Future Enhancements

- [ ] Implement refresh token mechanism
- [ ] Add role-based access control (RBAC)
- [ ] Two-factor authentication (2FA)
- [ ] OAuth2 social login (Google, GitHub)
- [ ] Rate limiting per endpoint
- [ ] Audit logging of all security events
- [ ] Device management and login history
- [ ] Biometric authentication support
- [ ] IP whitelist/blacklist functionality
- [ ] Session management (multi-device login control)

---

## References

- [JWT.io](https://jwt.io) - JWT specification and debugger
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [HS256 vs RS256](https://auth0.com/blog/critical-vulnerabilities-in-json-web-token-libraries/)

---

## License & Contact

**Project**: FullStackAuth  
**Created By**: Arpon007  
**Purpose**: Educational - Full Stack Authentication System

---

*Last Updated: November 14, 2025*
*Documentation Version: 1.0*

