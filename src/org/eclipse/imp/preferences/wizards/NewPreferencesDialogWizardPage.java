package org.eclipse.imp.preferences.wizards;


import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.imp.core.ErrorHandler;
import org.eclipse.imp.runtime.RuntimePlugin;
import org.eclipse.imp.wizards.ExtensionPointEnabler;
import org.eclipse.imp.wizards.ExtensionPointWizard;
import org.eclipse.imp.wizards.ExtensionPointWizardPage;
import org.eclipse.imp.wizards.WizardPageField;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.swt.widgets.Composite;

/**

 */
public class NewPreferencesDialogWizardPage extends ExtensionPointWizardPage
{
	
 	 NewPreferencesDialogWizardPage(ExtensionPointWizard owner) {
 		 // The "false" value provided at the end of the parameters
 		 // controls whether fields for extension name and id are shown
 		 // in the wizard--the parameter is "omitIDName", so "false"
 		 // means don't omit them (and "true" means do omit them)
 		 //super(owner, RuntimePlugin.UIDE_RUNTIME, "preferencesDialog", false);
 		 super(owner, RuntimePlugin.IMP_RUNTIME, "preferencesDialog", false, true);
    }


 	 /**
 	  * Create the other controls, then use the language name
 	  * to set a default value for the (preferences menu) cagetory
 	  * under which the new preferences dialog should appear.
 	  */
    public void createControl(Composite parent)
    {
		super.createControl(parent);
								
		// Try to assure that the language is defined
		setLanguageIfEmpty();
		
		setPrefspecsIfEmpty();
		
		// Don't set category if empty, because the most
		// likely (or, anyway, safest) default value is
		// no category (i.e., a top-level item)
    }

    
    // copied from package org.jikespg.uide.wizards.NewUIDEParserWizardPage
    
    public String determineLanguage()
    {
		try {
		    IPluginModel pluginModel= ExtensionPointEnabler.getPluginModel(getProject());
	
		    if (pluginModel != null) {
				IPluginExtension[] extensions= pluginModel.getExtensions().getExtensions();
		
				for(int n= 0; n < extensions.length; n++) {
				    IPluginExtension extension= extensions[n];
		
                    if (!extension.getPoint().equals("org.eclipse.uide.runtime.languageDescription"))
                        continue;

                    IPluginObject[] children= extension.getChildren();
		
				    for(int k= 0; k < children.length; k++) {
						IPluginObject object= children[k];
			
						if (object.getName().equals("language")) {
						    return ((IPluginElement) object).getAttribute("language").getValue();
						}
				    }
				    System.err.println("NewPreferencesDialogWizardPage.determineLanguage():  Unable to determine language for plugin '" + pluginModel.getBundleDescription().getName() + "': no languageDescription extension.");
				}
		    } else if (getProject() != null)
		    	System.out.println("Not a plugin project: " + getProject().getName());
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return "";
    }

    	
    protected void setLanguageIfEmpty() {
        try {
            String pluginLang= determineLanguage(); // if a languageDesc exists
            if (pluginLang.length() == 0)
                return;

            WizardPageField field= getField("language");

            if (field.getText().length() == 0)
                field.setText(pluginLang);
        } catch (Exception e) {
            ErrorHandler.reportError("NewPreferencesDialogWizardPage.setLanguageIfEmpty():  Cannot set language", e);
        }
    }
    
    protected void setPrefspecsIfEmpty() {
        try {

            WizardPageField field= getField("fields");

            if (field.getText().length() == 0) {
            	IProject project = getProject();
            	String text = null;
            	IFolder prefsFolder = project.getFolder("preferences");
            	if (prefsFolder.exists())
            		text = prefsFolder.getLocation().toString();
            	else
            		text = project.getLocation().toString();
            	
                field.setText(text);
            }
        } catch (Exception e) {
            ErrorHandler.reportError("NewPreferencesDialogWizardPage.setPrefspecsIfEmpty():  Cannot set 'fields' fiels", e);
        }
    }
    
    
    protected Schema getSchema() {
    	return fSchema;
    }
    
}
