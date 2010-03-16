<?PHP
/*---------------------------
 Florian Jenett
 01.05.2005
 Lenny Burdette
 2005.08.15
-----------------------------
 include functions
 */
 
/*** XML PARSING FUNCTIONS ***
* openXML()
* getValue()
* getAttribute();
* innerHTML()
* getFragmentAsArray()
* convertToFilename()
* chars()
**/

/*** FILE FUNCTIONS ***
* getRefFiles()
* writeFile()
* make_necessary_directories()
* copydirr()
**/

/*** REFERENCE INDEX FUNCTIONS ***
* category_index()
* category_image()
* alpha_index()
**/

/*** MISC FUNCTIONS ***
* translate
* microtime_float()
**/

/****************************************************************
Creates a DOMIT_Document from $file and returns the document
element
*****************************************************************/
function openXML($file)
{
    $doc =& new DOMIT_Document();
    if ($doc->loadXML(CONTENTDIR.$file)) {
        $xml =& $doc->documentElement;
        return $xml;
    } else {
        echo('Could not open or read XML file: '. $file . '<br />' . $doc->getErrorString());
        return false;
    }
}

/****************************************************************
Returns text value from first child of $xml fragment with
node name $nodeName
*****************************************************************/
function getValue(&$xml, $nodeName)
{
	$nodes = $xml->getElementsByTagName($nodeName);
    if ($nodes->getLength() > 0) {
	    $node = $nodes->item(0);
	    return trim(chars($node->getText()));
    } else {
        return false;
    }
}

function getAttribute(&$xml, $attribute)
{
    return chars($xml->getAttribute($attribute));
}

/****************************************************************
Returns entire contents of the first child of $xml fragment with
node name $nodeName
*****************************************************************/
function innerHTML(&$xml, $nodeName)
{
	$nodes = $xml->getElementsByTagName($nodeName);
    if ($nodes->getLength() > 0) {
	    $node = $nodes->item(0);
	    eregi("<$nodeName>(.*)<\/$nodeName>", $node->toString(), $matches);
        // replace invalid <c> with <kbd>
        $string = str_replace(array('<c>', '</c>'), array('<kbd>', '</kbd>'), $matches[1]);
        if (substr($string, 0, 1) == "\n") { $string = substr($string, 1); }
	    return trim(chars($string));
    } else {
        return false;
    }
}

/****************************************************************
Returns array of all children of $xml fragment with node name
$nodeName including children with node names specified in array
$children
*****************************************************************/
function getFragmentsAsArray(&$xml, $nodeName, $children)
{
    $array = array();
    $nodes = $xml->getElementsByTagName($nodeName);
    $nodes = $nodes->toArray();
    foreach ($nodes as $node) {
        $nodeArray = array();
        $nodeArray['cdata'] = $node->nodeValue;
        if (count($children) > 0) {
            foreach ($children as $child) {
                $nodeArray[$child] = getValue($node, $child);
            }
        }
        array_push($array, $nodeArray);
    }
    return $array;
}

/****************************************************************
Converts reference item names to static filenames, removing 
punctuation, replacing () with _
*****************************************************************/ 
function convertToFilename($string, $translation = false)
{
    global $operator_compare;
    
    if (strstr($string, 'PI ')) {
        $string = preg_replace("/ \((.*)\)/", '', $string);
    }
    
    // if it's the parentheses
    if (preg_match("/\(\) \((.+)\)/", $string)) {
        $string = 'parentheses';
    } else {
        if (preg_match("/ \((.*)\)/", $string)) {
            $string = preg_replace("/\((.*)\)/", '', $string);
            $string = str_replace(' ', '', $string);
            $string = $operator_compare[$string];
        }
    }
    
    $string = str_replace(' ', '', $string);
    $string = str_replace('[]', '', $string);
    $string = str_replace('()', '_', $string);
    
    return trim($string) . '.html';
}

