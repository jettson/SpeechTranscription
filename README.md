# Real-time Speech-to-Text System

## Project Overview

This is a web-based real-time speech-to-text system that can stream voice input from the user's microphone, transcribe audio in real-time using STT (Speech-to-Text) models on the backend, and stream the output back to the website for display. The system uses containerized deployment and can be scaled through Kubernetes.

## System Architecture

### Frontend
- User-friendly interface developed with React framework
- Support for user microphone recording and real-time display of transcribed text
- Real-time communication with the backend using WebSocket

### Backend
- Java architecture using SpringBoot+Spring+Hibernate
- Implementation of audio stream reception and processing
- Integration of STT API for real-time speech transcription
- MySQL database for storing voice and text records
- WebSocket service for real-time data transmission

### Deployment
- Docker containerized application
- Kubernetes orchestration and deployment

## Business Process

1. User starts recording on the web interface
2. Audio stream is transmitted to the backend server via WebSocket
3. Backend server saves the audio to the database
4. Backend transcribes audio to text in real-time through STT API
5. Transcription results are sent to the frontend page in real-time via WebSocket
6. Users can view history and manage saved transcription content

## Technology Stack

### Frontend
- React.js
- WebSocket client
- HTML5 Web Audio API
- Bootstrap/Material-UI

### Backend
- Spring Framework (SpringBoot, Spring Core)
- Hibernate ORM
- WebSocket (Spring WebSocket)
- STT API integration
- MySQL database

### Deployment
- Docker
- Kubernetes
- Maven/Gradle build tools

## Installation and Running

### Prerequisites
- JDK 11+
- Node.js 14+
- Docker
- Kubernetes cluster (production environment)
- MySQL database

### Local Development Environment Setup

1. Clone the repository
```bash
git clone https://github.com/jettson/SpeechTranscription.git
cd speech-translate
```

2. Start the backend service
```bash
cd backend
./mvnw spring-boot:run
```

3. Start the frontend service
```bash
cd frontend
npm install
npm start
```

4. Access the application
```
http://localhost:3000
```

### Docker Deployment

```bash
# Build Docker images
docker-compose build

# Start services
docker-compose up -d
```

### Kubernetes Deployment

```bash
# Apply Kubernetes configuration
kubectl apply -f kubernetes/
```

## User Guide

1. Open the application homepage
2. Click the "Start Recording" button
3. Allow browser access to the microphone
4. Start speaking, your speech will be transcribed in real-time and displayed on the page
5. Click the "Stop Recording" button to end
6. Optionally save or share the transcription results

## Project Structure

```
├── frontend/                # React frontend application
│   ├── public/              # Static resources
│   ├── src/                 # Source code
│   │   ├── components/      # React components
│   │   ├── services/        # API services
│   │   └── App.js           # Main application component
│   ├── package.json         # Dependency configuration
│   └── README.md            # Frontend documentation
├── backend/                 # Spring backend application
│   ├── src/                 # Source code
│   │   ├── main/
│   │   │   ├── java/       # Java code
│   │   │   └── resources/   # Configuration files
│   │   └── test/            # Test code
│   ├── pom.xml              # Maven configuration
│   └── README.md            # Backend documentation
├── docker/                  # Docker configuration
│   ├── frontend/            # Frontend Docker configuration
│   └── backend/             # Backend Docker configuration
├── kubernetes/              # Kubernetes configuration
├── docker-compose.yml       # Docker Compose configuration
└── README.md                # Project documentation
```

## License

MIT