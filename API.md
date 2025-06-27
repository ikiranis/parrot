# Parrot Tunes API Documentation

Base URL: `http://localhost:9999/api`

## Authentication Endpoints

### Health Check
```http
GET /auth/health
```
Returns server status and timestamp.

**Response:**
```json
{
  "status": "UP",
  "service": "Parrot Tunes Backend", 
  "timestamp": "2025-06-27T10:30:00Z"
}
```

### User Registration
```http
POST /auth/signup
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully"
}
```

### User Login
```http
POST /auth/signin
Content-Type: application/json

{
  "usernameOrEmail": "testuser",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer"
}
```

## Media Management

### List Media Files
```http
GET /media
Authorization: Bearer {token}
```

### Upload Media File
```http
POST /upload
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: {audio_file}
```

### Stream Media
```http
GET /stream/{mediaId}
Authorization: Bearer {token}
```

## Playlist Management

### Get User Playlists
```http
GET /playlists
Authorization: Bearer {token}
```

### Create Playlist
```http
POST /playlists
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "My Playlist",
  "description": "A great playlist",
  "isPublic": false
}
```

## Queue Management

### Get Current Queue
```http
GET /queue
Authorization: Bearer {token}
```

### Add to Queue
```http
POST /queue
Authorization: Bearer {token}
Content-Type: application/json

{
  "mediaFileId": 1,
  "position": 1
}
```

## User Profile

### Get Profile
```http
GET /users/profile
Authorization: Bearer {token}
```

### Update Profile
```http
PUT /users/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "newusername",
  "email": "newemail@example.com"
}
```

## Voting

### Vote on Queue Item
```http
POST /votes
Authorization: Bearer {token}
Content-Type: application/json

{
  "queueItemId": 1,
  "voteType": "UP"
}
```

## Error Responses

All endpoints may return these error responses:

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Invalid or expired token"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access denied"
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Resource not found"
}
```

### 400 Bad Request
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": ["Field error messages"]
}
```

## Authentication Flow

1. Register a new user with `POST /auth/signup`
2. Login with `POST /auth/signin` to get JWT token
3. Include token in Authorization header: `Bearer {token}`
4. Use token for all protected endpoints

## Notes

- JWT tokens expire after 24 hours by default
- File uploads support common audio formats (MP3, FLAC, WAV, etc.)
- Database is automatically created on first startup (embedded Derby)
- All timestamps are in UTC
- File uploads are limited to 500MB per file
