<?

$pages = array(
    'Cover'         => array('/', 0),
    'Exhibition'    => array('/exhibition/index.html', 1),
    'Learning'      => array('/learning/index.html', 1),
    'Examples'      => array('/learning/examples/index.html', 2),
    'Tutorials'     => array('/learning/tutorials/index.html', 2),
    'Reference'     => array('/reference/index.html', 1),
    'Download'      => array('/download/index.html', 1),
    'Discourse'     => array('/discourse/index.html', 1),
    'Contribute'    => array('/contribute/index.html', 1),
    'FAQ'           => array('/faq/index.html', 1),
    'Language'      => array('/reference/index.html', 1),
    'Environment'   => array('/reference/environment/index.html', 2),
    'Libraries'     => array('/reference/libraries/index.html', 2),
    'Comparison'    => array('/reference/compare/index.html', 2)
    
    );

function navigation($section = '')
{  
    $ref = array('Reference', 'Language', 'Environment', 'Libraries', 'Comparison');
    $learn = array('Learning', 'Examples', 'Tutorials');
    
    $html = "\t\t\t".'<div id="navigation">'."\n";

    $id = (in_array($section, $ref) || in_array($section, $learn)) ? 'mainnav' : 'mainnav_noSub';    
    $html .= "\t\t\t\t".'<div class="navBar" id="'.$id.'">'."\n";
    
    $html .= "\t\t\t\t\t" . l('Cover', $section == 'Cover') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Exhibition', $section == 'Exhibition') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Learning', in_array($section, $learn)) . " \\\n";
    $html .= "\t\t\t\t\t" . l('Reference', in_array($section, $ref)) . " \\\n";
    $html .= "\t\t\t\t\t" . l('Download', $section == 'Download') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Discourse', $section == 'Discourse') . " \\\n";
    $html .= "\t\t\t\t\t" . l('Contribute', $section == 'Contribute') . "\n";
    $html .= "\t\t\t\t\t" . "<a href=\"/faq/index.html\"" . ($section == 'FAQ' ? ' class="active faq"' : 'class="faq"') . ">FAQ</a>\n";
       
    $html .= "\t\t\t\t</div>\n";
    
    if (in_array($section, $ref)) {
        $html .= "\t\t\t\t" . '<div class="navBar" id="subNav">' . "\n";
        
        $html .= "\t\t\t\t\t" . l('Language', $section == 'Language') . " \\\n";
        $html .= "\t\t\t\t\t" . l('Libraries', $section == 'Libraries') . " \\\n";
        $html .= "\t\t\t\t\t" . l('Environment', $section == 'Environment') . " \\\n";
        $html .= "\t\t\t\t\t" . l('Comparison', $section == 'Comparison') . "\n";
        
        $html .= "\t\t\t\t</div>\n";
    } else if (in_array($section, $learn)) {
        $html .= "\t\t\t\t" . '<div class="navBar learning" id="subNav">' . "\n";
        
        $html .= "\t\t\t\t\t" . l('Examples', $section == 'Examples') . " \\\n";
        $html .= "\t\t\t\t\t" . l('Tutorials', $section == 'Tutorials') . "\n";
        
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
    $html .= "\t\t\t\t\t<a href=\"/reference/index.html\"" . ($section == 'Language' ? ' class="active"' : '') . ">Langauge</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/libraries/index.html\"" . ($section == 'Libraries' ? ' class="active"' : '') . ">Libraries</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/environment/index.html\"" . ($section == 'Environment' ? ' class="active"' : '') . ">Environment</a> \\ \n";
    $html .= "\t\t\t\t\t<a href=\"/reference/compare/index.html\"" . ($section == 'Comparison' ? 'class="active"' : '') . ">Comparison</a>\n";
    
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
    $html .= "\t\t\t\t\t<a href=\"/reference/$lang/compare/index.html\"" . ($section == 'Comparison' ? 'class="active"' : '') . ">$tr[comparison]</a>\n";
    
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
    return $html;
}

function language_nav($current)
{
    global $LANGUAGES;
    if (count($LANGUAGES) < 2) { return ''; }
    
    $html = "\t".'Language: <select name="nav" size="1" class="refnav" onChange="javascript:gogo(this)">'."\n";
    foreach ($LANGUAGES as $short => $array) {
        if ($array[3] != '' ) {
            $sel = ($current == $short) ? ' selected="selected"' : '';
            $html .= "\t\t<option value=\"$array[3]\"$sel>$array[0]</option>\n";
        }
    }
    $html .= "\t</select>\n";
    return $html;
}

function library_nav($libraries, $current)
{
    $html = "\n\t<span class=\"lib-nav\">\n";
    $html .= "\t\t<a href=\"index.html\">$current</a>\n";
    $html .= "\t</span>\n";
    return $html;
}


/** test **
?>
<style>

#mainnav { background: #cfc; }
#mainnav_noSub { background: #fcc; }
#mainnav, #mainnav_noSub { width: 600px; position: relative;}
#subNav { margin-left: 190px; }
.active { font-weight: bold; }
.faq { position: absolute; right: 10px; }

</style>
<?
foreach ($navigation_pages as $page => $array) {
    echo navigation($page);
}
**/
?>
