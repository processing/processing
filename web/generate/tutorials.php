<?

require('../config.php');
$benchmark_start = microtime_float();

// make troubleshooting page
$source = CONTENTDIR."static/tutorials/";
$path = BASEDIR;


// update the files on the server via SVN

// look for the .subversion folder somewhere else
// otherwise will go looking for /home/root/.subversion or some other user
$where = CONTENTDIR . 'static/tutorials';
putenv('HOME=' . CONTENTDIR);

// do the initial checkout
//`cd /var/www/processing && /usr/local/bin/svn co svn://processing.org/trunk/web/content/`;

`cd $where && /usr/local/bin/svn update`;

// Copy over the images for the tutorials index
if (!is_dir($path.'learning/imgs')) { 
	mkdir($path.'learning/imgs', '0757'); 
}
if (is_dir($path.'learning/imgs')) { 
	copydirr($source.'imgs', $path.'learning/imgs', null, 0757, true);
}

$page = new Page("Tutorials", "Tutorials");
$page->content(file_get_contents($source."index.html"));
writeFile('learning/index.html', $page->out());

$page = new Page("Getting Started", "Tutorials");
$page->content(file_get_contents($source."gettingstarted/index.html"));
writeFile('learning/gettingstarted/index.html', $page->out());
if (!is_dir($path.'learning/gettingstarted/imgs')) { 
	mkdir($path.'learning/gettingstarted/imgs', '0757'); 
}
if (is_dir($path.'learning/gettingstarted/imgs')) { 
	copydirr($source.'gettingstarted/imgs', $path.'learning/gettingstarted/imgs', null, 0757, true);
}

$page = new Page("Processing in Eclipse", "Tutorials");
$page->content(file_get_contents($source."eclipse/index.html"));
writeFile('learning/eclipse/index.html', $page->out());
if (!is_dir($path.'learning/eclipse/imgs')) { 
	mkdir($path.'learning/eclipse/imgs', '0757'); 
}
if (is_dir($path.'learning/eclipse/imgs')) { 
	copydirr($source.'eclipse/imgs', $path.'learning/eclipse/imgs', null, 0757, true);
}

$page = new Page("Drawing", "Tutorials");
$page->content(file_get_contents($source."drawing/index.html"));
writeFile('learning/drawing/index.html', $page->out());
if (!is_dir($path.'learning/drawing/imgs')) { 
	mkdir($path.'learning/drawing/imgs', '0757'); 
}
if (is_dir($path.'learning/drawing/imgs')) { 
	copydirr($source.'drawing/imgs', $path.'learning/drawing/imgs', null, 0757, true);
}

$page = new Page("Color", "Tutorials");
$page->content(file_get_contents($source."color/index.html"));
writeFile('learning/color/index.html', $page->out());
if (!is_dir($path.'learning/color/imgs')) { 
	mkdir($path.'learning/color/imgs', '0757'); 
}
if (is_dir($path.'learning/color/imgs')) { 
	copydirr($source.'color/imgs', $path.'learning/color/imgs', null, 0757, true);
}

$page = new Page("Objects", "Tutorials");
$page->content(file_get_contents($source."objects/index.html"));
writeFile('learning/objects/index.html', $page->out());
if (!is_dir($path.'learning/objects/imgs')) { 
	mkdir($path.'learning/objects/imgs', '0757'); 
}
if (is_dir($path.'learning/objects/imgs')) { 
	copydirr($source.'objects/imgs', $path.'learning/objects/imgs', null, 0757, true);
}

$page = new Page("Two-Dimensional Arrays", "Tutorials");
$page->content(file_get_contents($source."2darray/index.html"));
writeFile('learning/2darray/index.html', $page->out());
if (!is_dir($path.'learning/2darray/imgs')) { 
	mkdir($path.'learning/2darray/imgs', '0757'); 
}
if (is_dir($path.'learning/2darray/imgs')) { 
	copydirr($source.'2darray/imgs', $path.'learning/2darray/imgs', null, 0757, true);
}

$page = new Page("Trig", "Tutorials");
$page->content(file_get_contents($source."trig/index.html"));
writeFile('learning/trig/index.html', $page->out());
if (!is_dir($path.'learning/trig/imgs')) { 
	mkdir($path.'learning/trig/imgs', '0757'); 
}
if (is_dir($path.'learning/trig/imgs')) { 
	copydirr($source.'trig/imgs', $path.'learning/trig/imgs', null, 0757, true); 
}

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Static page generation Successful</h2>
<h2>Updated <?=$where?> </h2>
<p>Generated files in <?=$execution_time?> seconds.</p>
<!--<p>Page put here: <?=$source."faq.html"?></p>-->
<!--<p>Page put here: <?=$path.'faq.html'?></p>-->
<!--<p><?=$path.'learning/eclipse/imgs'?></p>-->