$operator_compare = array(
  "!" => "logical NOT",
  "!=" => "inequality", 
  "%" => "modulo", 
  "&&" => "logical AND", 
  "&amp;&amp;" => "logical AND",
  "&gt;" => "greater than",
  ">" => "greater than",
  "&gt;=" => "greater than or equal to", 
  ">=" => "greater than or equal to",
  "&lt;" => "less than", 
  "<" => "less than",
  "&lt;=" => "less than or equal to", 
  "<=" => "less than or equal to",
  "()" => "parentheses", 
  "*" => "multiply", 
  "+" => "addition", 
  "++" => "increment", 
  "+=" => "add assign", 
  "," => "comma", 
  "-" => "minus", 
  "--" => "decrement", 
  "-=" => "subtract assign", 
  "." => "dot", 
  "/" => "divide", 
  "/**/" => "multiline comment", 
  "/* */" => "multiline comment",
  "/***/" => "doc comment", 
  "/** */" => "doc comment",
  "//" => "comment", 
  ";" => "semicolon", 
  "=" => "assign", 
  "==" => "equality", 
  "[]" => "array access", 
  "{}" => "curly braces", 
  "||" => "logical OR", 
  "&gt;&gt;" => "right shift",
  "&lt;&lt;" => "left shift",
  "&" => "bitwise AND",
  "&amp;" => "bitwise AND",
  "|" => "bitwise OR",
  "?:" => "conditional",
  ">>" => "right shift",
  "&gt;&gt;" => "right shift",
  "<<" => "left shift",
  "&lt;&lt;" => "left shift"
);

/****************************************************************
Escape XML chars
*****************************************************************/ 

function chars($string)
{
	//$string = str_replace('& ', '&amp; ', $string);
    //$string = str_replace(' > ', ' &gt; ', $string);
    //$string = str_replace(' < ', ' &lt; ', $string);
    //$string = str_replace('<<', '&lt;&lt;', $string);
    //$string = str_replace('>>', '&gt;&gt;', $string);
	$string = str_replace('<=', '&lt;=', $string);
	$string = preg_replace("/&[!#](\W)/", "&amp;$1", $string);
	$string = str_replace('&amp;&', '&amp;&amp;', $string);
	$string = preg_replace("/<(!\/\W)/", "&lt;$1", $string);
	$string = preg_replace("/>(!\s\W)/", "&gt;$1", $string);
    $string = stripslashes($string);
    return $string;
}

function codeExampleConvert($string)
{
	//$string = str_replace('& ', '&amp; ', $string);
    $string = str_replace('>', '&gt;', $string);
    $string = str_replace('<', '&lt;', $string);
    //$string = str_replace('<<', '&lt;&lt;', $string);
    //$string = str_replace('>>', '&gt;&gt;', $string);
    return $string;
}

/****************************************************************
Returns array of xml files for a language
*****************************************************************/ 
function getRefFiles($lang)
{
    // set directory path
    $dir = CONTENTDIR."api_$lang";
    // open directory pointer
    if ($dp = @opendir($dir)) {
        // iterate through file pointers
        while ($fp = readdir($dp)) {
            // points to relative paths
            if ($fp == '.' || $fp == '..') { continue; }
            // point to another directory
            if (is_dir($dir .'/'. $fp)) { continue; }
            // add file pointer to array 
            if (strstr($fp, '.xml') && $fp != 'blank.xml') {
                $files[] = $fp;
            }
        }
    } else {
        return false;
    }
    return $files;
}

function getXMLFiles($dir)
{
    // open directory pointer
    if ($dp = @opendir($dir)) {
        // iterate through file pointers
        while ($fp = readdir($dp)) {
            // points to relative paths
            if ($fp == '.' || $fp == '..') { continue; }
            // point to another directory
            if (is_dir($dir .'/'. $fp)) { continue; }
            // add file pointer to array 
            if (strstr($fp, '.xml') && $fp != 'blank.xml') {
                $files[] = $fp;
            }
        }
    } else {
        return false;
    }
    return $files;
}

/****************************************************************
Write a file
*****************************************************************/ 
function writeFile($filename, $content)
{
    make_necessary_directories(BASEDIR.$filename);
    $fp = fopen(BASEDIR.$filename, 'w');
    fwrite($fp, $content);
    fclose($fp);
}

function make_necessary_directories($filepath)
{	
	foreach(split('/',dirname($filepath)) as $dirPart) {
		if (!is_dir("$newDir$dirPart/") && !is_file("$newDir$dirPart/")) {
			@mkdir("$newDir$dirPart/", 0777);
		}
		$newDir="$newDir$dirPart/";
	}
}

/****************************************************************
Copy a directory and all of its contents
*****************************************************************/ 
/*
26.07.2005
Author: Anton Makarenko
   makarenkoa at ukrpost dot net
   webmaster at eufimb dot edu dot ua
*/
function copydirr($fromDir,$toDir,$recursive=true,$chmod=0777,$verbose=false)



