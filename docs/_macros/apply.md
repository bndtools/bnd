---
layout: default
class: Macro
title: apply ';' MACRO (';' LIST)* 
summary: Convert a list to an invoction with arguments 
---

	/**
	 * Take a list and convert this to the argumets
	 */
	
	static String	_apply	= "${apply;<macro>[;<list>...]}";
	public String _apply(String args[]) throws Exception {
		verifyCommand(args, _apply, null, 2, Integer.MAX_VALUE);
		String macro = args[1];
		List<String> list = toList(args,2, args.length);
		List<String> result = new ArrayList<String>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("${").append(macro);
		for ( String s : list) {
			sb.append(";").append(s);
		}		
		sb.append("}");

		return process(sb.toString());
	}
