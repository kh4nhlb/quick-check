# QuickCheck — Burp Suite Extension

## Vấn đề cần giải quyết

Khi pentest API, tester dễ bỏ sót test case hoặc test lại những endpoint đã làm qua. QuickCheck giải quyết bằng cách gắn checklist trực tiếp vào từng request trong Burp, tự động lưu tiến độ theo endpoint và tổng hợp coverage cho toàn bộ engagement.

---

## Luồng sử dụng

```
1. Mở QuickCheck tab → set Project Directory (1 lần cho mỗi engagement)
        ↓
2. Right-click bất kỳ request trong Proxy / Repeater / HTTP History
        ↓
3. Chọn "QuickChecklist"
        ↓
4. ChecklistFrame mở ra — tất cả 7 category hiện sẵn, section thu gọn mặc định
   Extension tự normalize endpoint key → load progress đã lưu (nếu có)
        ↓
5. Tester mở từng category → tick checkbox khi test xong
   Click tên test case để xem mô tả
        ↓
6. Progress tự lưu sau mỗi thay đổi (debounced 500ms)
        ↓
7. Khi hoàn thành endpoint → nhấn Done (có popup xác nhận)
        ↓
8. Tab QuickCheck hiện bảng Coverage — toàn bộ endpoint đã track trong engagement
```

---

## Kiến trúc

```
QuickCheckExtension.java
├── ChecklistRepository       — load built-in JSON + external custom-checklists/
├── ProgressStore             — đọc/ghi quickcheck-progress.json, debounced save
├── ChecklistMerger           — gộp và dedup items theo category
├── QuickCheckContextMenu     — right-click menu item
├── ChecklistFrame (JFrame)   — cửa sổ checklist nổi
└── SettingsTab (JPanel)      — Burp tab: cấu hình + coverage table
```

### Class chi tiết

| Class | Nhiệm vụ |
|---|---|
| `ChecklistRepository` | Load 7 built-in JSON từ classpath, merge với `custom-checklists/` nếu có |
| `ProgressStore` | Đọc/ghi `quickcheck-progress.json`, lưu project dir vào `MontoyaApi.persistence()`, notify change listeners |
| `KeyNormalizer` | Chuẩn hóa endpoint key và display path |
| `ChecklistMerger` | Gộp items từ nhiều checklist, dedup theo `id`, sort theo severity |
| `QuickCheckContextMenu` | Đăng ký menu item "QuickChecklist", gọi `checklistFrame.loadChecklist(request)` |
| `ChecklistFrame` | JFrame nổi: header (method + path), content (sections + items), bottom bar (progress bar + Done btn) |
| `SettingsTab` | Burp tab: text field + browse button để set project dir, bảng coverage endpoint |

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

7 file JSON trong `src/main/resources/checklists/`:

| File | Category | Số items |
|---|---|---|
| `broken_access.json` | Broken Access Control | 10 |
| `auth_session.json` | Auth & Session | 12 |
| `injection.json` | Injection | 11 |
| `security_misconfig.json` | Security Misconfiguration | 10 |
| `file_resource.json` | File & Resource Handling | 10 |
| `business_logic.json` | Business Logic | 10 |
| `rate_dos.json` | Rate Limiting & DoS | 8 |

Schema item:

```json
{
  "id": "ba-01",
  "title": "IDOR via direct object reference",
  "severity": "CRITICAL",
  "description": "Thay đổi ID trong path/body để access resource của user khác."
}
```

Severity được dùng để sort items trong mỗi section: CRITICAL → HIGH → MEDIUM → LOW → INFO.

---

## Custom Checklists

Đặt file JSON trong `<project-dir>/custom-checklists/`. Click **Save & Reload** để áp dụng mà không cần restart Burp.

Quy tắc merge:
- Cùng `id` checklist → items được merge, item ngoài thắng khi trùng `id`
- `id` hoàn toàn mới → xuất hiện thêm như một category mới

---

## Progress File

