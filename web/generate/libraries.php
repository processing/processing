<?

require_once('../config.php');
require_once('lib/Ref.class.php');
require_once('lib/Translation.class.php');
$benchmark_start = microtime_float();

// arguments
$lang = isset($_POST['lang']) ? $_POST['lang'] : 'en';

// get translation file
$translation = new Translation($lang);

// each lib
$libraries = array('net', 'serial', 'video', 'opengl');

$lib_dir = 'reference/'.($lang != 'en' ? "$lang/" : '').'libraries';

// get library index
$index = CONTENTDIR."api_$lang/libraries.html";
$page = new Page('Libraries \\ Processing 1.0 (BETA)', 'Libraries');
$page->content(file_get_contents($index));
make_necessary_directories(BASEDIR.$lib_dir.'/images/include.php');
writeFile($lib_dir.'/index.html', $page->out());
copydirr(CONTENTDIR."api_$lang/LIB_images", BASEDIR.$lib_dir.'/images');

// foreach lib
foreach ($libraries as $lib) {
	$source = "api_$lang/LIB_$lib";
	$destination = ($lang != 'en' ? "$lang/" : '')."libraries/$lib";
	make_necessary_directories(REFERENCEDIR.$destination.'/images/include');

    // get xml files
    if (!$files = getXMLFiles(CONTENTDIR.$source)) { 
		echo "couldn't open files"; 
	} else {
	// parse xml files and create pages
	    foreach ($files as $file) {
	        echo $file.'<br/>';
	        $page = new LibReferencePage(new Ref($source.'/'.$file), $lib, $translation, $lang);
	        $page->write();
	    }
	}

    // template and copy index
    $index = CONTENTDIR.$source.'/index.html';
    $page = new Page(ucfirst($lib) . ' \\ Libraries \\ Processing 1.0 (BETA)', 'Libraries', 'Library-index');
    $page->content(file_get_contents($index));
    writeFile('reference/'.$destination.'/index.html', $page->out());
    
    // copy images directory
	copydirr(CONTENTDIR.$source.'/images', REFERENCEDIR.$destination.'/images');

}

?>