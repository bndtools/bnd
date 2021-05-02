module com.example.foo {
    requires java.logging;
    requires java.compiler;

    requires transitive java.desktop;

    exports toexport;
    exports toexporttosomeone to java.logging, java.naming;

    opens toopen;
    opens toopentosomeone to java.logging, java.naming;

    uses foo.Foo;
    provides foo.Foo with foo.FooImpl;
}
