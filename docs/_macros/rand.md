---
layout: default
class: Macro
title: rand (';' MIN ' (;' MAX )?)?
summary: A random number between 0 and 100, or between the given range (inclusive).
---

	static String	_rand	= "${rand;[<min>[;<end>]]}";
	static Random	random	= new Random();

	public long _rand(String args[]) throws Exception {
		verifyCommand(args, _rand, null, 2, 3);

		int min = 0;
		int max = 100;
		if (args.length > 1) {
			max = Integer.parseInt(args[1]);
			if (args.length > 2) {
				min = Integer.parseInt(args[2]);
			}
		}
		int diff = max - min;

		double d = random.nextDouble() * diff + min;
		return Math.round(d);
	}
