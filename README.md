# Campus Lost & Found Management System (Full-Stack Edition)

A beginner-friendly, full-stack Java application designed for university and college campuses. It allows students and campus staff to report lost and found items, view live campus statistics, automatically match lost items with found items using an intelligent scoring algorithm, and securely claim items using Student ID verification.

---

## 🌟 Highlights & Student-Friendly Features

- **Full-Stack Architecture**: Modern responsive Web Frontend connected to a lightweight, pure Java REST API Backend.
- **Zero Heavy Dependencies**: No Maven, Gradle, Spring Boot, Node.js, or external database installation required. Runs immediately on any computer with standard Java (JDK 8 or newer).
- **Dual Mode (Web + Console)**: Run either the interactive Web Application in your browser or the classic Terminal/Console menu.
- **Smart Match Algorithm**: Multi-factor scoring (up to 100 points) based on Category, Keywords, Location, and Date proximity.
- **Secure Lifecycle**: `Open` ➔ `Matched` (staff/algorithm confirmation) ➔ `Claimed` (verified with Owner's Student ID).
- **Safe File Persistence**: Data automatically persists to `data/lostfound.csv` with atomic writes, preventing file corruption.

---

## 📐 Architecture Overview

```
+-----------------------------------------------------------------------+
|                    FRONTEND (Web Client / Browser)                     |
|  - HTML5, CSS3 (Modern, Responsive Dashboard, Badge Tags, Cards)      |
|  - Vanilla JavaScript (app.js with fetch API, async/await, DOM)       |
+-----------------------------------▲-----------------------------------+
                                    | HTTP Requests (GET / POST)
                                    | JSON Data & Responses
+-----------------------------------▼-----------------------------------+
|                    BACKEND (Standard Java REST API)                   |
|  com.campus.lostfound.web:                                            |
|   - ServerMain / WebServer (com.sun.net.httpserver.HttpServer)        |
|   - SimpleJson (Zero-dependency JSON parser & serializer)             |
|   - StaticFileHandler (Serves index.html, style.css, app.js)         |
+-----------------------------------▲-----------------------------------+
                                    | Method Calls
+-----------------------------------▼-----------------------------------+
|                  CORE BUSINESS LOGIC & OOP SERVICES                   |
|  com.campus.lostfound.service:                                        |
|   - LostFoundService (Reporting, Searching, Matching, Claiming)       |
|   - KeywordMatchStrategy (Category, Keyword, Location, Date Scoring)  |
|   - DemoData (Preloaded sample items for quick presentation demos)    |
|                                                                       |
|  com.campus.lostfound.model:                                          |
|   - Item (Abstract Base Class), LostItem, FoundItem                   |
|   - Person, MatchResult, ItemCategory, ItemStatus                     |
+-----------------------------------▲-----------------------------------+
                                    | Read / Write
+-----------------------------------▼-----------------------------------+
|                       STORAGE & DATA PERSISTENCE                      |
|  com.campus.lostfound.storage:                                        |
|   - FileItemRepository (Atomic write to data/lostfound.csv)           |
+-----------------------------------------------------------------------+
```

---

## 🚀 How to Run the Project

### Option 1: 1-Click Launch (Windows)

- **For the Web Application**: Double-click `run-web.bat`.  
  *(It automatically compiles the code, launches the server, and opens http://localhost:8080 in your browser!)*
- **For the Console Menu**: Double-click `run-console.bat`.

---

### Option 2: Using the Command Line (Terminal / PowerShell / Bash)

#### Compile:
```bash
# Windows (PowerShell or Command Prompt)
javac -d bin -sourcepath src src/com/campus/lostfound/Main.java src/com/campus/lostfound/web/ServerMain.java

# Linux / macOS
javac -d bin $(find src -name "*.java")
```

#### Run Web Application:
```bash
java -cp bin com.campus.lostfound.web.ServerMain 8080
```
Then open your web browser and navigate to:  
👉 **http://localhost:8080**

#### Run Console Menu:
```bash
java -cp bin com.campus.lostfound.Main --console
```

---

### Option 3: In Eclipse IDE

1. Open Eclipse ➔ **File** ➔ **Import...** ➔ **General** ➔ **Existing Projects into Workspace**.
2. Browse to the `CampusLostFound` directory and click **Finish**.
3. To run the Web Server:
   - Navigate to `src/com/campus/lostfound/web/ServerMain.java`
   - Right-click ➔ **Run As** ➔ **Java Application**.
   - Open browser at `http://localhost:8080`.
4. To run the Console:
   - Navigate to `src/com/campus/lostfound/Main.java`
   - Right-click ➔ **Run As** ➔ **Java Application**.

---

## 💡 Quick Demo Walkthrough (For Viva / Presentations)

1. Start the application and open **http://localhost:8080**.
2. Click **⚡ Load Sample Data** in the top navigation bar.
3. Observe the dashboard statistics update instantly (Total Lost, Total Found, Open items).
4. Go to **⚡ Smart Match Finder**:
   - Select `[L001] Black Leather Wallet`.
   - The engine will display a **95% Match** with `[F001] Black Wallet` found at the Library Entrance!
   - Click **✅ Confirm Match**. The items are now linked and marked as `MATCHED`.
5. Go to **🤝 Claim & Return Desk**:
   - Select `[L001] Black Leather Wallet`.
   - Enter Owner Student ID: `S1001` (recorded during report).
   - Click **Verify & Return Item**.
   - Both items transition to `CLAIMED`!
6. Click **📄 Export Report** to export a clean text report to `data/report.txt`.

---

## 📚 Where Each Java & OOP Concept is Used

| Concept | Location | Explanation |
|---|---|---|
| **Abstraction** | `Item.java` | Abstract class defining common item behavior with abstract methods `getType()` and `getContactRole()`. |
| **Inheritance** | `LostItem.java`, `FoundItem.java` | Subclasses extend `Item` and inherit common attributes (id, name, location, date, status). |
| **Polymorphism** | Subclasses of `Item` | `getType()`, `getContactRole()`, and `extraDetails()` behave differently depending on whether the object is a `LostItem` or `FoundItem`. |
| **Encapsulation** | Models & Services | Private member fields, getters/setters, immutable `Person` object. |
| **Interfaces** | `ItemRepository`, `MatchStrategy`, `Searchable<T>`, `Comparable<T>` | Decouples storage and matching algorithms from business logic. |
| **Collections** | `LostFoundService`, `SimpleJson` | `LinkedHashMap` for fast lookup and insertion ordering, `ArrayList` for lists, `HashSet` for keyword intersections, `EnumMap` for statistics. |
| **Exception Handling** | `com.campus.lostfound.exception` | Custom checked exceptions (`ItemNotFoundException`, `MatchException`, `InvalidInputException`, `StorageException`). |
| **File I/O & Streams** | `FileItemRepository.java` | BufferedReader, BufferedWriter, UTF-8 encoding, and atomic temporary file swapping to prevent corruption. |
| **Networking & HTTP** | `WebServer.java` | Uses Java's built-in `com.sun.net.httpserver.HttpServer` and `HttpHandler` to build a RESTful API. |

---

## 📡 REST API Reference

| Method | Endpoint | Description | Query / Body Parameters |
|---|---|---|---|
| `GET` | `/api/stats` | Returns total and status breakdown counts | None |
| `GET` | `/api/items` | Lists all items (supports filtering & search) | `?type=LOST\|FOUND`, `?status=OPEN`, `?category=...`, `?q=wallet` |
| `GET` | `/api/items/{id}` | Returns full details of a specific item | Item ID in path (e.g. `/api/items/L001`) |
| `POST` | `/api/lost` | Reports a new lost item | `{ name, category, location, dateLost, description, personName, studentId, phone, email }` |
| `POST` | `/api/found` | Reports a new found item | `{ name, category, location, dateFound, storedAt, description, personName, studentId, phone, email }` |
| `GET` | `/api/matches` | Calculates matches for an item | `?id=L001` |
| `POST` | `/api/matches/confirm` | Confirms a match between lost and found items | `{ lostId: "L001", foundId: "F001" }` |
| `POST` | `/api/claim` | Verifies Student ID and marks item claimed | `{ lostId: "L001", studentId: "S1001" }` |
| `POST` | `/api/demo` | Populates database with sample test records | None |
| `GET` | `/api/export` | Generates text summary report in `data/report.txt` | None |

---

## 🧠 Viva / Exam Questions & Answers

1. **Q: Why is standard `HttpServer` used instead of Spring Boot or Node.js?**  
   *A:* Using Java's built-in `com.sun.net.httpserver.HttpServer` ensures zero setup friction: no external tools, no Maven plugins, and no dependency resolution issues. It runs immediately on any standard JDK while demonstrating core networking, HTTP protocol, and REST concepts.

2. **Q: How does the Smart Match algorithm work?**  
   *A:* `KeywordMatchStrategy` calculates a score from 0 to 100 based on 4 criteria:
   - **Category Match (30 points)**: Exact match between categories.
   - **Keyword Similarity (Up to 40 points)**: Tokenizes name and description, filters stop-words, and calculates set intersection.
   - **Location Overlap (15 points)**: Checks for same campus building or area.
   - **Date Proximity (15 points)**: Evaluates days between loss and discovery (within 3 days = 15 pts, within 7 days = 10 pts).
   Pairs with a score $\ge 50$ are suggested.

3. **Q: How is data protected against file corruption?**  
   *A:* In `FileItemRepository`, data is first written to a temporary file (`lostfound.csv.tmp`). Once the write succeeds completely, it is atomically moved (`StandardCopyOption.REPLACE_EXISTING`) to `lostfound.csv`. If an unexpected crash occurs mid-write, the existing database file remains intact.
