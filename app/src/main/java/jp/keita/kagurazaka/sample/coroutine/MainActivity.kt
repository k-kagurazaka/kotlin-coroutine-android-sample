package jp.keita.kagurazaka.sample.coroutine

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.rxSingle
import org.jetbrains.anko.button
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.onClick
import org.jetbrains.anko.verticalLayout
import kotlin.system.measureTimeMillis

object AsyncModel {
    fun returnTenAsync() = async(CommonPool) {
        delay(1000)
        return@async 10
    }

    fun returnTwentyAsync() = async(CommonPool) {
        delay(2000)
        return@async 20
    }
}

object RxModel {
    fun returnTenAsync() = rxSingle(CommonPool) {
        delay(1000)
        return@rxSingle 10
    }

    fun returnTwentyAsync() = rxSingle(CommonPool) {
        delay(2000)
        return@rxSingle 20
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var serialButton: Button

    private lateinit var parallelButton: Button

    private lateinit var rxButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            serialButton = button("Serial") {
                onClick { onSerialButtonClick() }
            }.lparams(width = matchParent)

            parallelButton = button("Parallel") {
                onClick { onParallelButtonClick() }
            }.lparams(width = matchParent)

            rxButton = button("Rx") {
                onClick { onRxButtonClick() }
            }.lparams(width = matchParent)

            button("Touch for checking freeze!")
        }
    }

    fun onSerialButtonClick() = launch(UI) {
        setButtonsEnabled(false)

        var message = ""
        val elapsed = measureTimeMillis {
            val ten = AsyncModel.returnTenAsync().await()
            val twenty = AsyncModel.returnTwentyAsync().await()
            message += "result = ${ten * twenty}"
        }

        showToast("$message, time = $elapsed [ms]")
        setButtonsEnabled(true)
    }

    fun onParallelButtonClick() = launch(UI) {
        setButtonsEnabled(false)

        var message = ""
        val elapsed = measureTimeMillis {
            val ten = AsyncModel.returnTenAsync()
            val twenty = AsyncModel.returnTwentyAsync()
            message += "result = ${ten.await() * twenty.await()}"
        }

        showToast("$message, time = $elapsed [ms]")
        setButtonsEnabled(true)
    }

    fun onRxButtonClick() = launch(UI) {
        setButtonsEnabled(false)

        var message = ""
        val elapsed = measureTimeMillis {
            val ten = RxModel.returnTenAsync()
            val twenty = RxModel.returnTwentyAsync()
            // message += "result = ${ten.await() * twenty.await()}" // serial
            message += "result = ${ten.zipWith(twenty) { t1, t2 -> t1 * t2 }.await()}" // parallel
        }

        showToast("$message, time = $elapsed [ms]")
        setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        serialButton.isEnabled = enabled
        parallelButton.isEnabled = enabled
        rxButton.isEnabled = enabled
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

// I desire RxKotlin2...
fun <T, U, R> Single<T>.zipWith(other: SingleSource<U>, zipper: (T, U) -> R)
        = zipWith(other, BiFunction { t1: T, t2: U -> zipper(t1, t2) })
