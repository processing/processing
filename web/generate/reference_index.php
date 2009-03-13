<?

require('../config.php');
require('lib/Ref.class.php');
require('lib/Translation.class.php');
$benchmark_start = microtime_float();

// arguments
$lang = isSet($_POST['lang']) ? $_POST['lang'] : 'en';


// make changes file
$lib_dir = 'reference/';
$index = CONTENTDIR."api_$lang/changes.html";
$page = new Page('Changes', 'Changes', 'Language');
$page->content(file_get_contents($index));
//make_necessary_directories(BASEDIR.$lib_dir.'/images/include.php');
writeFile($lib_dir.'/changes/index.html', $page->out());
//copydirr($source.'/images', $path.'/images');



// get translation file
$translation = new Translation($lang);

// get categories and subcategories
$complete = $translation->categories;

// get reference files for the language
$files = getRefFiles($lang);

sort($files);

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


$path = 'reference/' . ($lang != 'en' ? "$lang/" : '');

$title = ' '. tr('language') . ' (' . tr('api') . ')';
$alphaTitle = tr('alphabetical') . ' ' . $title;
$completeTitle = tr('complete') . ' ' . $title;
$alphaCompleteTitle = tr('alphabetical') . ' ' . tr('complete') . ' ' . $title;

/*** category_index() and alpha_index() are found in lib/functions.inc.php ***/

// abridged reference
#$page = new Page($title, 'Language');
#$page->subtemplate('template.ref.index.html');
#$page->content(category_index($abridged));
#$page->set('reference_nav', reference_nav());
#$page->set('language_nav', language_nav($lang));
#$page->set_array($translation->meta);
#$page->language($lang);
#writeFile($path.'index.html', $page->out());

// complete reference
$page = new Page($completeTitle, 'Language');
$page->subtemplate('template.ref.index.ext.html');
$page->content(category_index($complete));
$page->set('reference_nav', reference_nav());
$page->set('language_nav', language_nav($lang));
$page->set_array($translation->meta);
//$page->set('abridged_notice', $page->extended_notice);
$page->language($lang);
writeFile($path.'index.html', $page->out());

// sort alphabetically
#ksort($abridged_alpha);
ksort($complete_alpha);

// abridged alphaphabetical
#$page = new Page($alphaTitle, 'Language');
#$page->subtemplate('template.ref.index.html');
#$page->content(alpha_index($abridged_alpha));
#$page->set('reference_nav', reference_nav());
#$page->set('language_nav', language_nav($lang));
#$page->set_array($translation->meta);
#$page->language($lang);
#writeFile($path.'index_alpha.html', $page->out());

// complete alphabetical
$page = new Page($alphaCompleteTitle, 'A-Z');
$page->subtemplate('template.ref.index.ext.html');
$page->content(alpha_index($complete_alpha));
$page->set('reference_nav', reference_nav());
$page->set('language_nav', language_nav($lang));
$page->set_array($translation->meta);
//$page->set('abridged_notice', 'test for now...');
$page->language($lang);
writeFile($path.'alpha.html', $page->out());

// copy images directory from content folder to public folder
if (!is_dir(REFERENCEDIR.($lang != 'en' ? "$lang/" : '').'images')) { mkdir(REFERENCEDIR.($lang != 'en' ? "$lang/" : '').'images', 0757); }
copydirr(CONTENTDIR."api_$lang/images", REFERENCEDIR.($lang != 'en' ? "$lang/" : '').'images');

$benchmark_end = microtime_float();
$execution_time = round($benchmark_end - $benchmark_start, 4);
?>

<h2>Reference Index Generation Successful</h2>
<p>Generated 2 files in <?=$execution_time?> seconds.</p>