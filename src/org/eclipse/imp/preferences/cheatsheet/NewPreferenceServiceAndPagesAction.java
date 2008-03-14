/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

/*
 * Created on 28 Feb 2007
 */
package org.eclipse.imp.preferences.cheatsheet;

import org.eclipse.imp.preferences.wizards.NewPreferencesSpecificationWizard;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;

public class NewPreferenceServiceAndPagesAction extends Action implements ICheatSheetAction {
    public NewPreferenceServiceAndPagesAction() {
	this("Create a new syntax highlighter");
    }

    public NewPreferenceServiceAndPagesAction(String text) {
	super(text, null);
    }

    public void run(String[] params, ICheatSheetManager manager) {
    NewPreferencesSpecificationWizard newPreferencesDialog= new NewPreferencesSpecificationWizard();
	Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	WizardDialog wizDialog= new WizardDialog(shell, newPreferencesDialog);

	wizDialog.open();
    }
}
