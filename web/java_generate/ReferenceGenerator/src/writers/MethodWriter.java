package writers;

import java.io.IOException;
import java.util.HashMap;

import com.sun.javadoc.MethodDoc;

public class MethodWriter extends BaseWriter {
	public MethodWriter(){}
	
	/**
	 * 
	 * 
	 * @param vars the inherited vars from the method's ClassDoc
	 * @param doc the method doc
	 * @throws IOException
	 */
	public static void write( HashMap<String, String> vars, MethodDoc doc) throws IOException
	{
		String filename = getAnchor(doc);
		TemplateWriter templateWriter = new TemplateWriter();
		
		if(doc.containingClass().name().equals("PApplet")){
			vars.put("classname", "");
		} else {			
			vars.put("classname", getName(doc.containingClass()));
		}
		
		vars.put("examples", getExamples(doc));
		vars.put("description", basicText(doc));
		vars.put("name", getName(doc));
		String syntax = templateWriter.writeLoop("Method.Syntax.partial.html", getSyntax(doc, getInstanceName(doc)));
		vars.put("syntax", syntax);
		vars.put("returns", importedName(doc.returnType().toString()));
		
		vars.put("parameters", getParameters(doc));
		vars.put("usage", getUsage(doc));
		
		templateWriter.write("Generic.template.html", vars, filename);
	}
	
}
