export TRIGGERING_SHA="$(git rev-parse HEAD)"
curl -s -X POST "https://api.telegram.org/bot${TG_BOT_KEY}/sendMessage" -d chat_id="-1001415196670" \
  -d "disable_web_page_preview=true" \
  -d "parse_mode=markdown" \
  -d text="🤖 \`building with HEAD as\` [$(echo $TRIGGERING_SHA | cut -c1-8)](https://github.com/uditkarode/AVAMusicPlayer/commit/${TRIGGERING_SHA}) 🤖"
sed -i s/INSERT_FLURRY_KEY/${FLURRY_KEY}/ app/src/main/kotlin/io/github/uditkarode/able/utils/Constants.kt
sed -i s/INSERT_RAPID_KEY/${RAPID_KEY}/ app/src/main/kotlin/io/github/uditkarode/able/utils/Constants.kt
sed -i s/Debug/$(echo $TRIGGERING_SHA | cut -c1-8)/ app/src/main/kotlin/io/github/uditkarode/able/utils/Constants.kt
git clone -b builds --single-branch https://github.com/uditkarode/AVAMusicPlayer.git GitBuilds
git clone "https://x-access-token:${GH_TOKEN}@github.com/uditkarode/AVAMusicKeystore" --depth 1
mv AVAMusicKeystore/release.keystore app
rm -rf AVAMusicKeystore
cd GitBuilds
git reset --hard 1798529c23bb57742e544e75b030aa2ce16baebb
git reflog expire --all --expire=now
git gc --prune=now --aggressive
cd ..
./gradlew assembleRelease
bash movebuild.sh "arm64-v8a"
sed -i s/armeabi-v7a/arm64-v8a/ app/build.gradle
./gradlew assembleRelease
bash movebuild.sh "armeabi-v7a"
cd GitBuilds
git add .
git config --global user.name 'Bob The Builder'
git config --global user.email 'bob@the.builder'
git commit -m "bot: build installables <$(echo $TRIGGERING_SHA | cut -c1-8)>"
git remote set-url origin "https://x-access-token:${GH_TOKEN}@github.com/uditkarode/AVAMusicPlayer"
git push -u origin builds -f
curl -s -X POST "https://api.telegram.org/bot${TG_BOT_KEY}/sendMessage" -d chat_id="-1001415196670" \
  -d "disable_web_page_preview=true" \
  -d "parse_mode=markdown" \
  -d text="🤖 \`workflow for\` [$(echo $TRIGGERING_SHA | cut -c1-8)](https://github.com/uditkarode/AVAMusicPlayer/commit/${TRIGGERING_SHA}) \`finished\` 🤖"
curl -s -X POST "https://api.telegram.org/bot${TG_BOT_KEY}/sendSticker" -d chat_id="-1001415196670" \
 -d "sticker=CAACAgUAAxkBAAJc416cRVy18acXP6HgtZSnxuuoJi01AAK9AAMtO2YjehthhceJ__sYBA"
