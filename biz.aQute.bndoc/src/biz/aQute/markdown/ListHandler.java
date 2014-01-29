package biz.aQute.markdown;

import java.util.*;
import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.CompositeBlock;
import biz.aQute.markdown.Markdown.Rover;

/**
 * LISTS Markdown supports ordered (numbered) and unordered (bulleted) lists.
 * Unordered lists use asterisks, pluses, and hyphens — interchangably — as list
 * markers:
 * 
 * <pre>
 *   * Red
 *   * Green
 *   * Blue
 * </pre>
 * 
 * Ordered lists use numbers followed by periods:
 * 
 * <pre>
 * 1.  Bird
 * 2.  McHale
 * 3.  Parish
 * </pre>
 * 
 * It’s important to note that the actual numbers you use to mark the list have
 * no effect on the HTML output Markdown produces. To make lists look nice, you
 * can wrap items with hanging indents:
 * 
 * <pre>
 *  *  Lorem ipsum dolor sit amet, consectetuer adipiscing elit.
 *     Aliquam hendrerit mi posuere lectus. Vestibulum enim wisi,
 *     viverra nec, fringilla in, laoreet vitae, risus.
 *  *  Donec sit amet nisl. Aliquam semper ipsum sit amet velit.
 *  Suspendisse id sem consectetuer libero luctus adipiscing.
 *  *  Hello
 *      *  Goodbye
 *      	+ Hello
 * </pre>
 */

public class ListHandler implements Markdown.Handler {
	static Pattern	FIRST_PARA_	= Pattern.compile(".*(\n) {4,7}([-+*>]|\\d+\\.)");
	/*
	 * Define the content part of a list
	 */
	static String	CONTENT		= /*
								 * And line continued paragraph
								 */
								"(?<content>(.+\n)+"
								/*
								 * Continued paragraphs are indented with 4
								 * spaces.
								 */
								+ "(\n(    .+\n)+)*)";
	/*
	 * List markers must be followed by one or more spaces or a tab.
	 */
	static Pattern	LIST_P		= Pattern.compile("(?<spaced>\n)?"
								/*
								 * List markers typically start at the left
								 * margin, but may be indented by up to three
								 * spaces.
								 */
								+ " {0,3}"
								/*
								 * Unordered lists use asterisks, pluses, and
								 * hyphens — interchangably — as list markers:
								 */
								+ "(?<marker>([-*+]"
								/*
								 * OR
								 */
								+ "|"
								/*
								 * Ordered lists use numbers followed by
								 * periods:
								 */
								+ "\\d+\\.))"
								/*
								 * Marker is followed by one or more spaces
								 */
								+ " +"
								/*
								 * This matches also * * * and - - -for a rule
								 * :-( so make sure we do not have one of this
								 */
								+ "(?![-*])"
								/*
								 * List markers must be followed by one or more
								 * spaces or a tab.
								 */
								+ CONTENT);

	static Pattern	DL_P		= Pattern.compile(
								/*
								 * First the term, more than once
								 */
								"(?<dt>(\\S.*\n)+)"
								/*
								 * The marker
								 */
								+ ": +" + CONTENT
								/*
								 * We eat any remaining empty lines since we
								 * always compress (ol/ul need to make a
								 * difference between line seperated members)
								 */
								+ "\n*", Pattern.UNIX_LINES);
	private Markdown	markdown;

	@Override
	public Block process(Rover rover) throws Exception {
		if (rover.at(LIST_P)) {
			return orderedOrUnOrderedList(rover);
		}
		if (rover.at(DL_P)) {
			return definitionList(rover);
		}

		return null;
	}

	public Block orderedOrUnOrderedList(Rover rover) throws Exception {
		//
		// Lists are are compressed (no subtags)
		// if we have only one paragraph per item
		// so we collect the blocks/item first
		//
		List<List<Block>> list = new ArrayList<>();
		boolean compress = true;

		String marker = rover.group("marker");
		//
		// The numeric marker is always > 2
		//
		String tag = (marker.length() == 1) ? "ul" : "ol";

		//
		// Current paragraph is already a list start
		//
		do {

			boolean spaced = rover.group("spaced") != null;
			if (spaced && list.size() > 0) {
				compress = false;
			}

			//
			// we could have a compressed list
			// so split on \n*
			//

			String[] content = rover.group("content").toString().split("\n {0,3}([-*+]|\\d+\\.) *");
			for (CharSequence part : content) {
				part = unshift(part);
				List<Block> paras = markdown.parseContent(part);
				list.add(paras);
			}

			rover.next();
		} while (rover.at(LIST_P));

		List<Block> items = new ArrayList<Block>();
		for (List<Block> item : list) {
			CompositeBlock li = new CompositeBlock("li", item);
			if (compress)
				li.compress();

			items.add(li);
		}
		return new CompositeBlock(tag, items);
	}

	/**
	 * Definition list
	 * 
	 * @param md
	 * @param rover
	 * @return
	 * @throws Exception
	 */
	public Block definitionList(Rover rover) throws Exception {
		List<Block> dl = new ArrayList<>();

		//
		// We are always on one or more dt's
		//
		do {
			String dts[] = rover.group("dt").split("\n");
			for (String dt : dts) {
				dl.add(markdown.paragraph(dt, "dt"));
			}

			CharSequence content = rover.group("content");
			rover.next();
			content = unshift(content);
			List<Block> paras = markdown.parseContent(content);
			CompositeBlock dt = new CompositeBlock("dd", paras);
			dt.compress();
			dl.add(dt);
		} while (rover.at(DL_P));

		return new CompositeBlock("dl", dl);
	}

	/**
	 * <pre>
	 * para
	 * para					insert nl
	 * ....para
	 * ....para
	 * 						insert nl
	 * ....para
	 * para					insert nl
	 * para
	 * </pre>
	 * 
	 * @param content
	 * @param amount
	 * @return
	 */

	public static CharSequence unshift(CharSequence content) {
		StringBuilder sb = new StringBuilder(content);

		//
		// Adjust firs paragraph. We consume the block if it does not
		// start with a list or block quote character.
		//
		Matcher m = FIRST_PARA_.matcher(sb);
		if (m.lookingAt()) {
			sb.insert(m.start(1), "\n");
		}

		int pos = 0;
		char c = 0;
		for (int i = 0; i < sb.length(); i++) {
			c = sb.charAt(i);
			switch (c) {
				case '\n' :
					pos = 0;
					break;

				case ' ' :
					if (pos++ < 4) {
						sb.delete(i, i + 1);
						i--;
					}
					break;

				default :
					pos = 100;
					break;
			}
		}
		if (c != '\n')
			sb.append("\n");
		return sb;
	}

	@Override
	public void configure(Markdown markdown) throws Exception {
		this.markdown = markdown;
	}

}
