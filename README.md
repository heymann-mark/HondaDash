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

To find your last conversation, run `claude` and press **Up Arrow** to see recent sessions, or check your conversation history.

### Step 3: Start the browser dev server
In a **separate VS Code terminal** (click the + icon):
```
python -m http.server 8000 --directory "C:\Users\Owner\AndroidStudioProjects\HondaDash\app\src\main\assets"
```

### Step 4: Open the app in your browser
```
http://127.0.0.1:8000/index.html
```
All 10 tabs work. Simulated data runs automatically. No Android needed for UI testing.

### Step 5: Use PowerToys to pin Chrome on top of VS Code
Click Chrome window, press **Win + Ctrl + T** to pin it always-on-top.
Resize Chrome to tablet size and overlay it on VS Code so you can see both.

### Step 6: Android Studio (only for APK builds)
Open Android Studio separately when you need to build/deploy to the Nexus 7:
```
File → Open → C:\Users\Owner\AndroidStudioProjects\HondaDash
```
Shift+F10 to build and run on tablet.

### Browser zoom
If everything looks tiny, you probably hit Ctrl+Scroll by accident.
Press **Ctrl+0** to reset zoom to 100%.

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
│   │   ├── index.html                  ← Browser shell (iframe nav, 10 tabs)
│   │   ├── dashboard.html              ← Gauges: 4 themes, RPM, VTEC effects
│   │   ├── carview.html                ← 3D car + DTC codes (Three.js)
│   │   ├── multisystem.html            ← Multi-module PID viewer
│   │   ├── streetview.html             ← Navigation/maps (Mapbox)
│   │   ├── music.html                  ← Spotify player + visualizer
│   │   ├── datalog.html                ← Real-time datalogging + CSV export
│   │   ├── battery.html                ← Battery health test
│   │   ├── trips.html                  ← Trip tracking + merge
│   │   ├── hondata.html                ← FlashPro maps + vehicle profile
│   │   ├── tuner.html                  ← Pro tuner view + CSV email (NEW)
│   │   ├── config.js                   ← API keys (Mapbox, Google, Spotify)
│   │   ├── GLTFLoader.js               ← Three.js model loader
│   │   └── *.glb, *.png, *.mp3        ← 3D models, images, audio
│   │
│   ├── java/com/example/hondadash/
│   │   ├── MainActivity.kt             ← App shell: WebViews, nav bar, BT, email
│   │   ├── BluetoothService.kt         ← OBD-II Bluetooth SPP + polling
│   │   ├── OBD2Parser.kt               ← ELM327 hex parser
│   │   └── DevicePickerDialog.kt       ← BT device picker dialog
│   │
│   ├── res/xml/file_paths.xml          ← FileProvider config for CSV email
│   └── AndroidManifest.xml             ← Permissions, services, FileProvider
│
├── app/build.gradle.kts                ← App dependencies (min SDK 24, target 36)
├── build.gradle.kts                    ← Project-level build config
└── README.md                           ← THIS FILE
```

---

## ALL 10 TABS

| # | Tab | File | What it does |
|---|-----|------|-------------|
| 1 | GAUGES | dashboard.html | 4 switchable gauge themes, RPM, VTEC warp, redline |
| 2 | VEHICLE | carview.html | 3D Honda Civic Si model, tap sensors for DTC codes |
| 3 | MULTI | multisystem.html | Pick PIDs from ECM/TCM/ABS/BCM, view all on one graph |
| 4 | MAPS | streetview.html | Waze-style nav with 3D car model on Mapbox |
| 5 | MUSIC | music.html | Spotify playback, visualizer, synced lyrics |
| 6 | DATALOG | datalog.html | Real-time ECU graphing, 12 params, CSV recording |
| 7 | BATTERY | battery.html | SOC ring gauge, voltage/CCA test, export report |
| 8 | TRIPS | trips.html | Trip cards with maps, stats, merge feature |
| 9 | HONDATA | hondata.html | FlashPro map details, vehicle spec sheet |
| 10 | TUNER | tuner.html | Pro tuner dashboard, AFR/knock/timing, CSV auto-email |

---

## SCREENS — DETAIL

### 1. GAUGES (dashboard.html)
- 4 switchable themes via settings panel (⚙ button):
  - **CYBERPUNK** — Red/black hacker terminal, scanlines, injector firing viz
  - **NEON** — Glowing cyan radial gauges (canvas-gauges library)
  - **RACING** — Orange arc gauges with glow, needles (raw Canvas 2D)
  - **STEEL** — Carbon fiber face, brushed metal bezels, glass reflection (raw Canvas 2D)
- Theme + gauge selection saved to localStorage
- Boot sequence animation, VTEC warp at 5800 RPM, redline shake at 7800
- RPM sim slider at bottom for testing
- Gauge picker: toggle Speed, Coolant, Throttle, O2, IAT, Timing, Fuel Trims, Battery, Injector Duty

### 2. VEHICLE (carview.html)
- Three.js 3D model with enhanced lighting
- Touch drag to rotate, pinch to zoom
- 6 sensor dots: O2, Coolant, TPS, MAP, VTEC, Knock
- Tap sensor → info panel with real Honda K20 DTC codes

### 3. MULTI (multisystem.html)
- Left sidebar: 4 expandable modules (ECM, TCM, ABS, BCM)
- 33 total PIDs across all modules
- Tap PIDs from any module to add to unified real-time graph
- Module colors: ECM=red, TCM=green, ABS=blue, BCM=amber

### 4. MAPS (streetview.html)
- Mapbox GL JS with Waze-style navigation UI
- 3D car model on map (2007 Honda Civic Si GLB)
- Turn-by-turn with maneuver icons, ETA, speed display

### 5. MUSIC (music.html)
- Spotify Web Playback SDK (requires Premium)
- Home screen: profile, playlists, top tracks, search, genre browse
- Now-playing: animated album art, floating particles, audio visualizer
- Synced lyrics from LRCLIB (free, no API key)

### 6. DATALOG (datalog.html)
- 4 hero gauges: RPM, AFR, MAP, Coolant Temp
- Multi-trace scrolling graph, 12 toggleable parameters
- REC button records sessions, exports CSV

### 7. BATTERY (battery.html)
- Big animated SOC ring gauge
- Readouts: voltage, CCA (640A rated), internal resistance
- START TEST button — animated test with PASS/MARGINAL/FAIL verdict
- Export text report

### 8. TRIPS (trips.html)
- Trip card list with thumbnail maps
- Stats: distance, duration, max/avg speed
- Trip merge functionality

### 9. HONDATA (hondata.html)
- Stock / Map 1 (Sport) selector
- Map details: VTEC point, rev limit, fuel, timing
- Vehicle profile table (16 specs)

### 10. TUNER (tuner.html) — NEW
- **Built to impress a tuner** — shows what they'd see on a dyno screen
- Left panel stacked gauges:
  - AFR with rich/lean/OK status + delta from stoich (14.7)
  - Knock count with timing retard display
  - Ignition timing, injector duty (with headroom %)
  - RPM with VTEC badge, MAP, IAT (heat soak warning), Coolant, Fuel trims
- Right panel: multi-trace scrolling graph with toggleable params
- Knock alert banner flashes red when knock count is high
- REC button records all tuner params
- **On tablet:** CSV auto-emails to mark.heymann01@gmail.com via Gmail
- **In browser:** CSV downloads as a file
- Simulates a spirited drive cycle for testing

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
| `updateFromFlashPro(data)` | datalog, tuner | Object: `{rpm:6904, vss:72, ...}` |
| `updatePosition(lng, lat, bearing, speed)` | streetview | GPS coords |
| `Android.emailCSV(csv, filename)` | tuner → Kotlin | CSV string, sends via Gmail |

---

## CSV AUTO-EMAIL (Tuner Page, Android Only)

When you hit STOP on the tuner REC button:
1. Kotlin saves CSV to cache dir
2. Opens Gmail with CSV attached
3. Pre-filled to: mark.heymann01@gmail.com
4. Subject: "HondaDash Tuner Log — [filename]"
5. One tap to send

In the browser, it just downloads the CSV file instead.

---

## ANDROID PROJECT

- **Package:** `com.example.hondadash`
- **Language:** Kotlin
- **Min SDK:** API 24 (Android 7.0)
- **Target SDK:** API 36
- **IDE:** Android Studio
- **Permissions:** Bluetooth, Location, Internet (all declared in manifest)
- **FileProvider:** Configured for CSV email attachments

---

## API KEYS & SERVICES

| Service | Purpose | Config Key |
|---------|---------|------------|
| Mapbox | Navigation map | `MAPBOX_TOKEN` in config.js |
| Spotify | Music playback | `SPOTIFY_CLIENT_ID` in config.js |
| LRCLIB | Synced lyrics (free) | No key needed |
| canvas-gauges | Neon gauge theme (CDN) | No key needed |

---

## TECH STACK

- **Frontend:** Vanilla HTML/CSS/JS (no frameworks)
- **Android:** Kotlin + WebView
- **3D:** Three.js (r128)
- **Maps:** Mapbox GL JS v3.17
- **Music:** Spotify Web Playback SDK
- **Gauges:** canvas-gauges (Neon theme), raw Canvas 2D (Racing + Steel themes)
- **Fonts:** Share Tech Mono, Orbitron, Nunito
- **Theme:** Dark, 4 switchable color schemes

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

- [ ] Nexus 7 arrives April 1 — test everything on real hardware
- [ ] Touch optimization pass — bigger tap targets for in-car use
- [ ] Test BluetoothService with real ELM327 adapter
- [ ] Test tuner CSV auto-email on tablet
- [ ] Real GPS integration for navigation
- [ ] Physical tablet install — measure dash opening
- [ ] EDFC5 Active install + dashboard integration
- [ ] Facelift front end paint + install
- [ ] GitHub Pages demo site updates

---

*Last updated: March 2026*
