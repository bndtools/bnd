package aQute.bld.main;

import org.apache.felix.service.command.*;

import aQute.bnd.annotation.component.*;

@Component(properties={"osgi.command.scope=info","osgi.command.function=info|xyz"}, provide=InfoCommands.class)
public class InfoCommands {

	@Activate
	void activate() {
		System.out.println("activated");
	}

	@Descriptor("Provides general info")
	public Object xyz(@Parameter(names="-v", absentValue="xx") String string) {
		return string;
	}


}
