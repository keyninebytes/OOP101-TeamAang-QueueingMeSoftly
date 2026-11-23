<!-- README.md -->
# Queueing Me Softly

Offline-first TODA dispatch and driver net-profit tracker.
OOP 101 · Batangas State University · Team-Aang · IT 2107

Pure Java 17 backend (`com.sun.net.httpserver`, no frameworks, no libraries) and a vanilla HTML/CSS/JS frontend.

## Run it

You need **JDK 17 or newer** (`javac -version` should print 17+). Nothing else.

| | Windows | macOS / Linux |
|---|---|---|
| Start the app | double-click `run.bat` | `./run.sh` |
| Run self tests | double-click `test.bat` | `./test.sh` |

Then open **http://localhost:8080** in a browser.

Manual commands, if you prefer:

```
javac --release 17 -encoding UTF-8 -d out -sourcepath src src/com/toda/Main.java src/com/toda/SelfTest.java
java -cp out com.toda.Main
```

Options: `--port 8081`, `--data <folder>`, `--no-demo-history` (seed routes and drivers but no sample trips).

**First run** seeds five Batangas routes (Calumpang, SM City, Poblacion, Batangas Pier, Alangilan), drivers T-001 to T-008, a live queue, and six days of sample trips and expenses for the charts.
**Reset to a fresh demo:** stop the server and delete the `data/` folder.

## Where the data goes

| File | What |
|---|---|
| `data/logs/dispatch_logs_YYYY-MM-DD.csv` | One row per dispatch: `TripID, Timestamp, BodyNo, DriverName, Destination, RegularPax, DiscountedPax, TripType, TotalFare` |
| `data/logs/dispatch_logs.txt` | Human-readable journal of every dispatch and expense |
| `data/logs/expense_logs.csv` | Every logged expense |
| `data/state.json` | Settings, routes, roster, expenses and the live queue (survives restarts) |

## Project structure

```
src/com/toda/
  Main.java                 wires store → logger → dispatcher → HTTP server
  SelfTest.java             46 checks, no JUnit needed
  model/                    Person (abstract), Driver, Passenger, Trip (abstract),
                            RegularTrip, SpecialTrip, FareCalculator, QueuedTricycle,
                            Destination, ExpenseRecord, TerminalSettings, TripRecord ...
  structure/                Node, DoublyLinkedList<T>, CustomQueue<T>, TricycleQueue
  service/                  TerminalDispatcher (business rules), TripLogger (CSV/TXT),
                            DataStore (state.json), AnalyticsService, SeedData
  controller/               Router, ApiController, StaticFileHandler, ApiRequest/Response
  exception/                TerminalException + Duplicate/Capacity/InvalidInput/EmptyQueue/NotFound
  util/                     Json (own parser/writer), Csv, Money, RequestBody
web/
  index.html, css/styles.css, js/app.js
```

## Where each OOP pillar lives (for the rubric)

| Pillar | Where to look |
|---|---|
| **Abstraction & Inheritance** | `Person` is abstract with `getDetails()`; `Driver` and `Passenger` extend it. `Trip` is abstract; `RegularTrip` and `SpecialTrip` extend it. `TerminalException` is abstract; five exceptions extend it. |
| **Encapsulation** | Every model field is `private`. Setters validate and throw (negative fares/expenses, blank body numbers, discounted > party size, boarding past `maxCapacity`). `Driver.getExpenses()` and `CustomQueue.toList()` return read-only views. Money only changes through `addEarnings()` / `recordExpense()`. |
| **Custom data structure** | `DoublyLinkedList<T>` (own nodes, head/tail pointers) → `CustomQueue<T>` (`enqueue`, `dequeue`, `peek`, `remove`, `moveToFront`, `moveUp`, stable `sort`, `toList`) → `TricycleQueue` (`removeByBodyNumber` etc.). `java.util.LinkedList` is never used. |
| **Polymorphism** | `FareCalculator.calculateFare()` is implemented by `RegularTrip` (per head, 20% discount) and `SpecialTrip` (flat rate or base × multiplier). `TerminalDispatcher.buildTrip()` is the only place that picks the subclass; everything after is a plain `trip.calculateFare()` call. Exceptions map to HTTP codes through the overridden `getHttpStatus()`, with no `instanceof` chain. |
| **Exception handling** | Services throw domain exceptions; `Router.handle()` catches them once and returns `{"ok":false,"error":{"code","message"}}` with 400 / 404 / 409. The frontend shows them as toasts. |

Net Profit is always `Gross Earnings − Total Expenses` (`Driver.getNetProfit()`, `AnalyticsService`).

## How the main features behave

- **Smart batch boarding:** a party goes to the first unit in FIFO order that is unassigned or already heading to the same destination and has enough seats. If it does not fit the first eligible unit, the drawer offers **Split across tricycles** or **Keep the party together** on the next unit with room. A receipt preview shows the mixed fare (e.g. 2 regular + 1 student) before you confirm.
- **Special Trip Override:** locks an empty unit as a private hire, bypasses seat capacity and discounts, applies the special rate from Settings, and moves the unit to #1.
- **Status changes:** setting a queued driver to On Break or Maintenance removes them from the line. Their earnings and expenses are kept. It is blocked while passengers are aboard.
- **Auto-Route Sorting (Settings):** boarding units move ahead of empty ones, and parties fill units already heading their way first. FIFO order holds inside each group.

## REST endpoints

`GET /api/dashboard` · `GET|POST /api/queue` · `DELETE /api/queue/{bodyNo}` · `POST /api/queue/{bodyNo}/move-up|move-front|clear|special` · `POST /api/board/preview` · `POST /api/board` · `POST /api/dispatch` · `GET|POST /api/drivers` · `GET|PUT|DELETE /api/drivers/{id}` · `PUT /api/drivers/{id}/status` · `GET|POST /api/destinations` · `PUT|DELETE /api/destinations/{id}` · `GET|POST /api/expenses` · `GET /api/logs` · `GET /api/logs/export` · `GET /api/analytics?range=today|7d|30d|custom` · `GET|PUT /api/settings`

Everything runs locally. No cloud, GPS or online payments.
