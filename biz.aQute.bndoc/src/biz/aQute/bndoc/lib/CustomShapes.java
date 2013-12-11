package biz.aQute.bndoc.lib;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.regex.*;

import org.stathissideris.ascii2image.graphics.*;

public class CustomShapes extends CustomShapeDefinition {
	final static Map<String,CustomShapeDefinition>	shapes	= new HashMap<>();
	final static Map<String,Color>					colors	= new HashMap<String,Color>();
	Color											fill;
	GeneralPath										template;
	private Color	stroke;

	CustomShapes(String name, boolean border, boolean drop, boolean stretches, Color color) {
		this.fill = color;
		setDropsShadow(drop);
		setHasBorder(border);
		setStretches(stretches);
		setTag(name);
		shapes.put(name, this);
	}

	private static void add(String name, int r, int g, int b) {
		colors.put(name, new Color(r, g, b));
	}

	CustomShapes(String name, Map<String,String> props) {
		this.fill = getColor(props.get("fill"));
		this.stroke = getColor(props.get("stroke"));
		setDropsShadow(isTrue(props.get("shadow")));
		setHasBorder(isTrue(props.get("border")));
		setStretches(isTrue(props.get("stretches")));
		setTag(name);
		String path = props.get("path");
		if (path != null)
			template = parsePath(path);
	}

	public GeneralPath getPath(DiagramShape shape) {
		if (fill != null)
			shape.setFillColor(fill);
		if (stroke != null)
			shape.setStrokeColor(stroke);
		
		Rectangle b = shape.getBounds();
	    // "scaleX", "shearY", "shearX", "scaleY", "translateX", "translateY" 
		AffineTransform at = new AffineTransform(b.width, 0, 0, b.height, b.x, b.y);
		GeneralPath p = new GeneralPath(template);
		p.transform(at);
		return p;
	}

	private GeneralPath parsePath(String path) {
		String parts[] = path.split("\\s+");
		GeneralPath p = new GeneralPath();
		double cx = 0, cy = 0;

		String cmd = "M";
		for (int i = 0; i < parts.length;) {
			double dx = 0, dy = 0;
			char c = parts[i].charAt(0);
			
			if ( Character.isLetter(c))
				cmd = parts[i++];
			
			switch (cmd) {
				case "m" :
					dx = cx;
					dy = cy;
				case "M" :
					p.moveTo(cx = dx + f(parts[i]), cy = dy + f(parts[i + 1]));
					i += 2;
					break;

				case "l" :
					dx = cx;
					dy = cy;
				case "L" :
					p.lineTo(cx = dx + f(parts[i]), cy = dy + f(parts[i+1]));
					i += 2;
					break;

				case "h" :
					dx = cx;
				case "H" :
					p.lineTo(cx = dx + f(parts[i]), cy);
					i += 1;
					break;

				case "v" :
					dy = cy;
				case "V" :
					p.lineTo(cx, cy = dy + f(parts[i]));
					i += 1;
					break;

				case "c" :
					dx = cx;
					dy = cy;
				case "C" :
					p.curveTo(dx+f(parts[i]), dy+f(parts[i+1]), dx+f(parts[i+2]), dy+f(parts[i+3]), cx=dx+f(parts[4]), cy=dy+f(parts[i+5]));
					i += 6;
					break;

					
					// TODO Need a further look at S, treat them as q now but that 
					// sounds wrong
				case "q" : 
				case "s" :
					dx = cx;
					dy = cy;
				case "Q" :
				case "S" :
					p.quadTo(dx+f(parts[i]), dy+f(parts[i+1]), cx=dx+f(parts[2]), cy=dy+f(parts[i+3]));
					i += 4;
					break;

				case "z" :
				case "Z" :
					p.closePath();
					break;
			}
		}
		return p;
	}

	private double f(String string) {
		return Float.parseFloat(string);
	}

	private boolean isTrue(String string) {
		return string != null && !string.equalsIgnoreCase("false");
	}

	static Pattern	COLOR_P	= Pattern
									.compile("(?:#([A-F\\d]{3,3}))|(?:#([A-F\\d]{6,6}))|(?:rgb\\(\\s*(\\d*%?)\\s*,\\s*(\\d*%?)\\s*,\\s*(\\d*%?)\\s*\\))", Pattern.CASE_INSENSITIVE);

