@file:Suppress("unused")

package org.gradle.kotlin.dsl

import aQute.bnd.gradle.Bundle
import aQute.bnd.gradle.BundleTaskExtension

val Bundle.bundle: BundleTaskExtension
    get() = the()

fun Bundle.bundle(configure: BundleTaskExtension.() -> Unit) =
    extensions.configure(BundleTaskExtension.NAME, configure)
