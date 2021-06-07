---
layout: default
class: Macro
title: unescape ( ';' STRING )*
summary: The concatenated input will have all \n, \r, \b, \f, and \t replaced with their control code.
---

	public String _unescape(String args[]) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			sb.append(args[i]);
		}

		for (int j = 0; j < sb.length() - 1; j++) {
			if (sb.charAt(j) == '\\') {
				switch (sb.charAt(j + 1)) {

					case 'n' :
						sb.replace(j, j + 2, "\n");
						break;

					case 'r' :
						sb.replace(j, j + 2, "\r");
						break;

					case 'b' :
						sb.replace(j, j + 2, "\b");
						break;

					case 'f' :
						sb.replace(j, j + 2, "\f");
						break;

					case 't' :
						sb.replace(j, j + 2, "\t");
						break;

					default :
						break;
				}
			}
		}
		return sb.toString();
	}
