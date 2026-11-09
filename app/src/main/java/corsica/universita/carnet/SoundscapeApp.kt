package corsica.universita.carnet

import android.app.Application
import corsica.universita.carnet.data.SoundscapeContainer
import corsica.universita.carnet.notifications.createNotificationChannels
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

class SoundscapeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        SoundscapeContainer.init(this)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM", "Token: $token")
        }
    }
}
