package writers;

import java.io.File;
import java.util.ArrayList;

import com.sun.javadoc.ProgramElementDoc;

public class Shared {
//	what we're looking for
	private static Shared instance;
	private String webrefTagName = "webref";
	private String coreClassName = "PApplet";
	
	//where things go
	private String outputDirectory = "web_reference";
	private String localOutputDirectory = "local_reference";
	private String imageDirectory = "images";
	private String fileExtension = ".html";
	
	//where things come from
	private String templateDirectory = "templates";
	private String exampleDirectory = "web_examples";
	public ArrayList<String> corePackages;

	private Shared(){
		corePackages = new ArrayList<String>();
	}
	public static Shared i()
	{
		if(instance == null)
		{
			instance = new Shared();
		}
		return instance;
	}
	
	public void setWebrefTagName(String webrefTagName) {
		this.webrefTagName = webrefTagName;
	}
	public String getWebrefTagName() {
		return webrefTagName;
	}
	public void setCoreClassName(String coreClassName) {
		this.coreClassName = coreClassName;
	}
	public String getCoreClassName() {
		return coreClassName;
	}
	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}
	public String getOutputDirectory() {
		return outputDirectory;
	}
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}
	public String getFileExtension() {
		return fileExtension;
	}
	public void setTemplateDirectory(String templateDirectory) {
		this.templateDirectory = templateDirectory;
	}
	public String getTemplateDirectory() {
		return templateDirectory;
	}
	public String TEMPLATE_DIRECTORY(){
		return templateDirectory + "/";
	}
	
	public void setExampleDirectory(String exampleDirectory) {
		this.exampleDirectory = exampleDirectory;
	}
	public String getExampleDirectory() {
		return exampleDirectory;
	}
	public String EXAMPLE_DIRECTORY(){
		return exampleDirectory + "/";
	}
	
	public void setImageDirectory(String imageDirectory) {
		this.imageDirectory = imageDirectory;
	}
	public String getImageDirectory() {
		return imageDirectory;
	}
	
	public String IMAGE_DIRECTORY(){
		return imageDirectory + "/";
	}
	public void setLocalOutputDirectory(String localOutputDirectory) {
		this.localOutputDirectory = localOutputDirectory;
	}
	public String getLocalOutputDirectory() {
		return localOutputDirectory;
	}
	
	public String LOCAL_OUTPUT_DIRECTORY()
	{
		return localOutputDirectory + "/";
	}
	
	public String OUTPUT_DIRECTORY()
	{
		return outputDirectory + "/";
	}
	
	public boolean isCore(ProgramElementDoc doc){
		return corePackages.contains(doc.containingPackage().name());
	}
	
	public boolean isWebref(ProgramElementDoc doc){
		return doc.tags(webrefTagName).length > 0;
	}
	
	public void createOutputDirectory(String dir){
		System.out.println("Creating output directory: " + dir );
		File f = new File(LOCAL_OUTPUT_DIRECTORY() + dir);
		f.mkdirs();
		
		f = new File(OUTPUT_DIRECTORY() + dir);
		f.mkdirs();
	}
}
