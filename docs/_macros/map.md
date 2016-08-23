---
layout: default
class: Macro
title: map ';' MACRO (';' LIST)* 
summary: Map a list to a new list using a function
---

	/**
	 * Map a value from a list to a new value
	 */
	
	static String	_map	= "${map;<macro>[;<list>...]}";
	public String _map(String args[]) throws Exception {
		verifyCommand(args, _map, null, 2, Integer.MAX_VALUE);
		String macro = args[1];
		List<String> list = toList(args,2, args.length);
		List<String> result = new ArrayList<String>();
		
		for ( String s : list) {
			String invoc = process("${" + macro +";" + s +"}");
			result.add(invoc);
		}

		return Processor.join(result);
	}

