package org.bndtools.builder.handlers;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.bndtools.api.Logger;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;

import aQute.bnd.properties.IRegion;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;

public class BndHeaderQuickFix implements ICompletionProposal, IMarkerResolution {

	private final String	header;
	private final String	value;
	private final String	label;
	private Point			selection;

	public BndHeaderQuickFix(String header, String value, String label) {
		this.header = header;
		this.value = value;
		this.label = label;
	}

	@Override
	public void apply(IDocument document) {
		replace(document);
	}

	public static boolean hasHeader(IMarker marker, String header) {
		if (!(marker.getResource() instanceof IFile file))
			return false;
		try {
			ITextFileBuffer buffer = FileBuffers.getTextFileBufferManager()
				.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
			IDocument document;
			if (buffer != null)
				document = buffer.getDocument();
			else {
				try (var input = file.getContents()) {
					document = new Document(new String(input.readAllBytes(), Charset.forName(file.getCharset())));
				}
			}
			PropertiesLineReader reader = new PropertiesLineReader(document.get());
			for (LineType type = reader.next(); type != LineType.eof; type = reader.next()) {
				if (type == LineType.entry && header.equals(reader.key()))
					return true;
			}
		} catch (Exception e) {
			Logger.getLogger(BndHeaderQuickFix.class).logError("Error reading " + header, e);
		}
		return false;
	}

	private boolean replace(IDocument document) {
		try {
			List<IRegion> regions = new ArrayList<>();
			PropertiesLineReader reader = new PropertiesLineReader(document.get());
			for (LineType type = reader.next(); type != LineType.eof; type = reader.next()) {
				if (type == LineType.entry && header.equals(reader.key()))
					regions.add(reader.region());
			}
			if (regions.isEmpty())
				return false;
			for (int index = regions.size() - 1; index >= 0; index--) {
				IRegion region = regions.get(index);
				int start = region.getOffset();
				int end = start + region.getLength();
				String replacement = value == null ? "" : header + ": " + value;
				if (value == null) {
					if (end < document.getLength() && document.getChar(end) == '\r')
						end++;
					if (end < document.getLength() && document.getChar(end) == '\n')
						end++;
				}
				document.replace(start, end - start, replacement);
				selection = new Point(start + replacement.length(), 0);
				if (value != null)
					break;
			}
			return true;
		} catch (Exception e) {
			Logger.getLogger(BndHeaderQuickFix.class).logError("Error changing " + header, e);
			return false;
		}
	}

	@Override
	public void run(IMarker marker) {
		run(marker, FileBuffers.getTextFileBufferManager());
	}

	void run(IMarker marker, ITextFileBufferManager manager) {
		if (!(marker.getResource() instanceof IFile file))
			return;
		var path = file.getFullPath();
		try {
			manager.connect(path, LocationKind.IFILE, null);
			try {
				ITextFileBuffer buffer = manager.getTextFileBuffer(path, LocationKind.IFILE);
				boolean dirty = buffer.isDirty();
				if (replace(buffer.getDocument()) && !dirty)
					buffer.commit(null, false);
			} finally {
				manager.disconnect(path, LocationKind.IFILE, null);
			}
		} catch (Exception e) {
			Logger.getLogger(BndHeaderQuickFix.class).logError("Error changing " + header, e);
		}
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public String getDisplayString() {
		return label;
	}

	@Override
	public Point getSelection(IDocument document) {
		return selection;
	}

	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	@Override
	public Image getImage() {
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
}