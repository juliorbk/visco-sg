#!/bin/sh
# JVM args fijos: evitar OOM en instancias small (512MB).
# unset JAVA_TOOL_OPTIONS evita que el platform inyecte flags conflictivos.
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
