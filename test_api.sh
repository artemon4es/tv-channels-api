#!/bin/bash

# Обновленный скрипт тестирования API для artemon4es
# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Базовый URL для GitHub Pages
BASE_URL="https://artemon4es.github.io/tv-channels-api"

echo -e "${BLUE}🧪 Тестирование GitHub Pages API${NC}"
echo -e "${BLUE}📍 Базовый URL: $BASE_URL${NC}"
echo "=================================="

# Функция для проверки URL
test_url() {
    local url=$1
    local name=$2
    local expected_content=$3
    
    echo -e "${YELLOW}Проверка: $name${NC}"
    echo -e "🔗 URL: $url"
    
    # Проверка доступности
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    
    if [ "$response" -eq 200 ]; then
        echo -e "${GREEN}✅ Статус: 200 OK${NC}"
        
        # Проверка содержимого
        if [ ! -z "$expected_content" ]; then
            content=$(curl -s "$url")
            if echo "$content" | grep -q "$expected_content"; then
                echo -e "${GREEN}✅ Содержимое корректное${NC}"
            else
                echo -e "${RED}❌ Содержимое не найдено: $expected_content${NC}"
                return 1
            fi
        fi
    else
        echo -e "${RED}❌ Статус: $response${NC}"
        return 1
    fi
    
    echo ""
    return 0
}

# Счетчик успешных тестов
success_count=0
total_tests=4

# Тест 1: Главная страница
if test_url "$BASE_URL" "Главная страница" "TV Channels API"; then
    ((success_count++))
fi

# Тест 2: Конфигурация
if test_url "$BASE_URL/api/config.json" "Конфигурация" "service_config"; then
    ((success_count++))
fi

# Тест 3: Список каналов
if test_url "$BASE_URL/files/channels.m3u8" "Список каналов" "#EXTM3U"; then
    ((success_count++))
fi

# Тест 4: Конфигурация безопасности
if test_url "$BASE_URL/files/security_config.xml" "Конфигурация безопасности" "network-security-config"; then
    ((success_count++))
fi

echo "=================================="
echo -e "${BLUE}📊 Результаты тестирования:${NC}"
echo -e "✅ Успешно: $success_count/$total_tests"

if [ $success_count -eq $total_tests ]; then
    echo -e "${GREEN}🎉 Все тесты пройдены успешно!${NC}"
    echo -e "${GREEN}📱 API готов для использования в приложении${NC}"
    exit 0
else
    echo -e "${RED}❌ Некоторые тесты не пройдены${NC}"
    echo -e "${YELLOW}💡 Убедитесь, что GitHub Pages активирован${NC}"
    exit 1
fi 