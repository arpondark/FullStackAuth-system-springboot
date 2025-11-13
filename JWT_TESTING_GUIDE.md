# JWT Authentication Testing Guide

## Quick Summary of the Fix

**Issue:** `/api/v1.1/auth/isAuthenticated` was returning `true` even when NOT logged in.

**Root Cause:** The endpoint was checking if `email` parameter was not null, but Spring was providing a default value even for unauthenticated requests.

**Solution:** Now it checks the actual `SecurityContextHolder.getContext().getAuthentication()` which properly returns null when not authenticated.

---

## API Endpoints

### All endpoints are now prefixed with `/api/v1.1/`

| Method | Endpoint | Authentication | Purpose |
|--------|----------|------------------|---------|
| POST | `/api/v1.1/auth/register` | ❌ Not Required | Create new user account |
| POST | `/api/v1.1/auth/login` | ❌ Not Required | Login with credentials, get JWT |
| GET | `/api/v1.1/auth/isAuthenticated` | ✅ Required | Check if currently authenticated |
| GET | `/api/v1.1/test` | ✅ Required | Test protected endpoint |

---

## Testing Steps

### Step 1: Register a New User

**Endpoint:** `POST /api/v1.1/auth/register`

**Request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "YourSecurePassword123"
}
```

**Response (201 Created):**
```json
{
  "name": "John Doe",
  "email": "john@example.com"
}
```

---

### Step 2: Check if Authenticated (Without Login)

**Endpoint:** `GET /api/v1.1/auth/isAuthenticated`

**Response:**
```
false
```

✅ **Expected:** Should return `false` because you haven't logged in yet.

---

### Step 3: Login to Get JWT Token

**Endpoint:** `POST /api/v1.1/auth/login`

**Request:**
```json
{
  "email": "john@example.com",
  "password": "YourSecurePassword123"
}
```

**Response (200 OK):**
```json
{
  "email": "john@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwiaWF0IjoxNzMxNDMyMzQwLCJleHAiOjE3MzE0NjgzNDB9.abc123..."
}
```

**Headers Set:**
- `Set-Cookie: jwt=eyJhbGc...; HttpOnly; Path=/; Max-Age=86400; SameSite=Strict`

---

### Step 4: Copy the Token

From the login response, copy the entire token value. It will look something like:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwiaWF0IjoxNzMxNDMyMzQwLCJleHAiOjE3MzE0NjgzNDB9.signature
```

---

### Step 5: Test with Authorization Header

**Endpoint:** `GET /api/v1.1/auth/isAuthenticated`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwiaWF0IjoxNzMxNDMyMzQwLCJleHAiOjE3MzE0NjgzNDB9.signature
```

**Response:**
```
true
```

✅ **Expected:** Should return `true` because you sent a valid JWT token.

---

### Step 6: Test Protected Endpoint

**Endpoint:** `GET /api/v1.1/test`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**
```json
{
  "message": "Hello, authenticated user!",
  "email": "john@example.com",
  "timestamp": 1731432340000
}
```

✅ **Expected:** Successfully accesses protected endpoint and shows your email.

---

## How It Works Under the Hood

### 1. Request Flow

```
Browser/Client
    ↓
POST /api/v1.1/auth/login
    ↓
AuthController.login()
    ↓
Authenticates credentials
    ↓
Creates JWT token
    ↓
Sets HTTP-only cookie + returns token in body
    ↓
Client receives token
```

### 2. Protected Endpoint Flow

```
Browser/Client sends request with token
    ↓
Token in Authorization header OR cookie
    ↓
JwtRequestFilter intercepts request
    ↓
Validates JWT (signature, expiration, email)
    ↓
Sets SecurityContext with user info
    ↓
Controller receives request with authenticated user
    ↓
@CurrentSecurityContext can access user email
```

### 3. isAuthenticated Check Flow

```
GET /api/v1.1/auth/isAuthenticated
    ↓
JwtRequestFilter validates token (if present)
    ↓
IF valid: SecurityContext.authentication = set
IF invalid/missing: SecurityContext.authentication = null
    ↓
Endpoint checks: SecurityContextHolder.getContext().getAuthentication()
    ↓
