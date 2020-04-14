// compiled with
// javac --enable-preview --release 14 MinMax.java
record MinMax<T>(@Foo T min, T max) { }

@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface Foo {}
