<? 

require('../config.php');
require('lib/Example.class.php');

$cat_order = array(
	'Structure',
	'Form',
	'Data',
	'Control',
	'Math',
	'Drawing',
	'Typography',
	'.',
	'Image',
	'Color',
	'Transform',
	'Motion',
	'Input',
	'GUI',
	'Simulate',
	'.',
	'3D-Form',
	'3D-Transform',
	'3D-Image',
	'3D-Typography',
	'3D-Lights',
	'3D-Camera',
	'Library-Video',
	'Library-Net',
	'Library-Serial',
	'Library-OpenGL'
);
	

$dir = CONTENTDIR.'examples/';

foreach ($cat_order as $cat) {
	if ($cat != '.' && $dp = opendir($dir.$cat)) {
		while ($fp = readdir($dp)) {
			if (substr($fp, 0, 1) != '.') {
				$categories[$cat][] = $fp;
				$ex = new Example($fp, $cat);
				$ex->output_file();
			}
		}
	}
}

$page = new Page('Examples', 'Examples');
$page->subtemplate('template.examples.html');

$html = "<div class=\"ref-col\">\n";
foreach ($cat_order as $cat) {
	if ($cat != '.') {
		$html .= "<h3><img src=\"images/".strtolower(removesymbols($cat)).".gif\" alt=\"$cat\" /></h3>\n<p>";
		foreach ($categories[$cat] as $file) {
			$html .= "\t<a href=\"examples/".strtolower($file).".html\">$file</a><br />\n";
		}
		echo '</p>';
	} else {
		$html .= "</div><div class=\"ref-col\">";
	}
}
$html .= "</div>";

function removesymbols($str)
{
	return preg_replace("/\W/", "", $str);
}

$page->content($html);
writeFile('learning/index.html', $page->out());

?>