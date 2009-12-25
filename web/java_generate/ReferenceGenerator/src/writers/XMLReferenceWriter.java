package writers;

import java.io.File;
import java.io.IOException;
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

public class XMLReferenceWriter extends BaseWriter {
	
	public static void write(String sourceDir, IndexWriter indexWriter) throws IOException
	{
		File directory = new File(sourceDir);
		File[] files = directory.listFiles();
		
		System.out.println("Loading XML files from: " + sourceDir );
		for(File f : files )
		{
			parseFile(f, indexWriter);
		}
	}
	
	private static void parseFile(File f, IndexWriter indexWriter)
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
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TemplateWriter templateWriter = new TemplateWriter();
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
		try {			
			String category = (String) xpath.evaluate("//category", doc, XPathConstants.STRING);
			String subcategory = (String) xpath.evaluate("//subcategory", doc, XPathConstants.STRING);
			String name = (String) xpath.evaluate("//name", doc, XPathConstants.STRING);
			String description = (String) xpath.evaluate("//description", doc, XPathConstants.STRING);
			String syntax = (String) xpath.evaluate("//syntax", doc, XPathConstants.STRING);
			String parameters = (String) xpath.evaluate("//parameters", doc, XPathConstants.STRING);
			String anchor = getAnchorFromName(name);
			indexWriter.addItem(category, subcategory, name, anchor);
			
			HashMap<String, String> vars = new HashMap<String, String>();
			vars.put("examples", getExamples(doc));
			vars.put("name", name);
			vars.put("description", description);
			vars.put("syntax", syntax);
			vars.put("parameters", parameters);	//need to write this out in a loop
			vars.put("related", (String) xpath.evaluate("//related", doc, XPathConstants.STRING));
			
			templateWriter.write("Generic.template.html", vars, anchor);
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
