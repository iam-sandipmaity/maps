# Telecom Architecture & Upstream Sync Guide

This document explains the software architecture of the telecom enhancements in this repository and details how the codebase automatically synchronizes with the upstream CoMaps repository on Codeberg every 7 days.

---

## 1. Upstream-Safe Modular Architecture

A primary design requirement of this fork is that **upstream CoMaps updates must never break telecom functionality or cause merge conflicts**.

### 1.1 Code Isolation
All custom telecom code is strictly isolated inside the package:
```
android/app/src/main/java/app/organicmaps/telecom/
├── db/          # SQLite database helper (TelecomDatabaseHelper)
├── importer/    # Excel (.xlsx) and CSV batch parsers & exporters
├── map/         # Native BookmarkManager KML layer manager (TelecomMapLayerManager)
├── model/       # TelecomSite data model & schemas
├── navigation/  # External navigation intents & BRouter/GraphHopper road snapper (TelecomRoadSnapper)
├── planner/     # Nearest-Neighbor & 2-Opt TSP visit route optimizer (VisitPlannerManager)
└── ui/          # Site details, search, visit planner, and data management activities
```

### 1.2 Pristine Core Guarantee
* **C++ Core Engine (`libs/*`):** **0 lines modified.** The offline C++ Dijkstra/A* routing graph (`index_router.cpp`), features road graph, and binary `.mwm` container parsers are completely untouched.
* **Core Java Routing (`RoutingController.java`, `RoutingPlanController.java`):** **0 lines modified.**
* **Core Map Rendering (`drape`, OpenGL shaders):** **0 lines modified.**

### 1.3 Minimal Integration Touchpoints
The telecom system integrates with the rest of CoMaps through only two minimal points:
1. **`AndroidManifest.xml`:** Registers the telecom activities and intent filters.
2. **`MwmActivity.java`:** Adds 3 menu items to the bottom navigation sheet:
   * **Telecom Search** (`TelecomSearchActivity`)
   * **Visit Planner** (`TelecomVisitPlannerActivity`)
   * **Telecom Data Management** (`TelecomDataManagementActivity`)

Because upstream CoMaps does not have the `app/organicmaps/telecom` directory, Git merges upstream updates cleanly with **zero conflicts**.

---

## 2. Automated 7-Day Upstream Sync (GitHub Actions)

The repository includes an automated workflow at [`.github/workflows/upstream-sync.yaml`](.github/workflows/upstream-sync.yaml) that runs on GitHub Actions.

### 2.1 Schedule
* **Frequency:** Every 7 days (every Sunday at 00:00 UTC).
* **Cron Expression:** `0 0 * * 0`.

### 2.2 How It Works
1. **Fetches Upstream:** Checks out `main` and fetches the latest commits from upstream Codeberg:
   ```
   https://codeberg.org/comaps/comaps.git (branch: main)
   ```
2. **Compares Commits:** Checks if Codeberg has new commits ahead of this fork.
3. **Automated Merge:**
   * If new commits exist, runs `git merge upstream/main`.
   * Pushes the merged codebase directly to `origin/main` on GitHub.
4. **Triggers Automated Build:** Pushing to `main` automatically triggers `.github/workflows/android-build.yaml`, building a fresh Android APK (`fdroidDebug`) containing both the latest upstream improvements and your telecom features.
5. **Safety Abort on Conflict:** If a conflict ever occurs on a shared file, the workflow safely runs `git merge --abort`, posts an alert, and leaves the `main` branch completely untouched and stable.

### 2.3 Manual Trigger (On-Demand Sync)
You can trigger an upstream sync at any time without waiting for the 7-day schedule:
1. Go to your repository on GitHub: `github.com/iam-sandipmaity/maps`.
2. Click the **Actions** tab.
3. In the left sidebar, click **Upstream Codeberg Sync**.
4. Click **Run workflow** ➔ Select branch `main` ➔ Click the green **Run workflow** button.

---

## 3. Manual Local Sync via Terminal

If you ever wish to sync manually from your local development machine:

```bash
# 1. Ensure you are on the main branch with a clean working tree
git checkout main
git pull origin main

# 2. Fetch the latest code from Codeberg upstream
git fetch upstream main

# 3. Merge upstream changes into your main branch
git merge upstream/main -m "chore(sync): manual sync with upstream Codeberg main"

# 4. Push the updated codebase to GitHub
git push origin main
```

---

## 4. Key Telecom Features Overview

### 4.1 BRouter / GraphHopper Off-Road Road Snapper (`TelecomRoadSnapper`)
* **Problem:** In rural telecom operations, towers are often located in fields, hills, or unmapped private tracks >150m from OpenStreetMap roads. Passing raw coordinates into CoMaps' offline C++ router aborts the route calculation with `INTERMEDIATE_POINT_NOT_FOUND` (12).
* **Solution:**
  * Calculates orthogonal road corridor projections for each off-road site.
  * Passes the calculated **Road Access Point** coordinate into CoMaps' router, ensuring 100% road-snapping success.
  * In the map overlay, renders a blue pin at the road access point (`[3-Road] Access for Site 3`) and an amber approach connector line (`#approach-track`) leading directly to the physical tower pin.

### 4.2 Multi-Stop Visit Planner & TSP Optimizer
* Supports adding up to dozens of telecom sites to an itinerary.
* **TSP Optimization:** Uses Nearest-Neighbor and 2-Opt heuristic optimization to order sites for the shortest driving distance from the technician's current GPS location.
* **Itinerary Export:**
  * **CoMaps:** Native offline turn-by-turn road route with numbered pins (`[1]`, `[2]`, ... `[N]`).
  * **Google Maps:** Direct deep link opening complete multi-stop routes with satellite view.
  * **Mappls (MapmyIndia):** Turn-by-turn navigation for next stop.

### 4.3 Intelligent Telecom Search
* Tokenized search engine handling SAP IDs (e.g. `I-WB-KGPO-ENB-9030`), unspaced queries (`kgpo9030`, `slbn9036`), RailTel sites (`RT01`, `RT10`), and COW sites.
* Automatically ignores non-critical technology codes (`-ENB-`, `-ESC-`, `-ILA-`, `-I-`, etc.) to match technician queries instantly.
