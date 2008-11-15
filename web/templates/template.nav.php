<?

/**
//require('../generate/exhibition.php');

//require(GENERATEDIR.'lib/Curated.class.php');
//require(GENERATEDIR.'lib/Network.class.php');

$curated = curated_xml2('all');
$network = network_xml2('all');
// Count number of items
$ctotal = count($curated);
$ntotal = count($network);
// Count number of pages needed
$cnum_pages = ceil($ctotal / CURATED_PER_PAGE);
$nnum_pages = ceil($ntotal / NETWORK_PER_PAGE);

$curatedPages = '/exhibition/curated_page_' . $cnum_pages . '.html';
$networkPages = '/exhibition/network_page_' . $nnum_pages . '.html';

function curated_xml2($num)
{
    // open and parse curated.xml
    $xml =& openXML('curated.xml');
    
    // get software nodes
    $softwares = $xml->getElementsByTagName('software');
    $softwares = $softwares->toArray();
    
    // create curated objects
    //$i = 1;
    //foreach ($softwares as $software) {
    //    $curated[] = new Curated($software);
    //    if ($i >= $num && $num != 'all') { break; }
    //    $i++;
    //}
       
    //return $curated;
    return $softwares;
}

function network_xml2($num)
{
    // open and parse network.xml
    $xml =& openXML('network.xml');
    
    // get software nodes
    $softwares = $xml->getElementsByTagName('software');
    $softwares = $softwares->toArray();
    
    // create network objects
    //$i = 1;
    //foreach ($softwares as $software) {
    //    $network[] = new Network($software);
    //    if ($i >= $num && $num != 'all') { break; }
    //    $i++;
    //}
    
    //return $network;
    return $softwares;   
}

*/

$pages = array(
    'Cover'         => array('/', 0),

    'Exhibition'    => array('/exhibition/index.html', 1),
    'Index'        => array('/exhibition/index.html', 2),
    #'Collection'    => array($curatedPages, 2),
    'Collection'    => array('/exhibition/curated_page_new.html', 2),
    #'Network Links'    => array($networkPages, 2),
    'Network Links'    => array('/exhibition/network_page_new.html', 2),
    'Features'    => array('/exhibition/features/', 2),
    
    'Learning'      => array('/learning/index.html', 1),
    #'Examples'     => array('/learning/index.html', 2),
    #'Tutorials'    => array('/learning/tutorials/index.html', 2),
    'Overview'	    => array('/learning/index.html', 2),
    'Getting Started'	    => array('/learning/gettingstarted/index.html', 2),
    'Examples'	=> array('/learning/basics/index.html', 2),
    'Tutorials'    => array('/learning/tutorials/index.html', 2),
    #'Basics'        => array('/learning/basics/index.html', 2),
    #'Topics'        => array('/learning/topics/index.html', 2),
    #'3D & OpenGL'   => array('/learning/3d/index.html', 2),
    #'Library Examples'	=> array('/learning/libraries/index.html', 2),
    'Books'	     => array('/learning/books/index.html', 2),
    #'Hacks'          => array('/learning/hacks/index.html', 2),
	
    'Reference'     => array('/reference/index.html', 1),
    'Download'      => array('/download/index.html', 1),
    'Discourse'     => array('/discourse/index.html', 1),
    'Hacks'    => array('/hacks/', 1),
    'Contribute'    => array('/contribute/index.html', 1),
    'FAQ'           => array('/faq.html', 1),

    'Language'      => array('/reference/index.html', 1),
    'Environment'   	=> array('/reference/environment/index.html', 2),
    'Libraries'     	=> array('/reference/libraries/index.html', 2),
    'Compare'    	=> array('/reference/compare/index.html', 2),
    'Troubleshooting'	=> array('/reference/troubleshooting/index.html', 2)
    
    );