/*
   copies everything from directory $fromDir to directory $toDir
   and sets up files mode $chmod
*/
{
//* Check for some errors
$errors=array();
$messages=array();

$messages[] = 'Debugging recursive: ' . $recursive;

if (!is_writable($toDir))
   $errors[]='target '.$toDir.' is not writable';
if (!is_dir($toDir))
   $errors[]='target '.$toDir.' is not a directory';
if (!is_dir($fromDir))
   $errors[]='source '.$fromDir.' is not a directory';
if (!empty($errors))
   {
   if ($verbose)
       foreach($errors as $err)
           echo '<strong>Error</strong>: '.$err.'<br />';
   return false;
   }
//*/
$exceptions=array('.','..','.svn');
//* Processing
$handle=opendir($fromDir);
while (false!==($item=readdir($handle)))
   if (!in_array($item,$exceptions))
       {
       //* cleanup for trailing slashes in directories destinations
       $from=str_replace('//','/',$fromDir.'/'.$item);
       $to=str_replace('//','/',$toDir.'/'.$item);
       //*/
       if (is_file($from))
           {
           if (@copy($from,$to))
               {
               chmod($to,$chmod);
               touch($to,filemtime($from)); // to track last modified time
               $messages[]='File copied from '.$from.' to '.$to;
               }
           else
               $errors[]='cannot copy file from '.$from.' to '.$to;
           }
       if (is_dir($from)  && $recursive)
           {
           if (!is_dir($to)) { 
             mkdir($to, $chmod); 
             $messages[]='Directory created: '.$to;
           }
/*
           if (@mkdir($to))
           {
            chmod($to,$chmod);
           }
*/
           else
               $errors[]='cannot create directory '.$to;
           copydirr($from,$to,$chmod,$verbose);
           }
       }
closedir($handle);
//*/
//* Output
if ($verbose)
   {
   foreach($errors as $err)
       echo '<strong>Error</strong>: '.$err.'<br />';
   foreach($messages as $msg)
       echo $msg.'<br />';
   }
//*/
return true;
}

/****************************************************************
Formats reference objects for index
*****************************************************************/ 
function category_index($array)
{
    global $break_before;
    global $translation;
    
    $html = "<div class=\"ref-col\">";
    foreach ($array as $cat => $subs) {
        if (count($subs) > 0) {
            $empty = true;
            if (in_array($cat, $break_before)) {
                $html .= "\n</div><div class=\"ref-col\">\n";            
            }
            $section = "\n<div class=\"category\">\n<b>{$translation->cat_tr[$cat]}</b>\n";
            foreach ($subs as $sub => $refs) {
                if (count($refs) > 0) {
                    if ($sub != '') {
						$section .= "\t<h5>$sub</h5>\n";
					} else {
						$section .= "<br /><br />";
					}
                    foreach ($refs as $ref) {
                        $section .= "\t\t<a href=\"$ref[1]\">$ref[0]</a><br />\n";
                        $empty = false;
                    }
                }
            }
        }
        if (!$empty) $html .= $section . "\n</div>";
    }
    return $html . '</div>';
}

/****************************************************************
Returns name of image for category title
*****************************************************************/ 
function category_image($cat)
{
    return strtolower(eregi_replace("[^A-Za-z0-9]", '', eregi_replace("\&(.+);", '', $cat))) . ".gif";
}

/****************************************************************
Formats array of reference objects alphabetically
*****************************************************************/ 
function alpha_index($array)
{
    $per_col = ceil(count($array)/3);
    
    $firstchar = key($array);
    $firstchar = eregi_replace("[^A-Za-z0-9]", '', $firstchar{0});
    $count = 0;
    
    $html = "<div class=\"ref-col\">\n";
    foreach ($array as $key => $ref) {
        if (eregi_replace("[^A-Za-z0-9]", '', $key{0}) != $firstchar) {
            $firstchar = $key{0};
            $html .= "<br/>\n\n";
            if ($count >= $per_col) {
                $html .= "</div>\n<div class=\"ref-col\">\n";
                $count = 0;
            }
        }
        $html .= "\t\t<a href=\"$ref[1]\">$ref[0]</a><br />\n";
        $count++;
    }
    return $html . '</div>';
}

/****************************************************************
Replaces word from translation object
*****************************************************************/ 
function tr($word)
{
    global $translation;
    if (isset($translation->navigation[$word])) { return $translation->navigation[$word]; }
    else if (isset($translation->attributes[$word])) { return $translation->attributes[$word]; }
    else if (isset($translation->meta[$word])) { return $translation->meta[$word]; }
    else { return $word; }
}

/*** BENCHMARKING ***/
function microtime_float()
{
   list($usec, $sec) = explode(" ", microtime());
   return ((float)$usec + (float)$sec);
}
 
/**
 *		(Short description - used in indexlists)
 *
 *		functions.inc.php
 *
 *		(Multiple line detailed description.)
 *		(The handling of line breaks and HTML is up to the renderer.)
 *		(Order: short description - detailed description - doc tags.)
 *
 *
 *		@author 		Florian Jenett - mail@florianjenett.de
 *
 *		created:		01.05.2005 - 15:11 Uhr
 *		modified:		-last-modified-
 *
 *		@since 			-since-version-
 *		@version 		-current-version-
 *
 *		@see			-extends-
 *
 */

