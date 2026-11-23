#!/usr/bin/env sh
# test.sh — compile and run the dependency-free self test (macOS / Linux).
set -e
cd "$(dirname "$0")"
mkdir -p out
javac --release 17 -encoding UTF-8 -implicit:class -d out -sourcepath src src/com/toda/Main.java src/com/toda/SelfTest.java
exec java -Dfile.encoding=UTF-8 -cp out com.toda.SelfTest
