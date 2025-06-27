# Parrot Tunes - Quick Start Guide

## 🚀 Getting Started (5 minutes)

### Prerequisites
- Java 17+ 
- Nothing else! (No external database setup needed)

### 1. Clone & Navigate
```bash
cd "/home/rocean/dev/Java Dev/parrot"
```

### 2. Run the Application
```bash
# Option A: Using Maven Wrapper (Recommended)
./mvnw spring-boot:run

# Option B: Using convenience script
./start.sh

# Option C: Test Derby integration first
./test-derby.sh
```

### 3. Verify It's Working
- **API Base URL**: http://localhost:9999/api
- **Health Check**: http://localhost:9999/api/auth/health
- **Database**: Automatically created in `parrot_tunes_db/` directory

## 📱 Quick API Test

### Register a User
```bash
curl -X POST http://localhost:9999/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com", 
    "password": "password123"
  }'
```

### Login
```bash
curl -X POST http://localhost:9999/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "password123"
  }'
```

### Test Protected Endpoint
```bash
# Use the token from login response
curl -X GET http://localhost:9999/api/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🎵 Upload Music
```bash
curl -X POST http://localhost:9999/api/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/your/music.mp3"
```

## 🗄️ Database

- **Type**: Embedded Apache Derby
- **Location**: `./parrot_tunes_db/`
- **Setup**: Automatic (no configuration needed)
- **Reset**: Delete the `parrot_tunes_db/` directory

## 🔧 Troubleshooting

### Derby Database Issues
```bash
# Clean database and restart
rm -rf parrot_tunes_db/ derby.log
./mvnw spring-boot:run
```

### Port Already in Use
```bash
# Change port in application.properties
server.port=8080
```

### Memory Issues
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx1024m"
./mvnw spring-boot:run
```

## 📚 Full Documentation
- See `README.md` for complete setup instructions
- See `API.md` for full API documentation
- See `Dockerfile` for containerization

---
🎉 **You're ready to rock with Parrot Tunes!**
