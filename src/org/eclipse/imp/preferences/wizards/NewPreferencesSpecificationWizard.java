/**
 * 
 */
package org.eclipse.imp.preferences.wizards;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.imp.core.ErrorHandler;
import org.eclipse.imp.preferences.PreferencesPlugin;
import org.eclipse.imp.prefspecs.PrefspecsPlugin;
import org.eclipse.imp.prefspecs.builders.PrefspecsBuilder;
import org.eclipse.imp.prefspecs.compiler.PrefspecsCompiler;
import org.eclipse.imp.prefspecs.pageinfo.PreferencesPageInfo;
import org.eclipse.imp.wizards.CodeServiceWizard;
import org.eclipse.imp.wizards.ExtensionPointWizardPage;
import org.eclipse.imp.wizards.WizardPageField;
import org.eclipse.jface.operation.IRunnableWithProgress;

	

public class NewPreferencesSpecificationWizard extends CodeServiceWizard {
	
	protected String fPreferencesPackage;
	protected String fTemplate;	
	protected String fFileName;
	protected String fPagePackage;
	protected String fPageClassNameBase;
	protected String fPageName;
	protected String fPageId;
	protected String fMenuItem;
	protected String fInitializerFileName;
	protected String fAlternativeMessage;

    // This is actually the problem id for the prefs spec compiler
	public static final String PREFERENCES_ID = "org.eclipse.imp.preferences.problem";
	
	
	public void addPages() {
	    addPages(new ExtensionPointWizardPage[] { new NewPreferencesSpecificationWizardPage(this) } );
	}

