package com.veilreader.app.ui.screens

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import java.util.Collections
import java.util.WeakHashMap
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Makes Readium's constructor-bound navigator fragments safe during Activity restoration.
 *
 * Android restores FragmentManager state inside Activity.onCreate(), before Compose can rebuild
 * the publication-bound navigator factory. Readium provides dummy factories specifically for this
 * window. We let FragmentManager instantiate a dummy, mark it, then remove it immediately after
 * super.onCreate() and before onResume. ReaderScreen subsequently creates a fresh real navigator
 * from the persisted locator and the newly opened Publication.
 */
@OptIn(ExperimentalReadiumApi::class)
internal object ReaderFragmentRestoration {
    private val restoredDummies = Collections.newSetFromMap(
        WeakHashMap<Fragment, Boolean>()
    )

    val fragmentFactory: FragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            val fragment = when (className) {
                EpubNavigatorFragment::class.java.name ->
                    EpubNavigatorFragment.createDummyFactory()
                        .instantiate(classLoader, className)

                PdfNavigatorFragment::class.java.name ->
                    PdfNavigatorFragment.createDummyFactory(PdfiumEngineProvider())
                        .instantiate(classLoader, className)

                else -> return super.instantiate(classLoader, className)
            }

            synchronized(restoredDummies) {
                restoredDummies.add(fragment)
            }
            return fragment
        }
    }

    fun discardRestoredDummies(fragmentManager: FragmentManager) {
        val dummies = synchronized(restoredDummies) {
            fragmentManager.fragments.filter { it in restoredDummies }
        }
        if (dummies.isEmpty()) return

        fragmentManager.beginTransaction().apply {
            dummies.forEach(::remove)
        }.commitNowAllowingStateLoss()

        synchronized(restoredDummies) {
            restoredDummies.removeAll(dummies.toSet())
        }
    }
}
