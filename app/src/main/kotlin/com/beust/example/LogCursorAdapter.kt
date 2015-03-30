package com.beust.example

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView

class LogCursorAdapter(val context: Context, cursor: Cursor)
        : CursorAdapter(context, cursor, true) {
    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(R.layout.item_log, parent, false);

    }

    private fun getString(cursor: Cursor, key: String) : String {
        return cursor.getString(cursor.getColumnIndexOrThrow(key))
    }

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val tvText = view.findViewById(R.id.tvText) as TextView
        tvText.setText(getString(cursor, DbHelper.TEXT))

        val tvTimestamp = view.findViewById(R.id.tvTimestamp) as TextView
        tvTimestamp.setText(getString(cursor, DbHelper.TIMESTAMP))
    }

}
