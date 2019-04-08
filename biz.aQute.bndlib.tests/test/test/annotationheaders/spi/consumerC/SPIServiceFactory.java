package test.annotationheaders.spi.consumerC;

import aQute.bnd.annotation.spi.ServiceConsumer;
import aQute.bnd.annotation.spi.ServiceConsumer.Resolution;
import test.annotationheaders.spi.SPIService;

@ServiceConsumer(value = SPIService.class, resolution = Resolution.OPTIONAL)
public class SPIServiceFactory {
}
