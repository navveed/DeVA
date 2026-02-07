# Panda (Blurr) - AI Phone Operator Android App

Panda is an Android application written in Kotlin that serves as a proactive, on-device AI agent for Android. It uses accessibility services to understand and operate phone UI, supporting voice commands and AI-powered task automation.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Version Management
- **Android Gradle Plugin (AGP)**: Version 8.9.2 is specified in `gradle/libs.versions.toml`
- **CRITICAL**: AGP version must remain 8.9.2 - this is the latest stable version
- **NEVER** change AGP version to 8.5.2 - this is an older, outdated version
- **NEVER** downgrade AGP version without explicit project requirements
- **ALWAYS** verify version compatibility before making changes to dependency versions
- Current AGP 8.9.2 is the latest stable version and should remain unchanged

### Prerequisites and Environment Setup
- **CRITICAL**: This project requires Android Studio (latest version) or Android command-line tools
- **CRITICAL**: Android SDK must be installed and accessible at `/usr/local/lib/android/sdk` or equivalent
- **CRITICAL**: Only builds on systems with proper Android development environment - will NOT build in basic Linux CI environments
- Target Android API 35, minimum SDK 24
- Requires JDK 11+ (OpenJDK 17 recommended)
- **CRITICAL**: Android Gradle Plugin (AGP) version is 8.9.2 (defined in `gradle/libs.versions.toml`) - DO NOT change this to 8.5.2 or downgrade to any older version

### Initial Setup Commands
1. **Install Android Studio or Android Command Line Tools**
   - Download from https://developer.android.com/studio
   - Ensure Android SDK is properly configured
   - Verify SDK path: `ls $ANDROID_SDK_ROOT` or `ls /usr/local/lib/android/sdk`

2. **Clone and configure API keys:**
   ```bash
   git clone https://github.com/Ayush0Chaudhary/blurr.git
   cd blurr
   cp local.properties.template local.properties
   ```

3. **Configure local.properties with real API keys:**
   ```properties
   sdk.dir=/path/to/your/android/sdk
   GEMINI_API_KEYS=your_comma_separated_gemini_keys
   PICOVOICE_ACCESS_KEY=your_picovoice_key
   TAVILY_API=your_tavily_api_key
   MEM0_API=your_mem0_api_key
   GOOGLE_TTS_API_KEY=your_google_tts_key
   GCLOUD_GATEWAY_PICOVOICE_KEY=your_gateway_key
   GCLOUD_GATEWAY_URL=your_gateway_url
   GCLOUD_PROXY_URL=your_proxy_url
   GCLOUD_PROXY_URL_KEY=your_proxy_key
   ```

### Build Commands
- **NEVER CANCEL builds** - Android builds can take 10-30 minutes on first run
- **ALWAYS use minimum 45-minute timeouts** for initial builds
- **Subsequent builds** typically take 3-8 minutes after dependencies cached

```bash
# Clean build (first time may take 20-30 minutes)
./gradlew clean build --no-daemon
# NEVER CANCEL - Set timeout to 60+ minutes

# Faster incremental build
./gradlew assembleDebug
# Takes 3-8 minutes typically

# Build specific variant
./gradlew assembleRelease
```

### Testing Commands
```bash
# Run unit tests (2-5 minutes)
./gradlew test --no-daemon
# NEVER CANCEL - Set timeout to 15+ minutes

# Run all tests including instrumented (requires emulator/device)
./gradlew connectedAndroidTest
# NEVER CANCEL - Set timeout to 30+ minutes

# Specific test classes
./gradlew test --tests="com.blurr.voice.agent.v2.SystemPromptTest"
```

### Development Workflow
```bash
# Install app to connected device/emulator
./gradlew installDebug

# Install and launch
./gradlew installDebug && adb shell am start -n com.blurr.voice/.MainActivity

# View live logs (useful for debugging)
adb logcat | grep GeminiApi
```

## Validation Requirements

### ALWAYS run these before committing changes:
1. **Build validation**: `./gradlew assembleDebug` (wait for completion - 3-8 minutes)
2. **Unit tests**: `./gradlew test` (wait for completion - 5-15 minutes)  
3. **Code inspection**: Review any lint warnings in Android Studio

### Manual Testing Requirements
When making changes to core functionality:
1. **Install on device**: Use `./gradlew installDebug`
2. **Grant accessibility permission**: Enable "Panda" in Settings > Accessibility
3. **Test voice commands**: Press microphone, speak a command
4. **Test task execution**: Verify AI agent can interact with UI
5. **Check TTS output**: Ensure voice feedback works correctly

