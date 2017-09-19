package aQute.bnd.service.reporter;

import java.io.PrintWriter;

/**
 * Represent a part of the XML report.
 */
public interface XmlReportPart {

	/**
	 * Write a part of the Xml report to the output.
	 * <p>
	 * This method is called when computing the complete Xml report file.
	 * 
	 * @param out the output
	 */
	public void write(PrintWriter out) throws Exception;
}
