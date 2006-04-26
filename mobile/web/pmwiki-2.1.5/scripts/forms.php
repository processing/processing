<?php if (!defined('PmWiki')) exit();
/*  Copyright 2005-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.
*/

# $InputAttrs are the attributes we allow in output tags
SDV($InputAttrs, array('name', 'value', 'id', 'class', 'rows', 'cols', 
  'size', 'maxlength', 'action', 'method', 'accesskey', 
  'checked', 'disabled', 'readonly', 'enctype'));

# Set up formatting for text, submit, hidden, radio, etc. types
foreach(array('text', 'submit', 'hidden', 'password', 'radio', 'checkbox',
              'reset', 'file') as $t) 
  SDV($InputTags[$t][':html'], "<input type='$t' \$InputFormArgs />");
SDV($InputTags['text']['class'], 'inputbox');
SDV($InputTags['password']['class'], 'inputbox');
SDV($InputTags['submit']['class'], 'inputbutton');

# (:input form:)
SDVA($InputTags['form'], array(
  ':args' => array('action', 'method'),
  ':html' => "<form \$InputFormArgs>",
  'method' => 'post'));

# (:input end:)
SDV($InputTags['end'][':html'], '</form>');

# (:input textarea:)
SDVA($InputTags['textarea'], array(
  ':html' => "<textarea \$InputFormArgs></textarea>"));

Markup('input', 'directives', 
  '/\\(:input\\s+(\\w+)(.*?):\\)/ei',
  "InputMarkup(\$pagename, '$1', PSS('$2'))");

function InputMarkup($pagename, $type, $args) {
  global $InputTags, $InputAttrs, $InputValues, $FmtV;
  if (!@$InputTags[$type]) return "(:input $type $args:)";
  $opt = array_merge($InputTags[$type], ParseArgs($args));
  $args = @$opt[':args'];
  if (!$args) $args = array('name', 'value');
  while (count(@$opt['']) > 0 && count($args) > 0) 
    $opt[array_shift($args)] = array_shift($opt['']);
  foreach ((array)@$opt[''] as $a) 
    if (!isset($opt[$a])) $opt[$a] = $a;
  if (!isset($opt['value']) && isset($InputValues[@$opt['name']])) 
    $opt['value'] = $InputValues[$opt['name']];
  $attr = array();
  foreach ($InputAttrs as $a) {
    if (!isset($opt[$a])) continue;
    $attr[] = "$a='".str_replace("'", '&#39;', $opt[$a])."'";
  }
  $FmtV['$InputFormArgs'] = implode(' ', $attr);
  $out = FmtPageName($opt[':html'], $pagename);
  return preg_replace('/<(\\w+\\s)(.*)$/es',"'<$1'.Keep(PSS('$2'))", $out);
}

## Form-based authorization prompts (for use with PmWikiAuth)

SDVA($InputTags['auth_form'], array(
  ':html' => "<form action='{$_SERVER['REQUEST_URI']}' method='post' 
    name='authform'>\$PostVars"));
SDV($AuthPromptFmt, array(&$PageStartFmt, 'page:$SiteGroup.AuthForm',
  "<script language='javascript' type='text/javascript'><!--
    try { document.authform.authid.focus(); }
    catch(e) { document.authform.authpw.focus(); } //--></script>",
  &$PageEndFmt));

## The section below handles specialized EditForm pages.
## We don't bother to load it if we're not editing.

if ($action != 'edit') return;

SDV($PageEditForm, '$SiteGroup.EditForm');
SDV($PageEditFmt, '$EditForm');
if (@$_REQUEST['editform']) {
  $PageEditForm=$_REQUEST['editform'];
  $PageEditFmt='$EditForm';
}
$Conditions['e_preview'] = '(boolean)$_POST["preview"]';

XLSDV('en', array(
  'ak_save' => 's',
  'ak_saveedit' => 'u',
  'ak_preview' => 'p',
  'ak_textedit' => ',',
  'e_rows' => '23',
  'e_cols' => '60'));

# (:e_preview:) displays the preview of formatted text.
Markup('e_preview', 'directives',
  '/^\\(:e_preview:\\)/e',
  "Keep(\$GLOBALS['FmtV']['\$PreviewText'])");

# If we didn't load guiedit.php, then set (:e_guibuttons:) to
# simply be empty.
Markup('e_guibuttons', 'directives', '/\\(:e_guibuttons:\\)/', '');

SDVA($InputTags['e_form'], array(
  ':html' => "<form action='{\$PageUrl}?action=edit' method='post'><input 
    type='hidden' name='action' value='edit' /><input 
    type='hidden' name='n' value='{\$FullName}' /><input 
    type='hidden' name='basetime' value='\$EditBaseTime' />"));
SDVA($InputTags['e_textarea'], array(
  ':html' => "<textarea \$InputFormArgs 
    onkeydown='if (event.keyCode==27) event.returnValue=false;' 
    >\$EditText</textarea>",
  'name' => 'text', 'id' => 'text', 'accesskey' => XL('ak_textedit'),
  'rows' => XL('e_rows'), 'cols' => XL('e_cols')));
SDVA($InputTags['e_author'], array(
  ':html' => "<input type='text' \$InputFormArgs />",
  'name' => 'author', 'value' => $Author));
SDVA($InputTags['e_changesummary'], array(
  ':html' => "<input type='text' \$InputFormArgs />",
  'name' => 'csum', 'size' => '60', 'maxlength' => '100',
  'value' => htmlspecialchars(stripmagic(@$_POST['csum']), ENT_QUOTES)));
SDVA($InputTags['e_minorcheckbox'], array(
  ':html' => "<input type='checkbox' \$InputFormArgs />",
  'name' => 'diffclass', 'value' => 'minor'));
if (@$_POST['diffclass']=='minor') 
  SDV($InputTags['e_minorcheckbox']['checked'], 'checked');
SDVA($InputTags['e_savebutton'], array(
  ':html' => "<input type='submit' \$InputFormArgs />",
  'name' => 'post', 'value' => ' '.XL('Save').' ', 
  'accesskey' => XL('ak_save')));
SDVA($InputTags['e_saveeditbutton'], array(
  ':html' => "<input type='submit' \$InputFormArgs />",
  'name' => 'postedit', 'value' => ' '.XL('Save and edit').' ',
  'accesskey' => XL('ak_saveedit')));
SDVA($InputTags['e_savedraftbutton'], array(':html' => ''));
SDVA($InputTags['e_previewbutton'], array(
  ':html' => "<input type='submit' \$InputFormArgs />",
  'name' => 'preview', 'value' => ' '.XL('Preview').' ', 
  'accesskey' => XL('ak_preview')));
SDVA($InputTags['e_cancelbutton'], array(
  ':html' => "<input type='submit' \$InputFormArgs />",
  'name' => 'cancel', 'value' => ' '.XL('Cancel').' ' ));
SDVA($InputTags['e_resetbutton'], array(
  ':html' => "<input type='reset' \$InputFormArgs />",
  'value' => ' '.XL('Reset').' '));

