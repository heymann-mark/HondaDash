# HONDA DASH
## 2007 Honda Civic Si FA5 // Dashboard App

---

## PROJECT OVERVIEW

Custom Android dashboard app for a 2007 Honda Civic Si (FA5, K20Z3).
Built in Android Studio (Kotlin + WebView). UI is HTML/CSS/JS loaded via WebView.
Live OBD2 data from Hondata FlashPro via Bluetooth.
Runs on a Nexus 7 tablet mounted in the dash.

---

## WHAT'S NEW (v2)

### App Shell + Persistent Navigation
- New `index.html` shell with iframe-based page switching
- Bottom nav bar: GAUGES, VEHICLE, MAPS, MUSIC
- Pages stay loaded in memory — switching tabs doesn't kill state
- Music keeps playing while navigating between all screens

### Spotify Music Player + Visualizer
- Full Spotify Premium integration via Web Playback SDK
- Browser becomes a Spotify speaker called "HondaDash"
- Album art, track name, artist display with animated multi-layer background
- Real-time audio visualizer (bars mode) using tab audio capture
- Synced lyrics from LRCLIB — highlights current line as song plays
- Playback controls: play/pause, prev, next, seek, volume slider
- Browse panel: search, liked songs, playlists, recently played
- Animated background: 3-layer parallax album art with Ken Burns effect, floating particles, audio-reactive color pulse
- Blue-to-pink color gradient on visualizer

### Waze-Style Navigation (streetview.html)
- Redesigned from GTA-style to clean Waze-like UI
- Turn-by-turn directions with maneuver icons
- ETA display, Waze-blue route line
- Camera offset so car sits in lower third of screen
- Brake model swap: holds brake button → shows brake light GLB model
- Two preloaded car models (default + brake) for instant visual swap

### 3D Vehicle Diagnostic (carview.html)
- Increased lighting and tone mapping for better car visibility
- Ambient light 6.0, key light 7.0, fill 3.0, added top light
- Tone mapping exposure increased to 1.4

### Config System
- `config.js` stores API keys: Mapbox, Google Maps, Spotify Client ID
- Centralized configuration for all pages

---

## ANDROID PROJECT

- **Package:** `com.example.hondadash`
- **Language:** Kotlin
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** API 36
- **IDE:** Android Studio

---

## SCREENS

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
- `updateDTC(codes)` function ready for Kotlin Bluetooth bridge

### 3. MAPS (streetview.html)
- Mapbox GL JS with Waze-style navigation UI
- 3D car model on map (2007 Honda Civic Si GLB)
- Brake model swap — separate GLB with brake lights, hidden underneath default model
- Turn-by-turn with maneuver icons, ETA, speed display
- Route: Waltham to TD Garden demo route
- `updatePosition(lng, lat, bearing, speed)` for real GPS data

### 4. MUSIC (music.html)
- Spotify Web Playback SDK — streams music through the browser
- Real-time audio visualizer via tab audio capture (digital stream, not mic)
- Multi-layer animated album art background (deep blur + mid + front layers)
- Floating particles that react to audio energy
- Audio-reactive color pulse overlay
- Synced lyrics from LRCLIB (LRC format with timestamps)
- Browse: search tracks/playlists, liked songs, recently played
- Volume slider with mute toggle
- Prev / Play-Pause / Next / Seek controls

---

## ASSETS

| File | Description |
|------|-------------|
| `index.html` | App shell — iframe nav, keeps music alive across pages |
| `dashboard.html` | Gauge dashboard with boot sequence |
| `carview.html` | 3D car diagnostic viewer |
| `streetview.html` | Waze-style navigation map |
| `music.html` | Spotify player + visualizer + lyrics |
| `chassis.html` | Undercarriage schematic with build specs |
| `config.js` | API keys (Mapbox, Spotify, etc.) |
| `GLTFLoader.js` | Three.js GLB model loader |
| `sky.png` | Background image for car viewer |
| `2007_honda_civic_si.glb` | Default car model |
| `2007_honda_civic_si_brake.glb` | Brake lights car model |
| `honda__civic__sedan_2009.glb` | Original car model (map view) |

---

## API KEYS & SERVICES

| Service | Purpose | Config Key |
|---------|---------|------------|
| Mapbox | Navigation map + 3D car model on map | `MAPBOX_TOKEN` |
| Spotify | Music playback, browse, lyrics | `SPOTIFY_CLIENT_ID` |
| LRCLIB | Synced lyrics (free, no key needed) | N/A |

Spotify setup:
1. Create app at https://developer.spotify.com/dashboard
2. Enable Web API + Web Playback SDK
3. Add redirect URI matching your host
4. Copy Client ID to `config.js`
5. Requires Spotify Premium for Web Playback SDK

---

## KOTLIN BRIDGE — HOW DATA FLOWS

FlashPro Bluetooth → `BluetoothService.kt` → decodes OBD2 hex → `MainActivity.kt` → `webView.evaluateJavascript()`

### Functions ready for live data:
- `updateGauges(rpm, spd, clt, tps, o2)` — dashboard.html
- `updateDTC(codes)` — carview.html (e.g. `["P0136", "P2646"]`)
- `updatePosition(lng, lat, bearing, speed)` — streetview.html

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

- [ ] Wire up Hondata FlashPro Bluetooth
- [ ] Physical tablet install — measure dash opening
- [ ] Real GPS integration for navigation
- [ ] EDFC5 Active install + dashboard integration
- [ ] Facelift front end paint + install
- [ ] Alcantara steering wheel wrap

---

*Last updated: March 2026*
