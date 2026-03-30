# HONDA DASH
## 2007 Honda Civic Si FA5 // Dashboard App

---

## QUICK START — HOW TO RESUME WORK

### Step 1: Open VS Code
```
File → Open Folder → C:\Users\Owner\AndroidStudioProjects\HondaDash
```
Open the **full project**, not just the assets folder.

### Step 2: Open terminal in VS Code, start Claude
```
claude
```
Then tell Claude: **"Read README.md to get up to speed on HondaDash"**

### Step 3: Start the browser dev server
In a **separate VS Code terminal** (click the + icon):
```
python -m http.server 8000 --directory "C:\Users\Owner\AndroidStudioProjects\HondaDash\app\src\main\assets"
```

### Step 4: Open the app in your browser
```
http://127.0.0.1:8000/index.html
```
All 9 tabs work. Simulated data runs automatically. No Android needed for UI testing.

### Step 5: Android Studio (only for APK builds)
Open Android Studio separately when you need to build/deploy to the Nexus 7:
```
File → Open → C:\Users\Owner\AndroidStudioProjects\HondaDash
```
Shift+F10 to build and run on tablet.

---

## PROJECT OVERVIEW

Custom Android dashboard app for a 2007 Honda Civic Si (FA5, K20Z3).
Built in Android Studio (Kotlin + WebView). UI is HTML/CSS/JS loaded via WebView.
Live OBD2 data from ELM327 Bluetooth adapter or Hondata FlashPro.
Runs on a Nexus 7 tablet mounted in the dash.
Also fully testable in any browser via the dev server.

---

## PROJECT STRUCTURE

```
HondaDash/
├── app/src/main/
│   ├── assets/                         ← Web UI (all the HTML/JS/CSS)
│   │   ├── index.html                  ← Browser shell (iframe nav, 9 tabs)
│   │   ├── dashboard.html              ← Gauges: RPM, speed, VTEC effects
│   │   ├── carview.html                ← 3D car + DTC codes (Three.js)
│   │   ├── multisystem.html            ← Multi-module PID viewer (NEW)
│   │   ├── streetview.html             ← Navigation/maps (Mapbox)
│   │   ├── music.html                  ← Spotify player + visualizer
│   │   ├── datalog.html                ← Real-time datalogging + CSV export
│   │   ├── battery.html                ← Battery health test (NEW)
│   │   ├── trips.html                  ← Trip tracking + merge
│   │   ├── hondata.html                ← FlashPro maps + vehicle profile
│   │   ├── chassis.html                ← Undercarriage schematic (not in nav)
│   │   ├── config.js                   ← API keys (Mapbox, Google, Spotify)
│   │   ├── GLTFLoader.js               ← Three.js model loader
│   │   └── *.glb, *.png, *.mp3        ← 3D models, images, audio
│   │
│   ├── java/com/example/hondadash/
│   │   ├── MainActivity.kt             ← App shell: WebViews, nav bar, BT wiring
│   │   ├── BluetoothService.kt         ← OBD-II Bluetooth SPP + polling (NEW)
│   │   ├── OBD2Parser.kt               ← ELM327 hex parser (NEW)
│   │   └── DevicePickerDialog.kt       ← BT device picker dialog (NEW)
│   │
│   └── AndroidManifest.xml             ← Permissions, service registration
│
├── app/build.gradle.kts                ← App dependencies (min SDK 24, target 36)
├── build.gradle.kts                    ← Project-level build config
└── README.md                           ← THIS FILE
```

---

## ALL 9 TABS

