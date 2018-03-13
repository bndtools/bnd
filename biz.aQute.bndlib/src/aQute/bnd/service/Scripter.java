package aQute.bnd.service;

import java.io.StringReader;
import java.util.Map;

public interface Scripter {

	void eval(Map<String, Object> x, StringReader stringReader);

}
