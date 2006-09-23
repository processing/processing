<?

require('../config.php');
$benchmark_start = microtime_float();


// make troubleshooting page
$source = CONTENTDIR."static/";
#$path = BASEDIR;

// make troubleshooting page
#$source = CONTENTDIR."/api_$lang/troubleshooting/";
#$path = REFERENCEDIR . ($lang == 'en' ? '' : "/$lang") . "/troubleshooting/";
#make_necessary_directories($path."images/file");
#$page = new Page("Troubleshooting", "Troubleshooting", "Troubleshooting");
#$page->content(file_get_contents($source."index.html"));
#$page->language($lang);
#writeFile('reference/'.($lang=='en'?'':"$lang/").'troubleshooting/index.html', $page->out());
#copydirr($source.'/images', $path.'/images');

$page = new Page("Troubleshooting", "Troubleshooting");
$page->content(file_get_contents($source."faq.html"));
writeFile('faq.html', $page->out());
#copydirr($source.'/images', $path.'/images');


$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Static page generation Successful</h2>
<p>Generated files in <?=$execution_time?> seconds.</p>
<!--<p>Page put here: <?=$source."faq.html"?></p>-->
<!--<p>Page put here: <?=$path.'faq.html'?></p>-->