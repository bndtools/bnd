package test.annotationheaders.spi.consumerC;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceConsumer;
import test.annotationheaders.spi.SPIService;

@ServiceConsumer(value = SPIService.class, resolution = Resolution.OPTIONAL)
public class SPIServiceFactory {}
