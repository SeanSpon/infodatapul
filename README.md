# FitnessDataPuller Mobile

FitnessDataPuller Mobile is a tiny, read-only Android sync app for sending Health Connect data from a phone or watch ecosystem to the existing FitnessDataPuller backend.

- **Backend base URL:** `https://fitnessdatapuller.vercel.app`
- **Sync endpoint:** `POST /api/sync/samsung`
- **Auth header:** `Authorization: Bearer SYNC_API_KEY`

The app is intentionally manual-only: it does not schedule background jobs, and it never writes data back to Health Connect.

## What it reads

The app requests read-only Health Connect permissions for:

- Steps
- Active calories burned
- Sleep sessions
- Heart rate
- Weight

Steps and active calories are read with Health Connect aggregate APIs. This is especially important for cumulative values such as steps so overlapping records are not accidentally double-counted.

## Payload

`Sync Today` sends this shape to `/api/sync/samsung`:

```json
{
  "date": "YYYY-MM-DD",
  "steps": 28000,
  "active_calories": 900,
  "sleep_hours": 7.4,
  "sleep_quality": "unknown",
  "weight_lbs": 153.2,
  "resting_hr": 58,
  "source_updated_at": "ISO timestamp",
  "ai_summary": "Synced from Health Connect."
}
```

Unit conversions performed by the app:

- Calories are converted to kcal.
- Weight is converted to pounds.
- Sleep duration is converted to hours.
- `resting_hr` uses the average heart-rate sample for today when a direct resting heart-rate value is not available.

## Run in Android Studio

1. Clone/open this repository in Android Studio.
2. Let Android Studio sync the Gradle project.
3. Select an Android device or emulator running Android 9/API 28 or newer.
4. Press **Run**.

This project uses Kotlin, Jetpack Compose, and the Jetpack Health Connect client.

## Required Health Connect setup

1. On the Android phone, install or enable **Health Connect**.
2. Connect Samsung Health or another health source to Health Connect.
3. Make sure Health Connect contains data for today.
4. Launch FitnessDataPuller Mobile.
5. Tap **Request permissions**.
6. Grant the requested read permissions.

Health Connect can only return data that exists in Health Connect and that the user has allowed this app to read.

## Configure API URL and SYNC_API_KEY

1. Open the **Settings** tab.
2. Set **API Base URL**. The default is:

   ```txt
   https://fitnessdatapuller.vercel.app
   ```

3. Paste your backend `SYNC_API_KEY`.
4. Tap **Save settings locally**.

Settings are stored locally on the device in app private storage. Do not commit real API keys to this repository.

## Test sync

1. Open the **Sync** tab.
2. Confirm the permission card says all requested read permissions are granted.
3. Tap **Sync Today**.
4. Inspect the JSON preview.
5. Confirm the last sync result shows an HTTP 2xx response.
6. Refresh the FitnessDataPuller dashboard in Vercel.

If sync fails, the app displays the backend response or connection error in the **Error** card.
