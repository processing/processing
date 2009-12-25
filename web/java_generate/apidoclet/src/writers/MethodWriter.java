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
		String filename = getMethodAnchor(doc);
		TemplateWriter templateWriter = new TemplateWriter();
		
		vars.put("examples", getExamples(doc));
		vars.put("description", basicText(doc));
		vars.put("methodname", getName(doc));
		String syntax = templateWriter.writeLoop("Method.Syntax.partial.html", getSyntax(doc, getInstanceName(doc)));
		vars.put("syntax", syntax);
		vars.put("returns", importedName(doc.returnType().toString()));
		
		vars.put("parameters", getParameters(doc));
		vars.put("usage", getUsage(doc));
		
		templateWriter.write("Method.template.html", vars, filename);
	}
	
}