	protected List getPluginDependencies() {
	    return Arrays.asList(new String[] { "org.eclipse.core.runtime", "org.eclipse.core.resources",
		    "org.eclipse.imp.runtime" });
	}

	
    protected void collectCodeParms()
    {
    	//super.collectCodeParms();
    	
		ExtensionPointWizardPage page= (ExtensionPointWizardPage) pages[0];
    	
		WizardPageField field = null;
		
    	fProject = page.getProject();
		
		field = page.getField("template");
		fTemplate = field.fValue;
		
		field = page.getField("fileName");
		fFileName = field.fValue;
		
		field = page.getField("pagePackage");
		fPagePackage = field.fValue;
		
		field = page.getField("pageClassNameBase");
		fPageClassNameBase = field.fValue;
		
		field = page.getField("pageName");
		fPageName = field.fValue;
		
		field = page.getField("pageId");
		fPageId = field.fValue;
		
		field = page.getField("category");
        fMenuItem = field.fValue;

        field = page.getField("alternative");
        fAlternativeMessage = field.fValue;
    }
	
	
	public void generateCodeStubs(IProgressMonitor mon) throws CoreException {

        Map<String,String> subs= getStandardSubstitutions(fProject);

        // The user-specified user-friendly preferences page name
        // might include blanks, which should be excluded when the
        // name is used as an identifier within the page itself
        subs.remove("$PREFS_PAGE_NAME$");
        String identifierSegments[] = fPageName.split(" ");
        String pageIdentifier = identifierSegments[0];
        for (int i = 1; i < identifierSegments.length; i++) {
        	pageIdentifier = pageIdentifier + identifierSegments[i];
        }
        subs.put("$PREFS_PAGE_NAME$", pageIdentifier);

        String templateName = fTemplate.replace('\\', '/');
        subs.put("$PREFS_TEMPLATE_NAME$", templateName);
        int lastFileSep = templateName.length();
        if (templateName.lastIndexOf("/") > -1)
        	lastFileSep = templateName.lastIndexOf("/");
        subs.put("$PREFS_TEMPLATE_DIR$", templateName.substring(0, lastFileSep));
        
        if (fAlternativeMessage.length() != 0){
            subs.remove("$PREFS_ALTERNATIVE_MESSAGE$");
            subs.put("$PREFS_ALTERNATIVE_MESSAGE$", fAlternativeMessage);
            IFile pageSrc = createFileFromTemplate(fFullClassName + ".java", "preferencesPageAlternative.java", fPackageFolder, subs, fProject, mon);
            editFile(mon, pageSrc);
            return;
        }



//        PrefspecsCompiler prefspecsCompiler = new PrefspecsCompiler(PREFERENCES_ID);
        // fFieldSpecs has full absolute path; need to give project relative path
        String projectLocation = fProject.getLocation().toString();
        String fieldSpecsLocation = fPagePackage;
        fieldSpecsLocation = fieldSpecsLocation.replace("\\", "/");
        if (fieldSpecsLocation.startsWith(projectLocation)) {
        	fieldSpecsLocation = fieldSpecsLocation.substring(projectLocation.length());
        }
        // createFileFromTemplate takes a short (unqualified) name for the
        // template file, but we may have a full absolute path name for the file,
        // so extract the short name from that
        String templateNameForCreatingFile = fTemplate.replace('\\', '/');
        int lastIndex = templateNameForCreatingFile.lastIndexOf('/');
        if (lastIndex >= 0 && lastIndex < templateNameForCreatingFile.length())
        	templateNameForCreatingFile = templateNameForCreatingFile.substring(lastIndex+1);

//        IFile fieldSpecsFile = fProject.getFile(fieldSpecsLocation);
//        PreferencesPageInfo pageInfo = prefspecsCompiler.compile(fieldSpecsFile, new NullProgressMonitor());
//        String constantsClassName = fFullClassName + "Constants";
//        String initializerClassName = fFullClassName + "Initializer";
//
//		ISourceProject sourceProject = null;
//    	try {
//    		sourceProject = ModelFactory.open(fProject);
//    	} catch (ModelException me){
//            System.err.println("NewPreferencesSpecificationWizard.generateCodeStubs(..):  Model exception:\n" + me.getMessage() + "\nReturning without parsing");
//            return;
//    	}
        
        IFile prefSpecsSpec = createFileFromTemplate(fFileName, PreferencesPlugin.PREFERENCES_PLUGIN_ID, templateNameForCreatingFile, fPagePackage, subs, fProject, mon);

        editFile(mon, prefSpecsSpec);

	}
	
	
    /**
     * Creates a file of the given name from the named template in the given location in the
     * given project. Subjects the template's contents to meta-variable substitution.
     * 
     * SMS 4 Aug 2007:  Copied and adapted from ExtensionPointWizard, where the original
     * has a couple of features specific to Java files (use of project source location and
     * formatting of Java source).
     * 
     * @param fileName
     * @param templateName
     * @param folder
     * @param replacements a Map of meta-variable substitutions to apply to the template
     * @param project
     * @param monitor
     * @return a handle to the file created
     * @throws CoreException
     */
//    protected IFile createFileFromTemplate(
//    	String fileName, String templateName, String folder, Map replacements,
//	    IProject project, IProgressMonitor monitor)
//    throws CoreException
//	{
//		monitor.setTaskName("NewPreferencesSpecificationWizard.createFileFromTemplate:  Creating " + fileName);
//	
//		String packagePath = getProjectSourceLocation() + fPagePackage.replace('.', '/');
//		IPath specFilePath = new Path(packagePath + "/" + fFileName);
//		final IFile file= project.getFile(specFilePath);
//
//		String templateContents= new String(getTemplateFile(templateName));
//		String contents= performSubstitutions(templateContents, replacements);
//	
//		if (file.exists()) {
//		    file.setContents(new ByteArrayInputStream(contents.getBytes()), true, true, monitor);
//		} else {
//		    createSubFolders(packagePath, project, monitor);
//		    file.create(new ByteArrayInputStream(contents.getBytes()), true, monitor);
//		}
//	//	monitor.worked(1);
//		return file;
//    }

