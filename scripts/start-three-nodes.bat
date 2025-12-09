@echo off
setlocal

rem Start three node instances with predefined configs.
rem Usage: scripts\start-three-nodes.bat

pushd "%~dp0\.."

echo Starting three nodes...

start "node1" /min cmd /c ".\gradlew.bat :node:run --args=""config/nodes/node1.json"""
timeout /t 2 >nul
start "node2" /min cmd /c ".\gradlew.bat :node:run --args=""config/nodes/node2.json"""
timeout /t 2 >nul
start "node3" /min cmd /c ".\gradlew.bat :node:run --args=""config/nodes/node3.json"""

echo Nodes launched in separate windows. Close their windows to stop.

popd
endlocal