	private Color getColor(String name) {
		if (name == null)
			return null;

		name = name.toLowerCase();
		Color color = colors.get(name);
		if (color != null)
			return color;

		color = convert(name);
		if (color != null)
			colors.put(name, color);

		return color;
	}

	private Color convert(String name) {
		Matcher m = COLOR_P.matcher(name);
		if (m.matches()) {
			int r, g, b;
			if (m.group(2) != null) {
				r = Integer.parseInt(m.group(2).substring(0, 2), 16);
				g = Integer.parseInt(m.group(2).substring(2, 4), 16);
				b = Integer.parseInt(m.group(2).substring(4, 6), 16);
			} else if (m.group(1) != null) {
				r = Integer.parseInt(m.group(1).substring(0, 1), 16)*16;
				g = Integer.parseInt(m.group(1).substring(1, 2), 16)*16;
				b = Integer.parseInt(m.group(1).substring(2, 3), 16)*16;
			} else {
				r = percent(m.group(3));
				g = percent(m.group(4));
				b = percent(m.group(5));
			}
			return new Color(r,g,b);
		}

		return null;
	}

	private int percent(String value) {
		boolean perc = false;
		if (value.endsWith("%"))
			value = value.substring(0, value.length() - 1);
		float v = Float.parseFloat(value);
		if (perc)
			return (int) (v * 256 / 100);
		else
			return (int) v;
	}

	GeneralPath triangle(int ax, int ay, int bx, int by, int cx, int cy) {

		GeneralPath p = new GeneralPath();
		p.moveTo(ax, ay);
		p.lineTo(bx, by);
		p.lineTo(cx, cy);
		p.closePath();

		return p;
	}

	@Override
	public boolean render(Graphics2D g2, DiagramShape shape) {
		return true;
	}

