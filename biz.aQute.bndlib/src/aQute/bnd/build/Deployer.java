package aQute.bnd.build;

import aQute.bnd.osgi.*;
import aQute.libg.reporter.*;

public class Deployer {

	private final Processor			processor;
	private final ProjectMessages	msgs;

	public Deployer(Processor processor) {
		this.processor = processor;
		this.msgs = ReporterMessages.base(this.processor, ProjectMessages.class);
	}

}
