# 🔧 НАСТРОЙКА GITHUB НА WINDOWS

## 📋 ПОШАГОВАЯ ИНСТРУКЦИЯ

### **Шаг 1: Установка Git**

1. **Скачайте Git для Windows:**
   - Перейдите: https://git-scm.com/download/win
   - Скачайте "64-bit Git for Windows Setup"

2. **Установите Git:**
   - Запустите установщик
   - **ВАЖНО:** Оставьте все настройки по умолчанию
   - Нажимайте "Next" везде
   - После установки перезагрузите PowerShell

3. **Проверьте установку:**
   ```powershell
   git --version
   ```
   Должно показать версию Git

---

### **Шаг 2: Настройка Git**

```powershell
# Замените на ваши данные GitHub
git config --global user.name "artemon4es"
git config --global user.email "ваш-email@example.com"

# Проверка настройки
git config --global --list
```

---

### **Шаг 3: Создание GitHub репозитория**

1. **Перейдите на GitHub.com и войдите в аккаунт**

2. **Создайте новый репозиторий:**
   - Нажмите "+" → "New repository"
   - **Repository name**: `tv-channels-api`
   - **Public** ✅ (обязательно для GitHub Pages)
   - **Add README file** ✅
   - Нажмите "Create repository"

3. **Скопируйте URL репозитория:**
   ```
   https://github.com/artemon4es/tv-channels-api.git
   ```

---

### **Шаг 4: Создание Personal Access Token**

1. **Перейдите в настройки GitHub:**
   - GitHub.com → Аватар → Settings
   - Developer settings → Personal access tokens → Tokens (classic)

2. **Создайте новый токен:**
   - "Generate new token" → "Generate new token (classic)"
   - **Note**: `TV Channels API Management`
   - **Expiration**: `No expiration` или `1 year`
   - **Scopes**: ✅ **repo** (полный доступ к репозиториям)
   - Нажмите "Generate token"

3. **СОХРАНИТЕ ТОКЕН НЕМЕДЛЕННО!**
   ```
   Пример: ghp_1234567890abcdefghijklmnopqrstuvwxyz123
   ```
   ⚠️ Токен больше не будет показан!

---

### **Шаг 5: Инициализация проекта**

```powershell
# Переходим в папку проекта
cd "C:\Users\ghostLeto\Desktop\Android TV"

# Инициализируем Git репозиторий
git init

# Добавляем удаленный репозиторий
git remote add origin https://github.com/artemon4es/tv-channels-api.git

# Проверяем подключение
git remote -v
```

---

### **Шаг 6: Первая загрузка файлов**

```powershell
# Добавляем все файлы
git add .

# Делаем первый коммит
git commit -m "🚀 Первая загрузка IPTV Control Panel"

# Загружаем на GitHub (будет запрошен логин и пароль)
git push -u origin main
```

**При запросе авторизации:**
- **Username**: `artemon4es`
- **Password**: `ваш_personal_access_token`

---

### **Шаг 7: Активация GitHub Pages**

1. **Перейдите в настройки репозитория:**
   https://github.com/artemon4es/tv-channels-api/settings/pages

2. **Настройте GitHub Pages:**
   - **Source**: "Deploy from a branch"
   - **Branch**: "main"
   - **Folder**: "/ (root)"
   - Нажмите "Save"

3. **Подождите 5-10 минут**
   - GitHub Pages активируется
   - Ваш сайт будет доступен по адресу:
   ```
   https://artemon4es.github.io/tv-channels-api/
   ```

---

### **Шаг 8: Тестирование скриптов**

```powershell
# Проверка API (после активации GitHub Pages)
./test_api_simple.ps1

# Загрузка изменений на GitHub
./upload_to_github.ps1

# Быстрое отключение сервиса
./disable_service.ps1

# Быстрое включение сервиса
./enable_service.ps1
```

---

## 🛠️ **РЕШЕНИЕ ПРОБЛЕМ**

### **❌ Проблема: "git command not found"**
**Решение:**
1. Перезагрузите PowerShell
2. Перезагрузите компьютер
3. Проверьте PATH: `$env:PATH -split ';' | Select-String git`

### **❌ Проблема: "Authentication failed"**
**Решение:**
1. Убедитесь что используете Personal Access Token, а не обычный пароль
2. Проверьте права токена (должен быть scope "repo")
3. Попробуйте еще раз: `git push origin main`

### **❌ Проблема: "Permission denied"**
**Решение:**
```powershell
# Настройте Git Credential Manager
git config --global credential.helper manager-core

# Попробуйте загрузку снова
git push origin main
```

### **❌ Проблема: "GitHub Pages не активируется"**
**Решение:**
1. Убедитесь что репозиторий публичный
2. Проверьте что файлы загружены в ветку main
3. Подождите 10-15 минут

---

## 📋 **ЧЕКЛИСТ НАСТРОЙКИ**

- [ ] Git установлен и работает
- [ ] Настроены user.name и user.email
- [ ] Создан репозиторий tv-channels-api
- [ ] Создан Personal Access Token
- [ ] Инициализирован локальный репозиторий
- [ ] Добавлен remote origin
- [ ] Файлы загружены на GitHub
- [ ] GitHub Pages активирован
- [ ] API отвечает по адресу
- [ ] Скрипты работают

---

## 🚀 **КОМАНДЫ ДЛЯ ЕЖЕДНЕВНОЙ РАБОТЫ**

### **📝 Обновление файлов:**
```powershell
# Автоматическая загрузка всех изменений
./upload_to_github.ps1
```

### **🔧 Управление сервисом:**
```powershell
# Отключить сервис
./disable_service.ps1

# Включить сервис
./enable_service.ps1
```

### **📊 Проверка состояния:**
```powershell
# Тест API
./test_api_simple.ps1

# Статус Git
git status

# История коммитов
git log --oneline -5
```

---

## 🎯 **ФИНАЛЬНАЯ ПРОВЕРКА**

После настройки у вас должно работать:

1. **Веб-панель**: https://artemon4es.github.io/tv-channels-api/
2. **API endpoints**:
   - https://artemon4es.github.io/tv-channels-api/api/config.json
   - https://artemon4es.github.io/tv-channels-api/files/channels.m3u8
3. **PowerShell скрипты**: Все ./команды работают
4. **Git команды**: git push, git pull, git status

**🎉 ГОТОВО! Теперь вы можете управлять IPTV системой через GitHub!** 