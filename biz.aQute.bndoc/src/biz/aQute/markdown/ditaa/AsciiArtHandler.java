package biz.aQute.markdown.ditaa;

import java.awt.image.*;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import javax.imageio.*;
import javax.xml.parsers.*;

import org.stathissideris.ascii2image.core.*;
import org.stathissideris.ascii2image.graphics.*;
import org.stathissideris.ascii2image.text.*;
import org.xml.sax.*;

import aQute.lib.env.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import biz.aQute.markdown.*;
import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.Rover;

public class AsciiArtHandler implements Markdown.Handler {

	private Markdown				markdown;
	static Pattern					FIGURE_P	= Pattern
														.compile("\\[(Figure)(:(?<caption>(.+\n)+?)\\]\n(?<content>(    .*\n+)+)");
	File							resources;
	private ConversionOptions		options;
	private AsciiArtConfiguration	configuration;
	private float					quality_scale;

	@Override
	public Block process(Rover rover) throws Exception {
		if (rover.at(FIGURE_P)) {
			String content = rover.group("content");
			String caption = rover.group("caption");

			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.digest(content.getBytes("UTF-8"));
			byte[] digest = md.digest();
			String key = Hex.toHexString(digest);

			File resources = markdown.getResourcesDir();
			String relative = markdown.getResourcesRelative();

			String name = "img/" + key + ".png";
			File file = IO.getFile(resources, name);

			int width;
			int height;

			if (!file.isFile()) {
				file.getParentFile().mkdirs();
				RenderedImage image = render(content);
				width = image.getWidth();
				height = image.getHeight();
				ImageIO.write(image, "png", file);
			} else {
				BufferedImage image = ImageIO.read(file);
				width = image.getWidth();
				height = image.getHeight();
				image.flush();
			}

			Block p = markdown.paragraph(caption, "img");
			p.attr("src", relative + "/" + name);
			p.style("width:" + (int) (width / getQualityScale()) + "px");
			p.style("height:" + (int) (height / getQualityScale()) + "px");

			rover.next();
			return p;
		}

		return null;
	}

	private RenderedImage render(String text) throws Exception {
		TextGrid grid = new TextGrid();

		grid.addToMarkupTags(getConversionOptions().processingOptions.getCustomShapes().keySet());
		grid.initialiseWithText(text, getConversionOptions().processingOptions);
		Diagram diagram = new Diagram(grid, getConversionOptions());
		return new BitmapRenderer().renderToImage(diagram, getConversionOptions().renderingOptions);
	}

	public ConversionOptions getConversionOptions() throws ParserConfigurationException, SAXException, IOException {
		if (options == null) {

			options = new ConversionOptions();
			options.processingOptions = new ProcessingOptions();
			options.renderingOptions = new RenderingOptions();
			options.renderingOptions.setScale(getQualityScale());
			options.renderingOptions.setAntialias(true);
			ConfigurationParser cp = new ConfigurationParser();

			HashMap<String,CustomShapeDefinition> shapes = cp.getShapeDefinitionsHash();
			aQute.lib.env.Header h = configuration.symbols();
			if (h != null) {
				for (Entry<String,Props> entry : h.entrySet()) {
					CustomShapes shape = new CustomShapes(entry.getKey(), entry.getValue());
					if (shape.getError() != null)
						markdown.error("Invalid shape def %s : %s", entry.getKey(), shape.getError());
					
					shapes.put(entry.getKey(), shape);
				}
			}
			options.processingOptions.setCustomShapes(shapes);
		}
		return options;
	}

	float getQualityScale() {
		if (quality_scale < 1) {
			quality_scale = configuration.quality_scale();
			if (quality_scale < 1)
				quality_scale = 2.0f;
		}
		return quality_scale;
	}

	@Override
	public void configure(Markdown markdown) throws Exception {
		this.markdown = markdown;
		configuration = markdown.config(AsciiArtConfiguration.class, "markdown.asciiart.");
	}

}