function navigation($section = '')
{  
    global $lang;
    global $translation;
    $tr = $translation->navigation;

    $ref = array('Reference', 'Language', 'Environment', 'Libraries', 'Compare', 'Troubleshooting');
    #$learn = array('Learning', 'Examples', 'Tutorials');
    #$learn = array('Learning', 'Overview', 'Getting Started', 'Basics', 'Topics', '3D & OpenGL', 'Library Examples', 'Books', 'Hacks');
    $learn = array('Learning', 'Overview', 'Getting Started', 'Examples', 'Tutorials', 'Books');
    $exhib = array('Exhibition', 'Index', 'Collection', 'Network Links', 'Features');    
    #$exhib = array('Exhibition', 'Index', 'Collection', 'Network Links');    

    $html = "\t\t\t".'<div id="navigation">'."\n";

    $id = (in_array($section, $ref) || in_array($section, $learn) || in_array($section, $exhib)) ? 'mainnav' : 'mainnav_noSub';    
    $html .= "\t\t\t\t".'<div class="navBar" id="'.$id.'">'."\n";
    
    $html .= "\t\t\t\t\t" . l('Cover', $section == 'Cover') . " \\\n";
    #$html .= "\t\t\t\t\t" . l('Exhibition', $section == 'Exhibition') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Exhibition', in_array($section, $exhib)) . " \\\n";
    $html .= "\t\t\t\t\t" . l('Learning', in_array($section, $learn)) . " \\\n";
    $html .= "\t\t\t\t\t" . l('Reference', in_array($section, $ref)) . " \\\n";
    $html .= "\t\t\t\t\t" . l('Download', $section == 'Download') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Discourse', $section == 'Discourse') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Hacks', $section == 'Hacks') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Contribute', $section == 'Contribute') . "\n";
    $html .= "\t\t\t\t\t" . "<a href=\"/faq.html\"" . ($section == 'FAQ' ? ' class="active faq"' : 'class="faq"') . ">FAQ</a>\n";
       
    $html .= "\t\t\t\t</div>\n";

    if (in_array($section, $exhib)) {
         $html .= "\t\t\t\t" . '<div class="navBar exhib" id="subNav">' . "\n";
	
         $html .= "\t\t\t\t\t" . l('Index', $section == 'Index') . " \\\n";
         $html .= "\t\t\t\t\t" . l('Collection', $section == 'Collection') . " \\\n";
	 	 $html .= "\t\t\t\t\t" . l('Network Links', $section == 'Network Links') . " \n";
	 	 $html .= "\t\t\t\t\t" . l('Features', $section == 'Features') . " \n";
	 	 $html .= "\t\t\t\t</div>\n";    
   
     } else if (in_array($section, $ref)) {
        $html .= "\t\t\t\t" . '<div class="navBar" id="subNav">' . "\n";
    
        if ($lang == 'en') {

          $html .= "\t\t\t\t\t" . l('Language', $section == 'Language') . " \\\n";
          $html .= "\t\t\t\t\t" . l('Libraries', $section == 'Libraries') . " \\\n";
          $html .= "\t\t\t\t\t" . l('Environment', $section == 'Environment') . " \\\n";
	  $html .= "\t\t\t\t\t" . l('Compare', $section == 'Compare') . " \\\n";
          $html .= "\t\t\t\t\t" . l('Troubleshooting', $section == 'Troubleshooting') . "\n";
        
	} else {

          $html .= "\t\t\t\t\t<a href=\"/reference/$lang/index.html\"" . ($section == 'Language' ? ' class="active"' : '') . ">$tr[language]</a> \\ \n";
          $html .= "\t\t\t\t\t<a href=\"/reference/$lang/libraries/index.html\"" . ($section == 'Libraries' ? ' class="active"' : '') . ">$tr[libraries]</a> \\ \n";
          $html .= "\t\t\t\t\t<a href=\"/reference/$lang/environment/index.html\"" . ($section == 'Environment' ? ' class="active"' : '') . ">$tr[environment]</a> \\ \n";
          $html .= "\t\t\t\t\t<a href=\"/reference/$lang/compare/index.html\"" . ($section == 'Compare' ? 'class="active"' : '') . ">$tr[comparison]</a> \\ \n";
          $html .= "\t\t\t\t\t<a href=\"/reference/$lang/troubleshooting/index.html\"" . ($section == 'Troubleshooting' ? 'class="active"' : '') . ">$tr[troubleshooting]</a>\n";

	}
    
        $html .= "\t\t\t\t</div>\n";
		
    } else if (in_array($section, $learn)) {
        $html .= "\t\t\t\t" . '<div class="navBar learning" id="subNav">' . "\n";
		
        $html .= "\t\t\t\t\t" . l('Overview', $section == 'Overview') . " \\\n";
        $html .= "\t\t\t\t\t" . l('Getting Started', $section == 'Getting Started') . " \\\n";
		#$html .= "\t\t\t\t\t" . l('Basics', $section == 'Basics') . " \\\n";
		#$html .= "\t\t\t\t\t" . l('Topics', $section == 'Topics') . " \\\n";
		#$html .= "\t\t\t\t\t" . l('3D & OpenGL', $section == '3D & OpenGL') . " \\\n";
		#$html .= "\t\t\t\t\t" . l('Library Examples', $section == 'Library Examples') . " \\\n";
		$html .= "\t\t\t\t\t" . l('Examples', $section == 'Examples') . " \\\n";
		$html .= "\t\t\t\t\t" . l('Tutorials', $section == 'Tutorials') . " \\\n";
		$html .= "\t\t\t\t\t" . l('Books', $section == 'Books') . " \n";
		//$html .= "\t\t\t\t\t" . l('Hacks', $section == 'Hacks') . " \n";
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
    $html .= "\t\t\t\t\t<a href=\"/reference/environment/index.html\"" . ($section == 'Environment' ? ' class="active"' : '') . ">Environment</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/compare/index.html\"" . ($section == 'Compare' ? 'class="active"' : '') . ">Compare</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/troubleshooting/index.html\"" . ($section == 'Troubleshooting' ? 'class="active"' : '') . ">Troubleshooting</a>\n";
	   
    $html .= "\t\t\t\t</div>\n";
    $html .= "\t\t\t</div>\n";
    
    return $html;
}

