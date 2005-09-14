<?

include('domit/xml_domit_include.php');
include('lib/xhtml.class.php');
include('lib/functions.inc.php');
include('lib/Ref.class.php');

$ref = new Ref('../content/api_en/abs.xml');

$page = new xhtml_page('../templates/template.html');
$page->set('title', $ref->title());
$page->set('content_for_layout', $ref->display());
echo $page->out();

?>