// javac --release 16 MinMax.java
record MinMax<T>(@Foo T min, T max) { }

@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface Foo {}
