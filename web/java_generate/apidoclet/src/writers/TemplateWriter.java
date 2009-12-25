package writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class TemplateWriter extends BaseWriter {
	
	public static String varPrefix = "<!-- ";
	public static String varSuffix = " -->";
	
	public TemplateWriter()
	{
		
	}
	
	public void write( String templateName, HashMap<String, String> vars, String outputName ) throws IOException
	{
		write( templateName, vars, outputName, false );
		write( templateName, vars, outputName, true );
		System.out.println("Writing " + outputName + " from template");
	}
	
	private void write( String templateName, HashMap<String, String> vars, String outputName, Boolean isLocal ) throws IOException
	{
		String[] templateFile = FileUtils.loadStrings(Shared.i().TEMPLATE_DIRECTORY() + templateName);
		ArrayList<String> output = new ArrayList<String>();
		vars.put("timestamp", timeStamp());
		if(isLocal)
		{ //add local nav
			vars.put("navigation", writePartial("Nav.local.template.html", vars));
		} else
		{
			vars.put("navigation", writePartial("Nav.web.template.html", vars));
		}
		
		BufferedWriter out = makeWriter(outputName, isLocal);
		
		for( String line : templateFile )
		{
			//check if it contains a variable we want to replace, then replace it
			line = writeLine(line, vars);		
			output.add(line);
		}
		for( String line : output )
		{
			out.write(line+"\n");			
		}
		out.close();
	}
	
	public String writePartial( String templateName, HashMap<String, String> vars ) throws IOException
	{	//use to write partials to be assigned to vars keys
		String[] templateFile = FileUtils.loadStrings(Shared.i().TEMPLATE_DIRECTORY()+templateName);
		String ret = "";
		
		for( String line : templateFile )
		{
			line = writeLine(line, vars);
			ret = ret.concat(line+"\n");
		}
		
		return ret;
	}
	
	public String writeLoop( String templateName, ArrayList<HashMap<String, String>> varSet ) throws IOException
	{
		return writeLoop(templateName, varSet, "\n");
	}
	
	public String writeLoop( String templateName, ArrayList<HashMap<String, String>> varSet, String separator ) throws IOException
	{
		String[] templateFile = FileUtils.loadStrings(Shared.i().TEMPLATE_DIRECTORY()+templateName);
		String ret = "";
		
		for( HashMap<String, String> vars : varSet )
		{			
			for( String line : templateFile )
			{
				ret = ret + writeLine(line, vars) + separator;
			}
		}
		if(ret.endsWith(separator)){
			ret = ret.substring(0, ret.lastIndexOf(separator));
		}
//		if( varSet.size() == 0 ) {System.out.println("Sending back after empty map: " + ret);}
		return ret;
	}
	
	private String writeLine(String line, HashMap<String, String> map)
	{
		for( String key : map.keySet() )
		{
			String var = varPrefix + key + varSuffix;
			if(line.contains(var))
			{
				String value = map.get(key);
				line = line.replaceFirst(var, value);
				String requireStart = varPrefix + "require:" + key + varSuffix;
				String requireEnd = varPrefix + "end" + varSuffix;
				if(value.equals(""))
				{	//remove html around things that are absent (like images)
					while(line.contains(requireStart))
					{	
						String sub = line.substring(line.indexOf(requireStart), line.indexOf(requireEnd) + requireEnd.length());
						line = line.replace(sub, "");
					}
				} else{
					line = line.replaceAll(requireStart, "");
					line = line.replaceAll(requireEnd, "");
				}
			}
		}			
		return line;
	}
	
}
