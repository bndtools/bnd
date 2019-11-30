@ServiceProvider(register = Provider.class, value = SPIService.class, attribute = {
	"foo=bar"
})
package test.annotationheaders.spi.providerF;

import aQute.bnd.annotation.spi.ServiceProvider;
import test.annotationheaders.spi.SPIService;
