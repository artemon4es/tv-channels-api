/**
 * Функция для загрузки APK файла в репозиторий
 * Исправлена проблема "Maximum call stack size exceeded"
 */
async function uploadAPK() {
    const newVersion = document.getElementById('new-version').value;
    const changelog = document.getElementById('changelog').value;
    const updateRequired = document.getElementById('update-required').checked;
    const apkFile = document.getElementById('apk-file').files[0];
    
    if (!newVersion) {
        alert('❌ Укажите версию APK');
        return;
    }
    
    if (!apkFile) {
        alert('❌ Выберите APK файл');
        return;
    }
    
    log(`🔄 Загрузка APK v${newVersion} через GitHub API (${(apkFile.size / 1024 / 1024).toFixed(1)} MB)...`);
    
    try {
        // Проверяем наличие токена
        if (!authToken) {
            throw new Error('Токен GitHub не настроен. Авторизуйтесь для внесения изменений.');
        }
        
        // Загружаем APK напрямую в репозиторий
        log('📤 Загрузка APK файла в репозиторий...');
        
        // Создаем имя файла
        const apkFileName = `app-v${newVersion}.apk`;
        const apkPath = `files/updates/${apkFileName}`;
        
        // Читаем файл как binary и конвертируем в base64 с использованием FileReader
        // для избежания ошибки Maximum call stack size exceeded
        const base64Content = await new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
                // Получаем base64 строку, удаляя префикс data:application/...
                const base64 = reader.result.split(',')[1];
                resolve(base64);
            };
            reader.onerror = reject;
            reader.readAsDataURL(apkFile);
        });
        
        // Загружаем APK файл
        log('📦 Загрузка APK файла...');
        const uploadResponse = await fetch(`https://api.github.com/repos/artemon4es/tv-channels-api/contents/${apkPath}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'X-GitHub-Api-Version': '2022-11-28',
                'Accept': 'application/vnd.github.v3+json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: `📱 Add APK v${newVersion}`,
                content: base64Content,
                branch: 'main'
            })
        });
        
        if (!uploadResponse.ok) {
            const errorData = await uploadResponse.json();
            throw new Error(`Ошибка загрузки APK: ${errorData.message}`);
        }
        
        log('✅ APK файл успешно загружен');
        
        // Обновляем config.json
        log('🔄 Обновление конфигурации...');
        
        // Получаем текущую конфигурацию
        const configResponse = await fetch(`https://api.github.com/repos/artemon4es/tv-channels-api/contents/api/config.json`, {
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'X-GitHub-Api-Version': '2022-11-28',
                'Accept': 'application/vnd.github.v3+json'
            }
        });
        
        if (!configResponse.ok) {
            throw new Error(`Ошибка получения конфигурации: ${configResponse.status}`);
        }
        
        const configData = await configResponse.json();
        const config = JSON.parse(atob(configData.content));
        
        // Генерируем version_code из версии
        const versionCode = parseInt(newVersion.replace(/\./g, '')) || 1;
        
        // Обновляем конфигурацию
        config.app_info.latest_version = newVersion;
        config.app_info.version_code = versionCode;
        config.app_info.download_url = `https://artemon4es.github.io/tv-channels-api/files/updates/${apkFileName}`;
        config.app_info.update_required = updateRequired;
        config.app_info.changelog = changelog;
        
        // Загружаем обновленную конфигурацию
        // Используем безопасное кодирование в base64 для Unicode строк
        function safeBase64Encode(str) {
            // Преобразуем строку в UTF-8 массив байтов
            const utf8Bytes = new TextEncoder().encode(str);
            // Преобразуем массив байтов в строку base64
            return btoa(String.fromCharCode.apply(null, utf8Bytes));
        }
        
        const configJson = JSON.stringify(config, null, 2);
        const base64Config = safeBase64Encode(configJson);
        
        log('📝 Конфигурация подготовлена для загрузки');
        
        const updateConfigResponse = await fetch(`https://api.github.com/repos/artemon4es/tv-channels-api/contents/api/config.json`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'X-GitHub-Api-Version': '2022-11-28',
                'Accept': 'application/vnd.github.v3+json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: `📱 Update config for APK v${newVersion}`,
                content: base64Config,
                sha: configData.sha,
                branch: 'main'
            })
        });
        
        if (!updateConfigResponse.ok) {
            const errorData = await updateConfigResponse.json().catch(() => ({ message: 'Unknown error' }));
            throw new Error(`Ошибка обновления конфигурации: ${errorData.message}`);
        }
        
        log('✅ Конфигурация успешно обновлена');
        log('🎉 APK v' + newVersion + ' успешно загружен и готов к использованию');
        log('📱 Приложения получат уведомление об обновлении в течение 30 минут');
        
        // Очищаем форму
        document.getElementById('new-version').value = '';
        document.getElementById('changelog').value = '';
        document.getElementById('update-required').checked = false;
        document.getElementById('apk-file').value = '';
        
    } catch (error) {
        log(`❌ Ошибка загрузки APK: ${error.message}`);
    }
}