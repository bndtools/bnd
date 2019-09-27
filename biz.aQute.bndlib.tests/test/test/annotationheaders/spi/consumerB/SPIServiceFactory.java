package test.annotationheaders.spi.consumerB;

import aQute.bnd.annotation.spi.ServiceConsumer;
import test.annotationheaders.spi.SPIService;

@ServiceConsumer(value = SPIService.class, effective = "active")
public class SPIServiceFactory {}
