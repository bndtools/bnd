---
layout: default
class: Macro
title: js (';' JAVASCRIPT )*
summary: Execute Javascript, return the value of the last expression
---

	ScriptEngine	engine		= new ScriptEngineManager().getEngineByName("javascript");
	ScriptContext	context		= null;
	Bindings		bindings	= null;
	StringWriter	stdout		= new StringWriter();
	StringWriter	stderr		= new StringWriter();

	static String	_js			= "${js [;<js expr>...]}";

	public Object _js(String args[]) throws Exception {
		verifyCommand(args, _js, null, 2, Integer.MAX_VALUE);

		StringBuilder sb = new StringBuilder();

		for (int i = 1; i < args.length; i++)
			sb.append(args[i]).append(';');

		if (context == null) {
			context = engine.getContext();
			bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.put("domain", domain);
			String javascript = domain.mergeProperties("javascript", ";");
			if (javascript != null && javascript.length() > 0) {
				engine.eval(javascript, context);
			}
			context.setErrorWriter(stderr);
			context.setWriter(stdout);
		}
		Object eval = engine.eval(sb.toString(), context);
		StringBuffer buffer = stdout.getBuffer();
		if (buffer.length() > 0) {
			domain.error("Executing js: %s: %s", sb, buffer);
			buffer.setLength(0);
		}

		if (eval != null) {
			return toString(eval);
		}

		String out = stdout.toString();
		stdout.getBuffer().setLength(0);
		return out;
	}
