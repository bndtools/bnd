// javac --enable-preview --release 15 MinMax.java
record MinMax<T>(@Foo T min, T max) { }

@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface Foo {}
