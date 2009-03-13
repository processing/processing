<?

require('../config.php');
require('lib/Translation.class.php');
$benchmark_start = microtime_float();

// arguments
$lang = isSet($_POST['lang']) ? $_POST['lang'] : 'en';

// get translation file
$translation = new Translation($lang);

$source = CONTENTDIR."/api_$lang/compare/";
$path = EXAMPLESDIR . ($lang == 'en' ? '' : "/$lang") . "/compare/";
make_necessary_directories($path."images/file");

$files = array('index.html','java.html','actionscript.html', 'lingo.html', 'python.html', 'dbn.html');

foreach ($files as $file) {
	$page = new Page('Compare', 'Compare');
	$page->content(file_get_contents($source.$file));
	$page->language($lang);
	writeFile('learning/'.($lang=='en'?'':"$lang/")."compare/$file", $page->out());
}
copydirr($source.'/images', $path.'/images');

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Comparison page generation Successful</h2>
<p>Generated files in <?=$execution_time?> seconds.</p>