package test.annotationheaders.custom.e;

import test.annotationheaders.custom.CustomB;
import test.annotationheaders.custom.a.Consumer;

@CustomB(serviceProperty = {
	"foo=bar", "service.ranking:Integer=5"
}, value = Consumer.class)
public class Provider implements Consumer {}