Returns: true (if authenticated) or false (if not)
```

---

## Cookie vs Authorization Header

### How JWT is Transmitted

| Method | Automatic? | Security | Best For |
|--------|-----------|----------|----------|
| **HTTP-only Cookie** | ✅ Yes (browser auto-sends) | 🔒 High (XSS protected) | Web browsers |
| **Authorization Header** | ❌ No (must be sent manually) | 🔒 High (if HTTPS) | Mobile apps, SPAs |

### Our Implementation

When you login, the JWT is sent **BOTH ways**:

1. **In Response Body** → For manual storage (mobile, SPAs)
2. **As HTTP-only Cookie** → For automatic browser transmission

**JwtRequestFilter checks in this order:**
1. Authorization header: `Authorization: Bearer <token>`
2. If not found, checks for `jwt` cookie

---

## Token Expiration

- **Validity Period:** 10 hours from login
- **After Expiration:** User gets 401 Unauthorized
- **Fix:** User must login again to get a new token

**Check token expiration:**
```javascript
// Decode token (without verification) to see expiration
const token = "eyJhbGc...";
const payload = JSON.parse(atob(token.split('.')[1]));
const exp = new Date(payload.exp * 1000);
console.log("Token expires at:", exp);
```

---

## Common Issues & Solutions

### Issue 1: Getting 401 Unauthorized

**Causes:**
- Token is missing
- Token is expired
- Token is malformed
- Token is from different secret key

**Solutions:**
- Ensure Authorization header is present: `Authorization: Bearer <token>`
- Check if 10 hours have passed since login
- Copy the entire token (no extra spaces)
- Verify `jwt.secret.key` in `application.properties`

---

### Issue 2: isAuthenticated returns false after login

**Causes:**
- Token not being sent (missing Authorization header)
- Cookie not being sent by browser
- SameSite=Strict prevents cookie transmission

**Solutions:**
- Check DevTools → Network → See if Authorization header is sent
- Check DevTools → Application → Cookies → Verify "jwt" cookie exists
- For cross-origin requests: Change SameSite to "None" and use `{ credentials: 'include' }`

---

### Issue 3: Cookie not being set

**Causes:**
- Response doesn't have Set-Cookie header
- Browser rejecting the cookie (SameSite mismatch)
- HTTPS not enabled (if secure flag is set)

**Solutions:**
- Check Network tab → Response headers for `Set-Cookie`
- Check AuthController sets cookie correctly
- If frontend is different domain: set SameSite="None" and secure=true

---

## JWT Claims Breakdown

### Token Structure Example

```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "john@example.com",           ← Email (subject)
  "iat": 1731432340,                   ← Issued at (timestamp)
  "exp": 1731468340                    ← Expires at (timestamp)
}

Signature: (binary - validates token integrity)
```

### What Each Claim Means

| Claim | Meaning | Example |
|-------|---------|---------|
| `sub` (subject) | User email | `john@example.com` |
| `iat` (issued at) | When token was created | `1731432340` |
| `exp` (expiration) | When token expires | `1731468340` |

---

## Security Best Practices

✅ **What We're Doing:**
- Signing tokens with HS256 (tamper-proof)
- Setting httpOnly flag (prevents XSS attacks)
- 10-hour expiration (old tokens can't be reused forever)
- SameSite=Strict (prevents CSRF)

⚠️ **Optional Improvements:**
- Implement refresh tokens (get new access tokens without re-entering password)
- Add token revocation (logout invalidates tokens)
- Use asymmetric keys (RS256) in microservices architecture
- Rotate secret key periodically

---

## Testing with cURL

### Login
```bash
curl -X POST http://localhost:8080/api/v1.1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"YourSecurePassword123"}'
```

### Test isAuthenticated (with token)
```bash
curl -X GET http://localhost:8080/api/v1.1/auth/isAuthenticated \
  -H "Authorization: Bearer eyJhbGc..."
```

### Test isAuthenticated (without token)
```bash
curl -X GET http://localhost:8080/api/v1.1/auth/isAuthenticated
```

---

## Testing with Postman/Insomnia

1. **Create Login Request**
   - Method: POST
   - URL: `http://localhost:8080/api/v1.1/auth/login`
   - Body: JSON with email and password
   - Click "Send"

2. **Copy Token from Response**
   - Select token value from response body
   - Copy to clipboard

3. **Test isAuthenticated**
   - Method: GET
   - URL: `http://localhost:8080/api/v1.1/auth/isAuthenticated`
   - Headers Tab → Add new header:
     - Key: `Authorization`
     - Value: `Bearer <paste-token-here>`
   - Click "Send"

4. **Expected Result:** Returns `true`

---

## Next Steps

1. ✅ Test login endpoint → should get token
2. ✅ Test isAuthenticated WITHOUT token → should return false
3. ✅ Test isAuthenticated WITH token → should return true
4. ✅ Test /test endpoint → should return user email
5. 🔄 Wait 10 hours (or modify JwtUtil expiration) → token expires
6. ✅ Verify expired token returns 401

---

## Need Help?

Check these files for configuration:
- `application.properties` → API prefix, JWT secret, database config
- `AuthController.java` → Login and isAuthenticated endpoints
- `JwtRequestFilter.java` → JWT validation for every request
- `JwtUtil.java` → Token creation and validation logic
- `SecurityConfig.java` → Authorization rules and filter chain

