package aQute.bnd.main;

import java.util.*;
import java.util.Map.Entry;

import aQute.bnd.main.bnd.scriptOptions;

public class ScriptCommand {
//	static ScriptEngineManager	mgr		= new ScriptEngineManager();
//	static ScriptEngine			engine	= mgr.getEngineByName("JavaScript");

	public ScriptCommand(bnd bnd, scriptOptions opts) { //throws IOException, ScriptException {
//		String f = opts.file();
//		String s;
//
//		if (f != null) {
//			File ff = bnd.getFile(f);
//			if (ff.isFile()) {
//				s = IO.collect(ff);
//			} else {
//				bnd.error("No such file " + ff);
//				return;
//			}
//		} else
//			s = Processor.join(opts._(), " ");
//
//		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
//		bindings.put("bnd", this);
//		engine.eval(s);
	}

	public Object copy(Map<Object, Object> from, Map<Object, Object> to) {
		for (Entry<Object, Object> entry : from.entrySet()) {
			to.put(entry.getKey(), entry.getValue());
		}
		return to;
	}
}
