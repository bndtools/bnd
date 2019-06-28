package bndtools.editor.completion;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;

import aQute.bnd.help.Syntax;

public class BndHover extends DefaultTextHover {

	public BndHover(ISourceViewer sourceViewer) {
		super(sourceViewer);
	}

	@Deprecated
	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		String info = super.getHoverInfo(textViewer, hoverRegion);
		if (info != null)
			return info;

		if (hoverRegion != null) {
			IDocument doc = textViewer.getDocument();
			try {
				String key = doc.get(hoverRegion.getOffset(), hoverRegion.getLength());

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
				return text;
			} catch (Exception e) {
				return e + "";
			}
		}
		return null;
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		IDocument doc = textViewer.getDocument();
		try {
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
