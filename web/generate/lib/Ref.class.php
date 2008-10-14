<?
/**
* Reference Item class
*
* usage:
* $ref = new Ref('path_to_xml_file');
* $ref->display();
*
* includes refTableRow() function
*
* Lenny Burdette
* 2005 08 16 / 2005 08 16
*
**/

class Ref
{
    var $xmlFile;
	var $filepath;
    var $xml;

    // simple nodes
	var $name;
	var $category;
	var $subcategory;
	var $usage;
	var $returns;
	var $related;
	var $availability;
	var $partof;
	var $level;
	var $type;
    
    // embedded html
	var $description;
	var $syntax;
    var $constructor;
    
    // optionally multiple
    var $examples = array();
	var $fields = array();
	var $methods = array();
	var $parameters = array();
	var $cparameters = array();
	
	function Ref($filename)
	{
			$this->filepath = $filename;
        if ($this->xml =& openXML($filename)) {
            $this->parse($this->xml);
	    }
        $this->xmlFile = basename($filename);
	}
    
    function parse($xml)
    {
		$this->name 		= getValue($xml, 'name');
		$this->category 	= getValue($xml, 'category');
		$this->subcategory	= getValue($xml, 'subcategory');
		$this->usage		= getValue($xml, 'usage');
		$this->returns		= getValue($xml, 'returns');
		$this->related		= getValue($xml, 'related');
        $this->related      = preg_split("/\n/", $this->related, -1, PREG_SPLIT_NO_EMPTY);
		$this->availability = getValue($xml, 'availability');
		$this->partof		= getValue($xml, 'partof');
		$this->level		= getValue($xml, 'level');
		$this->type         = getValue($xml, 'type');
		
		$this->hasParameter  = getValue($xml, 'label');  // Added for 149
		$this->hasCode       = getValue($xml, 'code');   // Added for 149
        
		$this->description	= innerHTML($xml, 'description');
		$this->syntax		= innerHTML($xml, 'syntax');
        $this->constructor  = innerHTML($xml, 'constructor');
        
        $this->examples     = getFragmentsAsArray($xml, 'example', array('image', 'code'));
        $this->fields       = getFragmentsAsArray($xml, 'field', array('fname', 'fdescription'));
        $this->methods      = getFragmentsAsArray($xml, 'method', array('mname', 'mdescription'));
        $this->parameters   = getFragmentsAsArray($xml, 'parameter', array('label', 'description'));
        $this->cparameters  = getFragmentsAsArray($xml, 'cparameter', array('clabel', 'cdescription'));
    }
    
    function name()
    {
        $file = str_replace('.xml', '', $this->xmlFile);
        $file = str_replace('_var', '', $file);
        if (strstr($this->name, '()') && substr($this->name, 0, 2) != '()') {
            $file .= '_';
        }
        $file = str_replace('convert', '', $file);
        return $file .= '.html';
    }
    
    function title()
    {
        return (($this->type == 'Method') ? $this->category . '::' : '') . $this->name;
    }
    
    function index()
    {
        return (strtolower($this->type) != 'method' && strtolower($this->type) != 'field');
    }
	
	function display()
	{
        global $lang;
        $html = '<table cellpadding="0" cellspacing="0" border="0" class="ref-item">';
        
        if ($this->type == 'Method') {
            $html .= refTableRow('<!--*-->Class<!--*-->', '<p>'.$this->category.'</p>');
        }
        
		$html .= refTableRow('<!--*-->Name<!--*-->', '<h3>'.$this->name.'</h3>', 'name-row');
        
        if(!empty($this->hasCode)) {  // Change for 149!
          $examples = '';
		  $count = 0;
          foreach ($this->examples as $ex) {
        	  //echo $ex[code];
        	  //echo 'BBBBBBBBRERRRRRRRREEEEEEEEEAAAAAAAAAKKKKKKKKK';
        	  //$ex[code] = codeExampleConvert($ex[code]); // Adding this line to try to fix problems with match() and matchAll()
        	  echo $ex[code];
              $examples .= '<div class="example">';
              $path = ($lang != 'en' ? '../media' : 'media');
              $examples .= !empty($ex['image']) ? "<img src=\"$path/$ex[image]\" alt=\"example pic\" />" : '';
              $examples .= !empty($ex['image']) ? "<pre class=\"margin\">$ex[code]</pre>" : "<pre>$ex[code]</pre>";
              $examples .= '</div>';
			  if (count($this->examples) != ++$count && empty($ex['image'])) {
			  	$examples .= '<hr class="noShade" noshade="noshade" size="1" />';
		  	  }
          }
          $html .= refTableRow('<!--*-->Examples<!--*-->', $examples);
          }
        
        $html .= refTableRow('<!--*-->Description<!--*-->', $this->description);
        
        if (!empty($this->syntax)) {
            $html .= refTableRow('<!--*-->Syntax<!--*-->', '<pre>'.$this->syntax.'</pre>');
        }

        if (!empty($this->fields)) {
            $fields = '<table cellpadding="0" cellspacing="0" border="0">';
            foreach ($this->fields as $f) {
                $fields .= refTableRow(
                    "<a href=\"{$this->name}_" . convertToFilename($f['fname']) . "\">$f[fname]</a>", 
                    $f['fdescription']
                );
            }
            $fields .= '</table>';
            $html .= refTableRow('<!--*-->Fields<!--*-->', $fields);
        }
        
        if (!empty($this->methods)) {
            $methods = '<table cellpadding="0" cellspacing="0" border="0">';
            foreach ($this->methods as $m) {
                $methods .= refTableRow(
                    "<a href=\"{$this->name}_" . convertToFilename($m['mname']) . "\">$m[mname]</a>",
                    $m['mdescription']
                );
            }
            $methods .= '</table>';
            $html .= refTableRow('<!--*-->Methods<!--*-->', $methods);
        }
        
        if (!empty($this->constructor)) {
            $html .= refTableRow('<!--*-->Constructor<!--*-->', '<pre>'.$this->constructor.'</pre>');
        }
        
        if (!empty($this->hasParameter)) {  // Change for 149!
            $parameters = '<table cellpadding="0" cellspacing="0" border="0">';
            foreach ($this->parameters as $p) {
                $parameters .= refTableRow($p['label'], $p['description']);
            }
            $parameters .= '</table>';
            $html .= refTableRow('<!--*-->Parameters<!--*-->', $parameters);
        }
        
        if (!empty($this->cparameters)) {
            $cparameters = '<table cellpadding="0" cellspacing="0" border="0">';
            foreach ($this->cparameters as $c) {
                $cparameters .= refTableRow($c['clabel'], $c['cdescription']);
            }
            $cparameters .= '</table>';
            $html .= refTableRow('<!--*-->Parameters<!--*-->', $cparameters);
        }
        
        if (!empty($this->returns)) {
            $html .= refTableRow('<!--*-->Returns<!--*-->', $this->returns);
        }
        
        $html .= refTableRow('<!--*-->Usage<!--*-->', $this->usage);
        
        if (!empty($this->related)) {
            $related = '';
            foreach ($this->related as $r) {
                $related .= '<a href="' . convertToFilename($r) . '">' . $r . '</a><br />';
            }
            $html .= refTableRow('<!--*-->Related<!--*-->', $related);
        }
        $html .= '</table>';
        return $html;
	}
}

function refTableRow($label, $data, $class = '')
{
    $html  = "\n\t<tr class=\"$class\">\n";
    $html .= "\t\t<th scope=\"row\">$label</th>\n";
    $html .= "\t\t<td>$data</td>\n";
    $html .= "\t</tr>\n";
    return $html;
}
?>