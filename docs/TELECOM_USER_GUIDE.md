# CoMaps Telecom Management System - User & Field Operation Guide

**Platform:** CoMaps (Offline GIS & Navigation for Android)  
**Edition:** Telecom Site Mapping & Field Navigation Edition  
**Data Privacy:** 100% Offline, Strict Local Storage, Zero Cloud Dependencies  

---

## 1. Overview & Security Guarantee

The CoMaps Telecom Edition transforms offline OpenStreetMap mapping into an end-to-end field engineering and infrastructure tracker tool.

> [!IMPORTANT]
> **Strict Offline Security & Zero Bundled Data:**  
> The application contains **zero pre-bundled telecom sites or credentials**. All site databases, technician assignments, and equipment specifications are imported directly by the end-user on-device. No location telemetry, equipment details, or site identifiers are ever transmitted over the network.

---

## 2. Key Capabilities & Features

### 📍 Instant Map Visualisation
- **Native Placemarks:** Thousands of imported telecom sites are rendered onto the offline vector map using CoMaps' native high-performance rendering engine (hardware clustering, 60fps culling, and smooth zoom).
- **Interactive Details:** Tapping any telecom map placemark reveals its SAP ID, JC/Cluster, equipment specs (EB, SMPS, Battery, DG), and technician phone numbers.

### 🔍 Smart Offline Search & Radius Filtering
- **Multi-Token Query Engine:** Rapidly locate sites by:
  - Exact or partial SAP ID (e.g. `9036`, `KGPU-ENB-9036`)
  - Wildcard token sets (e.g. `kgpo 9030`, `slbn 9036`, `egro 9006`)
  - Site type, location, cluster, or technician name
- **Live Radius Filter:**
  - One-tap distance chips: `< 100 m`, `< 500 m`, `< 1 km`, `< 5 km`, `< 10 km`, `Custom...`, and `All Sites`.
  - Calculates Haversine distances relative to current GPS location.
  - "Show on Map" button dynamically plots only the sites within the chosen perimeter.

### 🏢 Cascading Filters (JC & Technician)
- **Expandable Filter Bar:** Tap the filter icon next to the search box to toggle advanced filters.
- **Cascading Filter Logic:**
  - Selecting a **JC** (e.g., *Kharagpur*) immediately isolates sites in that JC.
  - The **Tech** dropdown automatically cascades to list **only technicians active in the chosen JC**.
  - Reset button to quickly clear active filters.

### 🚗 Multi-Stop Visit Planning & Route Optimization
- Build daily field inspection plans with multiple sites.
- **2-Opt TSP Optimizer:** Solves the Travelling Salesperson Problem locally to calculate the optimal driving sequence, minimizing travel time and fuel consumption.
- Syncs the ordered stops as a visual numbered route layer directly onto the map.

### ⚡ Navigation & Dispatch
- Direct one-tap navigation via **Google Maps Turn-by-Turn** or **Internal CoMaps Offline Routing**.
- One-tap phone dialer for technicians and supervisors (`tel:` intents).
- One-tap coordinate copying to clipboard in standardized decimal degrees (`lat, lon`).

---

## 3. Getting Started: Importing Site Data

### Supported File Formats
You can import your site tracker directly from your device storage:
1. **CSV (Comma-Separated Values):** Standard `.csv` spreadsheet export with header rows.
2. **Excel (.xlsx):** You can use the included conversion utility (`tools/telecom/convert_tracker.py`) to convert multi-sheet workbooks into standard CSV prior to import, or import clean CSV exports directly.

### Import Instructions
1. Open CoMaps and tap the **Menu** button.
2. Navigate to **Telecom Site Search** or **Telecom Data Management**.
3. Select **Import Sites from CSV**.
4. Choose your import mode:
   - **Add New:** Adds new sites without modifying existing ones.
   - **Update Existing:** Overwrites existing site IDs with updated fields while preserving other records.
   - **Replace All:** Completely replaces the local database with the imported file.
5. Tap **Sync All Sites to Map Placemarks** to generate map pins.

---

## 4. GitHub Release & APK Deployment

This repository includes automated CI/CD for Android builds:
- **Automatic APK Release:** Whenever changes are pushed to `main` or a release tag (`v*`) is created, GitHub Actions compiles the APK (`FdroidDebug` / `arm64` / universal) and uploads the installable `.apk` file directly to the **GitHub Releases** page.
- **Artifact Downloads:** Pre-release test builds are also available as downloadable zip artifacts from the GitHub Actions run summary.

---

## 5. Upstream Upgrades & Compatibility

This telecom module was designed strictly following the **Zero Upstream Conflict** architecture:
- All telecom logic, database helpers, models, and custom activities reside strictly within `app.organicmaps.telecom.*`.
- Native layouts and resources are self-contained.
- The root `README.md` and core C++ Drape engine files remain 100% untouched to ensure that the automated weekly upstream Codeberg sync runs cleanly without merge conflicts.
