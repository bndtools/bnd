@file:Suppress("unused")

package org.gradle.kotlin.dsl

import org.gradle.api.tasks.bundling.Jar
import aQute.bnd.gradle.BundleTaskExtension

val Jar.bundle: BundleTaskExtension
    get() = the()

fun Jar.bundle(configure: BundleTaskExtension.() -> Unit) =
    extensions.configure(BundleTaskExtension.NAME, configure)
