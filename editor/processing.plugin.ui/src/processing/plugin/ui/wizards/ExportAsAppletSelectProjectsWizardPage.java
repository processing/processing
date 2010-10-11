/**
 * Copyright (c) 2010 Chris Lonnen. All rights reserved.
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * Contributors:
 *     Chris Lonnen - initial API and implementation
 */
package processing.plugin.ui.wizards;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import processing.plugin.core.builder.SketchProject;

/** And export wizard page allowing users to select which projects are going to be exported. */
public class ExportAsAppletSelectProjectsWizardPage extends WizardPage {

	/** Internal class for building the export wizard table. */
	private class ProjectTableContentProvider implements IStructuredContentProvider{

		ArrayList<ProjectTableEntryModel> entries;

		public ProjectTableContentProvider(){
			this.updateProjectList();
		}
		
		public void dispose() { entries = null; }

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.updateProjectList();
		}

		/** Dumps the list of open SketchProjects */
		public Object[] getElements(Object inputElement) {
			return entries.toArray();
		}

		/** Grabs all open sketch projects from the workplace and adds them to the array */
		public void updateProjectList(){
			entries = new ArrayList<ProjectTableEntryModel>();
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for(IProject p: projects){
				if (p == null) break;
				if (!p.isAccessible()) break;
				if (SketchProject.isSketchProject(p)){
					ProjectTableEntryModel entry = new ProjectTableEntryModel();
					if (entry.setEntry(p)) entries.add(entry);
				}
			}
		}

	}

	/** A model for a sketch project and a message about its last build. */
	public class ProjectTableEntryModel {

		private SketchProject sp;

		public ProjectTableEntryModel(){}

		public boolean setEntry(IProject project){
			SketchProject candidate = SketchProject.forProject(project);
			return setEntry(candidate);
		}

		public boolean setEntry(SketchProject project){
			if (project == null) return false;
			sp = project;
//			System.out.println("Adding: " + project.getProject().getName());
			return true;
		}

		public SketchProject getProject(){	return sp; }

		public String getAdditionalMessage(){
			return (sp.wasLastBuildSuccessful()) ? "" : "Warning: last build unsuccessful. May not export.";
		}

	}

	/** Provides text for a model entry item */
	public class ProjectTableLabelProvider implements ITableLabelProvider{

		// do nothing
		public void addListener(ILabelProviderListener listener) { }

		// do nothing
		public void dispose() {	}

		// do nothing
		public boolean isLabelProperty(Object element, String property) { return false;	}

		// do nothing
		public void removeListener(ILabelProviderListener listener) { }

		// no images
		public Image getColumnImage(Object element, int columnIndex) { return null; }

		/** Gets the column text */
		public String getColumnText(Object element, int columnIndex) {
//			System.out.println(element.toString());
//			System.out.println(columnIndex);
			switch (columnIndex) {
			case 0: // project
				if (element instanceof ProjectTableEntryModel)
					return ((ProjectTableEntryModel) element).getProject().getProject().getName();
				if (element != null)
					element.toString();
				return "";
			case 1: // warning message
				if (element instanceof ProjectTableEntryModel)
					return ((ProjectTableEntryModel) element).getAdditionalMessage();
				return "";
			default:
				return "";
			}
		}

	}

	/** a checkbox table of workspace projects */
	private CheckboxTableViewer projectTable;

	/* constructor */
	protected ExportAsAppletSelectProjectsWizardPage(String pageName) {
		super(pageName);
		setTitle("Export Sketch as Applet Wizard");
		setDescription("Select the Sketch projects to be exported.");
	}

	/* create the GUI stuff */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new FormLayout());
		setControl(container);

		projectTable = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
		projectTable.setContentProvider(new ProjectTableContentProvider());
		projectTable.setLabelProvider(new ProjectTableLabelProvider());

		final FormData formData = new FormData();
		formData.bottom = new FormAttachment(95,0);
		formData.right = new FormAttachment(95,0);
		formData.top = new FormAttachment(5,0);
		formData.left = new FormAttachment(5,0);

		final Table table = projectTable.getTable();
		table.setLayoutData(formData);
		table.setHeaderVisible(true);

		final TableColumn leftTableColumn = new TableColumn(table,SWT.NONE);
		leftTableColumn.setWidth(200);
		leftTableColumn.setText("Name");

		final TableColumn rightTableColumn = new TableColumn(table,SWT.NONE);
		rightTableColumn.setWidth(250);
		rightTableColumn.setText("Notes");
	}
	
	/** 
	 * Override of setVisible to update and display the content only
	 * when the page is becoming visible.
	 * <p>
	 * {@inheritDoc} 
	 */
	public void setVisible(boolean visible){
		if (visible) projectTable.setInput(visible);
		/* setInput needs an object, but it really doesn't matter what is
		 * handed to it as long as the content provider is the private class 
		 * ProjectTableConentProvider, because that provider ignores the object
		 * and builds everything itself. */
		super.setVisible(visible);
	}
	
	public SketchProject[] getSelectedProjects(){
		Object[] elements = projectTable.getCheckedElements();
		SketchProject[] selectedProjects = new SketchProject[elements.length];
		for(int i=0; i<elements.length; i++){
			 selectedProjects[i] = ((ProjectTableEntryModel) elements[i]).getProject();
		}
		return selectedProjects;
	}

}
