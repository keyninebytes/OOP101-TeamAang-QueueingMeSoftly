#!/usr/bin/env sh
# run.sh — compile and start Queueing Me Softly (macOS / Linux). Requires JDK 17+.
set -e
cd "$(dirname "$0")"
mkdir -p out
javac --release 17 -encoding UTF-8 -implicit:class -d out -sourcepath src src/com/toda/Main.java src/com/toda/SelfTest.java
echo "Compiled. Starting server..."
exec java -Dfile.encoding=UTF-8 -cp out com.toda.Main "$@"
