# PenTracker — Burp Suite Extension

## Vấn đề cần giải quyết

Khi pentest API, tester dễ bỏ sót test case hoặc test lại những endpoint đã làm qua. PenTracker giải quyết bằng cách gắn checklist trực tiếp vào từng request trong Burp, tự động lưu tiến độ theo endpoint và tổng hợp coverage cho toàn bộ engagement.

---

## Luồng sử dụng

```
1. Mở PenTracker tab → set Project Directory (1 lần cho mỗi engagement)
        ↓
2. Right-click bất kỳ request trong Proxy / Repeater / HTTP History
        ↓
3. Chọn "PenTracker Checklist"
        ↓
4. ChecklistFrame mở ra — tất cả category hiện sẵn, section thu gọn mặc định
   Extension tự normalize endpoint key → load progress đã lưu (nếu có)
        ↓
5. Tester mở từng category → tick checkbox khi test xong
   Click tên test case để xem mô tả
        ↓
6. Progress tự lưu sau mỗi thay đổi (debounced 500ms)
        ↓
7. Khi hoàn thành endpoint → nhấn Done (có popup xác nhận)
        ↓
8. Tab PenTracker hiện bảng Coverage — toàn bộ endpoint đã track trong engagement
```

---

## Kiến trúc

```
PenTrackerExtension.java
├── ChecklistRepository       — load built-in JSON + external custom-checklists/
├── ProgressStore             — đọc/ghi pentracker-progress.json, debounced save
├── ChecklistMerger           — gộp và dedup items theo category
├── PenTrackerContextMenu     — right-click menu item
├── ChecklistFrame (JFrame)   — cửa sổ checklist nổi
└── SettingsTab (JPanel)      — Burp tab: cấu hình + coverage table
```

### Class chi tiết

| Class | Nhiệm vụ |
|---|---|
| `ChecklistRepository` | Load 27 built-in JSON từ classpath, merge với `custom-checklists/` và external dir nếu có |
| `ProgressStore` | Đọc/ghi `pentracker-progress.json`, lưu project dir + checklist dir vào `MontoyaApi.persistence()`, notify change listeners |
| `KeyNormalizer` | Chuẩn hóa endpoint key và display path |
| `ChecklistMerger` | Gộp items từ nhiều checklist, dedup theo `id`, sort theo severity |
| `PenTrackerContextMenu` | Đăng ký menu item "PenTracker Checklist", gọi `checklistFrame.loadChecklist(request)` |
| `ChecklistFrame` | JFrame nổi: header (method + path), content (sections + items), bottom bar (progress bar + Done btn) |
| `SettingsTab` | Burp tab: set project dir + external checklist dir, bảng coverage endpoint |

---

## Endpoint Key Normalization

Các segment trông giống ID trong URL path được thay thế bằng `{id}` để mọi request đến cùng endpoint dùng chung key:

| Pattern | Ví dụ | Thay bằng |
|---|---|---|
| Số nguyên | `/users/42/` | `/users/{id}/` |
| UUID | `/550e8400-e29b-41d4-a716-446655440000` | `/{id}` |
| MongoDB ObjectId (24 hex) | `/507f1f77bcf86cd799439011` | `/{id}` |
| Base64-like token dài ≥ 20 ký tự | `/eyJhbGciOiJIUzI1NiJ9` | `/{id}` |

Query params: giá trị → `param={param}`, ví dụ `?page=1&size=20` → `?page={page}&size={size}`

Key đầy đủ = `METHOD host/path?query`:
```
GET api.example.com/users/{id}/orders?page={page}&size={size}
```

---

## Checklists tích hợp sẵn

27 file JSON trong `src/main/resources/checklists/`, sắp xếp từ phổ biến đến hiếm:

| Tier | Categories |
|---|---|
| Mọi API | Auth & Session, Authorization & Access Control, Injection, Sensitive Data, Error Handling, Rate Limiting & DoS |
| Phổ biến | Register/Login, CRUD API, Business Logic, Client-Side, OTP/2FA/MFA, Open Redirect |
| Feature-specific | File Upload, SSRF, Payment Logic, Email Function, WebSocket, XXE |
| Ít phổ biến | Path Traversal, Cache, Host Header, eKYC |
| Tech-specific | LFI/RFI, Deserialization, HTTP Smuggling, Framework Specific |
| One-time | One-Time Testcases |

Schema item:

```json
{
  "id": "ba-01",
  "title": "IDOR — direct object reference",
  "severity": "CRITICAL",
  "description": "Thay đổi ID trong path/body để access resource của user khác.\n- Thử với ID số, UUID, slug\n- Test cả IDOR horizontal lẫn vertical"
}
```

Severity sort order: CRITICAL → HIGH → MEDIUM → LOW → INFO.

---

## Custom Checklists

Có 2 cách mở rộng checklist ngoài built-in:

### Cách 1 — External Directory (qua Settings tab)

