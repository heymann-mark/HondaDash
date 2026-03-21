# HONDA DASH — PROJECT README
## 2007 Honda Civic Si FA5 // CP2077 Dashboard App

---

## PROJECT OVERVIEW

Custom Android dashboard app for a 2007 Honda Civic Si (FA5, K20Z3).
Built in Android Studio (Kotlin + WebView). UI is HTML/CSS/JS loaded via WebView.
Live OBD2 data from Hondata FlashPro via Bluetooth.

---

## ANDROID PROJECT

- **Location:** `C:\Users\Owner\AndroidStudioProjects\HondaDash`
- **Package:** `com.example.hondadash`
- **Language:** Kotlin
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** API 36
- **IDE:** Android Studio Panda 2

---

## ASSETS FOLDER

Location: `app/src/main/assets/`

| File | Description | Source |
|------|-------------|--------|
| `dashboard.html` | Main gauge dashboard + boot sequence + VTEC warp | Built here |
| `carview.html` | 3D car viewer with diagnostic sensor overlay | Built here |
| `chassis.html` | CP2077 undercarriage schematic with build specs | Built here |
| `honda__civic__sedan_2009.glb` | 3D car model (blue 2006 coupe) | Sketchfab (free download) |
| `GLTFLoader.js` | Three.js GLB loader | Downloaded from: `https://raw.githubusercontent.com/mrdoob/three.js/r128/examples/js/loaders/GLTFLoader.js` |

---

## 3D MODEL — SOURCE

**File:** `honda__civic__sedan_2009.glb`
**Note:** Double underscore between "honda" and "civic", double underscore between "civic" and "sedan", single underscore before "2009"

**Where to find it:**
- Site: **Sketchfab.com**
- Search terms used: `Honda Civic FD2`, `Honda Civic coupe 2006`
- Downloaded as a `.zip` — the GLB is inside the `source/` folder
- Model is a blue 2006 Honda Civic coupe (close enough to FA5 sedan for our purposes)

**Other places to search for better models:**
- Sketchfab.com — search `Honda Civic FD2 detailed` or `Honda Civic undercarriage`
- Free3D.com
- TurboSquid.com (filter free)
- Poly.pizza
- GrabCAD.com (engineering community, very detailed models)
- GTA5-mods.com — needs OpenIV + Blender to extract and convert to GLB
- RaceDepartment.com — Assetto Corsa mods, high quality, needs Blender conversion

---

## JAVASCRIPT LIBRARIES

| Library | Version | CDN URL |
|---------|---------|---------|
| Three.js | r128 | `https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js` |
| GLTFLoader | r128 | Downloaded locally — see assets table above |

---

## FONTS

Both loaded from Google Fonts CDN (requires internet):
- **Orbitron** (bold, 700) — used for numbers and titles
- **Share Tech Mono** — used for labels and UI text

---

## SCREENS

### 1. GAUGES (dashboard.html)
- CP2077 boot sequence — hex scramble resolves into live gauges
- Live gauges: RPM (full width), Speed, Coolant, Throttle, Gear, O2
- **Redline effect** at 7800 RPM — screen shake, border flash, red overlay
- **VTEC warp effect** at 5800 RPM — persistent star warp, "VTEC ENGAGED" text slam, blue RPM bars
- Sim buttons in RPM box: REDLINE / VTEC / RESET

### 2. VEHICLE (carview.html)
- Three.js 3D model of the car
- Touch drag to rotate, pinch to zoom
- 6 sensor dots on model: O2, Coolant, TPS, MAP, VTEC, Knock
- Tap sensor dot → info panel with real Honda K20 DTC codes
- Right panel: fault sim buttons — cycles through real DTC codes per sensor
- Amber glowing/pulsing dots when fault active
- `updateDTC(codes)` function ready for Kotlin Bluetooth bridge

### 3. CHASSIS (chassis.html)
- CP2077 red wireframe schematic of undercarriage — top-down view
- Tappable components: engine, transmission, coilovers, exhaust, steering, brakes, fuel tank, subframes, wheels, Hondata
- Each component shows YOUR actual build specs (Tein Flex Z, Enkei RPF1, APEXi WS3, etc.)

---

## KOTLIN BRIDGE — HOW DATA FLOWS

FlashPro Bluetooth → `BluetoothService.kt` → decodes OBD2 hex → `MainActivity.kt` → `webView.evaluateJavascript("updateGauges(rpm, spd, clt, tps, o2)", null)`

### Functions ready to receive live data:
- `updateGauges(rpm, spd, clt, tps, o2)` — in dashboard.html
- `updateDTC(codes)` — in carview.html (array of DTC code strings e.g. `["P0136", "P2646"]`)

---

## OBD2 — FLASHPRO DATA

FlashPro uses ELM327 protocol over Bluetooth.

Key OBD2 PIDs:
| PID | Data | Formula |
|-----|------|---------|
| `010C` | RPM | `((A*256)+B)/4` |
| `010D` | Speed (km/h) | `A` |
| `0105` | Coolant temp | `A - 40` (°C) |
| `0111` | Throttle % | `(A/255)*100` |
| `0115` | O2 sensor voltage | `B/200` |

VTEC crossover: **5800 RPM** (K20Z3)
Redline: **8200 RPM** (K20Z3, stock)

---

## CAR SPECS — 2007 HONDA CIVIC Si FA5

| Spec | Value |
|------|-------|
| Engine | K20Z3, 2.0L DOHC i-VTEC |
| Power | 197hp @ 7800 RPM |
| Torque | 139 lb-ft @ 6200 RPM |
| Transmission | 6-speed manual |
| Redline | 8200 RPM |
| VTEC crossover | ~5800 RPM |
| Color | Nighthawk Black Pearl |
| Mileage | ~120k miles |

---

## BUILD MODS (as of early 2026)

| Component | Part | Status |
|-----------|------|--------|
| Coilovers | Tein Flex Z | Installed  |
| Wheels | Enkei RPF1 18x9.5 +38 | Installed |
| Tires | 245/40R18 | TBD brand |
| Exhaust | APEXi WS3 cat-back | Installed |
| ECU | Hondata FlashPro | Installed |
| Steering rack | OEM replacement | Installed |
| Shifter | Acuity components | Installed |
| EDFC5 Active | TEIN controller | Pending install |
| Front end | Facelift conversion (JP Auto bumper, Autoelements grille, Evan Fischer trim) | Ordered, to be painted Nighthawk Black Pearl |
| Steering wheel | OEM 8th gen | Purchased, pending alcantara wrap |
| Engine swap | K24A2 TSX longblock (K20Z3 head hybrid) | Planned — targeting ~260whp |

---
## TODO

- [ ] Add chassis.html as third tab in MainActivity.kt
- [ ] Wire up Hondata FlashPro Bluetooth (BluetoothService.kt)
- [ ] Add Mapbox navigation screen (fourth tab, CP2077 styled)
- [ ] Add boot MP3 audio
- [ ] Find better 3D model with detailed undercarriage
- [ ] Physical tablet install — measure dash opening
- [ ] Sort audio wiring (Bluetooth receiver module → factory speaker harness)
- [ ] EDFC5 Active install
- [ ] Facelift front end paint + install
- [ ] Alcantara steering wheel wrap

---

*Last updated: March 2026*
