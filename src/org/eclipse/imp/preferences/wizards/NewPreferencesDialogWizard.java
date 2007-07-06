/**
 * 
 */
package org.eclipse.uide.preferences.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.uide.core.ErrorHandler;
import org.eclipse.uide.model.ISourceProject;
import org.eclipse.uide.model.ModelFactory;
import org.eclipse.uide.model.ModelFactory.ModelException;
import org.eclipse.uide.preferences.PreferencesFactory;
import org.eclipse.uide.preferences.pageinfo.PreferencesPageInfo;
import org.eclipse.uide.wizards.CodeServiceWizard;
import org.eclipse.uide.wizards.ExtensionPointEnabler;
import org.eclipse.uide.wizards.ExtensionPointWizardPage;
import org.eclipse.uide.wizards.WizardPageField;

import prefspecs.PrefspecsPlugin;
import prefspecs.safari.builders.PrefspecsBuilder;
import prefspecs.safari.compiler.PrefspecsCompiler;



public class NewPreferencesDialogWizard extends CodeServiceWizard {
	
	protected String fPreferencesPackage;
	protected String fFieldSpecs;	
	protected String fMenuItem;
	protected String fInitializerFileName;
	protected String fAlternativeMessage;
	

	public static final String PREFERENCES_ID = "org.eclipse.uide.preferences.problem";
	
	
	public void addPages() {
	    addPages(new ExtensionPointWizardPage[] { new NewPreferencesDialogWizardPage(this) } );
	}