## Project Structure

### Key Directories
```
app/src/main/java/com/blurr/voice/
├── agent/               # AI agent logic and prompts
├── api/                 # API integrations (Gemini, etc.)
├── services/            # Android services (AgentTaskService, etc.)
├── ui/                  # UI components and activities
├── v2/                  # Version 2 agent implementation
└── utils/               # Utility classes
```

### Important Files
- `app/src/main/java/com/blurr/voice/services/AgentTaskService.kt` - Core agent execution service
- `app/src/main/java/com/blurr/voice/api/GeminiApi.kt` - Gemini AI integration
- `app/src/main/java/com/blurr/voice/v2/llm/GeminiAPI.kt` - Enhanced Gemini API v2
- `app/src/main/AndroidManifest.xml` - App permissions and services
- `local.properties` - API keys and SDK configuration (never commit)
- `gradle/libs.versions.toml` - Dependency versions

### Documentation
Check `/docs` directory for detailed feature documentation:
- `INTERACTIVE_DIALOGUE_SYSTEM.md` - Clarification system
- `TTS_DEBUG_MODE.md` - Text-to-speech debugging
- `STT_IMPLEMENTATION.md` - Speech-to-text features

## Common Issues and Solutions

### Copilot Guidelines - DO NOT IGNORE
- **NEVER suggest changing AGP version from 8.9.2 to 8.5.2** - 8.5.2 is outdated
- **NEVER downgrade Android Gradle Plugin** without explicit requirements
- **ALWAYS maintain current dependency versions** unless security updates are needed
- **VERIFY compatibility** before suggesting version changes

### Build Failures
- **"Plugin not found" errors**: Ensure Android SDK is properly installed and gradle can access Google Maven
- **"API key missing" errors**: Verify all required keys are in `local.properties`
- **Memory issues**: Increase JVM heap in `gradle.properties`: `org.gradle.jvmargs=-Xmx4g`

### Runtime Issues
- **Accessibility service not working**: Must manually enable in Android Settings
- **Voice commands not responding**: Check microphone permissions and TTS configuration
- **API call failures**: Verify internet connectivity and API key validity

### Network Requirements
- Internet access required for AI API calls
- Google Maven repository access needed for builds
- Firebase services connectivity required

## Development Notes

### API Dependencies
This app integrates with multiple external services:
- **Gemini AI**: Core reasoning and language processing
- **Picovoice**: Wake word detection ("Hey Panda")  
- **Google Cloud TTS**: High-quality speech synthesis
- **Tavily**: Web search capabilities
- **Mem0**: Persistent memory system

### Performance Considerations
- Initial app launch may take 10-30 seconds on first run
- AI responses typically take 2-8 seconds depending on complexity
- Screenshot processing adds 1-3 seconds to task execution
- Multiple API keys improve response speed through load balancing

### Testing Notes
- Unit tests focus on prompt generation and agent logic
- Integration tests require Android emulator or physical device
- Manual testing essential for accessibility service functionality
- Real API keys required for full feature testing

## Emergency Procedures

### If Build System Breaks
1. `./gradlew clean` - Clear all build artifacts
2. Delete `.gradle` directory in user home: `rm -rf ~/.gradle`
3. Re-sync in Android Studio: File > Sync Project with Gradle Files
4. If still failing, check Android SDK installation and update

### If App Crashes on Device
1. Check logs: `adb logcat | grep -E "(FATAL|ERROR|AndroidRuntime)"`
2. Verify API keys are valid
3. Check device has sufficient storage (2GB+ recommended)
4. Ensure accessibility permission is granted

## Quick Reference

### Essential Commands
```bash
# Setup
cp local.properties.template local.properties
# Edit local.properties with real API keys

# Build (45+ min first time)
./gradlew assembleDebug

# Test (15+ min)  
./gradlew test

# Install to device
./gradlew installDebug

# View logs
adb logcat | grep GeminiApi
```

### Required API Services
- Gemini API: https://makersuite.google.com/app/apikey
- Picovoice: https://console.picovoice.ai/
- Google Cloud TTS: https://cloud.google.com/text-to-speech
- Tavily: https://tavily.com/
- Mem0: https://mem0.ai/

### File Locations
- Main source: `app/src/main/java/com/blurr/voice/`
- Tests: `app/src/test/java/com/blurr/voice/`
- Resources: `app/src/main/res/`
- Build config: `app/build.gradle.kts`
- Dependencies: `gradle/libs.versions.toml` (AGP version 8.9.2 - NEVER change to 8.5.2)