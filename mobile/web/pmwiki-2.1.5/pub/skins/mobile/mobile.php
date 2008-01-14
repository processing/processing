<?php if (!defined('PmWiki')) exit();

global $SITE_ROOT;
$SITE_ROOT = '/';

if (strpos($_GET['n'], 'Tutorials.') === 0) {
  global $TUTORIALS_LINK;
  if (strpos($_GET['n'], 'Tutorials.HomePage') === 0) {
    $TUTORIALS_LINK = 'Tutorials';
  } else {
    $TUTORIALS_LINK = '<a href="/learning/tutorials/index.php">Tutorials</a>';
  }
  LoadPageTemplate($pagename, "$SkinDir/tutorials.tmpl");
} else if (strpos($_GET['n'], 'FAQ.') === 0) {
  LoadPageTemplate($pagename, "$SkinDir/faq.tmpl");
} else if (strpos($_GET['n'], 'Libraries.') === 0) {
  if ($_GET['include'] == '1') {
    LoadPageTemplate($pagename, "$SkinDir/libraries.tmpl");
  } else {//if (strpos($_SERVER['QUERY_STRING'], 'action=') > 0) {
    LoadPageTemplate($pagename, "$SkinDir/libraries_action.tmpl");
  }
} else {
  LoadPageTemplate($pagename, "$SkinDir/mobile.tmpl");
}

?>
