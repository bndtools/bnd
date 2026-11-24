package aQute.bnd.lsp.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensLegend;

import aQute.bnd.lsp.models.BndDocumentModel;
import aQute.bnd.lsp.models.BndToken;
import aQute.bnd.lsp.models.BndTokenType;

public class BndSemanticTokensProvider {
	public static final List<String>	TOKEN_TYPES		= Arrays.asList("keyword", "property", "variable", "string",
		"number", "comment", "macro", "operator", "type");
	public static final List<String>	TOKEN_MODIFIERS	= Arrays.asList("declaration", "definition", "readonly");

	public static SemanticTokensLegend getLegend() {
		return new SemanticTokensLegend(TOKEN_TYPES, TOKEN_MODIFIERS);
	}

	public SemanticTokens provideSemanticTokens(BndDocumentModel model) {
		List<Integer> data = new ArrayList<>();
		List<BndToken> tokens = model.getTokens();

		int prevLine = 0;
		int prevChar = 0;

		for (BndToken token : tokens) {
			int line = token.getLine();
			int character = token.getCharacter();
			int length = token.getLength();

			int deltaLine = line - prevLine;
			int deltaChar = (deltaLine == 0) ? (character - prevChar) : character;

			int typeIndex = mapTokenType(token.getType());
			int modifierBits = 0;

			data.add(deltaLine);
			data.add(deltaChar);
			data.add(length);
			data.add(typeIndex);
			data.add(modifierBits);

			prevLine = line;
			prevChar = character;
		}

		return new SemanticTokens(data);
	}

	private int mapTokenType(BndTokenType type) {
		switch (type) {
			case INSTRUCTION :
				return 1; // property
			case HEADER :
				return 0; // keyword
			case DIRECTIVE :
				return 7; // operator
			case ATTRIBUTE :
				return 2; // variable
			case MACRO :
				return 6; // macro
			case VALUE :
				return 3; // string
			case COMMENT :
				return 5; // comment
			case KEYWORD :
				return 0; // keyword
			case VARIABLE :
				return 2; // variable
			case STRING :
				return 3; // string
			case NUMBER :
				return 4; // number
			default :
				return 3;
		}
	}
}
