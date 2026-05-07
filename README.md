# FitnessDataPuller Mobile

FitnessDataPuller Mobile is a tiny, read-only Android sync app for sending Health Connect data from a phone or watch ecosystem to the existing FitnessDataPuller backend.

- **Backend base URL:** `https://fitnessdatapuller.vercel.app`
- **Sync endpoint:** `POST /api/sync/samsung`
- **Auth header:** `Authorization: Bearer SYNC_API_KEY`

The app is intentionally manual-only: it does not schedule background jobs, and it never writes data back to Health Connect.

## What it reads

The app requests read-only Health Connect permissions for:

- Steps
- Active and total calories burned
- Distance and floors climbed
- Exercise sessions/workouts, including Hevy workouts when Hevy writes them to Health Connect
- Nutrition macros and calories, including Cronometer entries when Cronometer writes them to Health Connect
- Hydration
- Sleep sessions
- Heart rate, resting heart rate, HRV, oxygen saturation, and respiratory rate from Samsung Health/Galaxy wearables or any other Health Connect source
- Weight

FitnessDataPuller reads from Health Connect as the single source of truth. Samsung Health/Galaxy wearables, Hevy, Cronometer, and other apps must be connected to Health Connect and allowed to write the relevant records before this app can read and send them.

Cumulative values such as steps, calories, distance, exercise duration, nutrition, and hydration are read with Health Connect aggregate APIs so overlapping records are not accidentally double-counted.

## Payload

`Sync Today` sends this shape to `/api/sync/samsung`:

```json
{
  "date": "YYYY-MM-DD",
  "steps": 28000,
  "active_calories": 900,
  "total_calories": 2450.5,
  "distance_miles": 7.25,
  "floors_climbed": 14,
  "exercise_minutes": 65,
  "workout_count": 1,
  "workouts": [
    {
      "title": "Push day",
      "exercise_type": 80,
      "start_time": "ISO timestamp",
      "end_time": "ISO timestamp",
      "duration_minutes": 65,
      "source": "app package"
    }
  ],
  "nutrition": {
    "calories": 2100,
    "protein_g": 160,
    "carbs_g": 225,
    "fat_g": 70,
    "sugar_g": 45,
    "fiber_g": 30,
    "sodium_mg": 2200
  },
  "hydration_liters": 2.6,
  "sleep_hours": 7.4,
  "sleep_quality": "unknown",
  "weight_lbs": 153.2,
  "resting_hr": 58,
  "avg_hr": 72,
  "hrv_rmssd_ms": 42.5,
  "oxygen_saturation_pct": 97.8,
  "respiratory_rate": 14.2,
  "sources": ["com.sec.android.app.shealth"],
  "source_updated_at": "ISO timestamp",
  "ai_summary": "Synced from Health Connect sources including Samsung Health/Galaxy wearables, Hevy, Cronometer, and any other connected apps that wrote today's permitted data."
}
```

Unit conversions performed by the app:

- Calories are converted to kcal.
- Weight is converted to pounds.
- Sleep duration is converted to hours.
- `resting_hr` uses Health Connect resting heart rate when present and falls back to the average heart-rate sample for today when a direct resting value is not available.
- Missing optional values are still shown as `null` in the JSON preview and sync body so it is clear which connected source did not provide data for today.

## Run in Android Studio

1. Clone/open this repository in Android Studio.
2. Let Android Studio sync the Gradle project.
3. Select an Android device or emulator running Android 9/API 28 or newer.
4. Press **Run**.

This project uses Kotlin, Jetpack Compose, and the Jetpack Health Connect client.

## Required Health Connect setup

1. On the Android phone, install or enable **Health Connect**.
2. Connect Samsung Health/Galaxy wearables, Hevy, Cronometer, or another health source to Health Connect.
3. In Health Connect, confirm those apps have write access and FitnessDataPuller has read access for the requested categories.
4. Make sure Health Connect contains data for today.
5. Launch FitnessDataPuller Mobile.
6. Tap **Request permissions**.
7. Grant the requested read permissions.

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
