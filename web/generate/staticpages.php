<?

require('../config.php');
$benchmark_start = microtime_float();


// make troubleshooting page
$source = CONTENTDIR."/static/";
$path = BASEDIR;

$page = new Page("FAQ", "FAQ", "FAQ");
$page->content(file_get_contents($source."faq.html"));
writeFile($path.'/faq.html', $page->out());
#copydirr($source.'/images', $path.'/images');


$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Static page generation Successful</h2>
<p>Generated files in <?=$execution_time?> seconds.</p>