/**
 *		Functions / Methods
 */
 

function xml_read( $xmlSource, $from_file=TRUE )
{
	global $xpath;
	
	$xpath			= NULL;
	
	$xmlOptions 	= array(	XML_OPTION_CASE_FOLDING => FALSE ,
								XML_OPTION_SKIP_WHITE 	=> TRUE 	);
									
	$xpath			= new XPath(FALSE, $xmlOptions);
	
	//$xpath->bDebugXmlParse = TRUE;
	
	if ($from_file)
	{
		
		if ( file_exists($xmlSource) && is_readable($xmlSource) )
		{
			if ( !$xpath->importFromFile($xmlSource) )
			{
				die( 'xml_read(): XPath error > '.$xpath->getLastError() );
			}
		}
		else
			die ( 'xml_read(): Can\'t find or open: '.realpath( $xmlSource ) );
	}
	else
	{
		if ( !empty($xmlSource) )
		{
			if ( !$xpath->importFromString($xmlSource) )
			{
				die( 'xml_read(): XPath error > '.$xpath->getLastError() );
			}
		}
		else
			die( 'xml_read(): Given source is empty in line.' );
	}
	
	return true;
}


// chars() converts any text to html with entities


function charsUTF8 ( $plain , $endings=TRUE )
{
	$plain = seems_utf8($plain) ? utf8_decode($plain) : $plain;
	
	$trans = get_html_translation_table(HTML_ENTITIES, ENT_COMPAT);
	
	foreach ($trans as $key => $value)
		$trans[$key] = '&#'.ord($key).';';

	$plain = strtr($plain, $trans);
	
	if ($endings) $plain = preg_replace( '/\r\n|\r|\n/', '<br />', $plain);
	
	return $plain;
}

function seems_utf8($Str)
{
	// bmorel at ssi dot fr
	// see: http://us2.php.net/utf8_encode
	
	for ($i=0; $i<strlen($Str); $i++)
	{
		if (ord($Str[$i]) < 0x80) continue; 				# 0bbbbbbb
		elseif ((ord($Str[$i]) & 0xE0) == 0xC0) $n=1; 		# 110bbbbb
		elseif ((ord($Str[$i]) & 0xF0) == 0xE0) $n=2; 		# 1110bbbb
		elseif ((ord($Str[$i]) & 0xF8) == 0xF0) $n=3; 		# 11110bbb
		elseif ((ord($Str[$i]) & 0xFC) == 0xF8) $n=4; 		# 111110bb
		elseif ((ord($Str[$i]) & 0xFE) == 0xFC) $n=5; 		# 1111110b
		else return false; 									# Does not match any model
		
		for ($j=0; $j<$n; $j++)
		{ 													# n bytes matching 10bbbbbb follow ?
			if ((++$i == strlen($Str)) || ((ord($Str[$i]) & 0xC0) != 0x80))
				return false;
		}
	}
	return true;
}


// savely encode to utf-8 ...

function utf8($str)
{
	$str = get_magic_quotes_gpc() ? stripslashes($str) : $str;
	return seems_utf8($str) ? $str : utf8_encode($str);
}


function ine( $var )
{
	return isset( $var ) && !empty( $var ) ;
}


function url_validate( $link )
{        

	// jack at jtr dot de
	// http://us4.php.net/manual/en/function.fsockopen.php

	$url_parts = @parse_url( $link );

   if ( empty( $url_parts["host"] ) ) return( false );

   if ( !empty( $url_parts["path"] ) )
   {
		$documentpath = $url_parts["path"];
   }
   else
   {
		$documentpath = "/";
   }

   if ( !empty( $url_parts["query"] ) )
   {
		$documentpath .= "?" . $url_parts["query"];
   }

	$host = $url_parts["host"];
	$port = $url_parts["port"];
	// Now (HTTP-)GET $documentpath at $host";

	if (empty( $port ) ) $port = "80";
	$socket = @fsockopen( $host, $port, $errno, $errstr, 30 );
   if (!$socket)
   {
	   return(false);
   }
   else
   {
		fwrite ($socket, "HEAD ".$documentpath." HTTP/1.0\r\nHost: $host\r\n\r\n");
		$http_response = fgets( $socket, 22 );
	   
	   if ( ereg("200 OK", $http_response, $regs ) )
	   {
		   return(true);
			fclose( $socket );
	   } else
	   {
//                echo "HTTP-Response: $http_response<br>";
			return(false);
	   }
   }
}

?>