	protected List getPluginDependencies() {
	    return Arrays.asList(new String[] { "org.eclipse.core.runtime", "org.eclipse.core.resources",
		    "org.eclipse.uide.runtime" });
	}

	
    protected void collectCodeParms()
    {
    	super.collectCodeParms();
    	
		ExtensionPointWizardPage page= (ExtensionPointWizardPage) pages[0];
    	
		WizardPageField field = null;
		
		field = pages[0].getField("fields");
		fFieldSpecs = field.fValue;
		
		field = pages[0].getField("category");
        fMenuItem = field.fValue;

        field = pages[0].getField("alternative");
        fAlternativeMessage = field.fValue;
    }
	
	
	public void generateCodeStubs(IProgressMonitor mon) throws CoreException {

        Map subs= getStandardSubstitutions(fProject);

        subs.remove("$PREFS_CLASS_NAME$");
        subs.put("$PREFS_CLASS_NAME$", fFullClassName);
        
        subs.remove("$PREFS_PACKAGE_NAME$");
        subs.put("$PREFS_PACKAGE_NAME$", fPackageName);
        
        if (fAlternativeMessage.length() != 0){
            subs.remove("$PREFS_ALTERNATIVE_MESSAGE$");
            subs.put("$PREFS_ALTERNATIVE_MESSAGE$", fAlternativeMessage);
            IFile pageSrc = createFileFromTemplate(fFullClassName + ".java", "preferencesPageAlternative.java", fPackageFolder, subs, fProject, mon);
            editFile(mon, pageSrc);
            return;
        }	
        
        // Generating a full tabbed preference page

        PrefspecsCompiler prefspecsCompiler = new PrefspecsCompiler(PREFERENCES_ID);
        // fFieldSpecs has full absolute path; need to give project relative path
        String projectLocation = fProject.getLocation().toString();
        String fieldSpecsLocation = fFieldSpecs;
        fieldSpecsLocation = fieldSpecsLocation.replace("\\", "/");
        if (fieldSpecsLocation.startsWith(projectLocation)) {
        	fieldSpecsLocation = fieldSpecsLocation.substring(projectLocation.length());
        }
        IFile fieldSpecsFile = fProject.getFile(fieldSpecsLocation);
        PreferencesPageInfo pageInfo = prefspecsCompiler.compile(fieldSpecsFile, new NullProgressMonitor());
        String constantsClassName = fFullClassName + "Constants";        

		ISourceProject sourceProject = null;
    	try {
    		sourceProject = ModelFactory.open(fProject);
    	} catch (ModelException me){
            System.err.println("NewPreferencesDialogWizard.generateCodeStubs(..):  Model exception:\n" + me.getMessage() + "\nReturning without parsing");
            return;
    	}
        
    	
        IFile constantsSrc = PreferencesFactory.generatePreferencesConstants(
        		pageInfo, sourceProject, getProjectSourceLocation(), fPackageName, constantsClassName,  mon);
        //IFile constantsSrc = createFileFromTemplate(fFullClassName + "Constants.java", "preferencesConstants.java", fPackageFolder, subs, fProject, mon);
        editFile(mon, constantsSrc);

        
        IFile initializerSrc = PreferencesFactory.generatePreferencesInitializers(
        		pageInfo,
        		getPluginPackageName(fProject, null), getPluginClassName(fProject, null), constantsClassName,
        		sourceProject, getProjectSourceLocation(), fPackageName, fFullClassName + "Initializer",  mon);
        //IFile initializerSrc = createFileFromTemplate(fFullClassName + "Initializer.java", "preferencesInitializer.java", fPackageFolder, subs, fProject, mon);
        editFile(mon, initializerSrc);
        
        fInitializerFileName = initializerSrc.getName();
        
        
        IFile pageSrc = createFileFromTemplate(fFullClassName + ".java", "preferencesPageWithTabs.java", fPackageFolder, subs, fProject, mon);
        editFile(mon, pageSrc);
        

        IFile defaultSrc = PreferencesFactory.generateDefaultTab(
        		pageInfo,
        		getPluginPackageName(fProject, null), getPluginClassName(fProject, null), constantsClassName,
        		sourceProject, getProjectSourceLocation(), fPackageName, fFullClassName + "DefaultTab",  mon);
        editFile(mon, defaultSrc);
        
        IFile configSrc = PreferencesFactory.generateConfigurationTab(
        		pageInfo,
        		getPluginPackageName(fProject, null), getPluginClassName(fProject, null), constantsClassName,
        		sourceProject, getProjectSourceLocation(), fPackageName, fFullClassName + "ConfigurationTab",  mon);
        editFile(mon, configSrc);
        

        IFile instanceSrc = PreferencesFactory.generateInstanceTab(
        		pageInfo,
        		getPluginPackageName(fProject, null), getPluginClassName(fProject, null), constantsClassName,
        		sourceProject, getProjectSourceLocation(), fPackageName, fFullClassName + "InstanceTab",  mon);
        editFile(mon, instanceSrc);
        
        IFile projectSrc = PreferencesFactory.generateProjectTab(
        		pageInfo,
        		getPluginPackageName(fProject, null), getPluginClassName(fProject, null), constantsClassName,
        		sourceProject, getProjectSourceLocation(), fPackageName, fFullClassName + "ProjectTab",  mon);
        editFile(mon, projectSrc);

	}
	
 
 
	   
    /**
     * This method is called when 'Finish' button is pressed in the wizard.
     * We will create an operation and run it using wizard as execution context.
     * 
     * SMS 18 Dec 2006:
     * Overrode this method so as to call ExtensionPointEnabler with the
     * point id for preference menu items
     */
	public boolean performFinish() {
		collectCodeParms(); // Do this in the UI thread while the wizard fields are still accessible
	   	if (!okToClobberFiles(getFilesThatCouldBeClobbered()))
    		return false;
		
		// SMS 18 Dec 2006
		// This is really the collection of code parameters, done here (in this thread) because
		// they can't be accessed from the runnable thread where they're needed.
		// For now assume that there's just one page; if this works, then can later arrange
		// to pass multiple instances to multiple instances of the runnable (or something)
		ExtensionPointWizardPage page= pages[0];
		WizardPageField prefIdField = page.getField("id");	
		WizardPageField prefNameField = page.getField("name");
		WizardPageField prefClassField = page.getField("class");
		WizardPageField prefCategoryField = page.getField("category");
		WizardPageField prefAlternativeField = page.getField("alternative");
		final String prefID = prefIdField.getText();
		final String prefName = prefNameField.getText();
		final String prefClass = prefClassField.getText();
		final String prefCategory = prefCategoryField.getText();
		final String prefAlternative = prefAlternativeField.getText();
		final ExtensionPointWizardPage[] pages = super.pages;
		final String fieldSpecsRelativeLocation = fFieldSpecs.substring(
				fProject.getLocation().toString().length(), fFieldSpecs.length());
		final IProject fProject = super.fProject;

		IRunnableWithProgress op= new IRunnableWithProgress() {
		    public void run(IProgressMonitor monitor) throws InvocationTargetException {
			IWorkspaceRunnable wsop= new IWorkspaceRunnable() {
			    public void run(IProgressMonitor monitor) throws CoreException {
				try {
					    for(int n= 0; n < pages.length; n++) {
						NewPreferencesDialogWizardPage page= (NewPreferencesDialogWizardPage) pages[n];	
						if (!page.hasBeenSkipped() && page.getSchema() != null) {
							// Enable an extension of org.eclipse.ui.preferencePages;
							// provide only information from fields that correspond to
							// elements for that extension-point schema.  (Any other
							// fields provided in the wizard	 should be ignored for
							// this purpose.)
							ExtensionPointEnabler.enable(
								page.getProject(), "org.eclipse.ui", "preferencePages", 
								new String[][] {
									{ "extension:id", "ext." + prefID },
									{ "extension:name", "ext." + prefName }, 
									{ "extension.page:id", prefID },
									{ "extension.page:name", prefName },
									{ "extension.page:class", prefClass },
									{ "extension.page:category", prefCategory },
								},
								true,
								monitor);
						}
				    }	

					// SMS 18 Jun 2007:  Duplicative if generateCodeStubs() calls compile?
					//PreferencesPageInfo pageInfo = compile(fProject.getFile(new Path(fieldSpecsRelativeLocation)), monitor);
					generateCodeStubs(new NullProgressMonitor());
//				    if (pageInfo != null)
//				    	System.out.println("NewPreferencesDialogWizard.performFinish():  got non-null page info in preparation for code generation");
//				    else {
//				    	System.out.println("NewPreferencesDialogWizard.performFinish():  got null page info in preparation for code generation");
//				    }
				    //generateCodeStubs(monitor);
				} catch (Exception e) {
				    ErrorHandler.reportError("NewPreferencesDialogWizard.performFinish():  Error adding extension or generating code", e);
				} finally {
					monitor.done();
				}
			    }
			};
			try {
			    ResourcesPlugin.getWorkspace().run(wsop, monitor);
			} catch (Exception e) {
			    ErrorHandler.reportError("Could not add extension points", e);
			}
		    }
		};
		try {
		    getContainer().run(true, false, op);
		} catch (InvocationTargetException e) {
		    Throwable realException= e.getTargetException();
		    ErrorHandler.reportError("Error", realException);
		    return false;
		} catch (InterruptedException e) {
		    return false;
		}
		
		postReminderAboutPreferencesInitializer();
		
		return true;	
	}
    
	
	protected void	postReminderAboutPreferencesInitializer()
	{
	   	String message = "REMINDER:  Update the plugin file for language to call the new preferences initializer\n";
	   	message = message + "to initialize default preferences:  " + fInitializerFileName;
		Shell parent = this.getShell();
		MessageBox messageBox = new MessageBox(parent, (SWT.OK));
		messageBox.setMessage(message);
		int result = messageBox.open();
	}
	
	
	protected PreferencesPageInfo compile(final IFile file, IProgressMonitor monitor) {
        try {
            // START_HERE
            System.out.println("Builder.compile with file = " + file.getName());
            PrefspecsCompiler compiler= new PrefspecsCompiler(PrefspecsBuilder.PROBLEM_MARKER_ID);
            PreferencesPageInfo pageInfo = compiler.compile(file, monitor);

            // May not need to refresh yet, that is, unless and until
            // the compiler is updating some files (which it's not
            // as of 7 May 2007)
            //doRefresh(file.getParent());
            
            return pageInfo;
        } catch (Exception e) {
        	PrefspecsPlugin.getInstance().writeErrorMsg(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
	
	
   /**
     * Return the names of any existing files that would be clobbered by the
     * new files to be generated.
     * 
     * @return	An array of names of existing files that would be clobbered by
     * 			the new files to be generated
     */
	   protected String[] getFilesThatCouldBeClobbered() {
	    	String prefix = fProject.getLocation().toString() + '/' + getProjectSourceLocation() + fPackageName.replace('.', '/') + '/';
	    	if (fAlternativeMessage.length() <= 0)
				return new String[] {
						prefix + fFullClassName + ".java", 
						prefix + fFullClassName + "DefaultTab.java", 
						prefix + fFullClassName + "ConfigurationTab.java", 
						prefix + fFullClassName + "InstanceTab.java", 
						prefix + fFullClassName + "ProjectTab.java", 
						prefix + fFullClassName + "Initializer.java", 
						prefix + fFullClassName + "Constants.java"
				};
	    	else
	    		return new String[] { prefix + fFullClassName + ".java" };
	    }
 
	
	   
	   /**
	     * Refreshes all resources in the entire project tree containing the given resource.
	     * Crude but effective.
	     * Copied from SAFARIBuilderBase, where it's not static, and probably wouldn't belong
	     * if it were.  Should probably put into a utility somewhere.
	     */
	    protected void doRefresh(final IResource resource) {
	        new Thread() {
	            public void run() {
	        	try {
	        	    resource.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
	        	} catch (CoreException e) {
	        	    e.printStackTrace();
	        	}
	            }
	        }.start();
	    }

}