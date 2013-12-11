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

import com.github.rjeschke.txtmark.*;

/**
 * This class is used to adjust the generated markdown. It gets called for all
 * major tags. that become part of the output. The class will generate paragraph
 * numbering and creates images for ascii art in markdown.
 */
class BndocDecorator extends DefaultDecorator {
	int					toclevel	= 2;
	ParagraphCounter	pgc			= new ParagraphCounter();

	int					codeStart	= -1;
	int 				paraStart = -1;
	int					imageCounter;
	DocumentBuilder		generator;

	BndocDecorator(DocumentBuilder generator) {
		this.generator = generator;
	}

	/**
	 * Handle tables
	 */
    @Override
    public void openParagraph(StringBuilder out)
    {
    	paraStart = out.length();
    }

    @Override
    public void closeParagraph(StringBuilder out)
    {
    	CharSequence text = out.subSequence(paraStart, out.length());
    	Matcher m = Table.TABLE_P.matcher(text);
    	if ( !m.matches()) {
    		out.insert(paraStart,"<p>");
            out.append("</p>\n");
            return;
    	}
    	Table table = new Table(text);
    	out.delete(paraStart, out.length());
    	table.append(out);
    }


	@Override
	public void openHeadline(StringBuilder out, int level) {
		super.openHeadline(out, level);
		pgc.level(level);
		out.append(">");
		if (level <= toclevel)
			out.append(pgc.toHtml(level, "."));
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
					if (" -|+/\\><:".indexOf(c) >= 0)
						artCharacters++;
					else
						otherCharacters++;
				}
			}

			if (artCharacters > otherCharacters) {
				byte[] digest = md.digest();
				String key = Hex.toHexString(digest);
				File file = new File(generator.getImages(), key + ".png");
				if (!file.isFile()) {
					file.getParentFile().mkdirs();
					String text = out.substring(codeStart, out.length());
					RenderedImage image = render(text);
					ImageIO.write(image, "png", file);
				}
				URI relative = generator.getCurrent().getParentFile().toURI().relativize(file.toURI());
				out.delete(codeStart, out.length());
				out.append("<img style='width:").append(100/DocumentBuilder.QUALITY_SCALE).append("%;' src='").append(relative).append("'>");
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
}
