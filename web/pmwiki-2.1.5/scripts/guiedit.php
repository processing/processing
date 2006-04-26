<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2005 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script adds a graphical button bar to the edit page form.
    The buttons are placed in the $GUIButtons array; each button
    is specified by an array of five values:
      - the position of the button relative to others (a number)
      - the opening markup sequence
      - the closing markup sequence
      - the default text if none was highlighted
      - the text of the button, either (a) HTML markup or (b) the 
        url of a gif/jpg/png image to be used for the button 
        (along with optional "title" text in quotes).

    The buttons specified in this file are the default buttons
    for the standard markups.  Some buttons (e.g., the attach/upload
    button) are specified in their respective cookbook module.
*/

$HTMLHeaderFmt[] = "<script language='javascript' type='text/javascript'
  src='\$FarmPubDirUrl/guiedit/guiedit.js'></script>\n";

SDV($GUIButtonDirUrlFmt,'$FarmPubDirUrl/guiedit');

SDVA($GUIButtons, array(
  'em'       => array(100, "''", "''", '$[Emphasized]',
                  '$GUIButtonDirUrlFmt/em.gif"$[Emphasized (italic)]"', 
                  '$[ak_em]'),
  'strong'   => array(110, "'''", "'''", '$[Strong]',
                  '$GUIButtonDirUrlFmt/strong.gif"$[Strong (bold)]"',
                  '$[ak_strong]'),
  'pagelink' => array(200, '[[', ']]', '$[Page link]', 
                  '$GUIButtonDirUrlFmt/pagelink.gif"$[Link to internal page]"'),
  'extlink'  => array(210, '[[', ']]', 'http:// | $[link text]',
                  '$GUIButtonDirUrlFmt/extlink.gif"$[Link to external page]"'),
  'big'      => array(300, "'+", "+'", '$[Big text]',
                  '$GUIButtonDirUrlFmt/big.gif"$[Big text]"'),
  'small'    => array(310, "'-", "-'", '$[Small text]',
                  '$GUIButtonDirUrlFmt/small.gif"$[Small text]"'),
  'sup'      => array(320, "'^", "^'", '$[Superscript]',
                  '$GUIButtonDirUrlFmt/sup.gif"$[Superscript]"'),
  'sub'      => array(330, "'_", "_'", '$[Subscript]',
                  '$GUIButtonDirUrlFmt/sub.gif"$[Subscript]"'),
  'h2'       => array(400, '\\n!! ', '\\n', '$[Heading]',
                  '$GUIButtonDirUrlFmt/h.gif"$[Heading]"'),
  'center'   => array(410, '%25center%25', '', '',
                  '$GUIButtonDirUrlFmt/center.gif"$[Center]"')));

Markup('e_guibuttons', 'directives',
  '/\\(:e_guibuttons:\\)/e',
  "Keep(FmtPageName(GUIButtonCode(\$pagename), \$pagename))");

function GUIButtonCode($pagename) {
  global $GUIButtons;
  $cmpfn = create_function('$a,$b', 'return $a[0]-$b[0];');
  usort($GUIButtons, $cmpfn);
  $out = "<script language='javascript' type='text/javascript'>\n";
  foreach ($GUIButtons as $k => $g) {
    if (!$g) continue;
    @list($when, $mopen, $mclose, $mtext, $tag, $mkey) = $g;
    if ($tag{0} == '<') { 
        $out .= "document.write(\"$tag\");\n";
        continue; 
    }
    if (preg_match('/^(.*\\.(gif|jpg|png))("([^"]+)")?$/', $tag, $m)) {
      $title = (@$m[4] > '') ? "title='{$m[4]}'" : '';
      $tag = "<img src='{$m[1]}' $title style='border:0px;' />";
    }
    $mopen = str_replace(array('\\', "'"), array('\\\\', "\\\\'"), $mopen);
    $mclose = str_replace(array('\\', "'"), array('\\\\', "\\\\'"), $mclose);
    $mtext = str_replace(array('\\', "'"), array('\\\\', "\\\\'"), $mtext);
    $out .= 
      "insButton(\"$mopen\", \"$mclose\", '$mtext', \"$tag\", \"$mkey\");\n";
  }
  $out .= '</script>';
  return $out;
}

