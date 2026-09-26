#!/bin/bash
if [ -f "app/build/outputs/bundle/release/app-release.aab" ]; then
    AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
elif [ -f "app/build/outputs/bundle/debug/app-debug.aab" ]; then
    AAB_PATH="app/build/outputs/bundle/debug/app-debug.aab"
else
    echo "Error: AAB file not found"
    exit 1
fi

rm -f Anadolu_Ticaret_Simulasyonu_*.aab Anadolu_Ticaret_Simulasyonu_*.zip
NEW_AAB="Anadolu_Ticaret_Simulasyonu_LATEST.aab"
cp "$AAB_PATH" "$NEW_AAB"
chmod 644 "$NEW_AAB"
echo "AAB exported as $NEW_AAB"
