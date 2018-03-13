package aQute.libg.asn1;

public interface Types {
	int			UNIVERSAL			= 0x00000000;
	int			APPLICATION			= 0x40000000;
	int			CONTEXT				= 0x80000000;
	int			PRIVATE				= 0xC0000000;
	int			CLASSMASK			= 0xC0000000;
	int			CONSTRUCTED			= 0x20000000;
	int			TAGMASK				= 0x1FFFFFFF;

	String[]	CLASSES				= {
		"U", "A", "C", "P"
	};

	// Payload Primitve
	int			EOC					= 0;																				// null
	// x
	int			BOOLEAN				= 1;																				// Boolean
	// x
	int			INTEGER				= 2;																				// Long
	// x
	int			BIT_STRING			= 3;																				// byte
	// [] -
	int			OCTET_STRING		= 4;																				// byte
	// [] -
	int			NULL				= 5;																				// null
	// x
	int			OBJECT_IDENTIFIER	= 6;																				// int[]
	// x
	int			OBJECT_DESCRIPTOR	= 7;																				//
	int			EXTERNAL			= 8;																				//
	int			REAL				= 9;																				// double
	// x
	int			ENUMERATED			= 10;																				//
	int			EMBEDDED_PDV		= 11;																				//
	int			UTF8_STRING			= 12;																				// String
	int			RELATIVE_OID		= 13;																				//
	int			SEQUENCE			= 16;																				//
	int			SET					= 17;
	int			NUMERIC_STRING		= 18;																				// String
	int			PRINTABLE_STRING	= 19;																				// String
	int			T61_STRING			= 20;																				// String
	int			VIDEOTEX_STRING		= 21;																				// String
	int			IA5STRING			= 22;																				// String
	int			UTCTIME				= 23;																				// Date
	int			GENERALIZED_TIME	= 24;																				// Date
	int			GRAPHIC_STRING		= 25;																				// String
	int			VISIBLE_STRING		= 26;																				// String
	int			GENERAL_STRING		= 27;																				// String
	int			UNIVERSAL_STRING	= 28;																				// String
	int			CHARACTER_STRING	= 29;																				// String
	int			BMP_STRING			= 30;																				// byte[]

	String[]	TAGS				= {
		"EOC               ", "BOOLEAN           ", "INTEGER           ", "BIT_STRING        ", "OCTET_STRING      ",
		"NULL              ", "OBJECT_IDENTIFIER ", "OBJECT_DESCRIPTOR ", "EXTERNAL          ", "REAL              ",
		"ENUMERATED        ", "EMBEDDED_PDV      ", "UTF8_STRING       ", "RELATIVE_OID      ", "?(14)             ",
		"?(15)             ", "SEQUENCE          ", "SET               ", "NUMERIC_STRING    ", "PRINTABLE_STRING  ",
		"T61_STRING        ", "VIDEOTEX_STRING   ", "IA5STRING         ", "UTCTIME           ", "GENERALIZED_TIME  ",
		"GRAPHIC_STRING    ", "VISIBLE_STRING    ", "GENERAL_STRING    ", "UNIVERSAL_STRING  ", "CHARACTER_STRING  ",
		"BMP_STRING        ",
	};

}
