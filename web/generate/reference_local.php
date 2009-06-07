<?

require('../config.php');
require('lib/Ref.class.php');
require('lib/Translation.class.php');
$benchmark_start = microtime_float();

// arguments
$lang = isSet($_POST['lang']) ? $_POST['lang'] : 'en';

// get translation file
$translation = new Translation($lang);

// get reference files for the language
$files = getRefFiles($lang);

// make changes file
$lib_dir = DISTDIR;
$index = CONTENTDIR."api_$lang/changes.html";
$page = new LocalPage('Language (API)', 'Changes', 'Changes', './');
$page->content(file_get_contents($index));
writeFile('distribution/changes.html', $page->out());


/*******************************************************
reference files
*******************************************************/

// create Ref objects for each file
foreach ($files as $file) {
    $refs[] = new Ref("api_$lang/".$file);
}

// create ReferencePage objects
$count = 0;
foreach ($refs as $ref) {
    $local = new LocalReferencePage($ref, $translation, $lang);
    $local->write();
    $count++;
}

/*******************************************************
indices
*******************************************************/

// get categories and subcategories
$complete = $translation->categories;

// populate index arrays with reference items
$abridged = $complete;
foreach ($files as $file) {
    $ref = new Ref("api_$lang/".$file);
    if ($ref->index()) {
        if ($ref->level != 'Extended') {
            if ($ref->subcategory == $ref->name) {
                $abridged[$ref->category][''][] = array($ref->name, $ref->name());
            } else {
                $abridged[$ref->category][$ref->subcategory][] = array($ref->name, $ref->name());
            }
            $abridged_alpha[strtolower($ref->name)] = array($ref->name, $ref->name());
        }
        if ($ref->subcategory == $ref->name) {
            $complete[$ref->category][''][] = array($ref->name, $ref->name());
        } else { 
            $complete[$ref->category][$ref->subcategory][] = array($ref->name, $ref->name());
        }
        $complete_alpha[strtolower($ref->name)] = array($ref->name, $ref->name());
    }
}

// sort alphabetically
ksort($abridged_alpha);
ksort($complete_alpha);

$path = 'distribution/';

/*** category_index() and alpha_index() are found in lib/functions.inc.php ***/

/**
// abridged reference
$page = new LocalPage('Language (API)', 'Language');
$page->subtemplate('template.ref.index.html');
$page->content(category_index($abridged));
$page->set('reference_nav', reference_nav());
$page->set('language_nav', language_nav($lang));
$page->set_array($translation->meta);
$page->language($lang);
writeFile($path.'index.html', $page->out());

// abridged alpha
$page = new LocalPage('Alphabetical Language (API)', 'Language');
$page->subtemplate('template.ref.index.html');
$page->content(alpha_index($abridged_alpha));
$page->set('reference_nav', reference_nav());
$page->set('language_nav', language_nav($lang));
$page->set_array($translation->meta);
$page->language($lang);
writeFile($path.'index_alpha.html', $page->out());
*/

// complete reference
$page = new LocalPage('Complete Language (API)', 'Language');
$page->subtemplate('template.ref.index.html');
$page->content(category_index($complete));
$page->set('reference_nav', reference_nav());
$page->set('language_nav', language_nav($lang));
$page->set_array($translation->meta);
$page->set('abridged_notice', '');
$page->language($lang);
//writeFile($path.'index_ext.html', $page->out());
writeFile($path.'index.html', $page->out());

// complete alpha
$page = new LocalPage('Alphabetical Complete Langauge (API)', 'Language');
$page->subtemplate('template.ref.index.html');
$page->content(alpha_index($complete_alpha));
$page->set('reference_nav', reference_nav());
$page->set('language_nav', language_nav($lang));
$page->set_array($translation->meta);
$page->set('abridged_notice', '');
$page->language($lang);
//writeFile($path.'index_alpha_ext.html', $page->out());
writeFile($path.'alpha.html', $page->out());


/*******************************************************
media
*******************************************************/

// copy images directory from content folder to public folder
$dirs = array('images', 'media', 'css', 'javascript', 'img', 'img/reference');
foreach ($dirs as $d) {
	if (!is_dir(DISTDIR.$d)) {
		mkdir(DISTDIR.$d, 0757);
	}
}
copydirr(CONTENTDIR."api_en/images", DISTDIR.'images');
copydirr(CONTENTDIR."api_media", DISTDIR.'media');
copydirr(BASEDIR."css", DISTDIR.'css');
copydirr(BASEDIR."javascript", DISTDIR.'javascript');
copydirr(BASEDIR."img", DISTDIR.'img', false);
copydirr(BASEDIR."img/reference", DISTDIR.'img/reference', false);


$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);

?>

<h2>Distribution Reference Generation Successful</h2>
<p>Generated <?=$counter?> files in <?=$execution_time?> seconds.</p>