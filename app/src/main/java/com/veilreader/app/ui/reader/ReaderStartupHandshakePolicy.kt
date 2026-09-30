package com.veilreader.app.ui.reader

/**
 * Startup is a three-party handshake between the durable Reader session, the Android lifecycle and
 * the navigator. Locator/lifecycle work must not run against a half-open session.
 */
internal fun shouldCollectReaderLocator(
    sessionReady: Boolean,
    navigatorAttached: Boolean
): Boolean =
    sessionReady && navigatorAttached

internal fun shouldResumeReaderAfterOpen(
    sessionReady: Boolean,
    lifecycleResumed: Boolean
): Boolean =
    sessionReady && lifecycleResumed

internal fun shouldFlushStartupLocatorInBackground(
    sessionReady: Boolean,
    lifecycleResumed: Boolean,
    initialLocatorCommitAccepted: Boolean
): Boolean =
    sessionReady && !lifecycleResumed && initialLocatorCommitAccepted
