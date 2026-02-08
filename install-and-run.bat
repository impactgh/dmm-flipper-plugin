@echo off
echo Building plugin...
call gradlew.bat build

echo Copying to RuneLite...
copy /Y build\libs\dmm-flipper-plugin-1.0.0.jar "%USERPROFILE%\.runelite\externalplugins\"

echo Done! Now launch RuneLite normally.
pause
