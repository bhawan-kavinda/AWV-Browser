# Awv browser (Android WebView) — Part 3/5: Menu + Data

Open this folder in Android Studio (Koala+ / JDK 17), let Gradle sync, then Run.

## Done so far
**Part 1** – project setup, theme, SVG icons (drawn natively), sunset wallpaper, home screen.
**Part 2** – real browsing and tabs:
- Omnibox on the start page: text is a URL (`example.com`, `http://localhost:3000`, `192.168.1.1`) → opened; anything else → Google search.
- Browsing page: address bar (lock icon, host while idle / full URL while editing, reload ⇄ stop), progress bar, WebView, bottom bar.
- Back / Forward (back from the first page returns to the start page, forward goes back to the page), new tab, tab-count badge.
- Tabs overlay: Normal / Private switch, tab cards with live thumbnails, close (✕), Done, New tab (creates a tab of the selected kind).
- Private tabs: purple badge + dark address bar + "Private tab" label on their start page.
- Load-error screen (offline, DNS, bad certificate, renderer crash) with "Try again".
- `target=_blank` / `window.open` (only after a user tap) open in a new tab; `mailto:` `tel:` `intent:` links go to the matching app.
- Fullscreen page video, Android Back handling (video → tabs overlay → page history).

**Part 3** – menu + data:
- Menu bottom sheet (⋮): Bookmark this page, Desktop site, New private tab, Bookmarks, History, Downloads.
- Bookmarks and History in Room (`data/AwvDb.kt`); History groups by day, skips Private tabs and error pages, 500 newest kept in the list.
- Downloads via `DownloadManager` (public Downloads folder on Android 10+, app folder on older versions); list shows live progress, tap opens, trash/✕ deletes/cancels.
- Desktop site per tab: desktop user-agent + 1024px viewport, then reload.
- New build deps: Room 2.6.1 + KSP 2.0.20-1.0.25.

## Structure
- `browser/BrowserTab.kt`        per-tab state (url, title, progress, error, thumbnail, WebView)
- `browser/BrowserController.kt` all tabs + WebView setup, clients, navigation, thumbnails
- `browser/UrlUtils.kt`          URL-or-search detection, host display
- `ui/browser/BrowserApp.kt`     root: start page / web page / tabs overlay / video fullscreen
- `ui/browser/BrowserPage.kt`    address bar, progress strip, WebView host, error view
- `ui/tabs/TabsOverlay.kt`       tabs screen
- `ui/home`, `ui/components`, `ui/theme`  from Part 1 (small additions: Lock/Reload/Globe icons, shared `awvText`)

## Manifest changes
`usesCleartextTraffic="true"` (so `http://` sites load) and `configChanges` (rotation keeps your tabs).

## Not wired yet (later parts)
Menu sheet, bookmarks, history, downloads, desktop site (Part 3) · ad/tracker blocking, shield, real stats, true Private isolation of cookies/cache (Part 4) · voice, QR scan, shortcut tiles, settings, search-engine choice, icon, release build (Part 5).

Tabs live in memory only: they are not restored if Android kills the app process.
