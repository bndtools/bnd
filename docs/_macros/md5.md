---
layout: default
class: Analyzer
title: md5 ';' RESOURCE
summary: The MD5 digest of an existing resource in the JAR
---


	/**
	 * md5 macro
	 */

	static String	_md5Help	= "${md5;path}";

	public String _md5(String args[]) throws Exception {
		Macro.verifyCommand(args, _md5Help, new Pattern[] {
				null, null, Pattern.compile("base64|hex")
		}, 2, 3);

		Digester<MD5> digester = MD5.getDigester();
		Resource r = dot.getResource(args[1]);
		if (r == null)
			throw new FileNotFoundException("From " + digester + ", not found " + args[1]);

		IO.copy(r.openInputStream(), digester);
		boolean hex = args.length > 2 && args[2].equals("hex");
		if (hex)
			return Hex.toHexString(digester.digest().digest());

		return Base64.encodeBase64(digester.digest().digest());
	}
