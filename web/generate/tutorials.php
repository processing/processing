<?

require('../config.php');
$benchmark_start = microtime_float();


// make troubleshooting page
$source = CONTENTDIR."static/tutorials/";
$path = BASEDIR;

$page = new Page("Tutorials", "Tutorials");
$page->content(file_get_contents($source."index.html"));
writeFile('learning/tutorials/index.html', $page->out());

$page = new Page("Processing in Eclipse", "Tutorials");
$page->content(file_get_contents($source."eclipse/index.html"));
writeFile('learning/tutorials/eclipse/index.html', $page->out());
// copydirr($source.'/eclipse/imgs', $path.'learning/tutorials/eclipse/imgs');
copydirr($path.'content/static/tutorials/eclipse/imgs', $path.'learning/tutorials/eclipse/imgs');

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Static page generation Successful</h2>
<p>Generated files in <?=$execution_time?> seconds.</p>
<!--<p>Page put here: <?=$source."faq.html"?></p>-->
<!--<p>Page put here: <?=$path.'faq.html'?></p>-->