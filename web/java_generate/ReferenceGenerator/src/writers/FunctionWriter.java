package writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.javadoc.MethodDoc;

public class FunctionWriter extends BaseWriter {
	
	static ArrayList<String> writtenFunctions;
	
	public FunctionWriter(){}
	
	public static void write(MethodDoc doc) throws IOException
	{
		if( wasWritten(doc)){
			return;
		}
		
		String anchor = getAnchor(doc);
		TemplateWriter templateWriter = new TemplateWriter();
		
		HashMap<String, String> vars = new HashMap<String, String>();
		String syntax = templateWriter.writeLoop("Function.Syntax.partial.html", getSyntax(doc, ""));
		
		vars.put("examples", getExamples( doc ));
		vars.put("description", basicText(doc));
		vars.put("name", doc.name() + "()");
		vars.put("syntax", syntax);
		vars.put("usage", getUsage(doc));
		vars.put("returns", importedName(doc.returnType().toString()));
		vars.put("parameters", getParameters(doc));
		vars.put("related", getRelated(doc));
		
		if(doc.name().equals("frameRate")){
			System.out.println("\n\nFunction:\n" + vars.get("related") + "\n" + vars.get("parameters") + "\n" + vars.size());
		}
		
		templateWriter.write("Function.template.html", vars, anchor);
	}	
}
