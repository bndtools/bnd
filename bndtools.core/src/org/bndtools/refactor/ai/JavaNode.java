package org.bndtools.refactor.ai;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.swt.graphics.Image;

public class JavaNode implements ITypedElement, IStreamContentAccessor, IEditableContent {
    private String name;
    private String content;

    public JavaNode(String name, String content) {
        this.name = name;
        this.content = content;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Image getImage() {
        return null; // You can return an appropriate image for Java files here
    }

    @Override
    public String getType() {
        return "java"; // Return the type of the node, in this case, Java
    }

    @Override
    public InputStream getContents() {
        return new ByteArrayInputStream(content.getBytes());
    }

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public void setContent(byte[] newContent) {
		content = new String(newContent, StandardCharsets.UTF_8);
	}

	@Override
	public ITypedElement replace(ITypedElement dest, ITypedElement src) {
		return null;
	}
}
