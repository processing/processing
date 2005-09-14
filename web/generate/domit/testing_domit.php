<html>
<!-- The DOMIT! XML Parser Testing Interface -->
<head><title>DOMIT! Testing Interface</title>
<link rel="stylesheet" href="testing_domit.css" />
<script language="javascript">
	function showWindow(url) {
		window.open(url, "", "width=760,height=540,scrollbars,resizable,menubar");
	} //showWindow
</script>
</head>

<body>
<h2>DOMIT! Testing Interface</h2>

<form action="testing_domit.php" method="POST">

<?php
	
class test_domit {
	var $xmldoc;
	var $xmlfile;
	var $xmlurl = "";
	var $xmltext = "";
	var $domparser = "domit";
	var $saxparser = "saxy";
	var $xmloutput = "tonormalizedstring";
	var $xmlaction = null;
	var $extension = "xml";
	
	function start() {
		$this->updateVars();
		$this->buildInterface();
		$this->parse();
	} //start
	
	function parse() {
		if ($this->xmlaction != null) {
			require_once("timer.php");
			$timer = new Timer();
			
			$success = false;
			($this->saxparser == "saxy") ? ($parseSAXY = true) : ($parseSAXY = false);
				
			$timer->start();
			
		    switch($this->domparser){
		    	case ("domit"): 
					require_once('xml_domit_parser.php');
					$this->xmldoc =& new DOMIT_Document();
					break;
					
		    	case ("domitlite"): 
		    		require_once('xml_domit_lite_parser.php');
					$this->xmldoc =& new DOMIT_Lite_Document();
					break;
		    } // switch
			
			switch($this->xmlaction){
				case "parsefile":
					$success = $this->xmldoc->loadXML($this->xmlfile, $parseSAXY);
					break;
					
				case "parseurl":
					$success = $this->xmldoc->loadXML($this->xmlurl, $parseSAXY);
					break;
					
				case "parsetext":
					$success = $this->xmldoc->parseXML($this->xmltext, $parseSAXY); 
					break;
				
			}
			
			$timer->stop();
			
			if ($success) {
				echo "<br /><br />Time elapsed: " . $timer->getTime() . "seconds<br /><br />\n";
				
				
				if ($this->xmloutput == "tostring") {
			    	echo $this->xmldoc->toString(true);
				}
				else if ($this->xmloutput == "tonormalizedstring") {
					echo $this->xmldoc->toNormalizedString(true);
				} 
				else if ($this->xmloutput == "toarray") {
					echo "<pre>\n";
					print_r($this->xmldoc->toArray());
					echo "</pre>\n";
				}
			}
			else {
				echo "<br /><br />Parsing error: xml document may be invalid or malformed.\n";
			}
			
		}
	} //parse
	
	function updateVars() {
		global $HTTP_POST_VARS;
		
		if (isset($HTTP_POST_VARS['xmlaction'])) {
			$this->xmlaction = $HTTP_POST_VARS['xmlaction'];
		}
		
		if (isset($HTTP_POST_VARS['domparser'])) {
			$this->domparser = $HTTP_POST_VARS['domparser'];
		}
		
		if (isset($HTTP_POST_VARS['saxparser'])) {
			$this->saxparser = $HTTP_POST_VARS['saxparser'];
		}
		
		if (isset($HTTP_POST_VARS['xmloutput'])) {
			$this->xmloutput = $HTTP_POST_VARS['xmloutput'];
		}
		
		if (isset($HTTP_POST_VARS['xmlfile'])) {
			$this->xmlfile = $HTTP_POST_VARS['xmlfile'];
		}
		
		if (isset($HTTP_POST_VARS['xmltext'])) {
			$this->xmltext = $HTTP_POST_VARS['xmltext'];
			$this->xmltext = str_replace("\\\"", '"', $this->xmltext);
			$this->xmltext = str_replace("\\'", "'", $this->xmltext);
		}
		
		if (isset($HTTP_POST_VARS['xmlurl'])) {
			$this->xmlurl = $HTTP_POST_VARS['xmlurl'];
		}
	} //updateVars
	
