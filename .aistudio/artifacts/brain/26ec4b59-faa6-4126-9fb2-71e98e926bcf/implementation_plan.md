# Dual Cloud Sync & Persistent Database Backup Architecture

Fix and enhance the **SH SKIN TOOL** cloud persistence system so that all skins, bundles, categories, folders, screens, package files, and admin settings added in the Admin Panel are saved permanently to a high-availability remote cloud database and persistent device backup, guaranteeing **100% skin retention and zero data loss** when the app is uninstalled and reinstalled or downloaded on a new device.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Based on your responses, we are implementing a **Dual Cloud Server Sync + Persistent Backup Architecture**. This ensures that even if a user or admin uninstalls the app and re-downloads it, all added skins and content remain fully intact and immediately visible.

- **Confirmed Decision 1**: **Dual Cloud Engine + Persistent Storage Backup**. Skins added from the Admin Panel are saved simultaneously to a high-availability remote cloud JSON database and a local hidden persistent device backup file (`.sh_skintool_cloud_db.json`).
- **Confirmed Decision 2**: **Persistent Asset & Direct URL Mapping**. Image cover URLs, unity3d file names, PNG icon references, and configuration parameters are stored as persistent references in the cloud payload so that reinstalled apps restore icons, descriptions, and file downloads seamlessly.

---

## 1. Overview & Core Concept

- **What It Does**: Establishes a fail-proof cloud database synchronization and backup pipeline for **SH SKIN TOOL**.
- **Target Audience**: Admins adding or managing Free Fire & Free Fire Max skins/bundles, and Users installing skins across multiple devices.
- **Key Value**: Guarantees that uninstalling, reinstalling, clearing app cache, or changing phones **never** deletes admin-created skins or user configuration.

---

## 2. User Experience & Visual Design

- **Admin Publishing Flow**: When the Admin taps **SAVE & STORE PERMANENTLY** on any skin, file, or setting, the app updates Room DB, posts the updated `CloudDatabasePayload` to the cloud server, and updates the device backup file. A green success badge displays *"Synced to Remote Cloud Database"*.
- **User Panel Auto-Restore**: When a user launches the app after a fresh install or update, the app displays a smooth background sync indicator while pulling all published skins from the cloud database, immediately restoring all skins on the Home Screen and Category pages.
- **Visual Identity**: Maintained exact dark gaming cyber aesthetic with Neon Green accents (`#00FF87`), Mint Glow (`#00F5D4`), Cyber Surface (`#0A121D`), and high-contrast typography.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual-Source Cloud Restoration Strategy**
  - *Chosen Approach*: On app startup, `syncWithCloud()` attempts to fetch remote cloud data first. If the network is unavailable, it automatically restores from the persistent local device backup file.
  - *Why*: Eliminates data loss scenarios during app re-installation or temporary network outages.

- **Decision 2: Preservation of Existing UI & Features**
  - *Chosen Approach*: Maintain all existing Composables, Navigation, Shizuku integrations, and Admin/User screens without modifying layout or visual design.
  - *Why*: Strict adherence to the NO-OTHER-CHANGES policy while fixing the underlying data persistence bug.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        SH SKIN TOOL DATA FLOW                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   [ Admin Panel ]  ───►  [ Room Database ]  ───► [ CloudSyncEngine ]    │
│   (Add/Edit Skins)          (Local DAO)            │                    │
│                                                    ├───► Remote Cloud   │
│                                                    │     Database API   │
│                                                    │                    │
│                                                    └───► Persistent     │
│                                                          Storage Backup │
│                                                          (.json File)   │
│                                                                         │
│   [ User Panel ]   ◄───  [ syncWithCloud() ] ◄───────────┘             │
│   (Fresh Install)        (Restores All Skins)                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Component Breakdown
1. **`CloudSyncEngine.kt`**:
   - Enhances HTTP client timeout, headers, and failover endpoints.
   - Provides methods `fetchCloudPayload()` and `saveCloudPayload()`.
   - Manages fallback read/write to `/sdcard/Download/.sh_skintool_cloud_db.json` or private external storage directory when offline.

2. **`SkinToolRepository.kt`**:
   - Updates `syncWithCloud()` logic:
     - Downloads remote cloud payload.
     - Merges packages, categories, folders, screens, package files, and app settings into Room DAOs.
     - If local DB has new items not present in remote payload, pushes local state to remote cloud server immediately.
   - Hooks `pushLocalToCloudInternal()` into all mutation methods: `savePackage()`, `deletePackage()`, `saveCategory()`, `deleteCategory()`, `saveFolder()`, `deleteFolder()`, `saveScreen()`, `deleteScreen()`, `savePackageFile()`, `deletePackageFile()`, `updateSecuritySettings()`.

3. **`SeedData.kt` & Initialization**:
   - Ensures seed data only fills empty initial slots if both Cloud Database and Device Backup file contain zero packages, preventing seed data from overwriting user-created skins.