| # | Tab | File | What it does |
|---|-----|------|-------------|
| 1 | GAUGES | dashboard.html | Live RPM gauge, speed, coolant, VTEC warp effect, boot animation |
| 2 | VEHICLE | carview.html | 3D Honda Civic Si model, tap sensors for DTC codes |
| 3 | MULTI | multisystem.html | Pick PIDs from ECM/TCM/ABS/BCM, view all on one graph |
| 4 | MAPS | streetview.html | Waze-style nav with 3D car model on Mapbox |
| 5 | MUSIC | music.html | Spotify playback, visualizer, synced lyrics |
| 6 | DATALOG | datalog.html | Real-time ECU graphing, 12 params, CSV recording |
| 7 | BATTERY | battery.html | SOC ring gauge, voltage/CCA test, export report |
| 8 | TRIPS | trips.html | Trip cards with maps, stats, merge feature |
| 9 | HONDATA | hondata.html | FlashPro map details, vehicle spec sheet |

---

## SCREENS — DETAIL

### 1. GAUGES (dashboard.html)
- CP2077 boot sequence — hex scramble resolves into live gauges
- Live gauges: RPM (full width), Speed, Coolant, Throttle, Gear, O2
- Redline effect at 7800 RPM — screen shake, border flash, red overlay
- VTEC warp effect at 5800 RPM — star warp, "VTEC ENGAGED" text slam
- Configurable gauge grid with picker sidebar

### 2. VEHICLE (carview.html)
- Three.js 3D model with enhanced lighting
- Touch drag to rotate, pinch to zoom
- 6 sensor dots: O2, Coolant, TPS, MAP, VTEC, Knock
- Tap sensor → info panel with real Honda K20 DTC codes
- Fault sim panel cycles through DTC codes per sensor

### 3. MULTI (multisystem.html) — NEW
- Left sidebar: 4 expandable modules (ECM, TCM, ABS, BCM)
- 33 total PIDs across all modules
- Tap PIDs from any module to add to unified real-time graph
- Top row: 4 hero gauges with module badges
- Module colors: ECM=red, TCM=green, ABS=blue, BCM=amber
- Bottom toggle bar to show/hide individual traces
- Inspired by Launch Throttle 5's multi-system data stream

### 4. MAPS (streetview.html)
- Mapbox GL JS with Waze-style navigation UI
- 3D car model on map (2007 Honda Civic Si GLB)
- Brake model swap — separate GLB with brake lights
- Turn-by-turn with maneuver icons, ETA, speed display
- Demo route: Waltham to TD Garden

### 5. MUSIC (music.html)
- Spotify Web Playback SDK (requires Premium)
- Home screen: profile, playlists, top tracks, search, genre browse
- Mini player bar always visible
- Now-playing: animated album art, floating particles, audio visualizer
- Synced lyrics from LRCLIB (free, no API key)

### 6. DATALOG (datalog.html)
- 4 hero gauges: RPM, AFR, MAP, Coolant Temp
- Multi-trace scrolling graph, 12 toggleable parameters
- REC button records sessions, exports CSV
- Simulated spirited drive data for testing

### 7. BATTERY (battery.html) — NEW
- Big animated SOC ring gauge
- Readouts: voltage, CCA (640A rated), internal resistance
- START TEST button — 3-second animated test sequence
- Charging system check (alternator output)
- PASS/MARGINAL/FAIL verdict with color coding
- Export text report
- Inspired by Launch Throttle 5's battery tester

### 8. TRIPS (trips.html)
- Trip card list with thumbnail maps
- Stats: distance, duration, max/avg speed
- Detail panel with full route map
- Trip merge functionality

### 9. HONDATA (hondata.html)
- Stock / Map 1 (Sport) selector
- Map details: VTEC point, rev limit, fuel, timing
- Vehicle profile table (16 specs)

---

## BLUETOOTH OBD-II (Android Only)

### How it works
1. `BluetoothService.kt` connects via Bluetooth SPP to an ELM327 adapter
2. Initializes: ATZ → ATE0 → ATL0 → ATS0 → ATH0 → ATSP6 (Honda CAN bus)
3. Polls PIDs on rotating schedule:
   - Every cycle (~7 Hz): RPM, Speed
   - Every 2nd cycle: Throttle, Coolant
   - Every 4th cycle: IAT, Timing, STFT, O2
   - Every 8th cycle: LTFT, MAP
   - Every 30th cycle: Battery voltage (ATRV)
   - Every 200th cycle: DTC scan (mode 03)
