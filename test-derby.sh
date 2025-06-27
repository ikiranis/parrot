#!/bin/bash

# Parrot Tunes Derby Database Test Script

echo "🧪 Testing Derby Database Integration..."
echo "======================================"

# Clean start
echo "🧹 Cleaning up previous database..."
rm -rf parrot_tunes_db/ derby.log app.log

echo "🚀 Starting application for database test..."
timeout 35s ./mvnw spring-boot:run > app.log 2>&1 &
APP_PID=$!

# Wait for application to start
echo "⏳ Waiting for application startup..."
sleep 20

# Check if database was created
if [ -d "parrot_tunes_db" ]; then
    echo "✅ Derby database directory created successfully"
    echo "📁 Database location: $(pwd)/parrot_tunes_db"
else
    echo "❌ Derby database directory not found"
    exit 1
fi

# Check if derby.log exists and has content
if [ -f "derby.log" ] && [ -s "derby.log" ]; then
    echo "✅ Derby log file created successfully"
    echo "📄 Derby database version:"
    grep "Booting Derby version" derby.log | head -1 | cut -d':' -f3-
else
    echo "❌ Derby log file not found or empty"
fi

# Check application logs for successful startup
if grep -q "Started ParrotTunesApplication" app.log 2>/dev/null; then
    echo "✅ Application started successfully"
elif grep -q "Tomcat started on port" app.log 2>/dev/null; then
    echo "✅ Tomcat server started successfully" 
else
    echo "⚠️  Application may still be starting or encountered issues"
    echo "📋 Last few lines from application log:"
    tail -5 app.log 2>/dev/null || echo "No application log available"
fi

# Test health endpoint (if application is running)
echo "🏥 Testing health endpoint..."
sleep 2
if curl -f -s http://localhost:9999/api/auth/health > /dev/null 2>&1; then
    echo "✅ Health endpoint responding"
    echo "📊 Health status:"
    curl -s http://localhost:9999/api/auth/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:9999/api/auth/health
else
    echo "⚠️  Health endpoint not responding"
fi

# Clean up
echo "🧹 Stopping test application..."
kill $APP_PID 2>/dev/null || true
wait $APP_PID 2>/dev/null || true
sleep 2

echo ""
echo "📊 Test Summary:"
echo "- Derby Database: $([ -d "parrot_tunes_db" ] && echo "✅ Created" || echo "❌ Failed")"
echo "- Derby Log: $([ -f "derby.log" ] && echo "✅ Present" || echo "❌ Missing")"
echo "- Application: $([ -f "app.log" ] && echo "✅ Logged" || echo "❌ No logs")"
echo "- Health Check: $(curl -f -s http://localhost:9999/api/auth/health > /dev/null 2>&1 && echo "✅ Working" || echo "⚠️  Not tested")"

echo ""
echo "🎯 Derby database integration test completed!"
echo ""
echo "🚀 To start the application normally:"
echo "   ./mvnw spring-boot:run"
echo ""
echo "📁 Database files location:"
echo "   $(pwd)/parrot_tunes_db/"

# Clean up test logs
rm -f app.log
