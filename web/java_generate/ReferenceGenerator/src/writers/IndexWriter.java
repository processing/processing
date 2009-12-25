package writers;

import java.io.IOException;
import java.util.HashMap;

import com.sun.javadoc.Doc;
import com.sun.javadoc.Tag;

public class IndexWriter extends BaseWriter {
	
	HashMap<String, String> sections;
	
	TemplateWriter templateWriter;
	
	public IndexWriter(){
		sections = new HashMap<String, String>();
		templateWriter = new TemplateWriter();
	}
	
	public void write(String template, String location) throws IOException{		
		templateWriter.write(template, sections, location+"index.html");
	}
	
	public void write() throws IOException{
		templateWriter.write("Index.template.html", sections, "index.html");
	}
	
	public void addItem(Doc doc, Tag webref) throws IOException{
		String name = getName(doc);
		String anchor = getAnchorFromName(name);
		String category = getCategory(webref);
		String subcategory = getSubcategory(webref);
		addItem(category, subcategory, name, anchor);
	}
	
	public void addItem(String category, String subcategory, String name, String anchor) throws IOException{
		String key = getKey(category, subcategory);
		HashMap<String, String> vars = new HashMap<String, String>();
		vars.put("anchor", anchor);
		vars.put("name", name);
		
		String value = templateWriter.writePartial("Index.entry.partial.html", vars);
		if( sections.containsKey(key))
		{
			String s = sections.get(key).concat("\n").concat(value);
			sections.put(key, s);
		} else {
			sections.put(key, value);			
		}
	}
	
	private String getKey(String category, String subCategory){
		if( !subCategory.equals("") ){
			return category.toLowerCase() + ":" + subCategory.toLowerCase();
		}
		return category.toLowerCase();
	}
	
	public String getCategory(Tag webref){
		String firstPart = webref.text().split("\\s")[0];
		String[] parts = firstPart.split(":");
		if( parts.length > 1 ){
			return parts[0];
		}
		return firstPart;
	}
	
	public String getSubcategory(Tag webref){
		String firstPart = webref.text().split("\\s")[0];
		String[] parts = firstPart.split(":");
		if( parts.length > 1 ){
			return parts[1];
		}
		return "";
	}
	
}