	static {
		add("aliceblue", 240, 248, 255);
		add("antiquewhite", 250, 235, 215);
		add("aqua", 0, 255, 255);
		add("aquamarine", 127, 255, 212);
		add("azure", 240, 255, 255);
		add("beige", 245, 245, 220);
		add("bisque", 255, 228, 196);
		add("black", 0, 0, 0);
		add("blanchedalmond", 255, 235, 205);
		add("blue", 0, 0, 255);
		add("blueviolet", 138, 43, 226);
		add("brown", 165, 42, 42);
		add("burlywood", 222, 184, 135);
		add("cadetblue", 95, 158, 160);
		add("chartreuse", 127, 255, 0);
		add("chocolate", 210, 105, 30);
		add("coral", 255, 127, 80);
		add("cornflowerblue", 100, 149, 237);
		add("cornsilk", 255, 248, 220);
		add("crimson", 220, 20, 60);
		add("cyan", 0, 255, 255);
		add("darkblue", 0, 0, 139);
		add("darkcyan", 0, 139, 139);
		add("darkgoldenrod", 184, 134, 11);
		add("darkgray", 169, 169, 169);
		add("darkgreen", 0, 100, 0);
		add("darkgrey", 169, 169, 169);
		add("darkkhaki", 189, 183, 107);
		add("darkmagenta", 139, 0, 139);
		add("darkolivegreen", 85, 107, 47);
		add("darkorange", 255, 140, 0);
		add("darkorchid", 153, 50, 204);
		add("darkred", 139, 0, 0);
		add("darksalmon", 233, 150, 122);
		add("darkseagreen", 143, 188, 143);
		add("darkslateblue", 72, 61, 139);
		add("darkslategray", 47, 79, 79);
		add("darkslategrey", 47, 79, 79);
		add("darkturquoise", 0, 206, 209);
		add("darkviolet", 148, 0, 211);
		add("deeppink", 255, 20, 147);
		add("deepskyblue", 0, 191, 255);
		add("dimgray", 105, 105, 105);
		add("dimgrey", 105, 105, 105);
		add("dodgerblue", 30, 144, 255);
		add("firebrick", 178, 34, 34);
		add("floralwhite", 255, 250, 240);
		add("forestgreen", 34, 139, 34);
		add("fuchsia", 255, 0, 255);
		add("gainsboro", 220, 220, 220);
		add("ghostwhite", 248, 248, 255);
		add("gold", 255, 215, 0);
		add("goldenrod", 218, 165, 32);
		add("gray", 128, 128, 128);
		add("grey", 128, 128, 128);
		add("green", 0, 128, 0);
		add("greenyellow", 173, 255, 47);
		add("honeydew", 240, 255, 240);
		add("hotpink", 255, 105, 180);
		add("indianred", 205, 92, 92);
		add("indigo", 75, 0, 130);
		add("ivory", 255, 255, 240);
		add("khaki", 240, 230, 140);
		add("lavender", 230, 230, 250);
		add("lavenderblush", 255, 240, 245);
		add("lawngreen", 124, 252, 0);
		add("lemonchiffon", 255, 250, 205);
		add("lightblue", 173, 216, 230);
		add("lightcoral", 240, 128, 128);
		add("lightcyan", 224, 255, 255);
		add("lightgoldenrodyellow", 250, 250, 210);
		add("lightgray", 211, 211, 211);
		add("lightgreen", 144, 238, 144);
		add("lightgrey", 211, 211, 211);
		add("lightpink", 255, 182, 193);
		add("lightsalmon", 255, 160, 122);
		add("lightseagreen", 32, 178, 170);
		add("lightskyblue", 135, 206, 250);
		add("lightslategray", 119, 136, 153);
		add("lightslategrey", 119, 136, 153);
		add("lightsteelblue", 176, 196, 222);
		add("lightyellow", 255, 255, 224);
		add("lime", 0, 255, 0);
		add("limegreen", 50, 205, 50);
		add("linen", 250, 240, 230);
		add("magenta", 255, 0, 255);
		add("maroon", 128, 0, 0);
		add("mediumaquamarine", 102, 205, 170);
		add("mediumblue", 0, 0, 205);
		add("mediumorchid", 186, 85, 211);
		add("mediumpurple", 147, 112, 219);
		add("mediumseagreen", 60, 179, 113);
		add("mediumslateblue", 123, 104, 238);
		add("mediumspringgreen", 0, 250, 154);
		add("mediumturquoise", 72, 209, 204);
		add("mediumvioletred", 199, 21, 133);
		add("midnightblue", 25, 25, 112);
		add("mintcream", 245, 255, 250);
		add("mistyrose", 255, 228, 225);
		add("moccasin", 255, 228, 181);
		add("navajowhite", 255, 222, 173);
		add("navy", 0, 0, 128);
		add("oldlace", 253, 245, 230);
		add("olive", 128, 128, 0);
		add("olivedrab", 107, 142, 35);
		add("orange", 255, 165, 0);
		add("orangered", 255, 69, 0);
		add("orchid", 218, 112, 214);
		add("palegoldenrod", 238, 232, 170);
		add("palegreen", 152, 251, 152);
		add("paleturquoise", 175, 238, 238);
		add("palevioletred", 219, 112, 147);
		add("papayawhip", 255, 239, 213);
		add("peachpuff", 255, 218, 185);
		add("peru", 205, 133, 63);
		add("pink", 255, 192, 203);
		add("plum", 221, 160, 221);
		add("powderblue", 176, 224, 230);
		add("purple", 128, 0, 128);
		add("red", 255, 0, 0);
		add("rosybrown", 188, 143, 143);
		add("royalblue", 65, 105, 225);
		add("saddlebrown", 139, 69, 19);
		add("salmon", 250, 128, 114);
		add("sandybrown", 244, 164, 96);
		add("seagreen", 46, 139, 87);
		add("seashell", 255, 245, 238);
		add("sienna", 160, 82, 45);
		add("silver", 192, 192, 192);
		add("skyblue", 135, 206, 235);
		add("slateblue", 106, 90, 205);
		add("slategray", 112, 128, 144);
		add("slategrey", 112, 128, 144);
		add("snow", 255, 250, 250);
		add("springgreen", 0, 255, 127);
		add("steelblue", 70, 130, 180);
		add("tan", 210, 180, 140);
		add("teal", 0, 128, 128);
		add("thistle", 216, 191, 216);
		add("tomato", 255, 99, 71);
		add("turquoise", 64, 224, 208);
		add("violet", 238, 130, 238);
		add("wheat", 245, 222, 179);
		add("white", 255, 255, 255);
		add("whitesmoke", 245, 245, 245);
		add("yellow", 255, 255, 0);
		add("yellowgreen", 154, 205, 50);

	}
}
