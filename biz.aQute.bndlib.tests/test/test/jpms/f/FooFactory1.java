package test.jpms.f;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.spi.ServiceConsumer;

@ServiceConsumer(value = Number.class, cardinality = Cardinality.MULTIPLE)
public class FooFactory1 {}
