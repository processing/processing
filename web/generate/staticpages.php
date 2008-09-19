<?

require('../config.php');
$benchmark_start = microtime_float();


// make troubleshooting page
$source = CONTENTDIR."static/";
#$path = BASEDIR;

$page = new Page("FAQ", "FAQ");
$page->content(file_get_contents($source."faq.html"));
writeFile('faq.html', $page->out());
#copydirr($source.'/images', $path.'/images');

$page = new Page("Contribute", "Contribute");
$page->content(file_get_contents($source."contribute.html"));
writeFile('contribute/index.html', $page->out());

$page = new Page("Copyright", "Copyright");
$page->content(file_get_contents($source."copyright.html"));
writeFile('copyright.html', $page->out());

$page = new Page("People", "People");
$page->content(file_get_contents($source."people.html"));
writeFile('people.html', $page->out());

// make the features interviews
$page = new Page("Igoe Interview", "Igoe Interview");
$page->content(file_get_contents($source."igoe.html"));
writeFile('exhibition/features/igoe/index.html', $page->out());

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Static page generation Successful</h2>
<p>Generated files in <?=$execution_time?> seconds.</p>
<!--<p>Page put here: <?=$source."faq.html"?></p>-->
<!--<p>Page put here: <?=$path.'faq.html'?></p>-->