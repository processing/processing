<?

require('../config.php');
$benchmark_start = microtime_float();


// make troubleshooting page
$source = CONTENTDIR."static/";
#$path = BASEDIR;

#$source = CONTENTDIR."/api_$lang/troubleshooting/";
#$path = DISTDIR."/troubleshooting/";
#make_necessary_directories($path."images/file");
#$page = new LocalPage('Troubleshooting \\ Processing 1.0 (BETA)', 'Troubleshooting', 'Troubleshooting', '../');
#$page->content(file_get_contents($source."index.html"));
#$page->language($lang);
#writeFile('distribution/troubleshooting/index.html', $page->out());
#copydirr($source.'/images', $path.'/images');

#$page = new LocalPage('FAQ \\ Processing 1.0 (BETA)', "FAQ", "FAQ", './');
#$page->content(file_get_contents($source."faq.html"));
#writeFile('distribution/faq.html', $page->out());

#$page = new LocalPage('Contribute \\ Processing 1.0 (BETA)', "Contribute", "Contribute", '../');
#$page->content(file_get_contents($source."contribute.html"));
#writeFile('distribution/contribute/index.html', $page->out());

$page = new LocalPage('Copyright', "Copyright", "Copyright", './');
$page->content(file_get_contents($source."copyright.html"));
writeFile('distribution/copyright.html', $page->out());

$page = new LocalPage('People', "People", "People", './');
$page->content(file_get_contents($source."people.html"));
writeFile('distribution/people.html', $page->out());


$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Static page generation Successful</h2>
<p>Generated files in <?=$execution_time?> seconds.</p>
<!--<p>Page put here: <?=$source."faq.html"?></p>-->
<!--<p>Page put here: <?=$path.'faq.html'?></p>-->