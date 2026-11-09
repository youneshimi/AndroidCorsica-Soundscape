package corsica.universita.carnet.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import corsica.universita.carnet.data.SoundscapeContainer
import java.io.IOException
import retrofit2.HttpException

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        var retry = false
        try {
            SoundscapeContainer.repository.syncPending()
        } catch (error: IOException) {
            Log.w(TAG, "Upload will retry", error)
            retry = true
        } catch (error: HttpException) {
            Log.w(TAG, "Server rejected upload temporarily", error)
            retry = true
        }
        val repository = SoundscapeContainer.repository
        for (sound in repository.getPendingDeletes()) {
            try {
                repository.executeServerDelete(sound)
            } catch (error: IOException) {
                Log.w(TAG, "Deletion will retry", error)
                retry = true
            } catch (error: HttpException) {
                Log.w(TAG, "Server rejected deletion temporarily", error)
                retry = true
            }
        }
        return if (retry) Result.retry() else Result.success()
    }

    private companion object {
        const val TAG = "SyncWorker"
    }
}
