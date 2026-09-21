package com.veilreader.app.manga.challenge

import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChallengeForegroundHostRegistry : ChallengeLaunchPolicy {

    data class Registration internal constructor(
        val id: String,
        internal val token: Any
    )

    private var active: WeakReference<Any>? = null
    private var activeId: String? = null

    private val _foreground = MutableStateFlow(false)
    val foreground: StateFlow<Boolean> = _foreground.asStateFlow()

    @Synchronized
    fun register(token: Any = Any()): Registration {
        val id = UUID.randomUUID().toString()
        active = WeakReference(token)
        activeId = id
        _foreground.value = true
        return Registration(id, token)
    }

    @Synchronized
    fun unregister(registration: Registration) {
        if (activeId != registration.id) return
        active = null
        activeId = null
        _foreground.value = false
    }

    @Synchronized
    override fun canLaunchInteractiveChallenge(): Boolean {
        val present = active?.get() != null
        if (!present) {
            active = null
            activeId = null
            _foreground.value = false
        }
        return present
    }
}