@echo off
REM test.bat - compile and run the dependency-free self test (Windows).
REM Works from any folder, including paths with spaces.
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
java -Dfile.encoding=UTF-8 -cp out com.toda.SelfTest
pause
