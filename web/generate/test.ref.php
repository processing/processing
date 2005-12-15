<?
require('../config.php');
include('lib/Ref.class.php');
include('lib/Translation.class.php');

$lang = 'en';
$translation = new Translation($lang);

$page = new ReferencePage(new Ref('/api_en/abs.xml'), $translation, $lang);
echo $page->out();
?>