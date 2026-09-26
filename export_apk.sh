#!/bin/bash
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    DATE=$(date +"%Y-%m-%d_%H-%M")
    NEW_APK="Anadolu_Ticaret_Simulasyonu_$DATE.apk"
    rm -f Anadolu_Ticaret_Simulasyonu_*.apk
    cp "$APK_PATH" "$NEW_APK"
    chmod 644 "$NEW_APK"
    echo "APK exported as $NEW_APK"
else
    echo "Error: APK not found at $APK_PATH"
fi
