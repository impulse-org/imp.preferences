/*
 * (C) Copyright IBM Corporation 2007
 * 
 * This file is part of the Eclipse IMP.
 */
/*
 * Created on 28 Feb 2007
 */
package org.eclipse.imp.preferences.cheatsheet;

import org.eclipse.imp.preferences.wizards.NewPreferencesDialogWizard;
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
    NewPreferencesDialogWizard newPreferencesDialog= new NewPreferencesDialogWizard();
	Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	WizardDialog wizDialog= new WizardDialog(shell, newPreferencesDialog);

	wizDialog.open();
    }
}
