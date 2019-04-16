package test.annotationheaders.cdi.d;

import javax.enterprise.inject.spi.Extension;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.cdi.ProvideCDIExtension;

@ProvideCDIExtension(name = "foo.extension", version = "1.0.0", resolution = Resolution.OPTIONAL)
public class ResolutionOptional implements Extension {
}
