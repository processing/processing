package writers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLReferenceWriter extends BaseWriter {
	
	public static void write(String sourceDir, IndexWriter indexWriter) throws IOException
	{
		write(sourceDir, "", indexWriter);
	}
	
	public static void write(String sourceDir, String dstDir, IndexWriter indexWriter) throws IOException
	{
		File directory = new File(sourceDir);
		File[] files = directory.listFiles();
		
		if(files == null ){
			return;
		}
		
		for(File f : files )
		{
			if(f.getAbsolutePath().endsWith(".xml")){				
				parseFile(f, dstDir, indexWriter);
			}
		}
	}
	
	private static void parseFile(File f, String dst, IndexWriter indexWriter)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		Document doc = null;
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(f.getPath());
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed to parse " + f.getAbsolutePath());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed to parse " + f.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed to parse " + f.getAbsolutePath());
		}
		
		TemplateWriter templateWriter = new TemplateWriter();
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
		try {			
			HashMap<String, String> vars = new HashMap<String, String>();
			
			String category = (String) xpath.evaluate("//category", doc, XPathConstants.STRING);
			String subcategory = (String) xpath.evaluate("//subcategory", doc, XPathConstants.STRING);
			String name = (String) xpath.evaluate("//name", doc, XPathConstants.STRING);
			String description = (String) xpath.evaluate("//description", doc, XPathConstants.STRING);
			String syntax = (String) xpath.evaluate("//syntax", doc, XPathConstants.STRING);
			String anchor = dst + getAnchorFromName(name);
			String usage = (String) xpath.evaluate("//usage", doc, XPathConstants.STRING);
			if(indexWriter instanceof LibraryIndexWriter ){				
				((LibraryIndexWriter) indexWriter).addEvent(name, anchor);
				vars.put("csspath", "../../");
			} else {				
				indexWriter.addItem(category, subcategory, name, anchor);
			}
			
			vars.put("examples", getExamples(doc));
			vars.put("name", name);
			vars.put("description", description);
			vars.put("syntax", syntax);
			vars.put("usage", usage);
			vars.put("parameters", getParameters(doc));	//need to write this out in a loop
			vars.put("related", getRelated(doc));
			
			templateWriter.write("Generic.template.html", vars, anchor);
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed to parse " + f.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed to parse " + f.getAbsolutePath());
		}
	}
	
	protected static String getParameters(Document doc) throws IOException{
		
		ArrayList<HashMap<String, String>> ret = new ArrayList<HashMap<String,String>>();
		//get parameters for this methos
		XPath xpath = getXPath();
		try{
			XPathExpression expr = xpath.compile("//parameter");
			Object result = expr.evaluate(doc, XPathConstants.NODESET);
			NodeList parameters = (NodeList) result;

			for (int i = 0; i < parameters.getLength(); i++) { 
				String name = (String) xpath.evaluate("label", parameters.item(i), XPathConstants.STRING);
				String desc = (String) xpath.evaluate("description", parameters.item(i), XPathConstants.STRING);

				HashMap<String, String> map = new HashMap<String, String>();
				map.put("name", name);
				map.put("description", desc);
				ret.add(map);						

			}
		} catch (XPathExpressionException e) {
			// TODO: handle exception
		}
		
		TemplateWriter templateWriter = new TemplateWriter();
		return templateWriter.writeLoop("Parameter.partial.html", ret);
	}
	
	protected static String getRelated(Document doc) throws IOException{
		TemplateWriter templateWriter = new TemplateWriter();
		ArrayList<HashMap<String, String>> vars = new ArrayList<HashMap<String,String>>();
		
		try{
			XPath xpath = getXPath();
			String relatedS = (String) xpath.evaluate("//related", doc, XPathConstants.STRING);
			if(relatedS.equals("")){
				return "";
			}
			String[] related = relatedS.split("\\n");
			
			for(int i=0; i < related.length; i++ ){
				HashMap<String, String> map = new HashMap<String, String>();
				String name = related[i];
				if(!name.equals("")){
					map.put("name", name);
					map.put("anchor", getAnchorFromName(name));				
					vars.add(map);					
				}
			}
		}catch(XPathExpressionException e){
			
		}
		return templateWriter.writeLoop("Related.partial.html", vars);
	}
	
	static protected XPath getXPath(){
		XPathFactory xpathFactory = XPathFactory.newInstance();
		return xpathFactory.newXPath();
	}
}
