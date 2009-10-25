package name.neilbartlett.eclipse.jareditor.internal;

import java.util.jar.JarEntry;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class JARContentMasterDetailsBlock extends MasterDetailsBlock {

	private JARContentTreePart contentTreePart;
	private JARContentDetailsPage jarContentDetailsPage;
	
	private Object input = null;

	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();
		
		Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
		contentTreePart = new JARContentTreePart(section, managedForm);
		contentTreePart.setFormInput(input);
		
		parent.setLayout(new FillLayout());
	}

	protected void createToolBarActions(IManagedForm managedForm) {
		// TODO
	}

	protected void registerPages(DetailsPart detailsPart) {
		detailsPart.setPageProvider(new IDetailsPageProvider() {
			public Object getPageKey(Object object) {
				if(object instanceof JarEntry)
					return JarEntry.class;
				
				return object.getClass();
			}
			public IDetailsPage getPage(Object key) {
				return null;
			}
		});
		
		jarContentDetailsPage = new JARContentDetailsPage();
		jarContentDetailsPage.setFormInput(input);
		
		detailsPart.registerPage(JarEntry.class, jarContentDetailsPage);
		detailsPart.registerPage(ZipTreeNode.class, jarContentDetailsPage);
	}

	public void setMasterPartInput(Object input) {
		this.input = input;
		if(contentTreePart != null && !contentTreePart.getSection().isDisposed())
			contentTreePart.setFormInput(input);
		if(jarContentDetailsPage != null)
			jarContentDetailsPage.setFormInput(input);
	}

}