4. `OBD2Parser.kt` converts hex → numbers
5. `MainActivity.kt` pushes to all WebViews via `evaluateJavascript()`

### How to use
1. Pair your ELM327 adapter in Android Bluetooth settings
2. Open HondaDash on the tablet
3. Tap the **BT** button (right side of nav bar)
4. Select your adapter from the device picker
5. Button turns **green** = connected, **yellow** = connecting, **dim** = disconnected
6. Tap BT again while connected to disconnect

### In the browser
Bluetooth doesn't work — all data is simulated automatically on every page.

---

## JAVASCRIPT BRIDGE FUNCTIONS

Called by Kotlin to push real OBD data into web pages:

| Function | Page | Parameters |
|----------|------|-----------|
| `updateGauges(rpm,spd,clt,tps,o2,iat,ign,stft,ltft,batt,idc)` | dashboard | 11 numeric values |
| `updateDTC(codes)` | carview | Array: `["P0136", "P2646"]` |
| `updateFromOBD(modKey, pidKey, value)` | multisystem | `"ECM"`, `"rpm"`, `6904` |
| `updateBatteryFromOBD(voltage)` | battery | Float: `12.55` |
| `updateFromFlashPro(data)` | datalog | Object: `{rpm:6904, vss:72, ...}` |
| `updatePosition(lng, lat, bearing, speed)` | streetview | GPS coords |

---

## ANDROID PROJECT

- **Package:** `com.example.hondadash`
- **Language:** Kotlin
- **Min SDK:** API 24 (Android 7.0)
- **Target SDK:** API 36
- **IDE:** Android Studio
- **Permissions:** Bluetooth, Location, Internet (all declared in manifest)

---

## API KEYS & SERVICES

| Service | Purpose | Config Key |
|---------|---------|------------|
| Mapbox | Navigation map | `MAPBOX_TOKEN` in config.js |
| Spotify | Music playback | `SPOTIFY_CLIENT_ID` in config.js |
| LRCLIB | Synced lyrics (free) | No key needed |

---

## TECH STACK

- **Frontend:** Vanilla HTML/CSS/JS (no frameworks)
- **Android:** Kotlin + WebView
- **3D:** Three.js (r128)
- **Maps:** Mapbox GL JS v3.17
- **Music:** Spotify Web Playback SDK
- **Fonts:** Share Tech Mono, Orbitron, Nunito
- **Theme:** Dark (#0a0000 bg, #cc2200 accent)

---

## CAR SPECS — 2007 HONDA CIVIC SI FA5

| Spec | Value |
|------|-------|
| Engine | K20Z3, 2.0L DOHC i-VTEC |
| Power | 197hp @ 7800 RPM |
| Torque | 139 lb-ft @ 6200 RPM |
| Transmission | 6-speed manual |
| Redline | 8200 RPM |
| VTEC crossover | ~5800 RPM |
| Color | Nighthawk Black Pearl |

---

## BUILD MODS

| Component | Part | Status |
|-----------|------|--------|
| Coilovers | Tein Flex Z | Installed |
| Wheels | Enkei RPF1 18x9.5 +38 | Installed |
| Exhaust | APEXi WS3 cat-back | Installed |
| ECU | Hondata FlashPro | Installed |
| Steering rack | OEM replacement | Installed |
| Shifter | Acuity components | Installed |
| EDFC5 Active | TEIN controller | Pending |
| Front end | Facelift conversion | Ordered |
| Engine swap | K24A2 TSX longblock | Planned ~260whp |

---

## TODO

- [ ] Test BluetoothService with real ELM327 adapter
- [ ] Real GPS integration for navigation
- [ ] Physical tablet install — measure dash opening
- [ ] EDFC5 Active install + dashboard integration
- [ ] Facelift front end paint + install
- [ ] GitHub Pages demo site updates

---

*Last updated: March 2026*
