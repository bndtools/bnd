package test.annotationheaders.spi.consumerD;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.spi.ServiceConsumer;
import test.annotationheaders.spi.SPIService;

@ServiceConsumer(value = SPIService.class, cardinality = Cardinality.MULTIPLE)
public class SPIServiceFactory {}
