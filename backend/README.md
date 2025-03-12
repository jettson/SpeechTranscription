# Real-time Speech-to-Text System - Backend

## Overview

This is the backend component of the real-time speech-to-text system, developed based on Spring MVC + Spring + Hibernate architecture, providing audio stream reception, STT conversion, and database storage functionality.

## Technology Stack

- Spring Framework (Spring Boot, Spring Core)
- Hibernate ORM
- WebSocket (Spring WebSocket)
- STT API Integration
- MySQL Database

## Features

- Receive audio streams from the frontend
- Process audio data in real-time
- Convert audio to text through STT API
- Send transcription results back to the frontend in real-time
- Store voice and text records in the database

## Development Environment Setup

### Prerequisites

- JDK 11+
- Maven 3.6+
- MySQL 8.0+

### Database Configuration

1. Create MySQL database

```sql
CREATE DATABASE speech_translate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Configure database connection

Edit the `src/main/resources/application.properties` file and set the database connection parameters:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/speech_translate?useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL5Dialect
```

### Build and Run

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/speech-translate-backend-1.0.0.jar
```

The application will run at http://localhost:8080

## API Documentation

### WebSocket Endpoints

- `/speech` - WebSocket endpoint for receiving audio streams and sending transcription results

### REST API Endpoints

- `GET /api/transcriptions` - Get all transcription records
- `GET /api/transcriptions/{id}` - Get transcription record by ID
- `DELETE /api/transcriptions/{id}` - Delete transcription record by ID

## Project Structure

```
├── src/
│   ├── main/
│   │   ├── java/com/speechtranslate/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # Controllers
│   │   │   ├── model/            # Data models
│   │   │   ├── repository/       # Data access layer
│   │   │   ├── service/          # Business logic layer
│   │   │   ├── websocket/        # WebSocket handling
│   │   │   └── SpeechTranslateApplication.java  # Application entry
│   │   └── resources/
│   │       ├── application.properties  # Application configuration
│   │       ├── static/                 # Static resources
│   │       └── templates/              # Template files
│   └── test/                           # Test code
└── pom.xml                             # Maven configuration
```

## Communication with Frontend

The backend establishes real-time communication with the frontend through WebSocket, receiving audio streams and sending transcription text. WebSocket configuration is in the `com.speechtranslate.config.WebSocketConfig` class.