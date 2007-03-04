package jasko.tim.lisp.editors.outline;

import jasko.tim.lisp.*;
import jasko.tim.lisp.editors.*;
import jasko.tim.lisp.swank.*;

import java.util.*;

import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.contentoutline.*;

/**
 * TODO: Make this not clear out and repopulate every time we save the document.
 *  A clever man could get it to only remove those items which were removed and add only those which we added.
 *  A <i>really</i> clever man could get it to update <i>as the user types</i>.
 *  Clever would be more than enough.
 * @author Tim Jasko
 *
 */
public class LispOutlinePage extends ContentOutlinePage implements MouseListener, KeyListener {
	private enum Sort {
		Alpha,
		Type
	}
	Sort sort = Sort.Alpha;
	IAction sortType;
	IAction sortAlpha;
	
	LispEditor editor;
	private ArrayList<OutlineItem> items = new ArrayList<OutlineItem>();
	
	
	public LispOutlinePage(LispEditor editor) {
		this.editor = editor;
	}
	
	public void makeContributions(IMenuManager menuMgr,
         IToolBarManager toolBarMgr,
         IStatusLineManager statusLineMgr) {
		
		sortAlpha = new Action("Sort by name") {
			public void run() {
				sort = Sort.Alpha;
				this.setChecked(true);
				sortType.setChecked(false);
				sortItems();
				redoTree();
			}
		};
		sortAlpha.setImageDescriptor(
				LispImages.getImageDescriptor(LispImages.SORT_ALPHA));
		sortAlpha.setChecked(true);
		sortAlpha.setToolTipText("Sort by name");
		
		
		sortType = new Action("Sort by type") {
			public void run() {
				sort = Sort.Type;
				this.setChecked(true);
				sortAlpha.setChecked(false);
				sortItems();
				redoTree();
			}
		};
		sortType.setImageDescriptor(
				LispImages.getImageDescriptor(LispImages.SORT_TYPE));
		sortType.setChecked(false);
		sortType.setToolTipText("Sort by type");
		
		toolBarMgr.add(sortAlpha);
		toolBarMgr.add(sortType);
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		IDocument doc = editor.getDocumentProvider().getDocument(
				  editor.getEditorInput());
		LispNode file = LispParser.parse(doc.get() + "\n)");
		
		getTreeViewer().getControl().addMouseListener(this);
		getTreeViewer().getControl().addKeyListener(this);
		
		fillTree(file);
	}
	
	private void fillTree(LispNode file) {
		items = new ArrayList<OutlineItem>(file.params.size());
		for (LispNode exp: file.params) {
			//System.out.println(exp);
			OutlineItem item = new OutlineItem();
			
			item.type = exp.get(0).value.toLowerCase();
			item.name = exp.get(1).toLisp();
			item.offset = exp.offset;
			if (! item.type.startsWith("def")) {
				item.name = item.type;
				if (item.type.equals("in-package")) {
					item.name = "in-package " + exp.get(1).toLisp(); 
				}
			} else if (item.type.equals("defstruct")) {
				LispNode name = exp.get(1); 
				if (!name.value.equals("")) {
					item.name = name.value;
				} else {
					item.name = name.get(0).value;
				}
			} else if (item.type.equals("defmethod")) {
				String name = exp.get(2).toLisp();
				if (name.startsWith(":")) {
					item.name += " " + name + " " + exp.get(3).toLisp();
				} else {
					item.name += " " + name;
				}
			}
			
			if (item.name.equals("")) {
				if (exp.params.size() >= 2) {
					if (exp.get(1).toLisp().startsWith(":")) {
						item.name = exp.get(1).toLisp() + " " + exp.get(2).toLisp();
					} else {
						item.name = exp.get(1).toLisp();
					}
				}
			}
			
			if (! item.name.equals("")) {
				items.add(item);
			}
		}
		
		sortItems();
		
		redoTree();
	}
	
	private void sortItems() {
		if (sort == Sort.Alpha) {
			Collections.<OutlineItem>sort(items);
		} else if (sort == Sort.Type) {
			Collections.<OutlineItem>sort(items, new TypeComparator());
		} else { // Really shouldn't ever happen
			Collections.<OutlineItem>sort(items);
		}
	}
	
