<?

require('../config.php');
require('lib/Translation.class.php');
$benchmark_start = microtime_float();

// arguments
$lang = isSet($_POST['lang']) ? $_POST['lang'] : 'en';


// get translation file
$translation = new Translation($lang);

// make overview page
$source = CONTENTDIR."/api_$lang/environment/";
$path = DISTDIR."/environment/";
make_necessary_directories($path."images/file");
$page = new LocalPage("Environment (IDE)", "Environment", "Environment", '../');
$page->content(file_get_contents($source."index.html"));
$page->language($lang);
writeFile('distribution/environment/index.html', $page->out());
copydirr($source.'/images', $path.'/images');


$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Environment page generation Successful</h2>
<p>Generated files in <?=$execution_time?> seconds.</p>