package test.annotationheaders.custom.provider;

import test.annotationheaders.custom.CustomB;
import test.annotationheaders.custom.a.Consumer;

@CustomB(Consumer.class)
public class Provider implements Consumer {}
