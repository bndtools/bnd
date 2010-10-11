package aQute.bnd.service;

import java.io.*;
import java.util.*;

public interface Scripter {

	void eval(Map<String, Object> x, StringReader stringReader);

}
