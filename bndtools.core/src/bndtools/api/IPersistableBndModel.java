package bndtools.api;

import java.io.IOException;

import org.eclipse.jface.text.IDocument;

public interface IPersistableBndModel extends IBndModel {

    void loadFrom(IDocument document) throws IOException;

    void saveChangesTo(IDocument document);

}
