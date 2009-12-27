import java.io.IOException;

import writers.ClassWriter;
import writers.Shared;
import writers.FunctionWriter;
import writers.IndexWriter;
import writers.LibraryWriter;
import writers.XMLReferenceWriter;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.standard.Standard;

/*
 * @author David Wicks
 * ProcessingWeblet generates the web reference for processing.org and download
 * The source code of processing is parsed for webref tags to determine what gets included
 * Flags for javadoc when running include:
 * -templatedir where to find the html templates for output
 * -examplesdir where to find the xml describing the examples to go in the reference
 */
public class ProcessingWeblet extends Standard {

	private static String examplesFlag = "-examplesdir";
	private static String templateFlag = "-templatedir";
	private static String outputFlag = "-webref";
	private static String exceptionsFlag = "-includedir";
	private static String imagesFlag = "-imagedir";
	private static String localFlag = "-localref";
	private static String coreFlag = "-corepackage"; //to allow for exceptions like XML being in the core
	private static String exceptionsList = "";
	private static IndexWriter indexWriter;

	public static boolean start(RootDoc root) {
		setConfig(root.options());
		Shared.i().createBaseDirectories();
		
		indexWriter = new IndexWriter();
		
		try {			
			System.out.println("\n===Writing .javadoc sourced reference.===");
			// write out everything in the .java files
			writeContents(root);
			
			System.out.println("===Source code @webref files written.===");
			
			if (!exceptionsList.equals("")) { // need to get something back from
												// this to create index
				System.out.println("\n===Writing XML-sourced reference.===");
				XMLReferenceWriter.write(exceptionsList, indexWriter);
				System.out.println("===Include directory files written.===");
			}
			System.out.println("\n===Telling the index to write itself.===");
			indexWriter.write();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("===All finished in the weblet.===");
		return true;
	}

	private static void setConfig(String[][] configOptions) {
		// 
		Shared.i().corePackages.add("processing.core");
		// look at all possible options (this .equals thing kills switch statements...or does it?)
		for (String[] option : configOptions) {
			if (option[0].equals(templateFlag)) {
				Shared.i().setTemplateDirectory(option[1]);
			} else if (option[0].equals(examplesFlag)) {
				Shared.i().setExampleDirectory(option[1]);
			} else if (option[0].equals(outputFlag)) {
				Shared.i().setOutputDirectory(option[1]);
			} else if (option[0].equals(exceptionsFlag)) {
				// write out files based on exceptions index
				exceptionsList = option[1];
			} else if (option[0].equals(imagesFlag)) {
				Shared.i().setImageDirectory(option[1]);
			} else if( option[0].equals(localFlag) )
			{
				Shared.i().setLocalOutputDirectory(option[1]);
			} else if( option[0].equals(coreFlag)){
				Shared.i().corePackages.add(option[1]);
			}
		}
	}

	public static int optionLength(String option) {
		if (option.equals(templateFlag) || option.equals(examplesFlag)
				|| option.equals(outputFlag) || option.equals(exceptionsFlag) || option.equals(imagesFlag) || option.equals(localFlag) || option.equals(coreFlag)) {
			return 2;
		}
		return 0;
	}

	private static void writeContents(RootDoc root) throws IOException {		
		for( ClassDoc classDoc : root.classes() ){

			if(Shared.i().isCore(classDoc)){
				//just record the methods of PApplet
//				System.out.println( "Writing core thing from " + classDoc.containingPackage().name());
				if(classDoc.name().equals("PApplet")){
					MethodDoc[] functions = classDoc.methods();
					for (MethodDoc fn : functions) {
						Tag[] tags = fn.tags(Shared.i().getWebrefTagName());
						if (tags.length != 0) {
							FunctionWriter.write(fn, tags[0]);
							indexWriter.addItem(fn, tags[0]);
						}
					}					
				} else {
					Tag[] classTags = classDoc.tags(Shared.i().getWebrefTagName());
					if (classTags.length != 0) {
						// document the class if it has a @webref tag
						new ClassWriter().write(classDoc);
						indexWriter.addItem(classDoc, classTags[0]);
					}
				}
			} else {
//				System.out.println("Writing library thing from " + classDoc.containingPackage().name());
				// check whether we've already written this package before
				LibraryWriter writer = new LibraryWriter(classDoc.containingPackage());
				writer.write();
			}
		}
	}
}