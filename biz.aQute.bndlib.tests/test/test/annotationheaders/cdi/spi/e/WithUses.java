package test.annotationheaders.cdi.spi.e;

import javax.enterprise.inject.spi.Extension;

import aQute.bnd.annotation.cdi.ProvideCDIExtension;

@ProvideCDIExtension.SPI(name = "foo.extension", version = "1.0.0", attribute = {
	"uses:='javax.enterprise.context.spi,javax.enterprise.inject.spi'"
})
public class WithUses implements Extension {
}
