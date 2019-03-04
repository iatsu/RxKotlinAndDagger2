package com.iatsu.rxkotlinanddagger2

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import kotlinx.android.synthetic.main.activity_main.*
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import dagger.Component
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import javax.inject.Inject

class Dagger @Inject constructor() {
    val text = "Is this the Dagger you are looking for?"
}

@Component
interface MainComponent {
    fun inject(app: MainActivity)
}

class MainActivity : AppCompatActivity() {
    @Inject lateinit var dagger: Dagger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DaggerMainComponent.create().inject(this)
        textView.text = dagger.text

        val buttonFlowable = buttonFlowable()
        val editTextFlowable = editTextFlowable()
        val searchTextFlowable = Flowable.merge<String>(buttonFlowable, editTextFlowable)

        var subscription = searchTextFlowable
            //.observeOn(Schedulers.io()) //if the next call required I/O
            .doOnNext { progressBar.visibility = View.VISIBLE } //called every time a new data is emitted
            .map { it + "mapping" }
            //.observeOn(AndroidSchedulers.mainThread()) // to get back to the main thread
            .subscribe { changedText ->
                Handler().postDelayed({
                    textView.text = changedText
                    progressBar.visibility = View.GONE
                }, 300)
            }
    }

    private fun buttonFlowable(): Flowable<String>{
        return Flowable.create<String>({ emitter ->
            button.setOnClickListener {
                emitter.onNext(editText.text.toString())
            }
            emitter.setCancellable {
                button.setOnClickListener(null)
            }
        }, BackpressureStrategy.BUFFER)
    }

    private fun editTextFlowable(): Flowable<String> {
        return Flowable.create<String>({ emitter ->
            val textChangedListener = object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    emitter.onNext(s.toString())
                }
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            }
            editText.addTextChangedListener(textChangedListener)

            emitter.setCancellable {
                editText.removeTextChangedListener(textChangedListener)
            }
        }, BackpressureStrategy.BUFFER).filter { it.length >= 2 }.debounce(300, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
    }
}