File `quickcheck-progress.json` lưu trong thư mục project do user cấu hình.

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
        "fu-02": { "done": false },
        "ba-01": { "done": true }
      },
      "totalItems": 71,
      "doneCount": 2,
      "completed": false,
      "firstTested": "2026-06-30T08:15:00Z",
      "lastUpdated": "2026-06-30T10:22:00Z"
    }
  }
}
```

Mỗi `ItemState` chỉ lưu `{ "done": boolean }`. File được ghi bởi background thread (daemon), debounce 500ms.

---

## UI Detail

### ChecklistFrame

```
┌─────────────────────────────────────────────────────────────┐
│  POST  /api/v1/users/{id}/upload-avatar                     │
├─────────────────────────────────────────────────────────────┤
│  ▸  Broken Access Control           2 / 10                  │ ← click để mở
│  ▾  File & Resource Handling        1 / 10                  │ ← đang mở
│      ☑  Extension bypass                                     │
│      ☐  MIME type sniffing          ← click tên để xem mô tả│
│          Gửi file .php đổi tên thành .jpg...                │
│      ☐  Path traversal via filename                         │
│  ▸  Injection                       0 / 11                  │
│  ...                                                        │
├─────────────────────────────────────────────────────────────┤
│  ████████░░░░░░░░  3 / 71                      [Done]       │
└─────────────────────────────────────────────────────────────┘
```

- Method label: màu theo HTTP method (GET=xanh dương, POST=xanh lá, PUT/PATCH=cam, DELETE=đỏ)
- Section header: có accent bar trái màu xanh, click toggle collapse/expand
- Item: checkbox bên trái + tên; click tên → tên bôi đậm, mô tả hiện ra bên dưới
- Bottom bar: progress bar + counter + Done button
- Done button: hiện popup xác nhận trước khi đánh dấu; nếu đã Done thì hiện "Un-done"

### SettingsTab (Burp tab "QuickCheck")

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Project Settings                                                           │
│  Project directory: [D:\Testing\MerchantX          ] [Browse…] [Save & Reload] │
│  progress file: D:\Testing\MerchantX\quickcheck-progress.json              │
│  12 endpoint(s) tracked                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  Coverage — Endpoints tracked                                  [Refresh]    │
│  Method │ Host            │ Endpoint              │ ✓ │ Done │ Total │ Last Updated  │
│  GET    │ api.example.com │ /users/{id}/orders    │   │  3   │  71   │ 2026-06-30... │
│  POST   │ api.example.com │ /auth/login           │ ✓ │  71  │  71   │ 2026-06-30... │
└─────────────────────────────────────────────────────────────────────────────┘
```

- Project dir được lưu vào `MontoyaApi.persistence().extensionData()` — tự restore khi mở lại Burp
- Coverage table tự refresh khi có thay đổi (change listener từ ProgressStore)
- Tất cả cột đều sortable (TableRowSorter)

---

## Tech Stack

- **Language**: Java 17
- **Burp API**: Montoya API 2022+ (scope `provided`)
- **UI**: Swing (native Burp theme)
- **JSON**: Gson
- **Build**: Maven, fat JAR qua `maven-assembly-plugin`

---

## Cấu trúc project

```
src/main/java/com/quickcheck/
├── QuickCheckExtension.java
├── data/
│   ├── ChecklistRepository.java
│   └── ProgressStore.java
├── engine/
│   ├── ChecklistMerger.java
│   └── KeyNormalizer.java
├── handler/
│   └── QuickCheckContextMenu.java
├── model/
│   ├── Checklist.java
│   ├── ChecklistItem.java
│   ├── EndpointProgress.java
│   ├── ItemState.java
│   └── ProjectProgress.java
└── ui/
    ├── ChecklistFrame.java
    └── SettingsTab.java
src/main/resources/checklists/
├── broken_access.json
├── auth_session.json
├── injection.json
├── security_misconfig.json
├── file_resource.json
├── business_logic.json
└── rate_dos.json
```

---

## Roadmap

### ✅ Phase 1 — Done
- Load 7 built-in checklists từ JSON
- ChecklistFrame: sections collapsible, description expandable, checkbox per item
- Progress lưu realtime vào `quickcheck-progress.json`
- Endpoint key normalization (numeric/UUID/token trong path, query params)
- Resume progress khi mở lại cùng endpoint
- Done / Un-done per endpoint
- SettingsTab: cấu hình project dir, coverage table sortable
- Custom checklist override qua `custom-checklists/`
- Reload checklists không cần restart Burp

### 🔲 Phase 2 — Planned
- Filter items theo severity (chỉ hiện CRITICAL + HIGH)
- Note per item (ghi chú ngắn bên cạnh từng test case)
- Export kết quả ra Markdown
- Double-click dòng trong coverage table → mở lại checklist endpoint đó
- HTTP History column "QuickCheck": hiện tiến độ `3/71` hoặc `✓ DONE` ngay trong bảng Proxy

### 🔲 Phase 3 — Future
- Auto-suggest checklist dựa trên pattern phân tích request (URL path, Content-Type, headers, body)
- Scoring engine: mỗi pattern có điểm, tổng điểm đạt ngưỡng → suggest
- Export coverage report toàn engagement
