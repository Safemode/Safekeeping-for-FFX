package com.safemode.safekeepingforffx.ui.screens.settings

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Where a backup's JSON goes and comes from.
 *
 * The ViewModel drives backup and restore but never learns what a content URI is: it hands the
 * document to one of these, and the screen supplies an implementation bound to whatever file the
 * player picked. That keeps the ViewModel free of Android framework types, which is the rule the
 * rest of the app follows, and makes both directions straightforward to test with a plain string.
 */
fun interface BackupSink {
    suspend fun write(text: String)
}

fun interface BackupSource {
    suspend fun read(): String
}

/**
 * Writes to a document the player chose through the system file picker.
 *
 * Opened in `"wt"` mode - truncate - because picking an existing backup to overwrite would
 * otherwise leave the tail of the longer old file behind and produce unreadable JSON.
 */
fun contentSink(resolver: ContentResolver, uri: Uri): BackupSink = BackupSink { text ->
    withContext(Dispatchers.IO) {
        val stream = resolver.openOutputStream(uri, "wt")
            ?: throw java.io.IOException("Couldn't open that location for writing.")
        stream.use { it.write(text.toByteArray()) }
    }
}

fun contentSource(resolver: ContentResolver, uri: Uri): BackupSource = BackupSource {
    withContext(Dispatchers.IO) {
        val stream = resolver.openInputStream(uri)
            ?: throw java.io.IOException("Couldn't open that file.")
        stream.use { it.bufferedReader().readText() }
    }
}

/**
 * The picked file's name as the player would recognise it, so the overwrite warning can say which
 * file is about to replace their progress. A content URI's own path is provider gibberish
 * (`msf:1000000031`), so the name has to be queried for.
 *
 * Null when the provider doesn't offer one - the warning simply drops the line rather than showing
 * something meaningless. This is a tiny metadata query on a URI the picker just handed back, so it
 * runs inline; nothing is read from the file itself until the player confirms.
 */
fun displayName(resolver: ContentResolver, uri: Uri): String? = runCatching {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
    }
}.getOrNull()
