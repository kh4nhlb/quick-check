# 🔍 QuickCheck — Burp Suite Pentest Checklist Extension

A Burp Suite extension that provides structured vulnerability checklists directly in the HTTP request context menu. Track testing coverage across all endpoints in a pentest engagement.

## ✨ Features

- 🖱️ **Right-click any request** (Proxy, Repeater, HTTP History) → **QuickChecklist** → checklist opens instantly
- 📋 **7 vulnerability-class checklists** loaded automatically, sections collapsed by default:
  - 🔓 Broken Access Control
  - 🔑 Auth & Session
  - 💉 Injection
  - ⚙️ Security Misconfiguration
  - 📁 File & Resource Handling
  - 🧠 Business Logic
  - 🚦 Rate Limiting & DoS
- 💾 **Persistent progress** — state auto-saved to `quickcheck-progress.json` (debounced 500ms)
- 🔀 **Endpoint normalization** — numeric/UUID path segments replaced with `{id}`, query params normalized to `param={param}`, so `/users/42` and `/users/99` share the same entry
- ✅ **Done / Un-done** — mark an endpoint fully tested; status reflected in coverage table
- 📊 **Coverage table** in QuickCheck tab — lists all tracked endpoints (Method / Host / Endpoint / Status / Done / Total / Last Updated), sortable by column

## 🛠️ Requirements

- Java 17+
- Maven 3.6+
- Burp Suite Pro / Community (Montoya API 2022+)

## 🔨 Build

```bash
mvn clean package -q
```

Output: `target/quickcheck-<version>-all.jar`

## 📦 Install in Burp

1. **Extensions → Add → Select file** → pick the fat JAR
2. A **QuickCheck** tab appears in the Burp Suite toolbar

## 🚀 Usage

### First-time setup

1. Open the **QuickCheck** tab in Burp
2. Set the **Project Directory** (the folder where progress will be saved)
3. Click **Save Settings** — a `quickcheck-progress.json` file will be created in that directory

### Running a checklist

1. Right-click any HTTP request → **QuickChecklist**
2. The checklist window opens with all 7 categories collapsed
3. Expand a category → tick off test cases as you work through them
4. Progress is saved automatically after each change
5. Re-opening the same endpoint restores previous state

### Marking an endpoint done

Click **Done** in the checklist window. A confirmation prompt appears. Click **Un-done** to revert.

### Coverage table

The **QuickCheck** tab shows all tracked endpoints. Columns are sortable — click any header to sort.

## 🧩 Custom Checklists

Place JSON files in `<project-dir>/custom-checklists/` using the same schema as the built-in checklists under `src/main/resources/checklists/`. Click **Reload Checklists** in the QuickCheck tab to apply without restarting Burp.

- Same checklist `id` → items are merged (custom item wins on duplicate `id`)
- New `id` → added as a new category

## 🗂️ Progress File

Progress is stored in `quickcheck-progress.json` in the configured project directory. Each endpoint is keyed by normalized `METHOD host/path?params`:

```
GET api.example.com/users/{id}/orders?page={page}&size={size}
```

The file is human-readable JSON and safe to commit alongside pentest notes (it contains no request/response data).

## 📁 Project Structure

```
src/main/java/com/quickcheck/
├── QuickCheckExtension.java        # Extension entry point
├── data/
│   ├── ChecklistRepository.java    # Loads built-in + custom checklists
│   └── ProgressStore.java          # Read/write progress JSON, debounced save
├── engine/
│   ├── ChecklistMerger.java        # Deduplicates and groups items across categories
│   └── KeyNormalizer.java          # Normalizes endpoint keys
├── handler/
│   └── QuickCheckContextMenu.java  # Right-click menu item
├── model/
│   ├── Checklist.java
│   ├── ChecklistItem.java
│   ├── EndpointProgress.java
│   └── ItemState.java
└── ui/
    ├── ChecklistFrame.java         # Floating checklist window (JFrame)
    └── SettingsTab.java            # Burp tab: settings + coverage table
src/main/resources/checklists/
├── broken_access.json
├── auth_session.json
├── injection.json
├── security_misconfig.json
├── file_resource.json
├── business_logic.json
└── rate_dos.json
```

## 📄 License

MIT
