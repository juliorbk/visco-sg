#!/bin/sh
# Force our JVM args ignoring Render's JAVA_TOOL_OPTIONS
unset JAVA_TOOL_OPTIONS

exec java \
  -XX:+UseG1GC \
  -Xmx256m \
  -Xms128m \
  -Xss512k \
  -XX:MaxMetaspaceSize=160m \
  -XX:+UseCompressedOops \
  -XX:+ExitOnOutOfMemoryError \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp \
  -Djava.security.egd=file:/dev/./urandom \
  -jar app.jar
