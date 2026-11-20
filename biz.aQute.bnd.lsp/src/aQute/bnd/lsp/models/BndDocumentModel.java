package aQute.bnd.lsp.models;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.properties.BadLocationException;
import aQute.bnd.properties.Document;
import aQute.bnd.properties.IRegion;
import aQute.bnd.properties.LineTracker;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;

public class BndDocumentModel {
	private static final Pattern		MACRO_PATTERN	= Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)(?:;([^}]*))?\\}");

	private final String				uri;
	private final Document				document;
	private final List<BndHeaderEntry>	entries			= new ArrayList<>();
	private final List<BndToken>		tokens			= new ArrayList<>();

	public BndDocumentModel(String uri, String text) {
		this.uri = uri;
		this.document = new Document(text != null ? text : "");
		parse();
	}

	public synchronized void update(String text) {
		this.document.setText(text != null ? text : "");
		parse();
	}

	private void parse() {
		entries.clear();
		tokens.clear();

		String content = document.get();
		PropertiesLineReader reader = new PropertiesLineReader(document);

		int lineIndex = 0;
		int totalLines = document.getNumberOfLines();

		// Parse line-by-line using PropertiesLineReader
		try {
			LineType type;
			while ((type = reader.next()) != LineType.eof) {
				if (type == LineType.comment) {
					int lineNum = Math.min(lineIndex, totalLines - 1);
					try {
						IRegion region = document.getLineInformation(lineNum);
						String lineText = document.get(region.getOffset(), region.getLength());
						tokens.add(new BndToken(region.getOffset(), region.getLength(), lineNum, 0,
							BndTokenType.COMMENT, lineText));
					} catch (BadLocationException e) {
						// ignore
					}
					lineIndex++;
				} else if (type == LineType.entry) {
					// Extract entry details
					int startLine = lineIndex;
					// Parse macros in content
					lineIndex++;
				} else {
					lineIndex++;
				}
			}
		} catch (Exception e) {
			// fallback line scanning
		}

		// Comprehensive token and header extraction
		parseEntriesAndTokens(content);
	}

	private void parseEntriesAndTokens(String content) {
		int totalLines = document.getNumberOfLines();
		int currentLine = 0;

		while (currentLine < totalLines) {
			try {
				IRegion lineInfo = document.getLineInformation(currentLine);
				String lineStr = document.get(lineInfo.getOffset(), lineInfo.getLength());
				String trimmed = lineStr.trim();

				if (trimmed.isEmpty()) {
					currentLine++;
					continue;
				}

				if (trimmed.startsWith("#") || trimmed.startsWith("!")) {
					int commentChar = lineStr.indexOf(trimmed.charAt(0));
					tokens.add(new BndToken(lineInfo.getOffset() + commentChar, trimmed.length(), currentLine,
						commentChar, BndTokenType.COMMENT, trimmed));
					currentLine++;
					continue;
				}

				// Check for key-value pair
				int sepIndex = -1;
				char sepChar = '=';
				for (int i = 0; i < lineStr.length(); i++) {
					char c = lineStr.charAt(i);
					if (c == ':' || c == '=') {
						sepIndex = i;
						sepChar = c;
						break;
					}
				}

				if (sepIndex != -1) {
					String rawKey = lineStr.substring(0, sepIndex).trim();
					int keyStart = lineStr.indexOf(rawKey);
					int keyOffset = lineInfo.getOffset() + keyStart;
					int keyLength = rawKey.length();

					BndTokenType keyType = rawKey.startsWith("-") ? BndTokenType.INSTRUCTION : BndTokenType.HEADER;
					tokens.add(new BndToken(keyOffset, keyLength, currentLine, keyStart, keyType, rawKey));

					// Collect multi-line value with continuations
					StringBuilder fullValue = new StringBuilder();
					int valueStartLine = currentLine;
					int endLine = currentLine;
					int valueOffset = lineInfo.getOffset() + sepIndex + 1;

					String initialVal = lineStr.substring(sepIndex + 1);
					boolean hasContinuation = initialVal.trim().endsWith("\\");
					if (hasContinuation) {
						String cleaned = initialVal.trim();
						fullValue.append(cleaned, 0, cleaned.length() - 1);
					} else {
						fullValue.append(initialVal);
					}

					while (hasContinuation && endLine + 1 < totalLines) {
						endLine++;
						IRegion nextLineInfo = document.getLineInformation(endLine);
						String nextLineStr = document.get(nextLineInfo.getOffset(), nextLineInfo.getLength());
						String nextTrimmed = nextLineStr.trim();
						hasContinuation = nextTrimmed.endsWith("\\");
						if (hasContinuation) {
							fullValue.append(" ")
								.append(nextTrimmed, 0, nextTrimmed.length() - 1);
						} else {
							fullValue.append(" ")
								.append(nextTrimmed);
						}
					}

					int leadingWs = 0;
					while (sepIndex + 1 + leadingWs < lineStr.length()
						&& Character.isWhitespace(lineStr.charAt(sepIndex + 1 + leadingWs))) {
						leadingWs++;
					}
					int valueStartChar = sepIndex + 1 + leadingWs;
					int valueStartOffset = lineInfo.getOffset() + valueStartChar;

					String valueStr = fullValue.toString().trim();
					int totalValueLength = valueStr.length();

					// Parse clauses and macros
					List<BndHeaderClause> clauses = parseClauses(rawKey, valueStr, valueStartLine, endLine);
					List<BndMacroRef> macros = parseMacros(valueStr, valueStartLine, valueStartChar, valueStartOffset);

					BndHeaderEntry entry = new BndHeaderEntry(rawKey, valueStr, valueStartLine, endLine, keyOffset,
						keyLength, valueOffset, totalValueLength, clauses, macros);
					entries.add(entry);

					currentLine = endLine + 1;
				} else {
					currentLine++;
				}
			} catch (BadLocationException e) {
				currentLine++;
			}
		}
	}

	private List<BndHeaderClause> parseClauses(String key, String value, int startLine, int endLine) {
		List<BndHeaderClause> result = new ArrayList<>();
		if (value == null || value.isEmpty()) {
			return result;
		}

		try {
			Parameters params = OSGiHeader.parseHeader(value);
			for (Map.Entry<String, Attrs> entry : params.entrySet()) {
				String clauseName = entry.getKey();
				Attrs attrs = entry.getValue();

				int offset = value.indexOf(clauseName);
				int length = clauseName.length();

				Map<String, Integer> attrOffsets = new HashMap<>();
				if (attrs != null) {
					for (String attrKey : attrs.keySet()) {
						int attrIdx = value.indexOf(attrKey);
						if (attrIdx != -1) {
							attrOffsets.put(attrKey, attrIdx);
						}
					}
				}

				result.add(new BndHeaderClause(clauseName, attrs, offset, length, startLine, 0, endLine, 0,
					attrOffsets));
			}
		} catch (Exception e) {
			// ignore parse failure
		}
		return result;
	}

	private List<BndMacroRef> parseMacros(String text, int line, int baseChar, int baseOffset) {
		List<BndMacroRef> result = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return result;
		}

		Matcher matcher = MACRO_PATTERN.matcher(text);
		while (matcher.find()) {
			String name = matcher.group(1);
			String args = matcher.group(2);
			int start = matcher.start();
			int len = matcher.end() - start;
			int charPos = baseChar + start;
			int offsetPos = baseOffset + start;
			BndMacroRef ref = new BndMacroRef(name, args, offsetPos, len, line, charPos);
			result.add(ref);

			tokens.add(new BndToken(offsetPos, len, line, charPos, BndTokenType.MACRO, matcher.group(0)));
		}
		return result;
	}

	public String getUri() {
		return uri;
	}

	public String getText() {
		return document.get();
	}

	public int getLineCount() {
		return document.getNumberOfLines();
	}

	public IRegion getLineInformation(int line) throws BadLocationException {
		return document.getLineInformation(line);
	}

	public List<BndHeaderEntry> getHeaderEntries() {
		return Collections.unmodifiableList(entries);
	}

	public List<BndToken> getTokens() {
		return Collections.unmodifiableList(tokens);
	}

	public BndHeaderEntry getEntryAt(int line) {
		for (BndHeaderEntry entry : entries) {
			if (line >= entry.getStartLine() && line <= entry.getEndLine()) {
				return entry;
			}
		}
		return null;
	}

	public BndToken getTokenAt(int line, int character) {
		for (BndToken token : tokens) {
			if (token.contains(line, character)) {
				return token;
			}
		}
		return null;
	}

	public BndMacroRef getMacroAt(int line, int character) {
		for (BndHeaderEntry entry : entries) {
			for (BndMacroRef macro : entry.getMacros()) {
				if (macro.contains(line, character)) {
					return macro;
				}
			}
		}
		return null;
	}

	public Position offsetToPosition(int offset) {
		try {
			// Find line by walking line information
			int totalLines = document.getNumberOfLines();
			for (int i = 0; i < totalLines; i++) {
				IRegion info = document.getLineInformation(i);
				if (offset >= info.getOffset() && offset <= (info.getOffset() + info.getLength())) {
					return new Position(i, offset - info.getOffset());
				}
			}
			return new Position(0, 0);
		} catch (BadLocationException e) {
			return new Position(0, 0);
		}
	}

	public int positionToOffset(Position position) {
		try {
			IRegion info = document.getLineInformation(position.getLine());
			return info.getOffset() + position.getCharacter();
		} catch (BadLocationException e) {
			return 0;
		}
	}

	public Range createRange(int startLine, int startChar, int endLine, int endChar) {
		return new Range(new Position(startLine, startChar), new Position(endLine, endChar));
	}
}
