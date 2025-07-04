#!/bin/bash

# 📦 Сборка APK для TV Channels с GitHub Pages API
# Использование: ./build_apk.sh [your-github-username] [version]

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}📦 TV Channels APK Builder${NC}"
echo "=========================="

# Проверяем аргументы
if [ $# -eq 0 ]; then
    echo -e "${RED}❌ Ошибка: Укажите ваш GitHub username${NC}"
    echo "Использование: ./build_apk.sh your-github-username [version]"
    exit 1
fi

GITHUB_USERNAME=$1
VERSION=${2:-"1.1"}
REPO_NAME="tv-channels-api"
API_BASE_URL="https://$GITHUB_USERNAME.github.io/$REPO_NAME"

echo -e "${YELLOW}🔧 Параметры сборки:${NC}"
echo "GitHub Username: $GITHUB_USERNAME"
echo "Версия: $VERSION"
echo "API URL: $API_BASE_URL"
echo ""

# Проверяем наличие Android SDK
ANDROID_PROJECT_DIR="Android TV"
if [ ! -d "$ANDROID_PROJECT_DIR" ]; then
    echo -e "${RED}❌ Директория Android проекта не найдена: $ANDROID_PROJECT_DIR${NC}"
    exit 1
fi

cd "$ANDROID_PROJECT_DIR"

# Проверяем наличие gradlew
if [ ! -f "gradlew" ]; then
    echo -e "${RED}❌ gradlew не найден. Убедитесь, что находитесь в корне Android проекта.${NC}"
    exit 1
fi

# Делаем gradlew исполняемым
chmod +x gradlew

# Проверяем наличие Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java не найден. Установите JDK и попробуйте снова.${NC}"
    exit 1
fi

# Обновляем URL API в RemoteConfigManager.kt
echo -e "${BLUE}🔧 Обновление API URL...${NC}"
CONFIG_FILE="app/src/main/java/com/example/androidtv/RemoteConfigManager.kt"

if [ -f "$CONFIG_FILE" ]; then
    # Создаем резервную копию
    cp "$CONFIG_FILE" "$CONFIG_FILE.backup"
    
    # Обновляем URL
    sed -i "s|private const val BASE_URL = \"https://.*\"|private const val BASE_URL = \"$API_BASE_URL\"|g" "$CONFIG_FILE"
    
    echo -e "${GREEN}✅ API URL обновлен в $CONFIG_FILE${NC}"
    
    # Показываем изменения
    echo -e "${BLUE}📝 Изменения:${NC}"
    grep "BASE_URL" "$CONFIG_FILE" || echo "Не найдено BASE_URL"
else
    echo -e "${RED}❌ Файл $CONFIG_FILE не найден${NC}"
    exit 1
fi

# Обновляем версию в build.gradle
echo -e "${BLUE}🔧 Обновление версии...${NC}"
BUILD_GRADLE="app/build.gradle"

if [ -f "$BUILD_GRADLE" ]; then
    # Создаем резервную копию
    cp "$BUILD_GRADLE" "$BUILD_GRADLE.backup"
    
    # Обновляем versionName
    sed -i "s/versionName .*/versionName \"$VERSION\"/" "$BUILD_GRADLE"
    
    # Увеличиваем versionCode
    CURRENT_VERSION_CODE=$(grep "versionCode" "$BUILD_GRADLE" | head -1 | grep -o '[0-9]\+' || echo "1")
    NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
    sed -i "s/versionCode .*/versionCode $NEW_VERSION_CODE/" "$BUILD_GRADLE"
    
    echo -e "${GREEN}✅ Версия обновлена: $VERSION (код: $NEW_VERSION_CODE)${NC}"
else
    echo -e "${RED}❌ Файл $BUILD_GRADLE не найден${NC}"
    exit 1
fi

# Очищаем предыдущие сборки
echo -e "${BLUE}🧹 Очистка предыдущих сборок...${NC}"
./gradlew clean

# Сборка Debug версии
echo -e "${BLUE}🔨 Сборка Debug версии...${NC}"
./gradlew assembleDebug

# Сборка Release версии
echo -e "${BLUE}🔨 Сборка Release версии...${NC}"
./gradlew assembleRelease

# Проверяем результаты сборки
echo -e "${BLUE}📋 Результаты сборки:${NC}"
echo ""

DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="app/build/outputs/apk/release/app-release.apk"

if [ -f "$DEBUG_APK" ]; then
    DEBUG_SIZE=$(du -h "$DEBUG_APK" | cut -f1)
    echo -e "${GREEN}✅ Debug APK: $DEBUG_APK ($DEBUG_SIZE)${NC}"
else
    echo -e "${RED}❌ Debug APK не найден${NC}"
fi

if [ -f "$RELEASE_APK" ]; then
    RELEASE_SIZE=$(du -h "$RELEASE_APK" | cut -f1)
    echo -e "${GREEN}✅ Release APK: $RELEASE_APK ($RELEASE_SIZE)${NC}"
else
    echo -e "${RED}❌ Release APK не найден${NC}"
fi

echo ""

# Создаем именованные копии
echo -e "${BLUE}📁 Создание именованных копий...${NC}"
OUTPUT_DIR="build_output"
mkdir -p "$OUTPUT_DIR"

if [ -f "$DEBUG_APK" ]; then
    DEBUG_OUTPUT="$OUTPUT_DIR/tv-channels-v$VERSION-debug.apk"
    cp "$DEBUG_APK" "$DEBUG_OUTPUT"
    echo -e "${GREEN}✅ Debug копия: $DEBUG_OUTPUT${NC}"
fi

if [ -f "$RELEASE_APK" ]; then
    RELEASE_OUTPUT="$OUTPUT_DIR/tv-channels-v$VERSION-release.apk"
    cp "$RELEASE_APK" "$RELEASE_OUTPUT"
    echo -e "${GREEN}✅ Release копия: $RELEASE_OUTPUT${NC}"
fi

# Генерируем хеши
echo -e "${BLUE}🔒 Генерация хешей...${NC}"
if [ -f "$RELEASE_OUTPUT" ]; then
    MD5_HASH=$(md5sum "$RELEASE_OUTPUT" | cut -d' ' -f1)
    SHA256_HASH=$(sha256sum "$RELEASE_OUTPUT" | cut -d' ' -f1)
    
    echo "MD5: $MD5_HASH" > "$OUTPUT_DIR/tv-channels-v$VERSION-release.apk.hashes"
    echo "SHA256: $SHA256_HASH" >> "$OUTPUT_DIR/tv-channels-v$VERSION-release.apk.hashes"
    
    echo -e "${GREEN}✅ Хеши сохранены в $OUTPUT_DIR/tv-channels-v$VERSION-release.apk.hashes${NC}"
fi

# Восстанавливаем исходные файлы
echo -e "${BLUE}🔄 Восстановление исходных файлов...${NC}"
if [ -f "$CONFIG_FILE.backup" ]; then
    mv "$CONFIG_FILE.backup" "$CONFIG_FILE"
    echo -e "${GREEN}✅ $CONFIG_FILE восстановлен${NC}"
fi

if [ -f "$BUILD_GRADLE.backup" ]; then
    mv "$BUILD_GRADLE.backup" "$BUILD_GRADLE"
    echo -e "${GREEN}✅ $BUILD_GRADLE восстановлен${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Сборка завершена!${NC}"
echo "===================="
echo ""
echo -e "${YELLOW}📱 Готовые APK файлы:${NC}"
ls -la "$OUTPUT_DIR"/*.apk 2>/dev/null || echo "Нет APK файлов"

echo ""
echo -e "${YELLOW}📋 Следующие шаги:${NC}"
echo ""
echo "1. 🧪 Протестируйте APK на устройстве:"
echo "   adb install $OUTPUT_DIR/tv-channels-v$VERSION-debug.apk"
echo ""
echo "2. 🚀 Загрузите в GitHub Pages:"
echo "   • Скопируйте $OUTPUT_DIR/tv-channels-v$VERSION-release.apk"
echo "   • В репозиторий: files/updates/"
echo "   • Обновите api/config.json с новой версией"
echo ""
echo "3. 📊 Обновите конфигурацию API:"
echo "   {"
echo "     \"app_info\": {"
echo "       \"latest_version\": \"$VERSION\","
echo "       \"version_code\": $NEW_VERSION_CODE,"
echo "       \"download_url\": \"$API_BASE_URL/files/updates/tv-channels-v$VERSION-release.apk\""
echo "     }"
echo "   }"
echo ""
echo "4. 🔍 Проверьте API:"
echo "   ./test_api.sh $GITHUB_USERNAME"
echo ""
echo -e "${GREEN}✅ APK готов к развертыванию!${NC}"

# Возвращаемся в корневую директорию
cd .. 