<?

require_once('../config.php');
require_once('lib/Translation.class.php');
$benchmark_start = microtime_float();

// arguments
$lang = isset($_POST['lang']) ? $_POST['lang'] : 'en';
//$tools_dir = 'reference/'.($lang != 'en' ? "$lang/" : '').'tools';
$tools_dir = DISTDIR.'tools';

// get translation file
$translation = new Translation($lang);

// get tools index
$index = CONTENTDIR."api_en/tools/index.html";
//$page = new Page('Tools', 'Tools');
$page = new LocalPage('Tools \\ Processing 1.0 (BETA)', 'Tools', 'Tools', '../');
$page->content(file_get_contents($index));
//make_necessary_directories(BASEDIR.$tools_dir.'/images/include.php');
writeFile('distribution/tools/index.html', $page->out());

copydirr(CONTENTDIR."api_$lang/TOOL_images", BASEDIR.'tools/images');

// copy over the files for the contributed libraries
copy(CONTENTDIR."static/tools.html", BASEDIR.'tools/tools.html');
//copydirr(CONTENTDIR.$source.'/images', DISTDIR.$destination.'/images');


$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Tool Generation Successful</h2>
<p>Generated files in <?=$execution_time?> seconds.</p>