	private void redoTree() {
		
		getControl().setRedraw(false);
		Tree tree = getTreeViewer().getTree();
		tree.removeAll();
		String currType = "()"; //impossible type
		TreeItem category = null;
		for (OutlineItem item: items) {
			TreeItem temp;
			if (sort == Sort.Alpha) {
				temp = new TreeItem(tree, SWT.NONE);
			} else { // sort by type
				if (!item.type.equals(currType)) {
					currType = item.type;
					category = new TreeItem(tree, SWT.NONE);
					category.setText(currType);
					category.setImage(getImageForType(currType));
					category.setData("");
				}
				temp = new TreeItem(category, SWT.NONE);
			}
			
			temp.setImage(getImageForType(item.type));
			temp.setText(item.name);
			temp.setData(item);
			
		}
		
		getControl().setRedraw(true);
	}
	
	private Image getImageForType(String type) {
		if (type.startsWith("def")) {
			if (type.endsWith("class") || type.endsWith("component")) {
				return LispImages.getImage(LispImages.DEFCLASS);
			} else if (type.endsWith("constant")) {
				return LispImages.getImage(LispImages.DEFCONSTANT);
			} else if (type.endsWith("generic")) {
				return LispImages.getImage(LispImages.DEFGENERIC);
			} else if (type.endsWith("macro")) {
				return LispImages.getImage(LispImages.DEFMACRO);
			} else if (type.endsWith("method")) {
				return LispImages.getImage(LispImages.DEFMETHOD);
			} else if (type.endsWith("package")) {
				return LispImages.getImage(LispImages.DEFPACKAGE);
			} else if (type.endsWith("system")) {
				return LispImages.getImage(LispImages.DEFSYSTEM);
			} else if (type.endsWith("parameter")) {
				return LispImages.getImage(LispImages.DEFPARAMETER);
			} else if (type.endsWith("struct")) {
				return LispImages.getImage(LispImages.DEFSTRUCT);
			} else if (type.endsWith("fun")) {
				return LispImages.getImage(LispImages.DEFUN);
			} else if (type.endsWith("action")) {
				return LispImages.getImage(LispImages.DEFACTION);
			} else if (type.endsWith("var")) {
				return LispImages.getImage(LispImages.DEFVAR);
			} else if (type.endsWith("type")) {
				return LispImages.getImage(LispImages.DEFTYPE);
			} else { // Well, they're probably defining *something*
				return LispImages.getImage(LispImages.DEFOTHER);
			}
		} else if (type.equals("in-package")) {
			return LispImages.getImage(LispImages.IN_PACKAGE);
		} else {
			return LispImages.getImage(LispImages.OTHER);
		}
	}
	
	private OutlineItem lastSelection;
	
	public void selectionChanged(SelectionChangedEvent event) {
		try {
			IStructuredSelection sel = (IStructuredSelection) event.getSelection();
			
			if (! sel.isEmpty()) {
				if (sel.getFirstElement() instanceof OutlineItem) {
					OutlineItem item = (OutlineItem) sel.getFirstElement();
					if (item != lastSelection) {
						lastSelection = item;
						editor.selectAndReveal(item.offset, item.type.length() + 1);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // void selectionChanged( ... )
	
	public void mouseDown(MouseEvent e) {
		IStructuredSelection sel = (IStructuredSelection) getTreeViewer().getSelection();
		if (! sel.isEmpty()) {
			if (sel.getFirstElement() instanceof OutlineItem) {
				OutlineItem item = (OutlineItem) sel.getFirstElement();
				lastSelection = item;
				editor.selectAndReveal(item.offset, item.type.length() + 1);
			}
		}
	}
	
	public void keyPressed(KeyEvent e) {
		String c = "" + e.character;
		
		for (TreeItem node: getTreeViewer().getTree().getItems()) {
			if (node.getText().startsWith(c)) {
				getTreeViewer().getTree().setSelection(node);
				if (node.getData() instanceof OutlineItem) {
					OutlineItem item = (OutlineItem) node.getData();
					lastSelection = item;
					editor.selectAndReveal(item.offset, item.type.length() + 1);
				}
				return;
			}
		}
	}
	
	
	/**
	 * Updates the content view to reflect changes in the file.
	 */
	public void update(LispNode file) {
		fillTree(file);
	} // void update()
	
	
	
	private class OutlineItem implements Comparable<OutlineItem> {
		public String name;
		public int offset;
		public String type;
		
		public int compareTo(OutlineItem o) {
			return name.toLowerCase().compareTo( 
				o.name.toLowerCase() );
		}
	}
	
	private class TypeComparator implements Comparator<OutlineItem> {
		public int compare(OutlineItem arg0, OutlineItem arg1) {
			return arg0.type.toLowerCase().compareTo(arg1.type.toLowerCase());
		}
	}
	
	

	public void mouseDoubleClick(MouseEvent e) {
		// meh
	}

	public void mouseUp(MouseEvent e) {
		// also meh
	}

	public void keyReleased(KeyEvent e) {
		// powers of meh combine!
	}
	

}