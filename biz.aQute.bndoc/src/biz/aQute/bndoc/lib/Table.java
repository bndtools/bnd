package biz.aQute.bndoc.lib;

import java.util.*;
import java.util.regex.*;

public class Table {
	final String			rows[];
	String[]				headers;
	List<List<String>>		matrix	= new ArrayList<>();
	public static Pattern	TABLE_P	= Pattern.compile("(\\+-+)+\\+");

	class Row {

	}

	public Table(CharSequence text) {
		rows = text.toString().split("\n");
		List<StringBuilder> row = new ArrayList<>();
		for (int r = 1; r <= rows.length; r++) {

		}
	}

	public void append(StringBuilder out) {
		// TODO Auto-generated method stub

	}

}
