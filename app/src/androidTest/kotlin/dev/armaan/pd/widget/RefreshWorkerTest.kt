package dev.armaan.pd.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** SPEC §6.2 refresh. */
@RunWith(AndroidJUnit4::class)
class RefreshWorkerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun refreshWorker_returnsSuccess() = runBlocking {
        val worker = TestListenableWorkerBuilder<RefreshWorker>(context).build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }

    @Test
    fun uniqueWork_isEnqueuedOnceAfterTwoAppStarts() {
        val work = WorkManager.getInstance(context)
        // The process under test already started once; these are two more starts.
        RefreshWorker.enqueue(context)
        val first = work.getWorkInfosForUniqueWork(RefreshWorker.NAME).get().single()
        RefreshWorker.enqueue(context)
        val infos = work.getWorkInfosForUniqueWork(RefreshWorker.NAME).get()

        assertEquals(1, infos.size)
        // KEEP: the second start leaves the first request in place rather than replacing it.
        assertEquals(first.id, infos.single().id)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        assertEquals(TimeUnit.MINUTES.toMillis(30), infos.single().periodicityInfo?.repeatIntervalMillis)
    }
}
