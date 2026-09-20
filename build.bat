@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo Building Tvvlr's Dragoon Multimod...
echo ===================================================

set JDK_PATH=..\..\jdk25\bin
if not exist "%JDK_PATH%\javac.exe" (
    set JDK_PATH=javac
)

if exist "bin" rmdir /S /Q "bin"
mkdir "bin"

echo Compiling Java sources...
"%JDK_PATH%\javac.exe" -cp "..\..\lod-game-b2a49e4df549b1941b1c5af45d0d150f27c2dc4e.jar;..\..\libs/*" -d "bin" src\legend\multimod\*.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo Copying resources...
if exist "resources" (
    xcopy /E /I /Y "resources\*" "bin\" >nul
)

echo Packaging into tvvlr-multimod.jar...
"%JDK_PATH%\jar.exe" cvf "tvvlr-multimod.jar" -C "bin" .
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Packaging failed!
    exit /b %ERRORLEVEL%
)

if exist "..\..\mods" (
    copy /Y "tvvlr-multimod.jar" "..\..\mods\tvvlr-multimod.jar" >nul
)

echo ===================================================
echo [SUCCESS] Mod built successfully!
echo ===================================================
exit /b 0
