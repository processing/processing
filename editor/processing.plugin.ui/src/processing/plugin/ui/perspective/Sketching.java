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
package processing.plugin.ui.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/** The perspective for writing sketches. */
public class Sketching implements IPerspectiveFactory {

	/* (non-JavaDoc) organization suggested by http://www.eclipse.org/articles/using-perspectives/PerspectiveArticle.html */
	public void createInitialLayout(IPageLayout layout) {
		//Editors are in assumed to be in the center, so there is no placement info for them.
		String editorAreaIdentifier = layout.getEditorArea();

		// Left set of stuff
		float leftPanelSizeAsRatio = 0.2f;
		IFolderLayout leftPanels =
			layout.createFolder("left", IPageLayout.LEFT, leftPanelSizeAsRatio, editorAreaIdentifier);

		leftPanels.addView("processing.plugin.ui.views.sketchView");
			
		// Bottom set of tabs
		// strangely, this number is the screen ratio the bottom panel should *not* take up
		// which is different than the fast view size as ratio
		float bottomPanelSizeAsInverseRatio = 0.7f;
		IFolderLayout bottomPanels = 
			layout.createFolder("bottom", IPageLayout.BOTTOM, bottomPanelSizeAsInverseRatio, editorAreaIdentifier);
		bottomPanels.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottomPanels.addView("org.eclipse.ui.console.ConsoleView");	}
	
	/* //TODO Fix the toolbars
	 * Frustratingly I cannot figure out much here. Toolbar contributions
	 * seem to be set in plugin extensions, and I don't see any API for 
	 * changing that in the perspective. Menu contributions are primarily
	 * controlled by plugin manifest entries. It would seem that we are
	 * looking at specifying a lot of commands as contributions with this
	 * perspective as the filter. 
	 * 
	 * Also, the documentation regarding any recent changes to this has
	 * only been updated to point out that things are broken and it
	 * has not been updated. It predates commands, so I'm not surprised.
	 * Things have only gotten more complicated since then.
	 * 
	 * Maybe try to implement some functions that purge the menus and call
	 * them here. Rely on the commands API's for populating the toolbar as
	 * the PDE toolbars are -- run, stop?, new, open, save, export  
	 * 
	 * For now, the Java perspective may actually be a better way for
	 * people to interact with the sketch.
	 * 
	 * @see Eclipse Bug 36968 -  "[Contributions] Improve action contributions "
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=36968   
	 */

}
