package writers;

import java.io.IOException;
import java.util.ArrayList;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.PackageDoc;

public class LibraryWriter extends BaseWriter {
	PackageDoc doc;
	String pkg;
	LibraryIndexWriter indexWriter;
	String dir;
	static TemplateWriter templateWriter;
	static ArrayList<String> writtenLibraries;
	
	public LibraryWriter(){
		if(templateWriter == null ){
			templateWriter = new TemplateWriter();
		}
		if(writtenLibraries == null){
			writtenLibraries = new ArrayList<String>();
		}
	}
	
	public void write(PackageDoc doc) {
		
		if(writtenLibraries.contains(doc.name())){
			return;
		}
		writtenLibraries.add(doc.name());
		
		String[] parts = doc.name().split("\\."); 
		String pkg = parts[parts.length-1] + "/";
		dir = "libraries/"+pkg;
		Shared.i().createOutputDirectory(dir);
		indexWriter = new LibraryIndexWriter(doc, dir);
		
		//grab all relevant information for the doc
		for( ClassDoc classDoc : doc.allClasses() ){
			// document the class if it has a @webref tag
			try {
				new ClassWriter().write(classDoc);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
