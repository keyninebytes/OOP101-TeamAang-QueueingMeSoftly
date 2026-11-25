@echo off
REM run.bat - compile and start Queueing Me Softly (Windows). Requires JDK 17+.
REM Works from any folder, including paths with spaces such as "queueing-me-softly (1)".
REM Only relative paths are passed to javac; -sourcepath lets it find every class that
REM Main.java and SelfTest.java use, so no list of source files is needed.
setlocal
cd /d "%~dp0"
if not exist "out" mkdir "out"
javac --release 17 -encoding UTF-8 -implicit:class -d out -sourcepath src src/com/toda/Main.java src/com/toda/SelfTest.java
if errorlevel 1 (
  echo.
  echo Compile failed. Check that JDK 17 or newer is installed: javac -version
  pause
  exit /b 1
)
echo Compiled. Starting server...
java -Dfile.encoding=UTF-8 -cp out com.toda.Main %*
pause
