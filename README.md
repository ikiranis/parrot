# Parrot Tunes - Media Library and Player

A modern rewrite of the OWMP (Open Web Media Library and Player) project, implemented as a Java Spring Boot backend with a planned Vue.js frontend.

## Features

- **User Authentication**: JWT-based authentication system with user registration and login
- **Media Library Management**: Upload, organize, and manage audio files with automatic metadata extraction
- **Playlist Support**: Create manual playlists and smart playlists with criteria-based filtering
- **Queue Management**: Real-time music queue with voting system for collaborative listening
- **Media Streaming**: HTTP-based audio streaming with range request support
- **Album Art Management**: Automatic album art extraction and management
- **File Upload**: Secure file upload with metadata extraction using Apache Tika
- **RESTful API**: Complete REST API for all functionality

## Technology Stack

### Backend
- **Java 17+**
- **Spring Boot 3.x**
- **Spring Security** (JWT Authentication)
- **Spring Data JPA** (Database abstraction)
- **Apache Derby** (Embedded database)
- **Apache Tika** (Metadata extraction)
- **Maven** (Dependency management)

### Frontend (Planned)
- **Vue.js 3**
- **Vue Router**
- **Vuex/Pinia** (State management)

## Project Structure

```
src/
├── main/
│   ├── java/com/parrottunes/
│   │   ├── ParrotTunesApplication.java     # Main application class
│   │   ├── config/                         # Configuration classes
│   │   │   └── SecurityConfig.java         # Security configuration
│   │   ├── controller/                     # REST controllers
│   │   │   ├── AuthController.java         # Authentication endpoints
│   │   │   ├── MediaController.java        # Media file management
│   │   │   ├── PlaylistController.java     # Playlist operations
│   │   │   ├── QueueController.java        # Queue management
│   │   │   ├── StreamController.java       # Media streaming
│   │   │   ├── UploadController.java       # File upload
│   │   │   ├── UserController.java         # User management
│   │   │   └── VoteController.java         # Voting system
│   │   ├── dto/                           # Data Transfer Objects
│   │   ├── entity/                        # JPA entities
│   │   │   ├── BaseEntity.java            # Base entity with common fields
│   │   │   ├── User.java                  # User entity
│   │   │   ├── MediaFile.java             # Media file entity
│   │   │   ├── MusicTag.java              # Music metadata
│   │   │   ├── AlbumArt.java              # Album artwork
│   │   │   ├── ManualPlaylist.java        # User-created playlists
│   │   │   ├── SmartPlaylist.java         # Dynamic playlists
│   │   │   ├── PlaylistItem.java          # Playlist items
│   │   │   ├── QueueItem.java             # Queue items
│   │   │   ├── Vote.java                  # Voting records
│   │   │   └── LogEntry.java              # Activity logs
│   │   ├── exception/                     # Exception handling
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── repository/                    # JPA repositories
│   │   ├── security/                      # Security components
│   │   │   ├── JwtTokenProvider.java      # JWT token utilities
│   │   │   └── JwtAuthenticationFilter.java
│   │   └── service/                       # Business logic services
│   └── resources/
│       └── application.properties         # Application configuration
```

## API Endpoints

### Authentication
- `POST /api/auth/signup` - User registration
- `POST /api/auth/signin` - User login

### Media Management
- `GET /api/media` - List all media files
- `GET /api/media/{id}` - Get specific media file
- `PUT /api/media/{id}` - Update media file
- `DELETE /api/media/{id}` - Delete media file
- `GET /api/media/search` - Search media files

### Playlists
- `GET /api/playlists` - List user playlists
- `POST /api/playlists` - Create new playlist
- `GET /api/playlists/{id}` - Get playlist details
- `PUT /api/playlists/{id}` - Update playlist
- `DELETE /api/playlists/{id}` - Delete playlist

### Queue Management
- `GET /api/queue` - Get current queue
- `POST /api/queue` - Add item to queue
- `DELETE /api/queue/{id}` - Remove item from queue
- `PUT /api/queue/{id}/position` - Change item position

### Voting
- `POST /api/votes` - Cast a vote
- `GET /api/votes/item/{itemId}` - Get votes for item

### File Upload
- `POST /api/upload` - Upload media files

### Streaming
- `GET /api/stream/{id}` - Stream media file

### User Management
- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile

## Setup and Installation

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Database Setup
No database setup required! The application uses embedded Apache Derby database which is automatically created on first run.

### Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd parrot
   ```

2. **Configure the database (optional)**
   
   The application uses embedded Derby by default. No configuration needed!
   
   To customize database location, edit `src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:derby:your_custom_path/parrot_tunes_db;create=true
   ```

3. **Build and run**

   ```bash
   ./mvnw clean compile
   ./mvnw spring-boot:run
   ```
   
   Or using the convenience script:
   
   ```bash
   ./start.sh
   ```

4. **Access the API**
   The API will be available at `http://localhost:9999/api`

### Configuration

Key configuration options in `application.properties`:

- **Server Port**: `server.port=9999`
- **Database URL**: `spring.datasource.url`
- **File Upload Directory**: `app.media.upload-dir=./uploads`
- **JWT Secret**: `app.jwtSecret=mySecretKey`
- **CORS Origins**: `app.cors.allowed-origins`

## Development Status

### ✅ Completed Backend Features
- Complete database schema implementation
- User authentication and authorization
- Media file management with metadata extraction
- Playlist creation and management (manual and smart playlists)
- Queue management with voting system
- File upload with automatic metadata extraction
- Media streaming with HTTP range support
- RESTful API endpoints for all functionality
- Security configuration with JWT
- Global exception handling

### 🚧 Planned Frontend Features
- Vue.js 3 frontend application
- Responsive web interface
- Audio player with queue management
- Playlist management interface
- File upload interface
- User authentication UI
- Real-time updates for collaborative features

### 🔮 Future Enhancements
- WebSocket support for real-time updates
- Advanced search and filtering
- User roles and permissions
- Audio format conversion
- Mobile application
- Docker containerization
- CI/CD pipeline

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is a rewrite of the OWMP project. License information will be added based on the original project's license.