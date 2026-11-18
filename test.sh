#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/classes
find src/main/java tests -name '*.java' -print > build/test-sources.txt
javac -encoding UTF-8 -d build/classes @build/test-sources.txt
java -ea -cp build/classes ru.mirea.cargograph.TestSuite
