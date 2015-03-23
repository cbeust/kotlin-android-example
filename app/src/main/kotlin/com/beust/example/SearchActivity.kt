package com.beust.example

import android.app.Activity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import com.google.gson.JsonObject
import kotlinx.android.synthetic.activity_search.addFriendButton
import kotlinx.android.synthetic.activity_search.addStatus
import kotlinx.android.synthetic.activity_search.editText
import kotlinx.android.synthetic.activity_search.loading
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.android.view.OnClickEvent
import rx.android.view.ViewObservable
import rx.android.widget.OnTextChangeEvent
import rx.android.widget.WidgetObservable
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

trait Server {
    fun findUser(name: String) : Observable<JsonObject>
    fun addFriend(user: User) : Observable<JsonObject>
}

class MockServer : Server {
    private fun createOk(key: String? = null, value: String? = null) : JsonObject {
        return create("ok", key, value)
    }

    private fun createError(key: String? = null, value: String? = null) : JsonObject {
        return create("error", key, value)
    }

    fun isOk(jo: JsonObject) : Boolean {
        return jo.get("status").getAsString() == "ok"
    }

    private fun create(status: String, key: String?, value: String?) : JsonObject {
        val result = JsonObject()
        result.addProperty("status", status)
        if (key != null) {
            result.addProperty(key, value)
        }
        return result
    }

    override fun addFriend(user: User) : Observable<JsonObject> {
        val result: JsonObject
        if (user.id == "123") {
            result = createOk()
        } else {
            result = createError()
        }
        return Observable.just(result)
    }

    override fun findUser(name: String) : Observable<JsonObject> {
        val result: JsonObject
        if (name == "cedric" || name == "jon") {
            result = createOk("id", if (name == "cedric") "123" else "456")
            result.addProperty("name", "cedric")
        } else {
            result = createError()
        }
        Thread.sleep(2000)
        return Observable.just(result)
    }
}

data class User(val id: String, val name: String)

class SearchActivity : Activity() {
    private val TAG = "SearchActivity"
    val mServer = MockServer()
    /** Called whenever a new character is type */
    val mNameObservable: BehaviorSubject<String> = BehaviorSubject.create()
    /** Called whenever we receive a response from the server about a name */
    val mUserObservable: BehaviorSubject<JsonObject> = BehaviorSubject.create()
    var mUser: User? = null

    fun mainThread() : String = "Main thread: " + (Looper.getMainLooper() == Looper.myLooper())

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Whenever a new character is typed
        WidgetObservable.text(editText)
            .doOnNext { e: OnTextChangeEvent ->
                addFriendButton.setEnabled(false)
                loading.setVisibility(View.INVISIBLE)
            }
            .map { e: OnTextChangeEvent -> e.text().toString() }
            .filter { s: String -> s.length() >= 3 }
            .debounce(500, TimeUnit.MILLISECONDS)
            .subscribe { s: String -> mNameObservable.onNext(s) }

        // We have a new name to search, ask the server about it (on the IO thread)
        mNameObservable
            .observeOn(Schedulers.io())
            .subscribe{ s: String ->
                Log.d(TAG, "Sending to server: ${s} " + mainThread())
                mServer.findUser(s).subscribe { jo: JsonObject ->
                    mUserObservable.onNext(jo)
                }
            }
        // ... and show our loading icon (on the main thread)
        mNameObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                s: String -> loading.setVisibility(View.VISIBLE)
            }

        // Manage the response from the server to "Search"
        mUserObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { jo: JsonObject ->
                val hasResult = mServer.isOk(jo)
                addFriendButton.setEnabled(hasResult)
                loading.setVisibility(View.INVISIBLE)
                mUser = if (hasResult) {
                    User(jo.get("id").getAsString(), jo.get("name").getAsString())
                } else {
                    null
                }
        }

        // When the user presses the "Add friend" button
        ViewObservable.clicks(addFriendButton)
            .subscribe { e: OnClickEvent ->
                mServer.addFriend(mUser!!)
                    .subscribe { jo: JsonObject ->
                        if (mServer.isOk(jo)) {
                            addStatus.setText("Friend added")
                            editText.setText("")
                        } else {
                            addStatus.setText("Friend not added")
                        }
                    }
            }
    }

}