package com.beust.example

import android.app.Activity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.google.gson.JsonObject
import kotlinx.android.synthetic.activity_search.addStatus
import kotlinx.android.synthetic.activity_search.editText
import kotlinx.android.synthetic.activity_search.searchButton
import retrofit.RestAdapter
import rx.Observable
import rx.android.view.OnClickEvent
import rx.android.view.ViewObservable
import rx.android.widget.OnTextChangeEvent
import rx.android.widget.WidgetObservable
import rx.subjects.BehaviorSubject

trait Server {
    fun findUser(name: String) : Observable<JsonObject>
    fun addFriend(user: User) : Observable<JsonObject>
}

class MockServer : Server {
    override fun addFriend(user: User) : Observable<JsonObject> {
        val result = JsonObject()
        if (user.id == "123") {
            result.addProperty("status", "ok")
        } else {
            result.addProperty("status", "error")
        }
        return Observable.just(result)
    }

    override fun findUser(name: String) : Observable<JsonObject> {
        val result = JsonObject()
        if (name == "cedric" || name == "jon") {
            result.addProperty("status", "ok")
            result.addProperty("id", if (name == "cedric") "123" else "456")
            result.addProperty("name", "cedric")
        } else {
            val result = JsonObject()
            result.addProperty("status", "error")
        }
        return Observable.just(result)
    }
}

data class User(val id: String, val name: String)

class SearchActivity : Activity() {
    val TAG = "SearchActivity"
    val mServer = MockServer()
    val mNameObservable: BehaviorSubject<String> = BehaviorSubject.create()
    var mUser: User? = null

    fun mainThread() : String = "Main thread: " + (Looper.getMainLooper() == Looper.myLooper())

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchButton.setEnabled(false)

        ViewObservable.clicks(searchButton)
            .subscribe{ (e: OnClickEvent) ->
                mServer.addFriend(mUser!!)
                    .subscribe{ (jo: JsonObject) ->
                        if (jo.get("status").getAsString() == "ok") {
                            addStatus.setText("Friend added")
                        } else {
                            addStatus.setText("Friend not added")
                        }
                    }
            }

        mNameObservable.subscribe {(s: String) ->
            Log.d(TAG, "Sending to server: ${s}")
            mServer.findUser(s).subscribe {(jo: JsonObject) ->
                val hasResult = jo.has("id")
                searchButton.setEnabled(hasResult)
                if (hasResult) {
                    mUser = User(jo.get("id").getAsString(), jo.get("name").getAsString())
                } else {
                    mUser = null
                }
                Log.d(TAG, "Response from server: ${jo}")
            }
        }

        WidgetObservable.text(editText)
            .map{ e: OnTextChangeEvent ->
                Log.d(TAG, "Event: ${e}")
                e.text().toString()
            }
            .filter{s : String ->
                Log.d(TAG, "filtering")
                s.length() >= 3 }
//            .debounce(500, TimeUnit.MILLISECONDS)
            .subscribe{s: String ->
                Log.d(TAG, "flatMap: " + mainThread())
                mNameObservable.onNext(s)
            }
       }
}