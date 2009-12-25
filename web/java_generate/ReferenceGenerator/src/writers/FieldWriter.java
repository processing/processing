package writers;
import java.io.IOException;
import java.util.HashMap;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;


public class FieldWriter extends BaseWriter {
	
	/**
	 * 
	 * @param vars inherited from containing ClassDoc
	 * @param doc
	 * @throws IOException
	 */
	
	public static void write(HashMap<String, String> vars, FieldDoc doc) throws IOException
	{
		//
		String filename = getFieldAnchor(doc);
		TemplateWriter templateWriter = new TemplateWriter();
		String syntax = templateWriter.writePartial("Field.Syntax.partial.html", getSyntax(doc));
		vars.put("syntax", syntax);
		vars.put("examples", getExamples(doc));
		vars.put("description", basicText(doc));
		vars.put("fieldname", doc.name());
		vars.put("parameters", templateWriter.writePartial("Parameter.partial.html", getParent(doc)));
		vars.put("usage", getUsage(doc));
		
		templateWriter.write("Field.template.html", vars, filename);
	}
	
	protected static HashMap<String, String> getSyntax(FieldDoc doc){
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("object", getInstanceName(doc));
		map.put("field", getName(doc));
		return map;
	}
	
	protected static HashMap<String, String> getParent(FieldDoc doc){
		HashMap<String, String> parent = new HashMap<String, String>();
		ClassDoc cd = doc.containingClass();
		parent.put("name", getInstanceName(doc));
		parent.put("description", cd.name() + ": " + getInstanceDescription(doc));
		return parent;
	}

}
