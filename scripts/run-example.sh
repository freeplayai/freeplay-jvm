#!/bin/bash
#
# Run a Java example class against the local Freeplay server.
#
# Usage:
#   ./scripts/run-example.sh <fully.qualified.ClassName>
#
# Examples:
#   ./scripts/run-example.sh ai.freeplay.example.java.ThinOpenAIResponsesExample
#   ./scripts/run-example.sh ai.freeplay.example.java.ThinOpenAIExample
#

set -euo pipefail
cd "$(dirname "$0")/.."

if [ $# -lt 1 ]; then
  echo "Usage: $0 <fully.qualified.ClassName>"
  echo ""
  echo "Available examples:"
  find examples/src/main/java -name "*.java" -exec grep -l "public static void main" {} \; \
    | sed 's|examples/src/main/java/||; s|/|.|g; s|\.java$||' \
    | sort
  exit 1
fi

MAIN_CLASS="$1"
shift

# Load environment variables
if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

# Build
./gradlew -q :lib:jar :examples:classes 2>/dev/null

# Setup truststore for local dev (merges default CA certs with local cert)
TRUSTSTORE="/tmp/freeplay-truststore.jks"
TRUSTSTORE_PASS="changeit"
LOCAL_CERT="../freeplay-app/.certs/localhost.pem"
JAVA_HOME_DIR=$(/usr/libexec/java_home 2>/dev/null || echo "$JAVA_HOME")
DEFAULT_CACERTS="$JAVA_HOME_DIR/lib/security/cacerts"

if [ ! -f "$TRUSTSTORE" ] && [ -f "$LOCAL_CERT" ]; then
  echo "Creating truststore with local dev cert..."
  cp "$DEFAULT_CACERTS" "$TRUSTSTORE"
  keytool -importcert -noprompt -alias localhost \
    -file "$LOCAL_CERT" \
    -keystore "$TRUSTSTORE" \
    -storepass "$TRUSTSTORE_PASS" 2>/dev/null
fi

# Build classpath
CLASSPATH="examples/build/classes/java/main"
CLASSPATH="$CLASSPATH:lib/build/classes/java/main"
CLASSPATH="$CLASSPATH:lib/build/resources/main"
CLASSPATH="$CLASSPATH:$(./gradlew -q :lib:printClasspath 2>/dev/null)"

# Build JVM args
JVM_ARGS=""
if [ -f "$TRUSTSTORE" ]; then
  JVM_ARGS="-Djavax.net.ssl.trustStore=$TRUSTSTORE -Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASS"
fi

exec java $JVM_ARGS -cp "$CLASSPATH" "$MAIN_CLASS" "$@"