User set một thư mục bất kỳ chứa file JSON trong PenTracker tab → **Load**. **Unload** để quay về mặc định. Đường dẫn được lưu vào `MontoyaApi.persistence()`, tự restore khi mở lại Burp.

### Cách 2 — Project subfolder

Đặt file JSON trong `<project-dir>/custom-checklists/`. Tự load khi set hoặc reload project directory.

### Quy tắc merge

- Cùng `id` checklist → items được merge; item ngoài thắng khi trùng `id` item
- `id` checklist hoàn toàn mới → xuất hiện thêm như một category mới
- Thứ tự ưu tiên: built-in → `custom-checklists/` → external dir

---

## Progress File

File `pentracker-progress.json` lưu trong thư mục project do user cấu hình.

```json
{
  "version": "1",
  "created": "2026-06-30T08:00:00Z",
  "updated": "2026-06-30T10:30:00Z",
  "endpoints": {
    "POST api.example.com/api/v1/users/{id}/upload-avatar": {
      "host": "api.example.com",
      "method": "POST",
      "pathPattern": "/api/v1/users/{id}/upload-avatar",
      "items": {
        "fu-01": { "done": true },
        "ba-01": { "done": true }
      },
      "totalItems": 150,
      "doneCount": 2,
      "completed": false,
      "firstTested": "2026-06-30T08:15:00Z",
      "lastUpdated": "2026-06-30T10:22:00Z"
    }
  }
}
```

---

## UI Detail

### ChecklistFrame

```
┌─────────────────────────────────────────────────────────────┐
│  POST  /api/v1/users/{id}/upload-avatar                     │
├─────────────────────────────────────────────────────────────┤
│  ▸  Authorization & Access Control Testing    2 / 7         │ ← click để mở
│  ▾  File Upload Testing                       1 / 12        │ ← đang mở
│      ☑  Dangerous extension bypass                          │
│      ☐  MIME type spoofing      ← click tên để xem mô tả   │
│          Đổi Content-Type header thành image/jpeg...        │
│      ☐  Path traversal via filename                         │
│  ▸  Injection Testing                         0 / 12        │
│  ...                                                        │
├─────────────────────────────────────────────────────────────┤
│  ████████░░░░░░░░  3 / 150                     [Done]       │
└─────────────────────────────────────────────────────────────┘
```

### SettingsTab (Burp tab "PenTracker")

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Project Settings                                                                │
│  Project directory:   [D:\Testing\MerchantX       ] [Browse…] [Save & Reload]   │
│  progress file: D:\Testing\MerchantX\pentracker-progress.json | 12 endpoint(s)  │
│                                                                                  │
│  Checklist directory: [D:\Checklists\custom       ] [Browse…] [Load] [Unload]   │
│  Loaded 3 external checklist(s) from: D:\Checklists\custom                      │
├──────────────────────────────────────────────────────────────────────────────────┤
│  Coverage — Endpoints tracked                                       [Refresh]    │
│  Method │ Host            │ Endpoint              │ ✓ │ Done │ Total │ Last Updated │
│  GET    │ api.example.com │ /users/{id}/orders    │   │  3   │ 150   │ 2026-06-30.. │
│  POST   │ api.example.com │ /auth/login           │ ✓ │ 150  │ 150   │ 2026-06-30.. │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

- **Language**: Java 17
- **Burp API**: Montoya API 2022+ (scope `provided`)
- **UI**: Swing (native Burp theme)
- **JSON**: Gson
- **Build**: Maven, fat JAR qua `maven-assembly-plugin`

---

## Roadmap

### ✅ Phase 1 — Done
- Load 27 built-in checklists từ JSON
- ChecklistFrame: sections collapsible, description expandable, checkbox per item
- Progress lưu realtime vào `pentracker-progress.json`
- Endpoint key normalization (numeric/UUID/token trong path, query params)
- Resume progress khi mở lại cùng endpoint
- Done / Un-done per endpoint
- SettingsTab: cấu hình project dir, coverage table sortable
- Custom checklist override qua `custom-checklists/`
- External checklist directory: import thư mục bất kỳ, Load/Unload từ Settings tab
- Reload checklists không cần restart Burp

### 🔲 Phase 2 — Planned
- Filter items theo severity (chỉ hiện CRITICAL + HIGH)
- Note per item (ghi chú ngắn bên cạnh từng test case)
- Export kết quả ra Markdown
- Double-click dòng trong coverage table → mở lại checklist endpoint đó
- HTTP History column "PenTracker": hiện tiến độ `3/150` hoặc `✓ DONE` ngay trong bảng Proxy

### 🔲 Phase 3 — Future
- Auto-suggest checklist dựa trên pattern phân tích request (URL path, Content-Type, headers, body)
- Scoring engine: mỗi pattern có điểm, tổng điểm đạt ngưỡng → suggest
- Export coverage report toàn engagement
