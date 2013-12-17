package biz.aQute.bndoc.lib;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.security.*;

import javax.imageio.*;

import org.stathissideris.ascii2image.graphics.*;
import org.stathissideris.ascii2image.text.*;

import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.tag.*;

import com.github.rjeschke.txtmark.*;

/**
 * This class is used to adjust the generated markdown. It gets called for all
 * major tags. that become part of the output. The class will generate paragraph
 * numbering and creates resources for ascii art in markdown.
 */
class BndocDecorator extends DefaultDecorator {
	int					toclevel	= 2;
	ParagraphCounter	pgc			= new ParagraphCounter();

	int					codeStart	= -1;
	int					paraStart	= -1;
	int					headStart	= -1;
	int					imageCounter;
	DocumentBuilder		generator;

	BndocDecorator(DocumentBuilder generator) {
		this.generator = generator;
	}

	@Override
	public void openHeadline(StringBuilder out, int level) {
		super.openHeadline(out, level);
		pgc.level(level);
		out.append(">");
		if (level <= toclevel)
			out.append(pgc.toHtml(level, "."));
		headStart = out.length();
	}

	@Override
	public void closeHeadline(StringBuilder out, int level) {
		String text = out.substring(headStart);
		super.closeHeadline(out, level);
		out.append("<div class='running-").append(level).append("'>").append(pgc.toString(level, ".")).append(" ")
				.append(text.substring(1)).append("</div>");
		out.append("<div class='");
		String del = "";
		for (int i = level + 1; i < 6; i++) {
			out.append(del).append("running-").append(i);
			del = " ";
		}
		out.append("' ></div>");
	}

	/**
	 * abc def ::
	 */
	@Override
	public void openParagraph(StringBuilder out) {
		paraStart = out.length();
	}

	@Override
	public void closeParagraph(StringBuilder out) {
		String para = out.subSequence(paraStart, out.length()).toString();

		String parts[] = para.split("\n\\s{0,3}:\\s+");
		if (parts.length > 1) {
			definitionList(out, parts);
		} else {
			out.insert(paraStart, "<p>");
			out.append("</p>\n");
		}
	}

	private void definitionList(StringBuilder out, String[] parts) {
		// first part are the terms
		// subsequent parts are the definitions
		out.delete(paraStart, out.length());
		Tag dl = new Tag("dl");
		dl.addAttribute("class", "dl-horizontal");
		for (String term : parts[0].trim().split("<br\\s*/>\\s*\n?\\s*")) {
			Tag dt = new Tag(dl, "dt");
			dt.addContent(term);
		}
		for (int i = 1; i < parts.length; i++) {
			Tag dd = new Tag(dl, "dd");
			dd.addContent(parts[i]);
		}
		out.append(dl.toString());
		if (paraStart > 6) {
			String s = out.substring(paraStart - 5, paraStart + 5);
			if (s.equals("</dl>\n<dl>"))
				out.delete(paraStart - 5, paraStart + 5);
		}
	}

	@Override
	public void openCodeBlock(StringBuilder out) {
		codeStart = out.length();
	}

	@Override
	public void closeCodeBlock(StringBuilder out) {
		try {
			Table table = Table.parse(out, codeStart);
			if (table != null) {
				if (table.getError() != null) {
					generator.error("table on has error %s", table.getError());
				} else {
					out.delete(codeStart, out.length());
					table.appendTo(out);
				}
			} else {
				int lstart = codeStart;
				int artCharacters = 0;
				int otherCharacters = 0;

				MessageDigest md = MessageDigest.getInstance("SHA-1");

				for (int i = lstart; i < out.length(); i++) {
					char c = out.charAt(i);
					md.update((byte) c);
					md.update((byte) (c >> 8));

					if (c == '\n') {
						lstart = i + 1;
					} else {
						if (" -|+/\\><:".indexOf(c) >= 0)
							artCharacters++;
						else
							otherCharacters++;
					}
				}

				if (artCharacters > otherCharacters) {
					byte[] digest = md.digest();
					String key = Hex.toHexString(digest);
					String name = "img/" + key + ".png";
					File file = IO.getFile(generator.getResources(), name);
					if (!file.isFile()) {
						file.getParentFile().mkdirs();
						String text = out.substring(codeStart, out.length());
						RenderedImage image = render(text);
						ImageIO.write(image, "png", file);
					}
					out.delete(codeStart, out.length());
					URI relative = generator.current.toURI().relativize(file.toURI());
					out.append("<img style='width:").append(100 / DocumentBuilder.QUALITY_SCALE).append("%;' src='")
							.append(relative).append("' />");
				} else {
					out.insert(codeStart, "<pre>");
					out.append("</pre>\n");
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private RenderedImage render(String text) throws Exception {
		TextGrid grid = new TextGrid();
		grid.addToMarkupTags(generator.getConversionOptions().processingOptions.getCustomShapes().keySet());
		grid.initialiseWithText(text, generator.getConversionOptions().processingOptions);
		Diagram diagram = new Diagram(grid, generator.getConversionOptions());
		return new BitmapRenderer().renderToImage(diagram, generator.getConversionOptions().renderingOptions);
	}
}
