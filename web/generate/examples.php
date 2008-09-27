<? 

require('../config.php');
require('lib/Example.class.php');
$benchmark_start = microtime_float();


// update the files on the server via SVN

// look for the .subversion folder somewhere else
// otherwise will go looking for /home/root/.subversion or some other user
$where = CONTENTDIR . 'static/examples';
putenv('HOME=' . CONTENTDIR);

// do the initial checkout
//`cd /var/www/processing && /usr/local/bin/svn co svn://processing.org/trunk/web/content/`;

`cd $where && /usr/local/bin/svn update`;


// Make the intro page
$source = CONTENTDIR."static/";
$page = new Page("Learning", "Learning");
$page->content(file_get_contents($source."learning.html"));
writeFile('learning/index.html', $page->out());

// Make the Books page
$page = new Page("Books", "Books");
$page->content(file_get_contents($source."books.html"));
writeFile('learning/books/index.html', $page->out());

// Make the Getting Started
$page = new Page("Getting Started", "Getting Started");
$page->content(file_get_contents($source."gettingstarted.html"));
writeFile('learning/gettingstarted/index.html', $page->out());

// Disabled by REAS 10 Sept 2008
// Make the hacks page
//$page = new Page("Hacks", "Hacks");
//$page->content(file_get_contents($source."hacks.html"));
//writeFile('learning/hacks/index.html', $page->out());


# --------------------------------- Basics

$categories = get_examples_list('examples.xml');
$break_after = array('Control', 'Color');
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

$page = new Page('Examples', 'Examples');
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
$break_after = array('Transform', 'Lights');
$subdir = '3D';
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

$page = new Page('Examples', 'Examples');
$page->subtemplate('template.examples-3d.html');

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
$break_after = array('File IO', 'Effects');
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

$page = new Page('Examples', 'Examples');
$page->subtemplate('template.examples-topics.html');

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
$break_after = array('Network', 'Candy (SVG Import)');
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

$page = new Page('Examples', 'Examples');
$page->subtemplate('template.examples-libraries.html');

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