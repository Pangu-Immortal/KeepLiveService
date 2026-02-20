# Android 项目规范

## 强制要求：GitHub Actions CI/CD

每当创建或初始化 Android 项目时，**必须**自动生成完整的 GitHub Actions 工作流配置，实现一键打包功能。

### 必须创建的文件

```
.github/
├── workflows/
│   └── android-build.yml    # 主构建工作流
└── dependabot.yml           # 依赖自动更新（可选）
```

### android-build.yml 强制规范

工作流必须包含以下功能：

#### 1. 触发条件
- `push` 到 `main`、`develop`、`release/*` 分支
- `pull_request` 到主要分支
- `workflow_dispatch` 手动触发，支持选择构建类型

#### 2. 构建产物
- **必须输出 AAB 包**（Android App Bundle）
- Release 构建必须签名
- 同时保留 APK 输出用于测试

#### 3. 必须包含的 Jobs
- `build`: 构建 Debug/Release AAB
- `lint`: 代码质量检查
- `test`: 单元测试
- `deploy`（可选）: 部署到 Google Play

#### 4. 必须配置的优化
- Gradle 缓存
- 依赖缓存
- 并行构建

### 标准工作流模板

创建 Android 项目时，自动生成以下工作流：

```yaml
name: Android CI/CD

on:
  push:
    branches: [main, develop, 'release/*']
    tags: ['v*']
  pull_request:
    branches: [main, develop]
  workflow_dispatch:
    inputs:
      build_type:
        description: '构建类型'
        required: true
        default: 'release'
        type: choice
        options: [debug, release]

env:
  JAVA_VERSION: '17'
  JAVA_DISTRIBUTION: 'temurin'

jobs:
  build:
    name: Build AAB
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}
          cache: gradle

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
            ~/.android/build-cache
          key: gradle-${{ runner.os }}-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: gradle-${{ runner.os }}-

      - name: Grant execute permission
        run: chmod +x gradlew

      - name: Run Tests
        run: ./gradlew testDebugUnitTest

      - name: Run Lint
        run: ./gradlew lint

      - name: Build Debug AAB
        if: github.event_name == 'pull_request' || (github.event_name == 'workflow_dispatch' && github.event.inputs.build_type == 'debug')
        run: ./gradlew bundleDebug

      - name: Decode Keystore
        if: github.event_name != 'pull_request' && (github.event_name != 'workflow_dispatch' || github.event.inputs.build_type == 'release')
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/keystore.jks

      - name: Build Release AAB
        if: github.event_name != 'pull_request' && (github.event_name != 'workflow_dispatch' || github.event.inputs.build_type == 'release')
        run: ./gradlew bundleRelease
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/app/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      - name: Build Release APK (for testing)
        if: github.event_name != 'pull_request' && (github.event_name != 'workflow_dispatch' || github.event.inputs.build_type == 'release')
        run: ./gradlew assembleRelease
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/app/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      - name: Upload AAB
        uses: actions/upload-artifact@v4
        with:
          name: app-bundle-${{ github.event_name == 'pull_request' && 'debug' || github.event.inputs.build_type || 'release' }}
          path: app/build/outputs/bundle/**/*.aab
          retention-days: 30

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-apk-${{ github.event_name == 'pull_request' && 'debug' || github.event.inputs.build_type || 'release' }}
          path: app/build/outputs/apk/**/*.apk
          retention-days: 14

      - name: Upload Lint Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: lint-report
          path: app/build/reports/lint-results*.html

  deploy-play-store:
    name: Deploy to Play Store
    needs: build
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')
    
    steps:
      - name: Download AAB
        uses: actions/download-artifact@v4
        with:
          name: app-bundle-release

      - name: Deploy to Play Store
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_SERVICE_ACCOUNT }}
          packageName: ${{ secrets.PACKAGE_NAME }}
          releaseFiles: release/*.aab
          track: internal
          status: completed
```

### build.gradle.kts 签名配置要求

创建项目时，必须在 `app/build.gradle.kts` 中包含环境变量签名配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### GitHub Secrets 配置说明

项目 README 必须包含以下 Secrets 配置说明：

| Secret 名称 | 说明 | 获取方式 |
|------------|------|---------|
| `KEYSTORE_BASE64` | 签名文件 base64 | `base64 -i keystore.jks` |
| `KEYSTORE_PASSWORD` | keystore 密码 | - |
| `KEY_ALIAS` | key 别名 | - |
| `KEY_PASSWORD` | key 密码 | - |
| `PLAY_STORE_SERVICE_ACCOUNT` | Play Console 服务账号 JSON | Google Cloud Console |
| `PACKAGE_NAME` | 应用包名 | 如 `com.example.app` |

### 检查清单

创建 Android 项目后，确认以下内容已生成：

- [ ] `.github/workflows/android-build.yml` 存在
- [ ] 工作流包含 AAB 构建步骤
- [ ] 工作流包含签名配置（通过环境变量）
- [ ] 工作流包含缓存配置
- [ ] 工作流包含产物上传
- [ ] `build.gradle.kts` 包含 CI 兼容的签名配置
- [ ] README 包含 Secrets 配置说明

---

**重要**：这是强制性要求，每个 Android 项目必须遵循此规范，不得跳过 GitHub Actions 配置步骤。
