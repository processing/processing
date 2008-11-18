<?

require('../config.php');
$benchmark_start = microtime_float();

// make troubleshooting page
$source = CONTENTDIR."static";
$path = BASEDIR;


// update the files on the server via SVN

// look for the .subversion folder somewhere else
// otherwise will go looking for /home/root/.subversion or some other user
$where = CONTENTDIR . 'static';
putenv('HOME=' . CONTENTDIR);

// do the initial checkout
//`cd /var/www/processing && /usr/local/bin/svn co svn://processing.org/trunk/web/content/`;

`cd $where && /usr/local/bin/svn update`;

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
$page = new Page("Features", "Features");
$page->content(file_get_contents($source."features.html"));
writeFile('exhibition/features/index.html', $page->out());

$page = new Page("Igoe Interview", "Features");
$page->content(file_get_contents($source."igoe.html"));
writeFile('exhibition/features/igoe/index.html', $page->out());

$page = new Page("Hodgin Interview", "Features");
$page->content(file_get_contents($source."hodgin.html"));
writeFile('exhibition/features/hodgin/index.html', $page->out());

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Static page generation Successful</h2>
<p>Generated files in <?=$execution_time?> seconds.</p>
<!--<p>Page put here: <?=$source."faq.html"?></p>-->
<!--<p>Page put here: <?=$path.'faq.html'?></p>-->