# 🔧 План рефакторинга и доработок

## 📋 Краткие исправления (30 минут - 2 часа каждое)

### 1. ⏰ Изменение частоты проверки каналов

**Файл**: `index.html`
**Строки**: 2303-2316

**Текущий код**:
```javascript
// Проверяем в 08:00 и 20:00 UTC (±1 минута)
if ((hour === 8 || hour === 20) && minute <= 1) {
```

**Новый код**:
```javascript
// Проверяем каждые 30 минут
if (minute === 0 || minute === 30) {
```

**Дополнительно изменить строку 275**:
```html
⏰ Автопроверка: каждые 30 минут
```

### 2. 📝 Уменьшение логов GitHub Actions

**Файл**: `.github/workflows/update-channels-from-url.yml`
**Строки**: 120-124

**Удалить этот блок**:
```javascript
console.log(`\n📋 Каналы в источнике (#EXTINF строки):`);
sourceChannels.forEach((ch, idx) => {
    console.log(`   ${idx + 1}. ${ch.extinfLine}`);
});
```

**Заменить на**:
```javascript
console.log(`\n📋 Загружено ${sourceChannels.length} каналов из источника`);
```

### 3. 🔧 Исправление отображения пустых плейсхолдеров

**Файл**: `Android TV/app/src/main/java/com/example/androidtv/ChannelLogoManager.kt`

**Проблема**: Нужно улучшить fallback логику

**В методе `loadChannelLogo`** добавить проверку:
```kotlin
private fun getLogoFileName(channelName: String): String? {
    // Список всех доступных логотипов
    val availableLogos = setOf(
        "rossiya-1", "ntv", "sts", "tnt", "tv-3", "tvzvezda", 
        "ren_tv", "tvc", "5-kanal", "domashniy", "friday", 
        "mir", "otr", "rbc", "spas", "karusel", "muz", 
        "match_tv", "rossiya-24", "kultura", "ortl"
    )
    
    val normalizedName = normalizeChannelName(channelName)
    
    // Проверяем есть ли логотип для этого канала
    return if (availableLogos.contains(normalizedName)) {
        "$normalizedName.png"
    } else {
        null // Не показывать пустой плейсхолдер
    }
}
```

---

## 🏗️ Средние доработки (полдня - день каждая)

### 4. 📁 Система загрузки логотипов для новых каналов

**Новая секция в админ-панели** (`index.html`):

```html
<!-- Управление логотипами каналов -->
<div class="section">
    <h3>🖼️ Управление логотипами каналов</h3>
    
    <div style="margin-bottom: 20px;">
        <h4>📋 Текущие каналы без логотипов</h4>
        <div id="channels-without-logos"></div>
    </div>
    
    <div style="margin-bottom: 20px;">
        <h4>📤 Загрузить логотип для канала</h4>
        <select id="channel-for-logo" style="width: 300px; margin-right: 10px;">
            <option value="">Выберите канал...</option>
        </select>
        <input type="file" id="logo-file" accept="image/png,image/jpg,image/jpeg">
        <button class="btn btn-success" onclick="uploadChannelLogo()">📤 Загрузить</button>
    </div>
    
    <div>
        <h4>🎨 Текущие логотипы</h4>
        <div id="current-logos" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 15px;"></div>
    </div>
</div>
```

**JavaScript функции**:
```javascript
// Загрузка списка каналов без логотипов
async function loadChannelsWithoutLogos() {
    const channels = await getCurrentChannelsFromGitHub();
    const channelList = parseM3UFile(channels);
    
    // Получить список существующих логотипов
    const logos = await getExistingLogos();
    
    // Найти каналы без логотипов
    const channelsWithoutLogos = channelList.filter(channel => {
        const logoName = normalizeChannelName(channel.name) + '.png';
        return !logos.includes(logoName);
    });
    
    displayChannelsWithoutLogos(channelsWithoutLogos);
}

// Загрузка логотипа для канала
async function uploadChannelLogo() {
    const channelSelect = document.getElementById('channel-for-logo');
    const fileInput = document.getElementById('logo-file');
    
    if (!channelSelect.value || !fileInput.files[0]) {
        alert('Выберите канал и файл логотипа');
        return;
    }
    
    const file = fileInput.files[0];
    const channelName = channelSelect.value;
    const fileName = normalizeChannelName(channelName) + '.png';
    
    // Загрузить в GitHub
    await uploadFileToGitHub(`files/channel-logos/${fileName}`, file);
    
    log(`✅ Логотип для канала "${channelName}" загружен`);
    loadChannelsWithoutLogos();
}
```

### 5. ✏️ Система realtime редактирования

**Новая секция в админ-панели**:

```html
<div class="section">
    <h3>✏️ Редактор каналов (Realtime)</h3>
    
    <div style="margin-bottom: 15px;">
        <button class="btn" onclick="loadForEditing()" style="background: #17a2b8; color: white;">📝 Загрузить для редактирования</button>
        <button class="btn btn-success" onclick="saveChanges()" id="save-btn" disabled>💾 Сохранить изменения</button>
        <button class="btn" onclick="discardChanges()" style="background: #6c757d; color: white;">↶ Отменить изменения</button>
    </div>
    
    <div style="margin-bottom: 15px;">
        <div id="edit-status" style="padding: 10px; background: #f8f9fa; border-radius: 5px; font-size: 14px;">
            Загрузите данные для редактирования
        </div>
    </div>
    
    <textarea id="realtime-editor" placeholder="Данные каналов будут загружены здесь..." style="height: 400px;"></textarea>
    
    <div style="margin-top: 15px; font-size: 12px; color: #666;">
        💡 Изменения сохраняются в локальной памяти. Нажмите "Сохранить изменения" для применения.
    </div>
</div>
```

**JavaScript функции**:
```javascript
let originalContent = '';
let hasChanges = false;

// Загрузка для редактирования
async function loadForEditing() {
    const content = await getCurrentChannelsFromGitHub();
    originalContent = content;
    
    document.getElementById('realtime-editor').value = content;
    document.getElementById('edit-status').innerHTML = '✅ Данные загружены. Можно редактировать.';
    
    // Отслеживание изменений
    document.getElementById('realtime-editor').addEventListener('input', () => {
        hasChanges = document.getElementById('realtime-editor').value !== originalContent;
        document.getElementById('save-btn').disabled = !hasChanges;
        
        if (hasChanges) {
            document.getElementById('edit-status').innerHTML = '⚠️ Есть несохраненные изменения';
            document.getElementById('edit-status').style.background = '#fff3cd';
        } else {
            document.getElementById('edit-status').innerHTML = '✅ Все изменения сохранены';
            document.getElementById('edit-status').style.background = '#d4edda';
        }
    });
}

// Сохранение изменений
async function saveChanges() {
    const content = document.getElementById('realtime-editor').value;
    
    if (!content.includes('#EXTM3U')) {
        alert('❌ Неверный формат данных. Должен содержать #EXTM3U');
        return;
    }
    
    try {
        // Сохранить через GitHub Actions
        await updateChannelsThroughActions(content);
        
        originalContent = content;
        hasChanges = false;
        document.getElementById('save-btn').disabled = true;
        document.getElementById('edit-status').innerHTML = '✅ Изменения сохранены';
        document.getElementById('edit-status').style.background = '#d4edda';
        
        log('✅ Изменения в каналах сохранены');
        
    } catch (error) {
        alert(`❌ Ошибка сохранения: ${error.message}`);
    }
}

// Отмена изменений
function discardChanges() {
    if (hasChanges && !confirm('Отменить все несохраненные изменения?')) {
        return;
    }
    
    document.getElementById('realtime-editor').value = originalContent;
    hasChanges = false;
    document.getElementById('save-btn').disabled = true;
    document.getElementById('edit-status').innerHTML = '↶ Изменения отменены';
    document.getElementById('edit-status').style.background = '#f8f9fa';
}
```

---

## 🏢 Крупные доработки (2-4 дня каждая)

### 6. 🖥️ Система учета устройств

#### Шаг 1: Android приложение

**Новый файл**: `Android TV/app/src/main/java/com/example/androidtv/DeviceManager.kt`

```kotlin
package com.example.androidtv

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class DeviceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceManager"
        private const val DEVICE_API_URL = "https://artemon4es.github.io/tv-channels-api/api/devices"
        private const val PREFS_NAME = "device_config"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_PING = "last_ping"
        private const val PING_INTERVAL = 5 * 60 * 1000L // 5 минут
    }
    
    private val client = OkHttpClient()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    data class DeviceInfo(
        val deviceId: String,
        val model: String,
        val manufacturer: String,
        val androidVersion: String,
        val appVersion: String,
        val screenResolution: String,
        val lastSeen: Long,
        val status: String = "active"
    )
    
    /**
     * Получает уникальный ID устройства
     */
    private fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            // Генерируем новый ID на основе Android ID + случайных данных
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            deviceId = "atv_${androidId}_${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        
        return deviceId
    }
    
    /**
     * Получает информацию об устройстве
     */
    private fun getDeviceInfo(): DeviceInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val displayMetrics = context.resources.displayMetrics
        
        return DeviceInfo(
            deviceId = getDeviceId(),
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            appVersion = packageInfo.versionName ?: "1.0",
            screenResolution = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}",
            lastSeen = System.currentTimeMillis()
        )
    }
    
    /**
     * Регистрирует устройство в системе
     */
    suspend fun registerDevice(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deviceInfo = getDeviceInfo()
                val json = JSONObject().apply {
                    put("device_id", deviceInfo.deviceId)
                    put("model", deviceInfo.model)
                    put("manufacturer", deviceInfo.manufacturer)
                    put("android_version", deviceInfo.androidVersion)
                    put("app_version", deviceInfo.appVersion)
                    put("screen_resolution", deviceInfo.screenResolution)
                    put("last_seen", deviceInfo.lastSeen)
                    put("status", deviceInfo.status)
                    put("action", "register")
                }
                
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$DEVICE_API_URL/register")
                    .post(body)
                    .build()
                
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                
                if (success) {
                    Log.i(TAG, "Устройство зарегистрировано: ${deviceInfo.deviceId}")
                } else {
                    Log.w(TAG, "Ошибка регистрации устройства: ${response.code}")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка регистрации устройства: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Отправляет ping для подтверждения активности
     */
    suspend fun pingDevice(): Boolean {
        val lastPing = prefs.getLong(KEY_LAST_PING, 0)
        val now = System.currentTimeMillis()
        
        // Отправляем ping не чаще чем раз в 5 минут
        if (now - lastPing < PING_INTERVAL) {
            return true
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val deviceInfo = getDeviceInfo()
                val json = JSONObject().apply {
                    put("device_id", deviceInfo.deviceId)
                    put("last_seen", deviceInfo.lastSeen)
                    put("action", "ping")
                }
                
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$DEVICE_API_URL/ping")
                    .post(body)
                    .build()
                
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                
                if (success) {
                    prefs.edit().putLong(KEY_LAST_PING, now).apply()
                    Log.d(TAG, "Device ping успешен")
                } else {
                    Log.w(TAG, "Ошибка ping устройства: ${response.code}")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка ping устройства: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Проверяет статус сервиса для этого устройства
     */
    suspend fun checkDeviceServiceStatus(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                val request = Request.Builder()
                    .url("$DEVICE_API_URL/status?device_id=$deviceId")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "{}")
                    json.optBoolean("service_enabled", true)
                } else {
                    true // По умолчанию разрешаем сервис
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка проверки статуса устройства: ${e.message}")
                true // По умолчанию разрешаем сервис
            }
        }
    }
    
    /**
     * Получает ID устройства
     */
    fun getCurrentDeviceId(): String {
        return getDeviceId()
    }
}
```

**Интеграция в MainActivity.kt**:
```kotlin
// Добавить в MainActivity
private lateinit var deviceManager: DeviceManager

override fun onCreate(savedInstanceState: Bundle?) {
    // ... существующий код ...
    
    deviceManager = DeviceManager(this)
    
    // Регистрируем устройство при запуске
    lifecycleScope.launch {
        deviceManager.registerDevice()
    }
}

// Добавить в периодическую проверку
private suspend fun periodicConfigCheck() {
    try {
        // Отправляем ping устройства
        deviceManager.pingDevice()
        
        // Проверяем статус сервиса для этого устройства
        val deviceServiceEnabled = deviceManager.checkDeviceServiceStatus()
        if (!deviceServiceEnabled) {
            showServiceUnavailableDialog("Сервис отключен для этого устройства.")
            return
        }
        
        // ... остальной код проверки ...
    } catch (e: Exception) {
        Log.e(TAG, "Ошибка периодической проверки: ${e.message}")
    }
}
```

#### Шаг 2: API структура

**Новые файлы**:

`api/devices/register.json`:
```json
{
  "status": "success",
  "message": "Device registered"
}
```

`api/devices/ping.json`:
```json
{
  "status": "success",
  "message": "Ping received"
}
```

`api/devices/list.json`:
```json
{
  "devices": [
    {
      "device_id": "atv_android123_abc12345",
      "model": "Android TV Box",
      "manufacturer": "Generic",
      "android_version": "9.0",
      "app_version": "1.0",
      "screen_resolution": "1920x1080",
      "last_seen": "2025-01-16T12:00:00Z",
      "status": "active",
      "service_enabled": true,
      "registered_at": "2025-01-15T10:30:00Z"
    }
  ],
  "total_devices": 1,
  "active_devices": 1
}
```

#### Шаг 3: Админ-панель

**Новая вкладка в index.html**:
```html
<button class="tab" onclick="showTab('devices')">🖥️ Устройства</button>

<!-- Вкладка: Управление устройствами -->
<div id="devices" class="tab-content">
    <h2>🖥️ Управление устройствами</h2>
    
    <div style="margin-bottom: 20px;">
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
            <div style="background: #d4edda; padding: 15px; border-radius: 8px; text-align: center;">
                <h3 style="margin: 0; color: #155724;">Всего устройств</h3>
                <div id="total-devices" style="font-size: 24px; font-weight: bold; color: #155724;">-</div>
            </div>
            <div style="background: #cce5ff; padding: 15px; border-radius: 8px; text-align: center;">
                <h3 style="margin: 0; color: #004085;">Активных</h3>
                <div id="active-devices" style="font-size: 24px; font-weight: bold; color: #004085;">-</div>
            </div>
            <div style="background: #fff3cd; padding: 15px; border-radius: 8px; text-align: center;">
                <h3 style="margin: 0; color: #856404;">Отключенных</h3>
                <div id="disabled-devices" style="font-size: 24px; font-weight: bold; color: #856404;">-</div>
            </div>
        </div>
    </div>
    
    <div style="margin-bottom: 15px;">
        <button class="btn" onclick="loadDevicesList()" style="background: #17a2b8; color: white;">🔄 Обновить список</button>
        <button class="btn btn-success" onclick="enableAllDevices()">✅ Включить все</button>
        <button class="btn btn-danger" onclick="disableAllDevices()">❌ Отключить все</button>
    </div>
    
    <div id="devices-list">
        <div style="text-align: center; padding: 40px; color: #666;">
            Нажмите "Обновить список" для загрузки устройств
        </div>
    </div>
</div>
```

**JavaScript функции для управления устройствами**:
```javascript
// Загрузка списка устройств
async function loadDevicesList() {
    try {
        showProgress(20, 'Загрузка списка устройств...');
        
        const response = await fetch(`https://artemon4es.github.io/tv-channels-api/api/devices/list.json?t=${Date.now()}`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        
        // Обновляем статистику
        document.getElementById('total-devices').textContent = data.total_devices || 0;
        document.getElementById('active-devices').textContent = data.active_devices || 0;
        document.getElementById('disabled-devices').textContent = (data.total_devices - data.active_devices) || 0;
        
        // Отображаем список устройств
        displayDevicesList(data.devices || []);
        
        showProgress(100, 'Список устройств загружен');
        setTimeout(() => hideProgress(), 2000);
        
    } catch (error) {
        log(`❌ Ошибка загрузки устройств: ${error.message}`);
        showProgress(0, `Ошибка: ${error.message}`, true);
        setTimeout(() => hideProgress(), 5000);
    }
}

// Отображение списка устройств
function displayDevicesList(devices) {
    const container = document.getElementById('devices-list');
    
    if (devices.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 40px; color: #666;">
                Устройства не найдены
            </div>
        `;
        return;
    }
    
    let html = `
        <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <thead style="background: #f8f9fa;">
                    <tr>
                        <th style="padding: 12px; text-align: left; border-bottom: 1px solid #dee2e6;">Устройство</th>
                        <th style="padding: 12px; text-align: left; border-bottom: 1px solid #dee2e6;">Модель</th>
                        <th style="padding: 12px; text-align: left; border-bottom: 1px solid #dee2e6;">Последняя активность</th>
                        <th style="padding: 12px; text-align: left; border-bottom: 1px solid #dee2e6;">Статус</th>
                        <th style="padding: 12px; text-align: center; border-bottom: 1px solid #dee2e6;">Управление</th>
                    </tr>
                </thead>
                <tbody>
    `;
    
    devices.forEach(device => {
        const lastSeen = new Date(device.last_seen).toLocaleString();
        const isOnline = (Date.now() - new Date(device.last_seen).getTime()) < 10 * 60 * 1000; // 10 минут
        const statusColor = device.service_enabled ? (isOnline ? '#28a745' : '#ffc107') : '#dc3545';
        const statusText = device.service_enabled ? (isOnline ? 'Активно' : 'Неактивно') : 'Отключено';
        
        html += `
            <tr style="border-bottom: 1px solid #f8f9fa;">
                <td style="padding: 12px;">
                    <div style="font-weight: bold;">${device.device_id}</div>
                    <div style="font-size: 12px; color: #666;">Android ${device.android_version} • App v${device.app_version}</div>
                    <div style="font-size: 12px; color: #666;">${device.screen_resolution}</div>
                </td>
                <td style="padding: 12px;">
                    <div>${device.manufacturer}</div>
                    <div style="font-size: 12px; color: #666;">${device.model}</div>
                </td>
                <td style="padding: 12px;">
                    <div>${lastSeen}</div>
                </td>
                <td style="padding: 12px;">
                    <span style="padding: 4px 8px; border-radius: 12px; font-size: 12px; background: ${statusColor}; color: white;">
                        ${statusText}
                    </span>
                </td>
                <td style="padding: 12px; text-align: center;">
                    <button class="btn ${device.service_enabled ? 'btn-danger' : 'btn-success'}" 
                            onclick="toggleDeviceService('${device.device_id}', ${device.service_enabled})"
                            style="font-size: 12px; padding: 4px 8px;">
                        ${device.service_enabled ? '❌ Отключить' : '✅ Включить'}
                    </button>
                    <button class="btn" onclick="removeDevice('${device.device_id}')" 
                            style="font-size: 12px; padding: 4px 8px; background: #6c757d; color: white; margin-left: 5px;">
                        🗑️ Удалить
                    </button>
                </td>
            </tr>
        `;
    });
    
    html += `
                </tbody>
            </table>
        </div>
    `;
    
    container.innerHTML = html;
}

// Включение/отключение сервиса для устройства
async function toggleDeviceService(deviceId, currentStatus) {
    const newStatus = !currentStatus;
    const action = newStatus ? 'включить' : 'отключить';
    
    if (!confirm(`${action.charAt(0).toUpperCase() + action.slice(1)} сервис для устройства ${deviceId}?`)) {
        return;
    }
    
    try {
        // В реальной реализации это будет отправка в GitHub API
        // для обновления файла со статусами устройств
        
        log(`${newStatus ? '✅' : '❌'} Сервис ${newStatus ? 'включен' : 'отключен'} для устройства ${deviceId}`);
        
        // Обновляем список
        setTimeout(() => loadDevicesList(), 1000);
        
    } catch (error) {
        log(`❌ Ошибка изменения статуса устройства: ${error.message}`);
    }
}

// Удаление устройства
async function removeDevice(deviceId) {
    if (!confirm(`Удалить устройство ${deviceId}? Это действие нельзя отменить.`)) {
        return;
    }
    
    try {
        // В реальной реализации это будет удаление из списка устройств
        log(`🗑️ Устройство ${deviceId} удалено`);
        
        // Обновляем список
        setTimeout(() => loadDevicesList(), 1000);
        
    } catch (error) {
        log(`❌ Ошибка удаления устройства: ${error.message}`);
    }
}

// Включение всех устройств
async function enableAllDevices() {
    if (!confirm('Включить сервис для всех устройств?')) {
        return;
    }
    
    log('✅ Сервис включен для всех устройств');
    setTimeout(() => loadDevicesList(), 1000);
}

// Отключение всех устройств
async function disableAllDevices() {
    if (!confirm('Отключить сервис для всех устройств?')) {
        return;
    }
    
    log('❌ Сервис отключен для всех устройств');
    setTimeout(() => loadDevicesList(), 1000);
}
```

---

## 📊 API инфраструктура для будущего

### Структура API v2

```
api/
├── v1/                          # Текущая версия
│   ├── config.json
│   └── ...
├── v2/                          # Новая версия
│   ├── config/
│   │   ├── app.json            # Конфигурация приложения
│   │   ├── channels.json       # Конфигурация каналов
│   │   └── security.json       # Настройки безопасности
│   ├── devices/
│   │   ├── register.json       # Регистрация устройств
│   │   ├── list.json          # Список устройств
│   │   ├── status.json        # Статусы устройств
│   │   └── analytics.json     # Аналитика по устройствам
│   ├── channels/
│   │   ├── list.m3u8          # Список каналов
│   │   ├── updates.json       # История обновлений
│   │   └── logos/             # Логотипы каналов
│   ├── admin/
│   │   ├── actions.json       # Журнал действий администратора
│   │   ├── logs.json         # Системные логи
│   │   └── stats.json        # Статистика системы
│   └── analytics/
│       ├── usage.json         # Статистика использования
│       ├── errors.json        # Логи ошибок
│       └── performance.json   # Метрики производительности
```

### Примеры новых API endpoints

**`api/v2/analytics/usage.json`**:
```json
{
  "summary": {
    "total_devices": 157,
    "active_devices": 142,
    "total_channels": 22,
    "most_watched_channels": [
      {"name": "Первый канал HD", "views": 1240},
      {"name": "Россия 1 HD", "views": 987},
      {"name": "НТВ HD", "views": 654}
    ]
  },
  "daily_stats": [
    {
      "date": "2025-01-16",
      "active_devices": 142,
      "total_watch_time": 180240,
      "channel_switches": 3420
    }
  ]
}
```

**`api/v2/admin/actions.json`**:
```json
{
  "actions": [
    {
      "timestamp": "2025-01-16T12:30:00Z",
      "action": "update_channels",
      "user": "admin",
      "details": "Updated 5 channel URLs",
      "affected_channels": ["НТВ HD", "СТС HD"]
    },
    {
      "timestamp": "2025-01-16T11:15:00Z", 
      "action": "disable_device",
      "user": "admin",
      "details": "Disabled service for device atv_123",
      "device_id": "atv_android123_abc12345"
    }
  ]
}
```

---

## ⏱️ Временные оценки

| Задача | Сложность | Время |
|--------|-----------|-------|
| Изменение частоты проверки | Легкая | 30 мин |
| Уменьшение логов | Легкая | 30 мин |
| Исправление плейсхолдеров | Средняя | 2-3 часа |
| Система загрузки логотипов | Средняя | 1 день |
| Realtime редактирование | Средняя | 1 день |
| Система учета устройств | Сложная | 3-4 дня |
| API инфраструктура | Сложная | 2-3 дня |

**Общее время**: 8-12 дней полной работы

---

## 🎯 Приоритеты

### Высокий приоритет (сделать в первую очередь):
1. ⏰ Изменение частоты проверки каналов
2. 📝 Уменьшение логов GitHub Actions  
3. 🖼️ Исправление плейсхолдеров логотипов

### Средний приоритет:
4. 📁 Система загрузки логотипов для новых каналов
5. ✏️ Realtime редактирование

### Низкий приоритет (по желанию):
6. 🖥️ Система учета устройств
7. 📊 API инфраструктура для будущего

Это позволит быстро исправить основные проблемы и постепенно добавлять новые функции.