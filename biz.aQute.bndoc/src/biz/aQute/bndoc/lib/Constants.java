package biz.aQute.bndoc.lib;

public interface Constants {
	enum OutputType {
		SINGLE, MULTI, PDF
	}

	String	DO					= "-do";
	String	DEFAULT_LANGUAGE	= "en";
	String	LANGUAGES			= "languages";
	String	LANGUAGE			= "langugage";
	String	TYPE				= "type";
	String	SOURCES				= "sources";
	String	CSS					= "css";
	String	TEMPLATE			= "template";
	String	LEVELS				= "levels";
	String	FILTER				= "filter";
	String	OUTPUT				= "output";
	String	IMAGES				= "images";

	String	DEFAULT_TEMPLATE	= "<html>\n" //
										+ "  <head>\n" + "    <title>${title}</title>\n" //
										+ "    ${css}" //
										+ "  </head>\n" //
										+ "  <body>\n" //
										+ "    <div class=bndoc-content>\n" + "      ${content}" //
										+ "    <div>\n" + "    <footer>Copyright ${tstamp;yyyy}</footer>\n" //
										+ "  </body>\n" + "</html>";

}
