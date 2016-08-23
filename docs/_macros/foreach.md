---
layout: default
class: Macro
title: foreach ';' MACRO (';' LIST)* 
summary: Iterator over a list, calling a macro with the value and index
---

	/**
	 * Map a value from a list to a new value, providing the value and the index
	 */
	
	static String	_foreach	= "${foreach;<macro>[;<list>...]}";
	public String _foreach(String args[]) throws Exception {
		verifyCommand(args, _foreach, null, 2, Integer.MAX_VALUE);
		String macro = args[1];
		List<String> list = toList(args,2, args.length);
		List<String> result = new ArrayList<String>();
		
		int n = 0;
		for ( String s : list) {
			String invoc = process("${" + macro +";" + s +";" + n++ +"}");
			result.add(invoc);
		}

		return Processor.join(result);
	}
