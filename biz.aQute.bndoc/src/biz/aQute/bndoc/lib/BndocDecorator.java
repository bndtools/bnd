package biz.aQute.bndoc.lib;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.regex.*;

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
	int					toclevel	= 3;
	ParagraphCounter	pgc			= new ParagraphCounter();

	int					codeStart	= -1;
	int					paraStart	= -1;
	int					headStart	= -1;
	int					imgStart	= -1;
	int					imageCounter;
	DocumentBuilder		generator;
	int					termid		= 1000;

	BndocDecorator(DocumentBuilder generator) {
		this.generator = generator;
	}

	@Override
	public void openHeadline(StringBuilder out, int level) {
		super.openHeadline(out, level);
		pgc.level(level);
		out.append(" id='_h-").append(pgc.toString(level, ".")).append("'");
		out.append(">");
		if (level <= toclevel) {
			out.append(pgc.toHtml(level, "."));
		}
		out.delete(out.length() - 1, out.length());
		headStart = out.length();
	}

	@Override
	public void closeHeadline(StringBuilder out, int level) {
		String text = out.substring(headStart);
		super.closeHeadline(out, level);
		out.append("<div class='running-").append(level).append("'>").append(pgc.toString(level, ".")).append(" ")
				.append(text.substring(1)).append("</div>");
		String del = "";
		for (int i = level + 1; i < 6; i++) {
			out.append("<div class='");
			out.append(del).append("running-").append(i).append("'> </div>");
		}
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
			//
			// Match definition terms
			Matcher m = DocumentBuilder.DEFINITION_P.matcher(out);
			boolean found = m.find(paraStart);
			while (found) {
				out.replace(m.start(1), m.end(1), "<dfn id='_term" + termid++ + "'>" + m.group(2) + "</dfn>");
				found = m.find();
			}
			out.insert(paraStart, "<p>");
			out.append("</p>\n");
		}
	}

	private void definitionList(StringBuilder out, String[] parts) {
		try {
			// first part are the terms
			// subsequent parts are the definitions
			out.delete(paraStart, out.length());
			Tag dl = new Tag("dl");
			dl.addAttribute("class", "dl-horizontal");
			for (String term : parts[0].trim().split("<br\\s*/>\\s*\n?\\s*")) {
				Tag dt = new Tag(dl, "dt");
				dt.addContent(trim(term));
			}
			for (int i = 1; i < parts.length; i++) {
				Tag dd = new Tag(dl, "dd");
				dd.addContent(trim(parts[i]));
			}
			DocumentBuilder.append(out, dl);
			if (paraStart > 6) {
				String s = out.substring(paraStart - 5, paraStart + 5);
				if (s.equals("</dl>\n<dl>"))
					out.delete(paraStart - 5, paraStart + 5);
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String trim(String string) {
		int n = -1;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			switch (c) {
				case '\t' :
				case '\r' :
				case '\n' :
				case ' ' :
					break;

				default :
					return string.substring(i);
			}
		}
		return string;
	}

	@Override
	public void openCodeBlock(StringBuilder out) {
		codeStart = out.length();
	}

	@Override
	public void closeCodeBlock(StringBuilder out) {
		try {
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
					if ("-|+/\\><:".indexOf(c) >= 0)
						artCharacters++;
					else if (!Character.isWhitespace(c))
						otherCharacters++;
				}
			}

			if (artCharacters > otherCharacters) {
				byte[] digest = md.digest();
				String key = Hex.toHexString(digest);
				String name = "img/" + key + ".png";
				File file = IO.getFile(generator.getOutput(), name);
				int width;
				int height;

				if (!file.isFile()) {
					file.getParentFile().mkdirs();
					String text = out.substring(codeStart, out.length());
					RenderedImage image = render(text);
					width = image.getWidth();
					height = image.getHeight();
					ImageIO.write(image, "png", file);
				} else {
					BufferedImage read = ImageIO.read(file);
					width = read.getWidth();
					height = read.getHeight();
				}
				out.delete(codeStart, out.length());
				URI relative = generator.currentOutput.toURI().relativize(file.toURI());
				out.append("<img src='").append(relative)//
						.append("' style='width:").append((int) (width / DocumentBuilder.QUALITY_SCALE)) //
						.append("px;height:").append((int) (height / DocumentBuilder.QUALITY_SCALE))//
						.append("px'").append("/>");
			} else {
				out.insert(codeStart, "<pre>");
				out.append("</pre>\n");
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

	@Override
	public void openImage(StringBuilder out) {
		out.append("<img");
		imgStart = out.length();
	}

	static Pattern	IMG_SRC_P	= Pattern.compile("src\\s*=\\s*['\"]([^'\"]*)['\"]");

	@Override
	public void closeImage(StringBuilder out) {
		Matcher matcher = IMG_SRC_P.matcher(out);
		if (matcher.find(imgStart)) {
			String replacement = generator.toResource("img", matcher.group(1));
			if (replacement != null) {
				out.replace(matcher.start(1), matcher.end(1), replacement);
			}
		}
		out.append(" />");
	}
}
