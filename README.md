# Amrapali GM Visitor Management

A visitor management system for a residential society. The Android app is used at the gate to register visitors and take their photo; a Node backend stores the records and sends a WhatsApp alert to the flat owner; a small web dashboard shows recent visitors.

## Components

- `app/` — Android app (Kotlin, Jetpack Compose). The guard enters visitor details, takes a photo, and submits it to the backend.
- `backend/` — Node.js (Express) server. Stores visitor records per month under `backend/data/`, serves the web dashboard, and sends WhatsApp notifications via whatsapp-web.js.
- `website/` — Static dashboard served by the backend. Shows today's visitors, flats visited, and visitor details.
- `scripts/` — Utility script that generates launcher icons from `assets/logo.png`.

## Requirements

- Android Studio (AGP 8.10, Kotlin 2.0, minSdk 24)
- Node.js 18+
- A WhatsApp account to link via QR code on the first backend start

## Setup

Backend:

```bash
cd backend
cp .env.example .env
npm install
npm start
```

On the first start a QR code appears in the terminal — scan it with WhatsApp (Linked Devices). The session is saved in `backend/src/.wwebjs_auth`, so later starts don't need a new scan.

The web dashboard is served by the backend at `http://<server>:2222/`.

Android app: open the project in Android Studio and build the `app` module. In the app's settings screen, set the base URL to your backend server (for example `http://192.168.1.10:2222/`). Flat contacts live in `backend/data/flat-contacts.json` — visitor alerts are sent to the numbers listed there.

## Configuration

Backend (`backend/.env`):

- `PORT` — server port, default `2222`

Android app: the base URL is configured in the in-app settings screen.

Web dashboard: if you open the dashboard from somewhere other than the backend server, set `API_URL` at the top of `website/script.js`.

## Project structure

```
app/        Android app (Kotlin + Compose)
backend/    Express server + WhatsApp notifications
website/    Dashboard frontend (HTML/CSS/JS)
scripts/    Icon generation helper
assets/     Source logo used for launcher icons
```

## Notes

- Visitor records and photos are stored locally on the server under `backend/data/` — there's no external database.
- `backend/data/`, `backend/uploads/`, and `backend/src/.wwebjs_auth/` are gitignored because they contain real visitor data and WhatsApp session credentials.
