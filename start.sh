#!/bin/bash

# Parrot Tunes Application Startup Script

echo "🦜 Starting Parrot Tunes Backend..."
echo "=================================="

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed or not in PATH"
    echo "Please install Java 17 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | grep "version" | awk '{print $3}' | tr -d '"' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java version $JAVA_VERSION is too old"
    echo "Please install Java 17 or higher"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed or not in PATH"
    echo "Please install Maven 3.6 or higher"
    exit 1
fi

# Create uploads directory if it doesn't exist
mkdir -p uploads/covers uploads/temp

echo "✅ Java version: $JAVA_VERSION"
echo "✅ Maven is available"
echo "✅ Upload directories created"
echo "✅ Using embedded Apache Derby database (no external database setup needed)"
echo ""

echo "🔧 Building application..."
./mvnw clean compile -q

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
    echo ""
    echo "🚀 Starting Parrot Tunes..."
    echo "API will be available at: http://localhost:9999/api"
    echo "Press Ctrl+C to stop the application"
    echo ""
    ./mvnw spring-boot:run
else
    echo "❌ Build failed"
    echo "Please check the error messages above"
    exit 1
fi
