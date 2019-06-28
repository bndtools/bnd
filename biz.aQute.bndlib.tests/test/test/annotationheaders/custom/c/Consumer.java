package test.annotationheaders.custom.c;

import test.annotationheaders.custom.CustomA;
import test.annotationheaders.custom.CustomA.Resolution;

@CustomA(resolution = Resolution.OPTIONAL)
public interface Consumer {}
