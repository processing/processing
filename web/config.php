<?

ini_set('memory_limit', -1);
ini_set('max_execution_time', -1);

/*** define paths to includes ***/
define('BASEDIR',       dirname(__FILE__).'/');
define('TEMPLATEDIR',   BASEDIR.'templates/');
define('CONTENTDIR',    BASEDIR.'content/');
define('GENERATEDIR',   BASEDIR.'generate/');
define('DOMITDIR',      GENERATEDIR.'domit/');
define('GENLIBDIR',     GENERATEDIR.'lib/');
define('REFERENCEDIR',  BASEDIR.'reference/');
define('DISTDIR',       BASEDIR.'distribution/');
define('EXAMPLESDIR',	BASEDIR.'learning/');

require_once(DOMITDIR.'xml_domit_include.php');
require_once(GENLIBDIR.'xhtml.class.php');
require_once(GENLIBDIR.'functions.inc.php');
require_once(TEMPLATEDIR.'template.php');

// Name, Encoding, Alphabetize, URL
$domain = "http://processing.org/reference/";
$LANGUAGES = array(
    'en' => array('English', 'utf-8', true, $domain) //,
    //'zh' => array('Chinese Traditional', 'big5', false, $domain."zh/"),
    //'zh-cn' => array('Chinese Simplified', 'GB2312', false, $domain."zh-cn/"),
    //'fr' => array('French', 'utf-8', true, $domain."fr/"),
    //'id' => array('Indonesian', 'utf-8', false, $domain."id/"),
    //'it' => array('Italian', 'utf-8', true, $domain."it/"),
    //'jp' => array('Japanese', 'Shift_JIS', false, 'http://stage.itp.tsoa.nyu.edu/~tk403/proce55ing_reference_jp/'),
    //'kn' => array('Korean', 'utf-8', false, 'http://www.nabi.or.kr/processing/'),
    //'es' => array('Spanish', 'utf-8', true, $domain."es/"),
    //'tr' => array('Turkish', 'ISO-8859-9', true, $domain."tr/"),
    //'he' => array('Hebrew', 'Windows-1255', false, ''),
    //'ru' => array('Russian', 'ISO-8859-5', false, ''),
    //'pl' => array('Polish', 'ISO-8859-2', false, '')
    );
// Langauges with finished references available to the public
$FINISHED = array('en');
    
// for reference index formatting
$break_before = array('Shape', 'Color');

?>