# Petinder

A native Android pet adoption app built with Kotlin, Jetpack Compose, and a FastAPI backend. Swipe to pass or save, undo your last choice, browse pet profiles, find nearby pets using GPS, open shelter directions, and submit adoption applications.

The interface uses original offline vector pet illustrations, a cream/coral/sage palette, and four tabs: Discover, Nearby, Saved, and Profile. It runs in a labeled demo mode when the API is unavailable. Demo favorites are stored separately from server favorites; demo mode never pretends to submit an application.

## Start the backend

Python 3.11 or newer is required. From the repository root in PowerShell:

```powershell
python -m venv backend/.venv
backend/.venv/Scripts/python -m pip install -r backend/requirements.txt
backend/.venv/Scripts/python -m uvicorn backend.main:app --host 0.0.0.0 --port 8000 --reload
```

Interactive API documentation: <http://localhost:8000/docs>. The backend seeds four **sample** pets in Chicago. SQLite data persists in `backend/petinder.db`; set `PETINDER_DB` to select another file. Only hashes of randomly generated bearer session tokens are stored server-side.

For the exact Python dependency versions used in verification, install `backend/requirements.lock.txt` instead of `backend/requirements.txt`.

## Run Android

Open this project in Android Studio, sync Gradle, and run `app` on an emulator or device (Android 7.0/API 24 or newer). The current project uses Android SDK 37 and Gradle 9.6; its generated daemon criteria select Java 25, available in the bundled Android Studio JBR.

- Emulator: the default backend URL is `http://10.0.2.2:8000`.
- Physical device: connect to the same Wi-Fi as the backend computer, allow port 8000 through the local firewall, and set `http://<computer-lan-ip>:8000` in **Profile → Backend URL → Connect & refresh**.
- Alternatively, run `adb reverse tcp:8000 tcp:8000` and set the URL to `http://127.0.0.1:8000` for a USB-connected device.
- Tap the location in the header or **Nearby → Use my current location** to request foreground location permission. Approximate location is supported. No background tracking is used. If no GPS fix is available, the app reports the issue and retains the selected location.
- The sample catalog is in Chicago. GPS elsewhere can legitimately show no nearby pets; Profile lets you return to the Chicago sample location.
- HTTP is permitted in **debug builds only**. Use an HTTPS backend for release builds.
- On Android 17/API 37, connecting to a local backend requests the [local network runtime permission](https://developer.android.com/privacy-and-security/local-network-permission). Tap **Connect & refresh** in Profile and allow it; demo mode remains usable if access is declined.

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
backend/.venv/Scripts/python -m unittest discover -s backend/tests -v
```

## API

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/health` | Health and demo catalog indicator |
| POST | `/sessions` | Create an anonymous device session |
| GET | `/pets` | Browse; optional `lat`, `lon`, `radius_km`, `species` |
| GET | `/pets/{id}` | Pet details |
| GET | `/me/decisions` | Restore swipe history |
| PUT / DELETE | `/me/decisions/{id}` | Save/pass or undo |
| GET | `/me/favorites` | Saved pets |
| POST | `/me/applications` | Validate and store an application |
| GET | `/me/applications` | Track the current session's applications |

All `/me` routes require `Authorization: Bearer <token>`. An application takes `pet_id`, `name`, `email`, and a `message` of 20–2000 characters. Duplicate applications return 409; missing pets return 404. Geographic distance uses the Haversine formula, with kilometers throughout. The Android client applies the same distance and category filtering locally to the small loaded catalog; the API also supports those filters for other clients.

## Prototype boundaries

This is a runnable adoption MVP, not a live shelter marketplace. Listings, shelter names, fees, and map coordinates are sample data; directions are approximate sample pins. Applications are stored for the current device session with status `submitted`; no shelter email, staff review, adoption approval, payment, messaging, pet posting, or mutual pet matching is implemented. Clearing Android app data loses the anonymous session; there is no account recovery or cross-device login. Before using real applicant data, add account authentication/recovery, staff access controls, rate limiting, retention/deletion policies, verified shelter listings, and production HTTPS hosting.
