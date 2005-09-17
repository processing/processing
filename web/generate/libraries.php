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

// foreach lib
foreach ($libraries as $lib) {

    // get xml files
    $directory = "api_$lang/LIB_".$lib;
    $files = getXMLFiles(CONTENTDIR.$directory);
    // parse xml files and create pages
    foreach ($files as $file) {
        $page = new LibReferencePage(new Ref($directory.'/'.$file), $lib, $translation, $lang);
        $page->write();
    }
    
    // get index
    // create page
    // write page
    
    // copy images directory
}

?>