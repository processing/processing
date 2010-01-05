package writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
/**
 * Writes the index page of libraries.
 * Also grabs the xml files in lib/dir/events
 * and sends them to the XMLReferenceWriter
 * 
 * @author davidwicks
 *
 */
public class LibraryIndexWriter extends IndexWriter {
	HashMap<String,String> sections;
	ArrayList<String> classes;
	ArrayList<String> events;
	TemplateWriter templateWriter;
	
	public LibraryIndexWriter(PackageDoc doc, String outputPath){
		sections = new HashMap<String,String>();
		classes = new ArrayList<String>();
		events = new ArrayList<String>();
		
		templateWriter = new TemplateWriter();
		writePartials(doc, outputPath);
	}
	
	private void writePartials(PackageDoc doc, String outputPath){
		for( ClassDoc cd : doc.allClasses() ){
			addItem(cd);
		}
		
		String examplePath = getExamplePath(doc.allClasses()[0]);
		examplePath = examplePath.substring(0, examplePath.lastIndexOf("/"));
		
		try {
			XMLReferenceWriter.write(examplePath + "/events", outputPath,  this);
			getXMLInformation(examplePath + "/index.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sections.put("classes", explode(classes));
		sections.put("events", explode(events));
		try{			
			write(outputPath);
		} catch (IOException e) {
			// TODO: handle exception
		}
	}
	
	private void getXMLInformation(String path){
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		Document xmlDoc = null;
		try {
			builder = factory.newDocumentBuilder();
			xmlDoc = builder.parse(path);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("WARNING: no index.xml file found at: " + path );
			return;
		}
		
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
		try {
			String name = (String) xpath.evaluate("//libraryName", xmlDoc, XPathConstants.STRING);
			String desc = (String) xpath.evaluate("//libraryDescription", xmlDoc, XPathConstants.STRING);
			
			sections.put("libraryname", name);
			sections.put("librarydescription", desc);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String explode(ArrayList<String> array){
		String ret = "";
		Collections.sort(array);
		for(String s : array){
			ret += s + "\n";
		}
		return ret;
	}
	
	private String libraryName(PackageDoc doc){
		//this should probably come from XML
		String[] parts = doc.name().split("\\.");
		String ret = parts[parts.length-1];
		return ret;
	}
	
	private String libraryDescription(PackageDoc doc){
		//this should probably also come from XML
		String[] parts = doc.name().split("\\.");
		String ret = parts[parts.length-1];
		return ret;
	}
	
	public void write(String path) throws IOException{
		templateWriter.write("Library.Index.template.html", sections, path+"index.html");
	}
	
	public void addItem(ClassDoc doc){
		ArrayList<HashMap<String, String>> methods = new ArrayList<HashMap<String,String>>();
		HashMap<String, String> cmap = new HashMap<String, String>();
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("name", getName(doc));
		map.put("anchor", getLocalAnchor(doc));
		methods.add(map);
		
		for(MethodDoc m : doc.methods()){
			if(Shared.i().isWebref(m)){
				HashMap<String, String> methodMap = new HashMap<String, String>();
				methodMap.put("name", getName(m));
				methodMap.put("anchor", getLocalAnchor(m));
				methods.add(methodMap);
			}
		}
		
		cmap.put("methods", templateWriter.writeLoop("Related.partial.html", methods));
		cmap.put("classname", getName(doc) + " Class");
		cmap.put("classdescription", shortText(doc));
		
		classes.add(templateWriter.writePartial("Library.Section.partial.html", cmap));
	}
	
	public void addEvent(String name, String anchor){
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("name", name);
		map.put("anchor", getAnchorFromName(name));
		events.add(templateWriter.writePartial("Related.partial.html", map) + "\n");
	}
}
