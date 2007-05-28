<? 

require('../config.php');
require('lib/Example.class.php');
$benchmark_start = microtime_float();


$categories = get_examples_list('examples.xml');
$break_after = array('Control', 'Transform');
$subdir = 'Basics';
$dir = CONTENTDIR.'examples/'.$subdir.'/';

$count = 0;
foreach ($categories as $cat => $array) {
	if ($dp = opendir($dir.$cat)) {
		while ($fp = readdir($dp)) {
			if (substr($fp, 0, 1) != '.') {
				$ex = new Example($fp, $subdir."/".$cat, $subdir);
				//$ex = new Example($fp, $cat);
				$ex->output_file($categories);
				$count++;
			}
		}
	}
}

$page = new Page('Learning', 'Basics');
$page->subtemplate('template.examples.html');

$html = "<div class=\"ref-col\">\n";
foreach ($categories as $cat => $array) {
	
	#$html .= "<h3><img src=\"images/".strtolower(removesymbols($cat)).".gif\" alt=\"$cat\" /></h3>\n<p>";
	$html .= "<p><br /><b>$cat</b><br /><br />";
	foreach ($array as $file => $name) {
	    $thisfile = strtolower($file);
		$html .= "\t<a href=\"$thisfile\">$name</a><br />\n";
	}
	echo '</p>';
	
	if (in_array($cat, $break_after)) {
		$html .= "</div><div class=\"ref-col\">";
	}
}
$html .= "</div>";

$page->content($html);
writeFile('learning/'.strtolower($subdir).'/index.html', $page->out());


# --------------------------------- 3D


$categories = get_examples_list('examples_3D.xml');
$break_after = array('Image', 'Textures');
$subdir = '3D';
$subdir3D = '3D and OpenGL';
$dir = CONTENTDIR.'examples/'.$subdir3D.'/';

$count = 0;
foreach ($categories as $cat => $array) {
	if ($dp = opendir($dir.$cat)) {
		while ($fp = readdir($dp)) {
			if (substr($fp, 0, 1) != '.') {
				$ex = new Example($fp, $subdir3D."/".$cat, $subdir);
				//$ex = new Example($fp, $cat);
				$ex->output_file($categories);
				$count++;
			}
		}
	}
}

$page = new Page('Learning', '3D & OpenGL');
$page->subtemplate('template.examples.html');

$html = "<div class=\"ref-col\">\n";
foreach ($categories as $cat => $array) {
	
	#$html .= "<h3><img src=\"images/".strtolower(removesymbols($cat)).".gif\" alt=\"$cat\" /></h3>\n<p>";
	$html .= "<p><br /><b>$cat</b><br /><br />";
	foreach ($array as $file => $name) {
	    $thisfile = strtolower($file);
		$html .= "\t<a href=\"$thisfile\">$name</a><br />\n";
	}
	echo '</p>';
	
	if (in_array($cat, $break_after)) {
		$html .= "</div><div class=\"ref-col\">";
	}
}
$html .= "</div>";

$page->content($html);
writeFile('learning/'.strtolower($subdir).'/index.html', $page->out());


# --------------------------------- Topics


$categories = get_examples_list('examples_topics.xml');
$break_after = array('Fractals', 'Interaction');
$subdir = 'Topics';
$dir = CONTENTDIR.'examples/'.$subdir.'/';

$count = 0;
foreach ($categories as $cat => $array) {
	if ($dp = opendir($dir.$cat)) {
		while ($fp = readdir($dp)) {
			if (substr($fp, 0, 1) != '.') {
				$ex = new Example($fp, $subdir."/".$cat, $subdir);
				//$ex = new Example($fp, $cat);
				$ex->output_file($categories);
				$count++;
			}
		}
	}
}

$page = new Page('Learning', 'Topics');
$page->subtemplate('template.examples.html');

$html = "<div class=\"ref-col\">\n";
foreach ($categories as $cat => $array) {
	
	#$html .= "<h3><img src=\"images/".strtolower(removesymbols($cat)).".gif\" alt=\"$cat\" /></h3>\n<p>";
	$html .= "<p><br /><b>$cat</b><br /><br />";
	foreach ($array as $file => $name) {
	    $thisfile = strtolower($file);
		$html .= "\t<a href=\"$thisfile\">$name</a><br />\n";
	}
	echo '</p>';
	
	if (in_array($cat, $break_after)) {
		$html .= "</div><div class=\"ref-col\">";
	}
}
$html .= "</div>";

$page->content($html);
writeFile('learning/'.strtolower($subdir).'/index.html', $page->out());



# --------------------------------- LIBRARIES


$categories = get_examples_list('examples_libraries.xml');
$break_after = array('Video (Movie)', 'Network');
$subdir = 'Libraries';
$dir = CONTENTDIR.'examples/'.$subdir.'/';

$count = 0;
foreach ($categories as $cat => $array) {
	if ($dp = opendir($dir.$cat)) {
		while ($fp = readdir($dp)) {
			if (substr($fp, 0, 1) != '.') {
				$ex = new Example($fp, $subdir."/".$cat, $subdir);
				//$ex = new Example($fp, $cat);
				$ex->output_file($categories);
				$count++;
			}
		}
	}
}

$page = new Page('Learning', 'Library Examples');
$page->subtemplate('template.examples.html');

$html = "<div class=\"ref-col\">\n";
foreach ($categories as $cat => $array) {
	
	#$html .= "<h3><img src=\"images/".strtolower(removesymbols($cat)).".gif\" alt=\"$cat\" /></h3>\n<p>";
	$html .= "<p><br /><b>$cat</b><br /><br />";
	foreach ($array as $file => $name) {
	    $thisfile = strtolower($file);
		$html .= "\t<a href=\"$thisfile\">$name</a><br />\n";
	}
	echo '</p>';
	
	if (in_array($cat, $break_after)) {
		$html .= "</div><div class=\"ref-col\">";
	}
}
$html .= "</div>";

$page->content($html);
writeFile('learning/'.strtolower($subdir).'/index.html', $page->out());




$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Examples pages generation Successful</h2>
<p>Generated <?= $count+1 ?> files in <?=$execution_time?> seconds.</p>

<?

function get_examples_list($exstr)
{
	$xml = openXML($exstr);
	$my_cats = array();
	foreach ($xml->childNodes as $c) {
	    $name = htmlspecialchars($c->getAttribute('label'));
    
	    if ($c->childCount > 0) {
	        foreach ($c->childNodes as $s) {
	            if ($s->nodeType == 1) {
	                $my_cats[$name][$s->getAttribute('file')] = trim($s->firstChild->nodeValue);
	            }
	        }
	    }
	}
	return $my_cats;
}

function removesymbols($str)
{
	return preg_replace("/\W/", "", $str);
}

?>