	/*
	 * TODO:
	 * - Figure out what to do about empty or null values, especially
	 *   the menu item, where null might be allowed in the extension
	 * - Define some constants to represent the constant strings that
	 *   may be used here (e.g., for the file name extension).
	 */
   	protected void writeOutGenerationParameters()
   		throws CoreException
   	{
   		String contents =
   			"PagePackage=" + fPagePackage + "\n" +
   			"PageClassNameBase=" + fPageClassNameBase + "\n" +
   			"PageName=" + fPageName + "\n" +
   			"PageId=" + fPageId + "\n" +
   			// Not sure about how to treat this last one ...
   			"PageMenuItem=" + (fMenuItem == null || fMenuItem.length() == 0 ? "TOP" : fMenuItem) + "\n" +
   			"AlternativeMessage=" + (fAlternativeMessage == null || fAlternativeMessage.length() == 0 ? "Message omitted." : fAlternativeMessage) + "\n";
   		
		String packagePath = getProjectSourceLocation() + fPagePackage.replace('.', '/');
		IPath paramFilePath = new Path(packagePath + "/" + fPageClassNameBase + ".genparams");	
		final IFile file= fProject.getFile(paramFilePath);
		
		IProgressMonitor mon = new NullProgressMonitor();
		if (file.exists()) {
		    file.setContents(new ByteArrayInputStream(contents.getBytes()), true, true, mon);
		} else {
		    createSubFolders(packagePath, fProject	, mon);
		    file.create(new ByteArrayInputStream(contents.getBytes()), true, mon);
		}
   		
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

	   	
	   	
//		ExtensionPointWizardPage page= pages[0];
//		WizardPageField prefIdField = page.getField("id");	
//		WizardPageField prefNameField = page.getField("name");
//		WizardPageField prefClassField = page.getField("class");
//		WizardPageField prefCategoryField = page.getField("category");
//		WizardPageField prefAlternativeField = page.getField("alternative");
//		final String prefID = prefIdField.getText();
//		final String prefName = prefNameField.getText();
//		final String prefClass = prefClassField.getText();
//		final String prefCategory = prefCategoryField.getText();
//		final String prefAlternative = prefAlternativeField.getText();
//		final ExtensionPointWizardPage[] pages = super.pages;
		//final String fieldSpecsRelativeLocation = fFieldSpecs.substring(
		//fProject.getLocation().toString().length(), fFieldSpecs.length());
		//final IProject fProject = super.fProject;

		IRunnableWithProgress op= new IRunnableWithProgress() {
		    public void run(IProgressMonitor monitor) throws InvocationTargetException {
			IWorkspaceRunnable wsop= new IWorkspaceRunnable() {
			    public void run(IProgressMonitor monitor) throws CoreException {
				try {
//					    for(int n= 0; n < pages.length; n++) {
//							NewPreferencesSpecificationWizardPage page= (NewPreferencesSpecificationWizardPage) pages[n];	
//							if (!page.hasBeenSkipped() && page.getSchema() != null) {
//								// Enable an extension of org.eclipse.ui.preferencePages;
//								// provide only information from fields that correspond to
//								// elements for that extension-point schema.  (Any other
//								// fields provided in the wizard should be ignored for
//								// this purpose.)
//								ExtensionPointEnabler.enable(
//									page.getProject(), "org.eclipse.ui", "preferencePages", 
//									new String[][] {
//										{ "page:id", prefID },
//										{ "page:name", prefName },
//										{ "page:class", prefClass },
//										
//										{ "extension:preferencesDialog:language", fLanguageName },
//	//									{ "extension:preferencesDialog:fields", fFieldSpecs },
//										{ "extension:preferencesDialog:class", prefClass },
//										{ "extension:preferencesDialog:category", prefCategory },
//									},
//									true,
//									getPluginDependencies(),
//									monitor);
//							}
//					    }	

					// SMS 18 Jun 2007:  Duplicative if generateCodeStubs() calls compile?
					//PreferencesPageInfo pageInfo = compile(fProject.getFile(new Path(fieldSpecsRelativeLocation)), monitor);
					generateCodeStubs(new NullProgressMonitor());
				   	writeOutGenerationParameters();
				   	
//				    if (pageInfo != null)
//				    	System.out.println("NewPreferencesSpecificationWizard.performFinish():  got non-null page info in preparation for code generation");
//				    else {
//				    	System.out.println("NewPreferencesSpecificationWizard.performFinish():  got null page info in preparation for code generation");
//				    }
				    //generateCodeStubs(monitor);
				} catch (Exception e) {
					ErrorHandler.reportError("NewPreferencesSpecificationWizard.performFinish():  Error adding extension or generating code", e);
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
		
		//postReminderAboutPreferencesInitializer();
		
		return true;	
	}
    
	
//	protected void	postReminderAboutPreferencesInitializer()
//	{
//	   	String message = "REMINDER:  Update the plugin file for language to call the new preferences initializer\n";
//	   	message = message + "to initialize default preferences:  " + fInitializerFileName;
//		Shell parent = this.getShell();
//		MessageBox messageBox = new MessageBox(parent, (SWT.OK));
//		messageBox.setMessage(message);
//		int result = messageBox.open();
//	}

	
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
			String packagePath = getProjectSourceLocation() + fPagePackage.replace('.', '/');
			IPath specFilePath = new Path(packagePath + "/" + fFileName);
			final IFile file= fProject.getFile(specFilePath);
			if (file.exists()) {
				return new String[] { file.getLocation().toString() } ;
			} else {
				return new String[] { };
			}
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