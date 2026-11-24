package ru.fishplaces

import android.app.Application
import androidx.room.Room
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil
import ru.fishplaces.data.SpotDatabase
import ru.fishplaces.data.SpotRepository
import java.io.File

class FishingApplication : Application() {
    val firebaseReady get() = com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()
    val authRepository by lazy { ru.fishplaces.auth.FirebaseAuthRepository(com.google.firebase.auth.FirebaseAuth.getInstance()) }
    val cloudSpots by lazy {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()).build()
        ru.fishplaces.data.FirestoreSpots(db)
    }
    // A single database and repository for the whole process; no DI framework needed.
    val database by lazy {
        Room.databaseBuilder(this, SpotDatabase::class.java, "fishing-spots.db").build()
    }
    val repository by lazy { SpotRepository(database.spotDao()) }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        HttpRequestUtil.setOkHttpClient(
            OkHttpClient.Builder()
                // Honour server cache headers. Do not prefetch tiles or force revalidation.
                .cache(Cache(File(cacheDir, "osm-http"), 50L * 1024 * 1024))
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder()
                        .header("User-Agent", "FishPlacesCoursework/1.0 (Android; ru.fishplaces)")
                        .build())
                }.build()
        )
    }
}
