package jp.keita.kagurazaka.sample.coroutine

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.rxSingle
import org.jetbrains.anko.*
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
            val ten = async(CommonPool) { RxModel.returnTenAsync().await() }
            val twenty = async(CommonPool) { RxModel.returnTwentyAsync().await() }
            message += "result = ${ten.await() * twenty.await()}"
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
