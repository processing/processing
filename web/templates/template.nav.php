<?

$pages = array(

    'Cover'         => array('/', 0),

    'Exhibition'    => array('/exhibition/', 1), 
    
    'Learning'      => array('/learning/', 1), 
    	'Tutorials'    => array('/learning/', 2),  
    	'Basics'	=> array('/learning/basics/', 2),  
		'Topics'	=> array('/learning/topics/', 2), 
		'3D'	=> array('/learning/3d/', 2),  
		'Library'	=> array('/learning/libraries/', 2),  
    	'Books'	     => array('/learning/books/', 2),
    		
    'Reference'     => array('/reference/', 1),
    	'Language'      => array('/reference/', 2),
    	'A-Z'	=> array('/reference/alpha.html', 2),
    	'Libraries'     	=> array('/reference/libraries/', 2),
    	'Tools'   	=> array('/reference/tools/', 2),
    	'Environment'   	=> array('/reference/environment/', 2), 
        
    'Download'      => array('/download/', 1),
    
    'Discourse'     => array('/discourse/', 1),
    
    'Contribute'    => array('/contribute/', 1),
    
    'About'    => array('/about/', 1),
    	'Overview'        => array('/about/', 2),
    	'People'        => array('/about/people/', 2),
    	'Patrons'        => array('/about/patrons/', 2),
    
    'FAQ'           => array('/faq.html', 1),
    
    );


function navigation($section = '')
{  
    global $lang;
    global $translation;
    $tr = $translation->navigation;

	$abo = array('About', 'Overview', 'People', 'Patrons');
    $ref = array('Reference', 'Language', 'A-Z', 'Libraries', 'Tools', 'Environment');
    $learn = array('Learning', 'Tutorials', 'Basics', 'Topics', '3D', 'Library', 'Books');

    $html = "\t\t\t".'<div id="navigation">'."\n";

    $id = (in_array($section, $ref) || in_array($section, $learn) || 
    	   in_array($section, $abo)) ? 'mainnav' : 'mainnav_noSub';   
    	    
    $html .= "\t\t\t\t".'<div class="navBar" id="'.$id.'">'."\n";
    
    $html .= "\t\t\t\t\t" . l('Cover', $section == 'Cover') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Exhibition', $section == 'Exhibition') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Reference', in_array($section, $ref)) . " \\\n";
    $html .= "\t\t\t\t\t" . l('Learning', in_array($section, $learn)) . " \\\n";
    $html .= "\t\t\t\t\t" . l('Download', $section == 'Download') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Discourse', $section == 'Discourse') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Contribute', $section == 'Contribute') . " \\\n";
    $html .= "\t\t\t\t\t" . l('About', in_array($section, $abo)) . " \n";
       
    $html .= "\t\t\t\t\t" . "<a href=\"http://wiki.processing.org/w/FAQ\"" . ($section == 'FAQ' ? ' class="active faq"' : 'class="faq"') . ">FAQ</a>\n";
       
    $html .= "\t\t\t\t</div>\n";
    
    if (in_array($section, $abo)) {
         $html .= "\t\t\t\t" . '<div class="navBar abo" id="subNav">' . "\n";
	
         $html .= "\t\t\t\t\t" . l('Overview', $section == 'Overview') . " \\\n";
         $html .= "\t\t\t\t\t" . l('People', $section == 'People') . " \\\n";
	 	 $html .= "\t\t\t\t\t" . l('Patrons', $section == 'Patrons') . " \n";
	 	 $html .= "\t\t\t\t</div>\n";      
   
     } else if (in_array($section, $ref)) {
        $html .= "\t\t\t\t" . '<div class="navBar" id="subNav">' . "\n";
    
        if ($lang == 'en') {
          $html .= "\t\t\t\t\t" . l('Language', $section == 'Language') . " (";
          $html .= l('A-Z', $section == 'A-Z') . ") \\\n";
          $html .= "\t\t\t\t\t" . l('Libraries', $section == 'Libraries') . " \\\n";
          $html .= "\t\t\t\t\t" . l('Tools', $section == 'Tools') . " \\\n";
          $html .= "\t\t\t\t\t" . l('Environment', $section == 'Environment') . " \n";
	    }
    
        $html .= "\t\t\t\t</div>\n";
		
    } else if (in_array($section, $learn)) {
        $html .= "\t\t\t\t" . '<div class="navBar learning" id="subNav">' . "\n";
		
		$html .= "\t\t\t\t\t" . l('Tutorials', $section == 'Tutorials') . " \\\n Examples: ";
		$html .= "\t\t\t\t\t" . l('Basics', $section == 'Basics') . ", \n";
		$html .= "\t\t\t\t\t" . l('Topics', $section == 'Topics') . ", \n";
		$html .= "\t\t\t\t\t" . l('3D', $section == '3D') . ",  \n";
		$html .= "\t\t\t\t\t" . l('Library', $section == 'Library') . " \\\n";
		$html .= "\t\t\t\t\t" . l('Books', $section == 'Books') . " \\\n";
        $html .= "\t\t\t\t</div>\n";
    }

    return $html . "\t\t\t</div>\n";
}

