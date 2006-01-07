<? 

require('../config.php');
require('lib/Example.class.php');
$benchmark_start = microtime_float();

$categories = get_examples_list();
$break_after = array('Typography', 'Simulate');

$dir = CONTENTDIR.'examples/';

$count = 0;
foreach ($categories as $cat => $array) {
	if ($dp = opendir($dir.$cat)) {
		while ($fp = readdir($dp)) {
			if (substr($fp, 0, 1) != '.') {
				$ex = new Example($fp, $cat);
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
	
	$html .= "<h3><img src=\"images/".strtolower(removesymbols($cat)).".gif\" alt=\"$cat\" /></h3>\n<p>";
	foreach ($array as $file => $name) {
		$html .= "\t<a href=\"examples/".strtolower($file)."\">$name</a><br />\n";
	}
	echo '</p>';
	
	if (in_array($cat, $break_after)) {
		$html .= "</div><div class=\"ref-col\">";
	}
}
$html .= "</div>";

$page->content($html);
writeFile('learning/index.html', $page->out());

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Comparison page generation Successful</h2>
<p>Generated <?= $count+1 ?> files in <?=$execution_time?> seconds.</p>

<?

function get_examples_list()
{
	$xml = openXML('examples.xml');
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