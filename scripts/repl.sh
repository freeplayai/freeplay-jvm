#!/bin/bash

# Freeplay Java SDK Interactive REPL
#
# By default, connects to production (app.freeplay.ai).
# Use --local flag to connect to localhost with SSL bypass.
#
# Usage:
#   ./scripts/repl.sh          # Production (default)
#   ./scripts/repl.sh --local  # Local development

# Exit on error
set -e

# Check if running in local mode
IS_LOCAL=false
if [[ "$1" == "--local" ]]; then
    IS_LOCAL=true
fi

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
fi

# Set default values based on mode
FREEPLAY_API_KEY=${FREEPLAY_API_KEY:-""}
FREEPLAY_PROJECT_ID=${FREEPLAY_PROJECT_ID:-""}
FREEPLAY_SESSION_ID=${FREEPLAY_SESSION_ID:-""}

if [ "$IS_LOCAL" = true ]; then
    echo "🔧 Local mode: Configuring for localhost..."
    FREEPLAY_API_URL=${FREEPLAY_API_URL:-"http://localhost:8000"}
    # Disable SSL verification for local development
    export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustAll=true"
else
    FREEPLAY_API_URL=${FREEPLAY_API_URL:-"https://app.freeplay.ai"}
    # Do not disable SSL verification in production mode
    unset JAVA_TOOL_OPTIONS
fi

# Check for required variables
if [ -z "$FREEPLAY_API_KEY" ]; then
    echo "⚠️  Warning: FREEPLAY_API_KEY not set in .env file"
fi

if [ -z "$FREEPLAY_PROJECT_ID" ]; then
    echo "⚠️  Warning: FREEPLAY_PROJECT_ID not set in .env file"
fi

# Build the project first
echo "Building project..."
./gradlew :lib:build -x test -q

# Get the classpath from Gradle
echo "Getting classpath..."
CLASSPATH=$(./gradlew :lib:printClasspath -q)

echo ""
echo "=== Freeplay Java SDK Interactive REPL ==="
echo ""
if [ "$IS_LOCAL" = true ]; then
    echo "Mode: 🔧 Local Development"
else
    echo "Mode: 🌐 Production"
fi
echo ""
echo "Environment:"
echo "  • FREEPLAY_API_KEY    : ${FREEPLAY_API_KEY:0:20}..."
echo "  • FREEPLAY_API_URL    : $FREEPLAY_API_URL"
echo "  • FREEPLAY_PROJECT_ID : $FREEPLAY_PROJECT_ID"
echo "  • FREEPLAY_SESSION_ID : $FREEPLAY_SESSION_ID"
echo ""

if [ "$IS_LOCAL" = true ]; then
    echo "⚠️  SSL verification disabled for local development"
else
    echo "🔒 SSL verification enabled (production mode)"
    echo "    Use --local flag to connect to localhost"
fi

echo ""
echo "Imports (pre-loaded):"
echo "  • import ai.freeplay.client.thin.Freeplay;"
echo "  • import ai.freeplay.client.thin.resources.recordings.*;"
echo "  • import ai.freeplay.client.thin.resources.prompts.ChatMessage;"
echo "  • import ai.freeplay.client.thin.resources.metadata.*;"
echo "  • import ai.freeplay.client.thin.resources.sessions.*;"
echo "  • import java.util.*;"
echo ""
echo "Variables (pre-initialized):"
echo "  • apiKey, baseUrl, projectId, sessionId"
echo ""
echo "Create client with:"
echo "  Freeplay client = new Freeplay(Freeplay.Config().freeplayAPIKey(apiKey).baseUrl(baseUrl));"
echo ""
echo "Exit with: /exit or Ctrl+D"
echo ""

# Create a temporary startup file with imports and variable initialization
STARTUP_FILE=$(mktemp)
cat > "$STARTUP_FILE" << 'EOF'
import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.recordings.*;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.metadata.*;
import ai.freeplay.client.thin.resources.sessions.*;
import java.util.*;

String apiKey = System.getenv("FREEPLAY_API_KEY");
String baseUrl = System.getenv("FREEPLAY_API_URL") + "/api";
String projectId = System.getenv("FREEPLAY_PROJECT_ID");
String sessionId = System.getenv("FREEPLAY_SESSION_ID");

System.out.println("✅ Ready! Variables initialized: apiKey, baseUrl, projectId, sessionId");
System.out.println("");
EOF

# Launch jshell with classpath and startup file
jshell --class-path "$CLASSPATH" --startup "$STARTUP_FILE"

# Cleanup
rm -f "$STARTUP_FILE"