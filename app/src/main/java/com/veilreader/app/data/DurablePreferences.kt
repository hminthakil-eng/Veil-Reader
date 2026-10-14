package com.veilreader.app.data

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.veilreader.app.diagnostics.DiagnosticLog

/**
 * A checked synchronous barrier for migrations, milestones and backup restore.
 * AndroidX edit(commit = true) returns Unit, so it cannot preserve the Boolean
 * disk-write result required by restore rollback and flushDurably().
 * Only this adapter uses the platform editor; ordinary writes use AndroidX edit.
 */
@SuppressLint("UseKtx")
internal fun SharedPreferences.commitDurably(
    update: SharedPreferences.Editor.() -> Unit
): Boolean {
    val editor = edit()
    editor.update()
    val committed = editor.commit()
    if (!committed) DiagnosticLog.error("VeilPreferences", "Synchronous preference write failed")
    return committed
}
