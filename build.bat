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

set LOD_JAR=
for /f "delims=" %%F in ('dir /b /a-d "..\..\lod-game-*.jar" 2^>nul ^| findstr /v /i "sources"') do (
    set LOD_JAR=..\..\%%F
)
if "!LOD_JAR!"=="" set LOD_JAR=..\..\lod-game-3a840708987eef4f1d8005854349cb4373fff832.jar

echo Compiling Java sources with !LOD_JAR!...
"%JDK_PATH%\javac.exe" -cp "!LOD_JAR!;..\..\libs/*" -d "bin" src\legend\multimod\*.java
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
