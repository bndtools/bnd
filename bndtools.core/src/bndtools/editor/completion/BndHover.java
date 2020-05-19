package bndtools.editor.completion;

import java.util.stream.Collectors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;

import aQute.bnd.help.Syntax;
import aQute.bnd.osgi.Processor;
import bndtools.editor.BndEditor;

public class BndHover extends DefaultTextHover {

	private final BndEditor bndEditor;

	public BndHover(BndEditor bndEditor, ISourceViewer sourceViewer) {
		super(sourceViewer);
		this.bndEditor = bndEditor;
	}

	@Deprecated
	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {

		if (hoverRegion != null) {
			IDocument doc = textViewer.getDocument();
			try {
				String key = doc.get(hoverRegion.getOffset(), hoverRegion.getLength())
					.trim();
				if (key.indexOf('$') >= 0) {
					Processor properties = bndEditor.getModel()
						.getProperties();
					String replaced = properties.getReplacer()
						.process(key);
					if (properties.isOk())
						return replaced;

					return properties.getErrors()
						.stream()
						.collect(Collectors.joining());
				}

				Syntax syntax = Syntax.HELP.get(key);
				if (syntax == null)
					return null;

				StringBuilder sb = new StringBuilder();
				sb.append(syntax.getLead());
				sb.append("\nE.g. ");
				sb.append(syntax.getExample());

				String text = sb.toString();

				if (text.length() > 30) {
					text = wrap(text, 30);
				}
				if (!text.isEmpty())
					return text;

			} catch (Exception e) {
				return e + "";
			}
		}
		String info = super.getHoverInfo(textViewer, hoverRegion);
		return info;
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		IDocument doc = textViewer.getDocument();
		try {

			Point selectedRange = textViewer.getSelectedRange();
			if (selectedRange.y > 0) {
				return new Region(selectedRange.x, selectedRange.y);
			}

			int start = offset;
			int end = offset;
			while (start >= 0 && isWordChar(doc.getChar(start)))
				start--;

			while (end < doc.getLength() && isWordChar(doc.getChar(end)))
				end++;

			start++;
			int length = Math.min(doc.getLength(), end - start);
			start = Math.max(0, start);
			return new Region(start, length);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	static boolean isWordChar(char c) {
		return Character.isJavaIdentifierPart(c) || c == '-' || c == '.';
	}

	static String wrap(String text, int width) {
		StringBuilder sb = new StringBuilder();
		int n = 0;
		int r = 0;
		while (r < text.length()) {
			char c = text.charAt(r++);
			switch (c) {
				case '\r' :
				case '\n' :
					if (n != 0)
						sb.append('\n');
					n = 0;
					break;
				case ' ' :
				case '\t' :
					if (n > width) {
						sb.append("\n");
						n = 0;
					} else {
						sb.append(" ");
						n++;
					}
					break;
				default :
					sb.append(c);
					n++;
			}
		}
		return sb.toString();
	}

}
