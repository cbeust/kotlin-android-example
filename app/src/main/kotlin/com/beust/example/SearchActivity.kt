package com.beust.example

import android.app.Activity
import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.google.gson.JsonObject
import kotlinx.android.synthetic.activity_search.addFriendButton
import kotlinx.android.synthetic.activity_search.editText
import kotlinx.android.synthetic.activity_search.list
import kotlinx.android.synthetic.activity_search.loading
import rx.Observable
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.android.view.OnClickEvent
import rx.android.view.ViewObservable
import rx.android.widget.OnTextChangeEvent
import rx.android.widget.WidgetObservable
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

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
        result.addProperty("name", user.name)
        result.addProperty("id", user.id)
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
        Thread.sleep(1000)
        return Observable.just(result)
    }
}

data class User(val id: String, val name: String)

class SearchActivity : LoaderManager.LoaderCallbacks<Cursor>, Activity() {
    private val TAG = "SearchActivity"
    val mServer = MockServer()
    /** Called whenever a new character is typed */
    val mNameObservable: BehaviorSubject<String> = BehaviorSubject.create()
    /** Called whenever we receive a response from the server about a name */
    val mUserObservable: BehaviorSubject<JsonObject> = BehaviorSubject.create()
    var mUser: User? = null

    fun mainThread() : String = "Main thread: " + (Looper.getMainLooper() == Looper.myLooper())

    private var mApp: MainApplication by Delegates.notNull()
    private var mAdapter: LogCursorAdapter by Delegates.notNull()

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super<Activity>.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        mApp = getApplication() as MainApplication
        mAdapter = LogCursorAdapter(this, mApp.dbHelper.getLogs())

        list.setAdapter(mAdapter)
        getLoaderManager().initLoader(0, null, this)

        //        WidgetObservable.text(editText)
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .flatMap { e: OnTextChangeEvent ->
//                    Log.d(TAG, "flatMap: " + mainThread())
//                    val o = Observable.just("foo").delay(3, TimeUnit.SECONDS)
//                            .subscribeOn(Schedulers.io())
//                    o.subscribe { s: String ->
//                        Log.d(TAG, "Result of just: ${s}   ${mainThread()}")
//                    }
//                    o
//                }
//                .doOnNext { s -> app.log("Character: ${s}")  }
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe{ o: String ->
//                    Log.d(TAG, "Subscribe ${mainThread()} received ${o}")
//                }

        // Whenever a new character is typed
        WidgetObservable.text(editText)
            .doOnNext { e: OnTextChangeEvent ->
                addFriendButton.setEnabled(false)
                loading.setVisibility(View.INVISIBLE)
            }
            .map { it.text().toString() }
            .filter { it.length() >= 3 }
            .debounce(500, TimeUnit.MILLISECONDS)
            .subscribe {
                mNameObservable.onNext(it)
            }

        mNameObservable
            .observeOn(Schedulers.computation())
            .subscribe{
                mApp.log("Character: ${it}")
//                getLoaderManager().getLoader(0).forceLoad();
//                mAdapter.notifyDataSetChanged()
//                mAdapter.getCursor().requery()
            }

        // We have a new name to search, ask the server about it (on the IO thread)
        mNameObservable
            .observeOn(Schedulers.io())
            .subscribe{
                Log.d(TAG, "Sending to server: ${it} " + mainThread())
                mServer.findUser(it).subscribe {
                    mUserObservable.onNext(it)
                }
            }
        // ... and show our loading icon (on the main thread)
        mNameObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                loading.setVisibility(View.VISIBLE)
            }

        // Manage the response from the server to "Search", turn the JsonObject into a User,
        // if the response is "ok"
        mUserObservable
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                if (mServer.isOk(it)) {
                    User(it.get("id").getAsString(), it.get("name").getAsString())
                } else {
                    null
                }
            }
            .subscribe {
                Log.d(TAG, "Enabling add friend")
                addFriendButton.setEnabled(it != null)
                Log.d(TAG, "Hiding loading")
                loading.setVisibility(View.INVISIBLE)
                mUser = it
            }

        // If the user presses the "Add friend" button, we know we have a valid User: send
        // the "Add friend" request to the server
        ViewObservable.clicks(addFriendButton)
            .subscribe { e: OnClickEvent ->
                mServer.addFriend(mUser!!)
                    .subscribe { jo: JsonObject ->
                        val toastText: String
                        if (mServer.isOk(jo)) {
                            toastText = "Friend added id: " + jo.get("id").getAsString()
                            editText.setText("")
                        } else {
                            toastText = "ERROR: Friend not added"
                        }
                        val toast = Toast.makeText(this, toastText, Toast.LENGTH_LONG)
                        toast.setGravity(Gravity.TOP, 0, 200)
                        toast.show()
                    }
            }
    }

    companion object {
        class DbCursorLoader(val app: MainApplication, context: Context) : CursorLoader(context) {
            override fun loadInBackground() : Cursor {
                return app.dbHelper.getLogs()
            }
        }
    }
    override fun onCreateLoader(p0: Int, p1: Bundle?): Loader<Cursor>? {
        return DbCursorLoader(mApp, this)
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, cursor: Cursor?) {
        mAdapter.swapCursor(cursor)
    }

    override fun onLoaderReset(p0: Loader<Cursor>?) {
        mAdapter.swapCursor(null)
    }
}