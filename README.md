<div align="center">
  <img src="screenshots/icon.png" alt="ParentControl" width="144" height="144" />
  <h1>ParentControl</h1>
</div>

<p align="center">
  <strong>Отслеживайте активность ребёнка, устанавливайте лимиты и обеспечивайте безопасность</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-green" />
  <img src="https://img.shields.io/badge/language-Java-orange" />
  <img src="https://img.shields.io/badge/license-MIT-blue" />
</p>

---

## ✨ Возможности

### 📱 Управление устройствами
- Привязка устройств ребёнка по уникальному коду
- Список всех привязанных устройств с индикатором онлайн-статуса
- Мгновенная блокировка/разблокировка устройства
- Отвязка устройства от аккаунта

### 📊 Аналитика активности
- **Детальная статистика** — общее время использования, количество запусков, число блокировок
- **Круговая диаграмма** — распределение времени по приложениям (топ-7)
- **Столбчатый график** — активность по дням недели
- **Топ приложений** — список самых используемых программ с временем

### 📜 Журнал событий
- Хронологическая лента событий с группировкой по дням
- Фильтрация по типу: приложения / веб / заблокированные
- Поиск по названию приложения или URL
- Подгрузка данных с пагинацией и pull-to-refresh

### 📸 Скриншоты экрана
- Галерея скриншотов в виде сетки
- Просмотр в полноэкранном режиме с указанием времени
- Автоматическая загрузка через Glide с кэшированием

### ⚙️ Гибкие настройки
- **Дневной лимит** экранного времени (30–480 минут)
- **Ночной режим** — запрет использования в заданные часы
- **Категории блокировки** — взрослый контент, соцсети, игры
- **Чёрный список** — блокировка конкретных приложений и доменов
- Быстрая блокировка прямо из журнала активности

### 👤 Аккаунт и безопасность
- Регистрация и вход по email/паролю
- Восстановление пароля
- Профиль пользователя
- Переключение темы (светлая / тёмная / системная)
- Очистка кэша приложения

---

## 🛠️ Технологический стек

| Категория | Технология |
|-----------|-----------|
| Язык | Java |
| UI | Android SDK, Material Design 3, ViewPager2, Navigation Component |
| Графика | Кастомные View (Canvas), Glide |
| Сеть | OkHttp3, Gson |
| Бэкенд | Supabase (Auth, REST API, Storage) |
| Архитектура | Repository Pattern, MVVM-подобная структура |
| Анимации | Shimmer (скелетон-загрузка) |

---

## 📁 Структура проекта

```
com.parentcontrolapp.agent/
├── data/
│   ├── model/          # Модели данных (Device, Profile, ActivityLog, DeviceRules...)
│   ├── remote/         # SupabaseApi — работа с REST API Supabase
│   └── repository/     # Репозитории (AuthRepository, DeviceRepository)
├── ui/
│   ├── auth/           # Экраны авторизации (Login, Register, ForgotPassword)
│   ├── main/           # Основные экраны (Devices, DeviceDetails, Profile, Settings)
│   ├── tabs/           # Вкладки устройства (Activity, Timeline, Screenshots, Settings)
│   ├── adapters/       # RecyclerView адаптеры и модели для списков
│   └── custom/         # Кастомные View (PieChartView, BarChartView)
└── utils/              # Утилиты (TimeUtils)
```

---

## 🚀 Установка и запуск

### Требования
- Android Studio (Arctic Fox или новее)
- JDK 11+
- Min SDK 21 (Android 5.0)
- Аккаунт [Supabase](https://supabase.com)

### Шаги

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/KuiYL/ParentControlApp.git
   ```

2. **Настройте Supabase:**
    - Создайте проект на [supabase.com](https://supabase.com)
    - Создайте таблицы: `profiles`, `devices`, `device_rules`, `activity_logs`
    - Создайте storage bucket `screenshots`
    - Создайте RPC функцию `claim_device` для привязки устройств

3. **Укажите свои ключи Supabase** в файле `SupabaseApi.java`:
   ```java
   private static final String BASE_URL = "https://YOUR_PROJECT.supabase.co";
   private static final String ANON_KEY = "YOUR_ANON_KEY";
   ```

4. **Откройте проект в Android Studio** и запустите на устройстве или эмуляторе.

---

## 📐 Архитектура

Проект построен по принципу разделения ответственности:

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  UI Layer    │────▶│  Repository  │────▶│  Supabase    │
│ (Fragments)  │     │   Layer      │     │   API        │
└──────────────┘     └──────┬───────┘     └──────────────┘
                            │
                     ┌──────▼───────┐
                     │  DataCache   │
                     │  Manager     │
                     └──────────────┘
```

- **UI** — фрагменты и адаптеры, отвечающие за отображение
- **Repository** — посредник между UI и сетью, управляет кэшем
- **DataCacheManager** — локальный кэш на базе `SharedPreferences` + `Gson`
- **SupabaseApi** — singleton для всех HTTP-запросов к бэкенду

---

## 🔑 Ключевые эндпоинты Supabase

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| POST | `/auth/v1/signup` | Регистрация |
| POST | `/auth/v1/token` | Вход |
| GET | `/rest/v1/devices` | Список устройств |
| GET | `/rest/v1/activity_logs` | Логи активности (с пагинацией) |
| PATCH | `/rest/v1/device_rules` | Обновление настроек |
| POST | `/rest/v1/rpc/claim_device` | Привязка устройства по коду |


## 📄 Лицензия

Этот проект распространяется под лицензией MIT. См. файл [LICENSE](LICENSE) для подробностей.

---

## 👩‍💻 Автор

Создано с ❤️ для заботливых родителей.

Если у вас есть вопросы или предложения — создайте [Issue](https://github.com/KuiYL/ParentControlApp/issues) или напишите мне!