function l($s, $c)
{
    global $pages;
    return "<a href=\"{$pages[$s][0]}\"" . ($c ? ' class="active"' : '') . ">$s</a>";
}

function short_nav($section)
{
    $html  = "\t\t\t".'<div id="navigation">'."\n";
    $html .= "\t\t\t\t".'<div class="navBar" id="mainnav_noSub">'."\n";
    
    $html .= "\t\t\t\t\t<a href=\"http://processing.org/\"" . ($section == 'Cover' ? ' class="active"' : '') . ">Cover</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/index.html\"" . ($section == 'Language' ? ' class="active"' : '') . ">Language</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/libraries/index.html\"" . ($section == 'Libraries' ? ' class="active"' : '') . ">Libraries</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/tools/index.html\"" . ($section == 'Tools' ? ' class="active"' : '') . ">Tools</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/environment/index.html\"" . ($section == 'Environment' ? ' class="active"' : '') . ">Environment</a>\n";
	   
    $html .= "\t\t\t\t</div>\n";
    $html .= "\t\t\t</div>\n";
    
    return $html;
}

function local_nav($section, $rel_path='')
{
    $html  = "\t\t\t".'<div id="navigation">'."\n";
    $html .= "\t\t\t\t".'<div class="navBar" id="mainnav_noSub">'."\n";

    $html .= "\t\t\t\t\t<a href=\"{$rel_path}index.html\"" . ($section == 'Language' ? ' class="active"' : '') . ">Language</a> (";
    $html .= "<a href=\"{$rel_path}alpha.html\"" . ($section == 'A-Z' ? ' class="active"' : '') . ">A-Z</a>) \\ \n";
    $html .= "\t\t\t\t\t<a href=\"{$rel_path}libraries/index.html\"" . ($section == 'Libraries' ? ' class="active"' : '') . ">Libraries</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"{$rel_path}tools/index.html\"" . ($section == 'Tools' ? ' class="active"' : '') . ">Tools</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"{$rel_path}environment/index.html\"" . ($section == 'Environment' ? ' class="active"' : '') . ">Environment</a>\n";
    
    $html .= "\t\t\t\t</div>\n";
    $html .= "\t\t\t</div>\n";
    
    return $html;	
}

function reference_nav($current = '')
{
    global $lang;
    global $translation;
    global $LANGUAGES;
    $tr = $translation->navigation;
    
    $html = "<a href=\"index.html\">$tr[abridged]</a>";
    if ($LANGUAGES[$lang][2]) {
        $html .= " (<a href=\"index_alpha.html\">$tr[az]</a>)";
    }
    $html .= " \ <a href=\"index_ext.html\">$tr[complete]</a>";
    if ($LANGUAGES[$lang][2]) {
        $html .= " (<a href=\"index_alpha_ext.html\">$tr[az]</a>)";
    }
	$html .= " \ <a href=\"changes.html\">$tr[changes]</a>";
    return $html;
}

function language_nav($current)
{
    global $LANGUAGES;
    global $FINISHED;
    if (count($FINISHED) < 2) { return ''; }
    
    $html = "\t".'Language: <select name="nav" size="1" class="refnav" onChange="javascript:gogo(this)">'."\n";
    foreach ($FINISHED as $code) {
        if ($LANGUAGES[$code][3] != '' ) {
            $sel = ($current == $code) ? ' selected="selected"' : '';
            $html .= "\t\t<option value=\"{$LANGUAGES[$code][3]}\"$sel>{$LANGUAGES[$code][0]}</option>\n";
        }
    }
    $html .= "\t</select>\n";
    return $html;
}

function library_nav($current=null)
{
	$html = "\n\t<span class=\"lib-nav\">\n";
	$html .= "\t\t<a href=\"../index.html\">Libraries</a>\n";
	if ($current) {
		$html .= "\t\t \ <a href=\"index.html\">".ucfirst($current)."</a>\n";
	}
	$html .= "\t</span>\n";
	return $html;
}

function examples_nav($current) {
	// $html = "\n\t<div id=\"examples-nav\">\n";
}
?>
