<?

define( 'PRO_EXHIBIT', 'ALL_GOOD' );

require('../config.php');
//require('lib/files.inc.php');
require('lib/functions.inc.php');
require('lib/xhtml.class.php');
require('lib/XPath.class.php');

$content = BASEDIR.'/content/test.xml';
$xpath;
$html = '';

function apply_template($data, $template)
{
	$piece = new xhtml_piece($template);
	foreach ($data as $key => $value) {
		$piece->set($key, $value);
	}
	return $piece->out();
}

if (xml_read($content)) {
	$root = $xpath->getNode('/'); //super
	$root = $xpath->getNode($root['childNodes'][0]['xpath']); //curated
	$xmlitems = $root['childNodes'];
	$items = array();
	$i = 0;
	foreach ($xmlitems as $item) {
		$items[$i]['name'] = $xpath->getAttributes($item['xpath'], 'name');
		$items[$i]['by'] = $xpath->getAttributes($item['xpath'], 'by');
		$items[$i]['scroll'] = $xpath->getAttributes($item['xpath'], 'scroll');
		$items[$i]['resize'] = $xpath->getAttributes($item['xpath'], 'resize');
		$items[$i]['width'] = $xpath->getAttributes($item['xpath'], 'width');
		$items[$i]['height'] = $xpath->getAttributes($item['xpath'], 'height');
		$items[$i]['image_file'] = $xpath->wholeText($item['xpath'].'/image[1]');
		$items[$i]['image_alt'] = $items[$i]['image_file'];
		$items[$i]['image_title'] = $items[$i]['name'] . ' by ' . $items[$i]['by'];
		$desc = $xpath->getNode($item['xpath'].'/description[1]');
		$items[$i]['description'] = implode(' ', $desc['childNodes']); //$xpath->wholeText($item['xpath'].'/description[1]');
		$items[$i]['location'] = $xpath->wholeText($item['xpath'].'/location[1]');
		$li = 1;
		$link = $xpath->getNode($item['xpath']."/link[$li]");
		while ($link !== false) {
			$items[$i]['links'] .= "<a href=\"{$link[attributes][href]}\">{$link[textParts][0]}</a><br />";
			$li++;
			$link = $xpath->getNode($item['xpath']."/link[$li]");
		}
		if (count($items[$i]['links']) > 0) 
			$items[$i]['links_title'] = 'Links:<br />';
		$html .= apply_template($items[$i], BASEDIR.'/templates/template.curated.item.html');
		$i++;
	}
}

$template = BASEDIR.'/templates/template.html';
$templatepiece = BASEDIR.'/templates/template.test.html';

$page = new xhtml_page($template);

$page->set('title', 'Testing');

$piece = new xhtml_piece($templatepiece);

$page->set('content', $piece->out());
$page->set('replaceme', $html);

$fp = fopen(BASEDIR.'/testout.php', 'w' );
fwrite( $fp, $page->out() );
fclose( $fp );

echo $page->out();

echo '<pre>';
print_r($items);
echo '</pre>';

?>

	<a href="javascript:MM_openBrWindow('<!--*-->location<!--*-->','<!--*-->name<!--*-->','status=yes,scrollbars=<!--*-->scroll<!--*-->,resizable=<!--*-->resize<!--*-->,width=<!--*-->width<!--*-->,height=<!--*-->height<!--*-->')" title="">
	<img src="<!--*-->img_file<!--*-->" alt="<!--*-->img_alt<!--*-->" title="<!--*-->img_title<!--*-->" class="curatedImg" /></a><br />
	<p>
		<a href="javascript:MM_openBrWindow('<!--*-->location<!--*-->','<!--*-->name<!--*-->','status=yes,scrollbars=<!--*-->scroll<!--*-->,resizable=<!--*-->resize<!--*-->,width=<!--*-->width<!--*-->,height=<!--*-->height<!--*-->')" title="">
		<strong><!--*-->name<!--*--></strong></a><br />
		<span class="author">by <!--*-->by<!--*--></span><br />
		<br />
		<!--*-->description<!--*--><br />
		<!--*-->source<!--*--><br />
		<br />
		<!--*-->links_title<!--*-->
		<!--*-->links<!--*-->
	</p>