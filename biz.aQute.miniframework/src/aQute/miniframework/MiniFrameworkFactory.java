package aQute.miniframework;

import java.util.*;

import org.osgi.framework.launch.*;

public class MiniFrameworkFactory implements FrameworkFactory {

	public Framework newFramework(Map configuration) {
		return new MiniFramework(configuration);
	}

}
