# Real-time Speech-to-Text System - Frontend

## Overview

This is the frontend part of the real-time speech-to-text system, developed based on the React framework, providing a user-friendly interface that supports user microphone recording and real-time display of transcribed text.

## Technology Stack

- React.js
- WebSocket client
- HTML5 Web Audio API
- Material-UI (UI component library)

## Features

- User-friendly interface design
- Real-time microphone recording functionality
- Real-time display of speech transcription results
- History management
- Responsive design, adapting to different devices

## Development Environment Setup

### Prerequisites

- Node.js 14+
- npm 6+ or yarn 1.22+

### Installing Dependencies

```bash
npm install
# or
yarn install
```

### Starting the Development Server

```bash
npm start
# or
yarn start
```

The application will run at http://localhost:3000

### Building for Production

```bash
npm run build
# or
yarn build
```

## Project Structure

```
├── public/              # Static resources
│   ├── index.html       # HTML template
│   └── favicon.ico      # Website icon
├── src/                 # Source code
│   ├── components/      # React components
│   │   ├── AudioRecorder.js    # Audio recording component
│   │   ├── TranscriptionDisplay.js  # Transcription display component
│   │   ├── Header.js   # Page header component
│   │   └── Footer.js   # Page footer component
│   ├── services/        # API services
│   │   └── websocket.js # WebSocket communication service
│   ├── styles/          # Style files
│   ├── App.js           # Main application component
│   └── index.js         # Application entry point
└── package.json         # Dependency configuration
```

## Communication with Backend

The frontend establishes real-time communication with the backend server via WebSocket, sending audio streams and receiving transcribed text. The WebSocket connection is configured in the `src/services/websocket.js` file.