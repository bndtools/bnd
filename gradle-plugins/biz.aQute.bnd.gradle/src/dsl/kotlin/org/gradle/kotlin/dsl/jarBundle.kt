@file:Suppress("unused")

package org.gradle.kotlin.dsl

import aQute.bnd.gradle.BundleTaskExtension
import org.gradle.api.tasks.bundling.Jar

val Jar.bundle: BundleTaskExtension
    get() = the()

fun Jar.bundle(configure: BundleTaskExtension.() -> Unit) =
    extensions.configure(BundleTaskExtension.NAME, configure)