function local_nav($section, $rel_path='')
{
    $html  = "\t\t\t".'<div id="navigation">'."\n";
    $html .= "\t\t\t\t".'<div class="navBar" id="mainnav_noSub">'."\n";

    $html .= "\t\t\t\t\t<a href=\"{$rel_path}index.html\"" . ($section == 'Language' ? ' class="active"' : '') . ">Language</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"{$rel_path}libraries/index.html\"" . ($section == 'Libraries' ? ' class="active"' : '') . ">Libraries</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"{$rel_path}environment/index.html\"" . ($section == 'Environment' ? ' class="active"' : '') . ">Environment</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"{$rel_path}compare/index.html\"" . ($section == 'Compare' ? 'class="active"' : '') . ">Compare</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"{$rel_path}troubleshooting/index.html\"" . ($section == 'Troubleshooting' ? 'class="active"' : '') . ">Troubleshooting</a>\n";
    #$html .= "\t\t\t\t\t<a href=\"{$rel_path}faq.html\"" . ($section == 'FAQ' ? ' class="active faq"' : 'class="faq"') . ">FAQ</a>\n";
    
    $html .= "\t\t\t\t</div>\n";
    $html .= "\t\t\t</div>\n";
    
    return $html;	
}

function navigation_tr($section)
{
    global $lang;
    global $translation;
    $tr = $translation->navigation;
    
    $html  = "\t\t\t".'<div id="navigation">'."\n";
    $html .= "\t\t\t\t".'<div class="navBar" id="mainnav_noSub">'."\n";
    
    $html .= "\t\t\t\t\t<a href=\"/\"" . ($section == 'Cover' ? ' class="active"' : '') . ">$tr[cover]</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/$lang/index.html\"" . ($section == 'Language' ? ' class="active"' : '') . ">$tr[language]</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/$lang/libraries/index.html\"" . ($section == 'Libraries' ? ' class="active"' : '') . ">$tr[libraries]</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/$lang/environment/index.html\"" . ($section == 'Environment' ? ' class="active"' : '') . ">$tr[environment]</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/$lang/compare/index.html\"" . ($section == 'Compare' ? 'class="active"' : '') . ">$tr[comparison]</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/$lang/troubleshooting/index.html\"" . ($section == 'Troubleshooting' ? 'class="active"' : '') . ">$tr[troubleshooting]</a>\n";
   
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
