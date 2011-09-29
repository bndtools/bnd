package bndtools.editor.common;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Table;

/**
 * Well ... the JFace <CODE>TableViewer<CODE> class does not provide
 * text celleditor keyboard support like if TAB is pressed in a column,
 * the focus does not go into the next column etc.,
 * 
 * After observing JDT method signature refactoring widget which does the
 * desired keyboard support, a generic utility listener has been developed
 * on the same lines.
 * 
 * Usage goes as ...
 * 
 * <P><PRE><CODE>
 * 
 *		TableViewer tblViewer = new TableViewer(SWT.SINGLE | SWT.FULL_SELECTION | SWT.| SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER)
 *		...
 *		CellEditor[] cellEditors = ...
 *		...
 *		tblViewer.setCellEditors(cellEditors);
 *		
 * 		TableViewerKeyBoardSupporter supporter = new TableViewerKeyBoardSupporter(tblViewer);
 * 		supporter.startSupport();
 *  
 * </CODE></PRE></P>
 */
public class TableViewerKeyBoardSupporter				
{
	protected TableViewer fTableViewer = null;		
	
	public TableViewerKeyBoardSupporter(TableViewer tableViewer)
	{
		fTableViewer = tableViewer;		
	}
	
	/**
	 * After the cell editors have been set on tableviewer, this method
	 * should be called to start giving keyboard support.
	 */
	public void startSupport()
	{
		/* add table key listener */
		getTable().addKeyListener(new TableKeyListener(this));
		
		/* add table traverse listener */
		getTable().addTraverseListener(new TableTraverseListener(this));
		
		/* add table-textcelleditors key and traverse listeners */
		CellEditor[] cellEditors = fTableViewer.getCellEditors();
		if( cellEditors != null )
		{
			for(int colIndex=0; colIndex<cellEditors.length; colIndex++)
			{
				CellEditor cellEditor = cellEditors[colIndex];
				if( cellEditor != null )
				{
                    // cellEditor.getControl().addKeyListener(new
                    // CellEditorKeyListener(this, cellEditor, colIndex));
					cellEditor.getControl().addTraverseListener(new 
							CellEditorTraverseListener(this, cellEditor, colIndex));
				}					
			}
		}		
	}
	
	protected TableViewer getTableViewer()
	{
		return fTableViewer;
	}
	
	protected Table getTable()
	{
		return fTableViewer.getTable();
	}
	
	protected int nextColumn(int column) {
		return (column >= getTable().getColumnCount() - 1) ? 0 : column + 1;
	}
	
	protected int prevColumn(int column) {
		return (column <= 0) ? getTable().getColumnCount() - 1 : column - 1;
	}
	
	protected void editColumnOrNextPossible(final int column)
	{
		Object selectedElem = getSelectedElement();
		if( selectedElem == null )
			return;
		
		int nextColumn= column;
		do {
			fTableViewer.editElement(selectedElem, nextColumn);
			if (fTableViewer.isCellEditorActive())
				return;
			nextColumn = nextColumn(nextColumn);
		} while (nextColumn != column);
	}
	
	protected void editColumnOrPrevPossible(int column)
	{
		Object selectedElem = getSelectedElement();
		if( selectedElem == null )
			return;
		
		int prevColumn= column;
		do {
			fTableViewer.editElement(selectedElem, prevColumn);
			if (fTableViewer.isCellEditorActive())
			    return;
			prevColumn= prevColumn(prevColumn);
		} while (prevColumn != column);
	}
	
	private Object getSelectedElement() {
		IStructuredSelection selection = (IStructuredSelection)fTableViewer.getSelection();
		if( selection == null || selection.isEmpty() )
			return null;

		return selection.getFirstElement();
	}
}

class CellEditorKeyListener extends KeyAdapter
{
	private TableViewerKeyBoardSupporter fKeyBoardSupporter = null;	
	private CellEditor fEditor = null;
	private int fEditorColumn = -1;	
	
	public CellEditorKeyListener(TableViewerKeyBoardSupporter keyBoardSupporter, CellEditor editor, int editorColumn)
	{
		fKeyBoardSupporter = keyBoardSupporter;
		fEditor = editor;
		fEditorColumn = editorColumn;
	}
	
