package com.safemode.safekeepingforffx.ui.screens.settings

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

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
            ?: throw IOException("Couldn't open that location for writing.")
        stream.use { it.write(text.toByteArray()) }
    }
}

fun contentSource(resolver: ContentResolver, uri: Uri): BackupSource = BackupSource {
    withContext(Dispatchers.IO) {
        val stream = resolver.openInputStream(uri)
            ?: throw IOException("Couldn't open that file.")
        stream.use { readBackupText(it) }
    }
}

/**
 * The most a restore will read. The picker isn't filtered to .json (see the Settings screen), so a
 * video picked by mistake would otherwise be read whole into memory. A route carrying every
 * character's path across the full grid is about 200 KB in a backup, so this still fits a library
 * of around a hundred of them on top of everything else.
 */
const val MAX_BACKUP_BYTES = 20 * 1024 * 1024

/** Thrown for a file over [MAX_BACKUP_BYTES]. The message is written to be shown as-is. */
class BackupTooLargeException : IOException("That file is too large to be a Safekeeping backup.")

/**
 * Reads [stream] as UTF-8, giving up with [BackupTooLargeException] as soon as it passes [limit]
 * rather than after reading the whole file. Counted while reading because providers don't reliably
 * report a file's size up front.
 */
fun readBackupText(stream: InputStream, limit: Int = MAX_BACKUP_BYTES): String {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val read = stream.read(buffer)
        if (read < 0) break
        if (out.size() + read > limit) throw BackupTooLargeException()
        out.write(buffer, 0, read)
    }
    return String(out.toByteArray(), Charsets.UTF_8)
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
