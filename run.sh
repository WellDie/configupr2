#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/classes
find src/main/java -name '*.java' -print > build/sources.txt
javac -encoding UTF-8 -d build/classes @build/sources.txt
exec java -cp build/classes ru.mirea.cargograph.Main "$@"