	@Override
    public void keyPressed(KeyEvent e) {
		if (e.stateMask == SWT.MOD1 || e.stateMask == SWT.MOD2)
		{
			if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
			    // allow starting multi-selection even if in edit mode
				fEditor.deactivate();
				e.doit= false;
				return;
			}
		}
		
		if (e.stateMask != SWT.NONE)
			return;
		
		switch (e.keyCode)
		{
			case SWT.ARROW_DOWN :
				e.doit= false;
				int nextRow= fKeyBoardSupporter.getTable().getSelectionIndex() + 1;
				if (nextRow >= fKeyBoardSupporter.getTable().getItemCount())
					break;
				fKeyBoardSupporter.getTable().setSelection(nextRow);
				fKeyBoardSupporter.editColumnOrPrevPossible(fEditorColumn);
				break;
				
			case SWT.ARROW_UP :
				e.doit= false;
				int prevRow= fKeyBoardSupporter.getTable().getSelectionIndex() - 1;
				if (prevRow < 0)
					break;
				fKeyBoardSupporter.getTable().setSelection(prevRow);
				fKeyBoardSupporter.editColumnOrPrevPossible(fEditorColumn);
				break;
				
			case SWT.F2 :
				e.doit= false;
				fEditor.deactivate();			
				break;
		}
	}	
}

class CellEditorTraverseListener implements TraverseListener
{	
	private TableViewerKeyBoardSupporter fKeyBoardSupporter = null;
	private CellEditor fEditor = null;
	private int fEditorColumn = -1;
	
	public CellEditorTraverseListener(TableViewerKeyBoardSupporter keyBoardSupporter, CellEditor editor, int editorColumn)
	{		
		fKeyBoardSupporter = keyBoardSupporter;
		fEditor = editor;
		fEditorColumn = editorColumn;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.TraverseListener#keyTraversed(org.eclipse.swt.events.TraverseEvent)
	 */
	public void keyTraversed(TraverseEvent e) {
		
		switch (e.detail) {
			case SWT.TRAVERSE_TAB_NEXT :
				fKeyBoardSupporter.editColumnOrNextPossible(fKeyBoardSupporter.nextColumn(fEditorColumn));
				e.detail= SWT.TRAVERSE_NONE;
				break;
	
			case SWT.TRAVERSE_TAB_PREVIOUS :
				fKeyBoardSupporter.editColumnOrPrevPossible(fKeyBoardSupporter.prevColumn(fEditorColumn));
				e.detail= SWT.TRAVERSE_NONE;
				break;
			
			case SWT.TRAVERSE_ESCAPE :
				fKeyBoardSupporter.getTableViewer().cancelEditing();
				e.detail= SWT.TRAVERSE_NONE;
				break;
			
			case SWT.TRAVERSE_RETURN :
				fEditor.deactivate();
				e.detail= SWT.TRAVERSE_NONE;
				break;
				
			default :
				break;
		}		
	}	
}

class TableTraverseListener implements TraverseListener
{
	private TableViewerKeyBoardSupporter fKeyBoardSupporter = null;
	
	public TableTraverseListener(TableViewerKeyBoardSupporter keyBoardSupporter)
	{		
		fKeyBoardSupporter = keyBoardSupporter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.TraverseListener#keyTraversed(org.eclipse.swt.events.TraverseEvent)
	 */
	public void keyTraversed(TraverseEvent e) {
		if (e.detail == SWT.TRAVERSE_RETURN && e.stateMask == SWT.NONE) {
			fKeyBoardSupporter.editColumnOrNextPossible(0);
			e.detail= SWT.TRAVERSE_NONE;
		}
	}	
}

class TableKeyListener extends KeyAdapter
{
	private TableViewerKeyBoardSupporter fKeyBoardSupporter = null;
	
	public TableKeyListener(TableViewerKeyBoardSupporter keyBoardSupporter)
	{		
		fKeyBoardSupporter = keyBoardSupporter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	 */
	@Override
    public void keyPressed(KeyEvent e) {
		if (e.keyCode == SWT.F2 && e.stateMask == SWT.NONE) {
			fKeyBoardSupporter.editColumnOrNextPossible(0);
			e.doit= false;
		}
	}
}