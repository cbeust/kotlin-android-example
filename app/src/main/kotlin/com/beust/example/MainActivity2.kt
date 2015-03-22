package com.beust.example

import android.content.Intent
import android.os.Bundle
import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.util.Log
import com.google.gson.JsonObject
import retrofit.RestAdapter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

import kotlinx.android.synthetic.activity_main2.textView

open class MainActivity2 : Activity() {
    val TAG = "MainActivity2"
    val HOST = "http://api.openweathermap.org"

    fun kelvinToFahrenheit(k: Float) =
            (k - 273.15) * 1.8 + 32.0

    fun jsonToTemp(jo : JsonObject) =
            kelvinToFahrenheit(jo.get("main").getAsJsonObject().get("temp").getAsFloat())

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        var restAdapter = RestAdapter.Builder()
                .setEndpoint(HOST)
                .build()

        val service = restAdapter.create(javaClass<Weather>())

        service.weather()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { (s : JsonObject) : Unit ->
                    textView.setText(s.toString())
                    Log.d(TAG, "Received weather " + s)
                } )

//        Log.d(TAG, "Starting yeah: " + w)
        val o = Observable.just("foo", "bar")
        o
                .map( { s: String -> s.toUpperCase()})
                .subscribe( { s: String -> Log.d(TAG, "Yeah: " + s) } )
        val next: Button = findViewById(R.id.searchButton) as Button
        next.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(view: View) {
                val intent: Intent = Intent()
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        })
    }

//    public override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main_activity2, menu)
//        return true
//    }
}
