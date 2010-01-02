import java.io.IOException;

import writers.ClassWriter;
import writers.FieldWriter;
import writers.FunctionWriter;
import writers.IndexWriter;
import writers.LibraryWriter;
import writers.Shared;
import writers.XMLReferenceWriter;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
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
 * -localref	the local reference output directory
 * -webref		the web reference output directory
 * -corepackage	pass in as many of these as necessary to have things considered as part of the core (not a library) e.g -corepackage processing.xml
 * -includedir	where to find things that aren't in the source, but only in xml e.g. [] (arrayaccess)
 */
public class ProcessingWeblet extends Standard {

	private static String examplesFlag = "-examplesdir";
	private static String templateFlag = "-templatedir";
	private static String outputFlag = "-webref";
	private static String exceptionsFlag = "-includedir";
	private static String imagesFlag = "-imagedir";
	private static String localFlag = "-localref";
	private static String coreFlag = "-corepackage"; //to allow for exceptions like XML being in the core
	private static String verboseFlag = "-noisy";
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
		Shared.i().rootClasses.add("PApplet");
		Shared.i().rootClasses.add("PConstants");
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
			} else if(option[0].equals(verboseFlag)){
				Shared.i().setNoisy(true);
			}
		}
	}

	public static int optionLength(String option) {
		if (option.equals(templateFlag) || option.equals(examplesFlag)
				|| option.equals(outputFlag) || option.equals(exceptionsFlag) || option.equals(imagesFlag) || option.equals(localFlag) || option.equals(coreFlag) ) {
			return 2;
		} else if ( option.equals(verboseFlag) ){
			return 1;
		}
		return 0;
	}

	private static void writeContents(RootDoc root) throws IOException {		
		for( ClassDoc classDoc : root.classes() ){
			
				System.out.println("Loaded class: " + classDoc.name());
			

			if(Shared.i().isCore(classDoc)){
				// Document the core functions and classes
				if(Shared.i().isRootLevel(classDoc)){
					//if it is in PApplet, PConstants or other classes where users can get
					//the variables without using dot syntax
					// document functions
					MethodDoc[] functions = classDoc.methods();
					for (MethodDoc fn : functions) {
						// write out html reference
						FunctionWriter.write(fn);
						Tag[] tags = fn.tags(Shared.i().getWebrefTagName());
						if (tags.length != 0) {
							// add to the index under the @webref category:sub_category
//							System.out.println("\nAdding " + fn.name() + " to webref\n");
							indexWriter.addItem(fn, tags[0]);
						}
					}
					//also need to add fields, duh
					for(FieldDoc doc : classDoc.fields()){
						if(Shared.i().needsWriting(doc)){
							FieldWriter.write(doc);
							indexWriter.addItem(doc, doc.tags(Shared.i().getWebrefTagName())[0] );
						}
					}
					
				} else {
					// document a class and its public properties
					new ClassWriter().write(classDoc);
					Tag[] classTags = classDoc.tags(Shared.i().getWebrefTagName());
					if (classTags.length != 0) {
						// add to the index under the @webref category:sub_category
						indexWriter.addItem(classDoc, classTags[0]);
					}
				}
			} else {
				// Document the library passed in
				LibraryWriter writer = new LibraryWriter(classDoc.containingPackage());
				writer.write();
			}
		}
	}
}