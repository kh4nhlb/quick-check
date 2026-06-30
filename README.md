# 🔍 PenTracker — Burp Suite Pentest Checklist Extension

A Burp Suite extension that provides structured vulnerability checklists directly in the HTTP request context menu. Track testing coverage across all endpoints in a pentest engagement.

## ✨ Features

- 🖱️ **Right-click any request** (Proxy, Repeater, HTTP History) → **PenTracker Checklist** → checklist opens instantly
- 📋 **27 checklist categories** loaded automatically, sections collapsed by default — sorted from most common to least:
  - Always: Auth & Session, Authorization & Access Control, Injection, Sensitive Data Exposure, Error Handling, Rate Limiting & DoS
  - Common: Register/Login, CRUD API, Business Logic, Client-Side, OTP/2FA/MFA, Open Redirect
  - Feature-specific: File Upload, SSRF, Payment Logic, Email Function, WebSocket, XXE
  - Less common: Path Traversal, Cache, Host Header, eKYC
  - Rare/tech-specific: LFI/RFI, Deserialization, HTTP Smuggling, Framework Specific
  - One-time: One-Time Testcases
- 💾 **Persistent progress** — state auto-saved to `pentracker-progress.json` (debounced 500ms)
- 🔀 **Endpoint normalization** — numeric/UUID path segments replaced with `{id}`, query params normalized to `param={param}`, so `/users/42` and `/users/99` share the same entry
- ✅ **Done / Un-done** — mark an endpoint fully tested; status reflected in coverage table
- 📊 **Coverage table** in PenTracker tab — lists all tracked endpoints (Method / Host / Endpoint / Status / Done / Total / Last Updated), sortable by column
- 📂 **External checklist directory** — import any folder of JSON checklists from Settings; unload to revert to built-in defaults

## 🛠️ Requirements

- Java 17+
- Maven 3.6+
- Burp Suite Pro / Community (Montoya API 2022+)

## 🔨 Build

```bash
mvn clean package -q
```

Output: `target/pentracker-1.0.0-jar-with-dependencies.jar`

## 📦 Install in Burp

1. **Extensions → Add → Select file** → pick the fat JAR
2. A **PenTracker** tab appears in the Burp Suite toolbar

## 🚀 Usage

### First-time setup

1. Open the **PenTracker** tab in Burp
2. Set the **Project Directory** (the folder where progress will be saved)
3. Click **Save & Reload** — a `pentracker-progress.json` file will be created in that directory

### Running a checklist

1. Right-click any HTTP request → **PenTracker Checklist**
2. The checklist window opens with all categories collapsed
3. Expand a category → tick off test cases as you work through them
4. Progress is saved automatically after each change
5. Re-opening the same endpoint restores previous state

### Marking an endpoint done

Click **Done** in the checklist window. A confirmation prompt appears. Click **Un-done** to revert.

### Coverage table

The **PenTracker** tab shows all tracked endpoints. Columns are sortable — click any header to sort.

## 🧩 Custom Checklists

There are two ways to extend the built-in checklists:

**Option A — External directory (recommended):** In the PenTracker tab, set a **Checklist Directory** pointing to any folder of JSON files, then click **Load**. Click **Unload** to revert to built-in defaults. The directory is persisted across Burp restarts.

**Option B — Project subfolder:** Place JSON files in `<project-dir>/custom-checklists/`. They are loaded automatically whenever the project directory is set or reloaded.

Merge rules (same for both options):
- Same checklist `id` → items are merged (external item wins on duplicate item `id`)
- New checklist `id` → added as a new category

## 🗂️ Progress File

Progress is stored in `pentracker-progress.json` in the configured project directory. Each endpoint is keyed by normalized `METHOD host/path?params`:

```
GET api.example.com/users/{id}/orders?page={page}&size={size}
```

The file is human-readable JSON and safe to commit alongside pentest notes (it contains no request/response data).

## 📁 Project Structure

```
src/main/java/com/pentracker/
├── PenTrackerExtension.java        # Extension entry point
├── data/
│   ├── ChecklistRepository.java    # Loads built-in + custom checklists
│   └── ProgressStore.java          # Read/write progress JSON, debounced save
├── engine/
│   ├── ChecklistMerger.java        # Deduplicates and groups items across categories
│   └── KeyNormalizer.java          # Normalizes endpoint keys
├── handler/
│   └── PenTrackerContextMenu.java  # Right-click menu item
├── model/
│   ├── Checklist.java
│   ├── ChecklistItem.java
│   ├── EndpointProgress.java
│   └── ItemState.java
└── ui/
    ├── ChecklistFrame.java         # Floating checklist window (JFrame)
    └── SettingsTab.java            # Burp tab: settings + coverage table
src/main/resources/checklists/
└── 27 JSON checklist files
```

## 📄 License

MIT
