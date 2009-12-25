package writers;

import java.io.IOException;
import java.util.ArrayList;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.PackageDoc;

public class LibraryWriter extends BaseWriter {
	PackageDoc doc;
	String pkg;
	IndexWriter indexWriter;
	String dir;
	static TemplateWriter templateWriter;
	static ArrayList<String> writtenLibraries;
	
	public LibraryWriter(PackageDoc doc){
		
		this.doc = doc;
		if(templateWriter == null ){
			templateWriter = new TemplateWriter();
		}
		if(writtenLibraries == null){
			writtenLibraries = new ArrayList<String>();
		}
		
		
	}
	
	public void write() throws IOException{
		if(writtenLibraries.contains(doc.name())){
			return;
		}
		writtenLibraries.add(doc.name());
		String[] parts = doc.name().split("\\."); 
		String pkg = parts[parts.length-1] + "/";
		Shared.i().createOutputDirectory("libraries/"+pkg);
		
		//grab all relevant information for the doc
		for( ClassDoc classDoc : doc.allClasses() ){
			// document the class if it has a @webref tag
			new ClassWriter().write(classDoc);
//			indexWriter.addItem(classDoc);
		}
		
//		indexWriter.write("Library.Index.template.html", dir);
		//grab events from xml
		
		//create library directory
		
		//write index file to directory (include links to all methods of classes)
		
		//write classes to directory (these will write their constituent methods to the directory)
		
		//also pass the library base directory to the templateWriter in outputName (libraries/net/...)
//		templateWriter.write(templateName, vars, outputName);
	}
}
