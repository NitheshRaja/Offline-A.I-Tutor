# Offline A.I Tutor

An Android application that provides an intelligent tutoring experience using offline AI models. The app offers various AI models including Gemma and DeepSeek for different learning scenarios.

## Features

- 🤖 Multiple AI Models Support
  - Gemma Model
  - DeepSeek Model
- 📚 Various Learning Scenarios
  - Interview Preparation
  - Professional Meetings
  - Role Play
  - Customer Care
- 🔒 Secure Authentication
  - Email/Password Login
  - Sign Up Functionality
- 💬 Interactive Chat Interface
  - Real-time AI Responses
  - Message History
- 🎨 Modern UI/UX
  - Material Design Components
  - Custom Animations
  - Responsive Layout

## Project Structure

```
Offline-A.I-Tutor/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── google/
│   │   │   │           └── mediapipe/
│   │   │   │               └── examples/
│   │   │   │                   └── llminference/
│   │   │   │                       ├── auth/
│   │   │   │                       │   ├── AuthActivity.kt
│   │   │   │                       │   └── SignUpActivity.kt
│   │   │   │                       ├── chat/
│   │   │   │                       │   └── ChatActivity.kt
│   │   │   │                       ├── main/
│   │   │   │                       │   └── MainActivity.kt
│   │   │   │                       ├── ui/
│   │   │   │                       │   └── theme/
│   │   │   │                       │       └── Theme.kt
│   │   │   │                       ├── ChatViewModel.kt
│   │   │   │                       └── PermissionHandler.kt
│   │   │   ├── res/
│   │   │   │   ├── anim/
│   │   │   │   ├── drawable/
│   │   │   │   ├── font/
│   │   │   │   ├── layout/
│   │   │   │   ├── mipmap/
│   │   │   │   └── values/
│   │   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Setup Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/NitheshRaja/Offline-A.I-Tutor.git
   ```

2. Open the project in Android Studio

3. Sync the project with Gradle files

4. Build and run the application

## Requirements

- Android Studio Arctic Fox or newer
- Android SDK 21 or higher
- Kotlin 1.8.0 or higher
- Gradle 7.0 or higher

## Dependencies

- Material Design Components
- AndroidX Core KTX
- AndroidX AppCompat
- AndroidX ConstraintLayout
- AndroidX Lifecycle Components
- Kotlin Coroutines
- MediaPipe

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details

## Contact

Nithesh Raja - [Your GitHub Profile](https://github.com/NitheshRaja)

Project Link: [https://github.com/NitheshRaja/Offline-A.I-Tutor](https://github.com/NitheshRaja/Offline-A.I-Tutor) 