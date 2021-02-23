package com.example

class Example {
    fun interface Greet {
        fun greet(): String
    }
    class Nested : Greet {
        override fun greet() = "Hello World!"
    }
    val world : String = "Hello World!"
    inner class Inner : Greet {
        override fun greet() = world
    }
    fun anonClass() {
        @Suppress("UNUSED_VARIABLE")
        val anon = object : Greet {
          override fun greet() = world
        }
    }
    fun localClass() {
        class Local  : Greet {
           override fun greet() = world
        }
        @Suppress("UNUSED_VARIABLE")
        val local = Local()
    }
    fun lambdaClass() {
        @Suppress("UNUSED_VARIABLE")
        val lambda = Greet { world }
    }
}

