#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home}"

# Build Kotlin and Swift separately to avoid overlapping their peak memory use.
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 \
  -Pkotlin.native.cacheKind=none \
  -Pkotlin.compiler.execution.strategy=in-process \
  --no-daemon --console=plain

CASHFLOW_SKIP_KOTLIN_BUILD=YES xcodebuild \
  -project iosApp/Cashflow.xcodeproj -scheme Cashflow \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/ios ARCHS=arm64 ONLY_ACTIVE_ARCH=YES \
  SWIFT_ENABLE_EXPLICIT_MODULES=NO CODE_SIGNING_ALLOWED=NO build
