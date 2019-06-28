package test.jpms.f;

import aQute.bnd.annotation.spi.ServiceConsumer;

@ServiceConsumer(Foo.class)
@ServiceConsumer(Integer.class)
public class FooFactory3 {}
