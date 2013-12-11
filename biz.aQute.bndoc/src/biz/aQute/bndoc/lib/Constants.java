package biz.aQute.bndoc.lib;

public interface Constants {
	enum OutputType {
		SINGLE, MULTI, PDF
	}

	String	DO						= "-do";
	String	DEFAULT_LANGUAGE		= "en";
	String	LANGUAGES				= "languages";
	String	LANGUAGE				= "language";
	String	TYPE					= "type";
	String	SOURCES					= "sources";
	String	CSS						= "css";
	String	TEMPLATE				= "template";
	String	INNER_TEMPLATE			= "inner-template";
	String	LEVELS					= "levels";
	String	FILTER					= "filter";
	String	OUTPUT					= "output";
	String	IMAGES					= "images";
	String	SHAPES					= "shapes";
	String	CLEAN					= "clean";
	String	SYMBOLS					= "symbols";

	String	DEFAULT_TEMPLATE		= "<html>\n" //
											+ "  <head>\n" + "    <title>${title}</title>\n" //
											+ "    ${css}" //
											+ "  </head>\n" //
											+ "  <body>\n" //
											+ "    <div class=bndoc-content>\n" + "      ${content}" //
											+ "    <div>\n" + "    <footer>Copyright ${tstamp;yyyy}</footer>\n" //
											+ "  </body>\n" + "</html>";
	String	DEFAULT_INNER_TEMPLATE	= "<div class=inner>${content}</div>";

}
