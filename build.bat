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
for %%F in (..\..\lod-game-*.jar) do (
    echo %%F | findstr /i "sources" >nul
    if errorlevel 1 set LOD_JAR=%%F
)
if "%LOD_JAR%"=="" set LOD_JAR=..\..\lod-game-d3d4c02cbf1c74a4db39cfc10520431a6b8cc150.jar

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
