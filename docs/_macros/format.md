---
layout: default
class: Macro
title: format ';' STRING (';' ANY )* 
summary: Print a formatted string, automatically converting variables to the specified format if possible.
---

	static String	_format	= "${format;<format>[;args...]}";

	public String _format(String args[]) throws Exception {
		verifyCommand(args, _format, null, 2, Integer.MAX_VALUE);

		Object[] args2 = new Object[args.length + 10];
		
		Matcher m = PRINTF_P.matcher(args[1]);
		int n = 2;
		while ( n < args.length && m.find()) {
			char conversion = m.group(5).charAt(0);
			switch(conversion) {
				// d|f|c|s|h|n|x|X|u|o|z|Z|e|E|g|G|p|\n|%)");
				case 'd':
				case 'u':
				case 'o':
				case 'x':
				case 'X':
				case 'z':
				case 'Z':
					args2[n-2] = Long.parseLong(args[n]);
					n++;
					break;
					
				case 'f':
				case 'e':
				case 'E':
				case 'g':
				case 'G':
				case 'a':
				case 'A':
					args2[n-2] = Double.parseDouble(args[n]);
					n++;
					break;

				case 'c':
					if ( args[n].length() != 1)
						throw new IllegalArgumentException("Character expected but found '"+args[n]+"'");
					args2[n-2] = args[n].charAt(0);
					n++;
					break;
					
				case 'b':
					String v = args[n].toLowerCase();
					if ( v == null || v.equals("false") || v.isEmpty() || (NUMERIC_P.matcher(v).matches() && Double.parseDouble(v)==0.0D))
						args2[n-2] = false;
					else
						args2[n-2] = false;
					n++;
					break;
					
				case 's':
				case 'h':
				case 'H':
				case 'p':
					args2[n-2] = args[n];
					n++;
					break;

				case 't':
				case 'T':
					String dt = args[n];
					
					if ( NUMERIC_P.matcher(dt).matches()) {
						args2[n-2]= Long.parseLong(dt);
					} else {
						DateFormat df;
						switch(args[n].length()) {
							case 6:
								df = new SimpleDateFormat("yyMMdd");
								break;
								
							case 8:
								df = new SimpleDateFormat("yyyyMMdd");
								break;
								
							case 12:
								df = new SimpleDateFormat("yyyyMMddHHmm");
								break;
								
							case 14:
								df = new SimpleDateFormat("yyyyMMddHHmmss");
								break;
							case 19:
								df = new SimpleDateFormat("yyyyMMddHHmmss.SSSZ");
								break;
								
							default:
								throw new IllegalArgumentException("Unknown dateformat " + args[n]);
						}
						args2[n-2] = df.parse(args[n]);
					}	
					break;
					
				case 'n':
				case '%':
					break;
			}
		}

		Formatter f = new Formatter();
		try {
			f.format(args[1], args2);
			return f.toString();
		}
		finally {
			f.close();
		}
	}

