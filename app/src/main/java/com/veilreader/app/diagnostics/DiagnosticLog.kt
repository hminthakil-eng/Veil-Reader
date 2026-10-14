package com.veilreader.app.diagnostics

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import timber.log.Timber

/**
 * Owned Timber sinks keep the existing diagnostic tags and synchronous QA markers.
 * They are not planted in Timber's global forest: enabling third-party Readium
 * debug trees would also emit publication JavaScript and change hot-path logging.
 */
internal object DiagnosticLog {
    private val sinks = ConcurrentHashMap<String, Timber.Tree>()

    private fun sink(tag: String): Timber.Tree = sinks.computeIfAbsent(tag) { fixedTag ->
        object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // The platform writer is the final sink of this owned Timber tree.
                Log.println(priority, fixedTag, message)
            }
        }
    }

    fun debug(tag: String, message: String) { sink(tag).d(message) }
    fun info(tag: String, message: String) { sink(tag).i(message) }
    fun warn(tag: String, message: String) { sink(tag).w(message) }
    fun error(tag: String, message: String, error: Throwable? = null) {
        sink(tag).e(error, message)
    }
}
