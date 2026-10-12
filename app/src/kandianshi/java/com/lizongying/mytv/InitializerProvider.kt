package com.lizongying.mytv

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri

internal class InitializerProvider : ContentProvider() {

    // Happens before Application#onCreate.It's fine to init something here
    override fun onCreate(): Boolean {
        SP.init(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ) = unsupported()

    override fun getType(uri: Uri) = unsupported()

    override fun insert(uri: Uri, values: ContentValues?) = unsupported()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) =
        unsupported()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = unsupported()

    private fun unsupported(errorMessage: String? = null): Nothing =
        throw UnsupportedOperationException(errorMessage)
}
