package com.beust.example

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import kotlin.properties.Delegates

class MainApplication : Application() {

    var dbHelper: DbHelper by Delegates.notNull()
    var db: SQLiteDatabase by Delegates.notNull()

    override fun onCreate() {
        dbHelper = DbHelper(this)
        db = dbHelper.getWritableDatabase()
    }

    fun log(text: String) {
        dbHelper.log(text)
    }

    fun getCursor() {
        dbHelper.getLogs()
    }
}