	function buildInterface() {
		$files = $this->getFiles($this->extension);
		
		echo "<table width=\"760\" cellpadding=\"0\" cellspacing=\"0\"\n";
		
		//DOM TITLE
		echo "<tr class=\"row0\">\n";
		echo "<td><p>Choose a DOM Parser</p></td>\n";		
		echo "<td>&nbsp;</td></tr>\n\n";
		
		//CHOOSE DOM PARSER
		
		echo "<tr class=\"row1\">\n";
		echo "<td><p>\n";
		echo "<select name=\"domparser\">\n";		
		echo "<option value=\"domit\"" . 
				(($this->domparser == "domit") ? "selected" : "") .
				">DOMIT!</option>\n";
		echo "<option value=\"domitlite\"" . 
				(($this->domparser == "domitlite") ? "selected" : "") .
				">DOMIT! Lite</option>\n";
		echo "</select>\n";	
		echo "</p></td><td>&nbsp;</td></tr>\n\n";

		//SPACER
		echo "<tr><td>&nbsp;</td><td>&nbsp;</td></tr>\n\n";
		
		//SAX TITLE
		echo "<tr class=\"row0\">\n";
		echo "<td><p>Choose a SAX Parser</p></td>\n";		
		echo "<td>&nbsp;</td></tr>\n\n";
		
		//CHOOSE SAX PARSER
		echo "<td><p>\n";
		echo "<select name=\"saxparser\">\n";		
		echo "<option value=\"saxy\"" . 
				(($this->saxparser == "saxy") ? "selected" : "") .
				">SAXY</option>\n";
		echo "<option value=\"expat\"" . 
				(($this->saxparser == "expat") ? "selected" : "") .
				">Expat</option>\n";
		echo "</select>\n";	
		echo "</p></td><td>&nbsp;</td></tr>\n\n";
		
		//SPACER
		echo "<tr><td>&nbsp;</td><td>&nbsp;</td></tr>\n\n";
		
		//OUTPUT FORMAT TITLE
		echo "<tr class=\"row0\">\n";
		echo "<td><p>Choose an Output Format</p></td>\n";		
		echo "<td>&nbsp;</td></tr>\n\n";
		
		//CHOOSE OUTPUT FORMAT
		echo "<td><p>\n";
		echo "<select name=\"xmloutput\">\n";		
		echo "<option value=\"tonormalizedstring\"" . 
				(($this->xmloutput == "tonormalizedstring") ? "selected" : "") .
				">toNormalizedString()</option>\n";
		echo "<option value=\"tostring\"" . 
				(($this->xmloutput == "tostring") ? "selected" : "") .
				">toString()</option>\n";
		echo "<option value=\"toarray\"" . 
				(($this->xmloutput == "toarray") ? "selected" : "") .
				">toArray()</option>\n";
		echo "</select>\n";	
		echo "</p></td><td>&nbsp;</td></tr>\n\n";
		
		//SPACER
		echo "<tr><td>&nbsp;</td><td>&nbsp;</td></tr>\n\n";
		
		//PARSER SOURCE DATA TITLING
		echo "<tr class=\"row0\">\n";
		echo "<td><p>Choose an XML File and click \"Parse File\"</p></td>\n";		
		
		echo "<td><p>Paste in some XML Text and click \"Parse Text\"</p></td>\n";
		echo "</tr>\n\n";
		
		//CHOOSE XML FILE
		echo "<tr class=\"row1\">\n";
		echo "<td><p>\n";
		echo "<select name=\"xmlfile\" size=\"5\">\n";		
		echo "<option value=\"-1\">------------------------------Choose a File----------------------------</option>\n";
		
		$total = count($files);
		
		for ($i = 0; $i < $total; $i++) {
			$currFile = $files[$i];
			
			echo "<option value=\"" . $currFile . "\"" . 
				(($this->xmlfile == $currFile) ? "selected" : "") .
				">" .  $currFile . "</option>\n";
		}
	
		echo "</select>\n";	
		echo "</p>\n";
		echo "</td>\n\n";
		
		//PASTE XML TEXT
		echo "<td><p>\n";
		echo "<textarea name=\"xmltext\" cols=\"40\" rows=\"5\"></textarea>\n";
		echo "</p></td>\n";
		echo "</tr>\n\n";
		
		//SPACER AND HIDDEN TEXT FIELD
		echo "<tr><td>&nbsp;<input type=\"hidden\" name=\"xmlaction\"></td><td>&nbsp;</td></tr>\n\n";

		//PARSE FILE BUTTON
		echo "<tr class=\"row1\">\n";
		echo "<td><p>\n";
		echo "<input type=\"button\" name=\"parsefile\" value=\"Parse File\"" . 
				"onclick=\"this.form.xmlaction.value='parsefile';this.form.submit();\">\n";
		echo "</p>\n";
		echo "</td>\n\n";
		
		//PARSE TEXT BUTTON
		echo "<td><p>\n";
		echo "<input type=\"button\" name=\"parsetext\" value=\"Parse Text\"" . 
				"onclick=\"this.form.xmlaction.value='parsetext';this.form.submit();\" />\n";
		echo "</p></td>\n";
		echo "</tr>\n\n";
		
		//SPACER
		echo "<tr><td>&nbsp;</td><td>&nbsp;</td></tr>\n\n";
		echo "<tr><td>&nbsp;</td><td>&nbsp;</td></tr>\n\n";
		
		//PARSE URL TITLING
		echo "<tr class=\"row0\">\n";
		echo "<td><p>Enter the url of an XML file and click \"Parse URL\"</p></td>\n";		
		echo "<td>&nbsp;</td></tr>\n\n";
		
		//PARSE URL FIELD
		echo "<td><p>\n";
		echo "<input type=\"text\" name=\"xmlurl\" size=\"53\" value=\"" . $this->xmlurl . "\" />\n";	
		echo "</p></td><td>&nbsp;</td></tr>\n\n";
		
		//SPACER
		echo "<tr><td>&nbsp;</td><td>&nbsp;</td></tr>\n\n";
		
		//PARSE URL BUTTON
		echo "<td><p>\n";
		echo "<input type=\"button\" name=\"parseurl\" value=\"Parse URL\"" . 
				"onclick=\"this.form.xmlaction.value='parseurl';this.form.submit();\" />\n";
		echo "</p></td>\n";
		echo "</tr>\n\n";
		
		echo "</table>\n\n";
	} //buildInterface
	
	function getFiles($extension) {
		$arFiles = array();
		
		if ($handle = opendir('.')) {
			while (false !== ($file = readdir($handle))) {			 
				if ($file != "." && $file != "..") { 
					if ($extension == $this->getExtension($file)) {
			    		$arFiles[] = $file; 
					}					
				} 
			}
		}
		
   		closedir($handle);
		 
		return $arFiles;
	} //getFiles	
	
	function getExtension($filename) {
		$extension = "";
		$dotPos = strpos($filename, "."); 
		
		if ($dotPos !== false) {
		    $extension = substr($filename, ($dotPos + 1));
		}

		return $extension;
	} //getExtension
} //test_domit

$testSuite = new test_domit();
$testSuite->start();

?>

</form>

<br />
<p><a href="http://www.engageinteractive.com/domit/"><img src='domitBanner.gif' width="120" height="60"></a></p>
</body>
</html>