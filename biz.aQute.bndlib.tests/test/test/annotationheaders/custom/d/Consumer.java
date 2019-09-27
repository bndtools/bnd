package test.annotationheaders.custom.d;

import test.annotationheaders.custom.CustomA;
import test.annotationheaders.custom.CustomA.Cardinality;

@CustomA(cardinality = Cardinality.MULTIPLE)
public interface Consumer {}
