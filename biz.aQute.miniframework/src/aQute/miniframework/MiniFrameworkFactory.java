package aQute.miniframework;

import java.io.*;
import java.util.*;

import org.osgi.framework.launch.*;

public class MiniFrameworkFactory implements FrameworkFactory {

	public Framework newFramework(Map configuration) {
		try {
			return new MiniFramework(configuration);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
