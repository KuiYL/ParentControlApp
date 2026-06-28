<div align="center">
  <img src="screenshots/icon.png" alt="ParentControl" width="144" height="144" />
  <h1>ParentControl</h1>
</div>

<p align="center">
  <a href="README.md">🇬🇧 English</a> | <a href="README_RU.md">🇷🇺 Русский</a>
</p>

<p align="center">
  <strong>Track your child's activity, set limits, and ensure online safety</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-green" />
  <img src="https://img.shields.io/badge/language-Java-orange" />
  <img src="https://img.shields.io/badge/license-MIT-blue" />
  <img src="https://img.shields.io/badge/status-demo-orange" />
</p>

<p align="center">
  <a href="https://github.com/KuiYL/ParentControlApp/releases/latest">
    <img src="https://img.shields.io/badge/Download-APK-4CAF50?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

---

## ⚠️ Project Status

**Demo version** — Supabase backend is currently disabled.

### 🔴 What doesn't work:
- ❌ Authentication and registration
- ❌ Device synchronization
- ❌ Activity data retrieval
- ❌ Cloud settings storage

### 🟢 What works:
- ✅ Application interface
- ✅ Navigation between screens
- ✅ Local theme settings
- ✅ UI component visualization

For full functionality, you need to set up your own Supabase backend (see "Installation and Setup" section).

---

## ✨ Features

### 📱 Device Management
- Link child devices via unique pairing code
- List of all linked devices with online status indicator
- Instant device lock/unlock
- Unlink device from account

### 📊 Activity Analytics
- **Detailed statistics** — total usage time, number of launches, blocks count
- **Pie chart** — time distribution by apps (top-7)
- **Bar chart** — activity by day of the week
- **Top apps** — list of most used applications with time

### 📜 Event Timeline
- Chronological event feed grouped by days
- Filter by type: apps / web / blocked
- Search by app name or URL
- Paginated data loading with pull-to-refresh

### 📸 Screen Screenshots
- Screenshot gallery in grid view
- Fullscreen preview with timestamp
- Automatic loading via Glide with caching

### ⚙️ Flexible Settings
- **Daily limit** — screen time limit (30–480 minutes)
- **Night mode** — block usage during specified hours
- **Content categories** — adult content, social networks, games
- **Blacklist** — block specific apps and domains
- Quick block directly from the activity log

### 👤 Account & Security
- Email/password registration and login
- Password recovery
- User profile
- Theme switching (light / dark / system)
- App cache clearing

---

## 🛠️ Technology Stack

| Category | Technology |
|----------|------------|
| Language | Java |
| UI | Android SDK, Material Design 3, ViewPager2, Navigation Component |
| Graphics | Custom Views (Canvas), Glide |
| Network | OkHttp3, Gson |
| Backend | Supabase (Auth, REST API, Storage) |
| Architecture | Repository Pattern, MVVM-like structure |
| Animations | Shimmer (skeleton loading) |

---

## 📁 Project Structure

```text
com.parentcontrolapp.agent/
├── data/
│   ├── model/          # Data models (Device, Profile, ActivityLog, DeviceRules...)
│   ├── remote/         # SupabaseApi — Supabase REST API integration
│   └── repository/     # Repositories (AuthRepository, DeviceRepository)
├── ui/
│   ├── auth/           # Auth screens (Login, Register, ForgotPassword)
│   ├── main/           # Main screens (Devices, DeviceDetails, Profile, Settings)
│   ├── tabs/           # Device tabs (Activity, Timeline, Screenshots, Settings)
│   ├── adapters/       # RecyclerView adapters and list models
│   └── custom/         # Custom Views (PieChartView, BarChartView)
└── utils/              # Utilities (TimeUtils)
```

---

## 🚀 Installation and Setup

### Requirements
- Android Studio (Arctic Fox or later)
- JDK 11+
- Min SDK 21 (Android 5.0)
- [Supabase](https://supabase.com) account

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/KuiYL/ParentControlApp.git
   ```

2. **Set up Supabase:**
   - Create a project at [supabase.com](https://supabase.com)
   - Create tables: `profiles`, `devices`, `device_rules`, `activity_logs`
   - Create storage bucket `screenshots`
   - Create RPC function `claim_device` for device pairing

3. **Specify your Supabase keys** in `SupabaseApi.java`:
   ```java
   private static final String BASE_URL = "https://YOUR_PROJECT.supabase.co";
   private static final String ANON_KEY = "YOUR_ANON_KEY";
   ```

4. **Open the project in Android Studio** and run on a device or emulator.

---

## 📐 Architecture

The project is built following the separation of concerns principle:

```text
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

- **UI** — fragments and adapters responsible for display
- **Repository** — mediator between UI and network, manages cache
- **DataCacheManager** — local cache based on `SharedPreferences` + `Gson`
- **SupabaseApi** — singleton for all HTTP requests to the backend

---

## 🔑 Key Supabase Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/v1/signup` | Registration |
| POST | `/auth/v1/token` | Login |
| GET | `/rest/v1/devices` | List of devices |
| GET | `/rest/v1/activity_logs` | Activity logs (with pagination) |
| PATCH | `/rest/v1/device_rules` | Update settings |
| POST | `/rest/v1/rpc/claim_device` | Link device by code |

---

## 📄 License

This project is distributed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## 👩‍💻 Author

Created with ❤️ for caring parents.

If you have any questions or suggestions — create an [Issue](https://github.com/KuiYL/ParentControlApp/issues) or contact me!
