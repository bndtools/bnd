package test.annotationheaders.cdi.spi.b;

import javax.enterprise.inject.spi.Extension;

import aQute.bnd.annotation.cdi.ProvideCDIExtension;

@ProvideCDIExtension.SPI(name = "foo.extension", version = "1.0.0", attribute = {
	"foo:Integer=15"
})
public class WithServiceProperty implements Extension {
}
