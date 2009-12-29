package writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.Tag;

public class ClassWriter extends BaseWriter {
	private ClassDoc classDoc;
	
	public ClassWriter() {
		
	}

	@SuppressWarnings("unchecked")
	public void write(ClassDoc classDoc) throws IOException {
		if( needsWriting(classDoc)){
			TemplateWriter templateWriter = new TemplateWriter();
			this.classDoc = classDoc;
			String classname = getName(classDoc);
			String anchor = getAnchor(classDoc);
			
			HashMap<String, String> vars = new HashMap<String, String>();
			
			
			vars.put("classname", classname);
			vars.put("classanchor", anchor);
			vars.put("description", basicText(classDoc));
			
			ArrayList<HashMap<String, String>> methodSet = new ArrayList<HashMap<String, String>>();
			ArrayList<HashMap<String, String>> fieldSet = new ArrayList<HashMap<String, String>>();
			
			// Write all @webref methods for core classes (the tag tells us where to link to it in the index)
			
			for (MethodDoc m : classDoc.methods()) {
				if(needsWriting(m)){
					MethodWriter.write((HashMap<String, String>)vars.clone(), m);				
					methodSet.add(getPropertyInfo(m));
				}
			}
			
			for (FieldDoc f : classDoc.fields()) {
				if(needsWriting(f)){
					FieldWriter.write((HashMap<String, String>)vars.clone(), f);
					fieldSet.add(getPropertyInfo(f));				
				}
			}
			String constructors = getConstructors();
			
			String methods = templateWriter.writeLoop("Property.partial.html", methodSet);
			String fields = templateWriter.writeLoop("Property.partial.html", fieldSet);
			vars.put("methods", methods);
			vars.put("fields", fields);
			vars.put("examples", getExamples(classDoc));
			vars.put("constructors", constructors);
			vars.put("related", getRelated(classDoc));
			
			Tag[] tags = classDoc.tags("usage");
			if (tags.length != 0){
				vars.put("usage", tags[0].text());				
			}
			
			templateWriter.write("Class.template.html", vars, anchor);
		}
		
	}
	
	private String getConstructors()
	{
		String constructors = "";
		for( ConstructorDoc c : classDoc.constructors() )
		{
			String constructor = c.name() + "(";
			
			for( Parameter p : c.parameters() )
			{
				constructor += "<kbd>"+p.name() + "</kbd>, ";
			}
			if( constructor.endsWith(", ") )
			{
				constructor = constructor.substring(0, constructor.length()-2);
			}
			constructors += constructor + ")\n"; 
		}
		return constructors;
	}

	private HashMap<String, String> getPropertyInfo(ProgramElementDoc doc) {
		HashMap<String, String> ret = new HashMap<String, String>();
		ret.put("name", docName(doc));
		ret.put("anchor", getLocalAnchor(doc));
		ret.put("desc", shortText(doc));
		return ret;
	}
}
