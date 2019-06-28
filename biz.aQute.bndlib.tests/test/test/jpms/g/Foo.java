package test.jpms.g;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(Cloneable.class)
public class Foo implements Cloneable {}
