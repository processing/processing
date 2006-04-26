<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script defines PmWiki's standard markup.  It is automatically
    included from stdconfig.php unless $EnableStdMarkup==0.

    Each call to Markup() below adds a new rule to PmWiki's translation
    engine (unless a rule with the same name has already been defined).  
    The form of the call is Markup($id,$where,$pat,$rep); 
    $id is a unique name for the rule, $where is the position of the rule
    relative to another rule, $pat is the pattern to look for, and
    $rep is the string to replace it with.
*/

## first we preserve text in [=...=] and [@...@]
function PreserveText($sigil, $text, $lead) {
  if ($sigil=='=') return $lead.Keep($text);
  if (strpos($text, "\n")===false) 
    return "$lead<code class='escaped'>".Keep($text)."</code>";
  $text = preg_replace("/\n[^\\S\n]+$/", "\n", $text);
  if ($lead == "" || $lead == "\n") 
    return "$lead<pre class='escaped'>".Keep($text)."</pre>";
  return "$lead<:pre,1>".Keep($text);
}

Markup('[=','_begin',"/(\n[^\\S\n]*)?\\[([=@])(.*?)\\2\\]/se",
    "PreserveText('$2', PSS('$3'), '$1')");
Markup('restore','<_end',"/$KeepToken(\\d.*?)$KeepToken/e",
    '$GLOBALS[\'KPV\'][\'$1\']');
Markup('<:', '>restore',
  "/<:[^>]*>/", "");

## remove carriage returns before preserving text
Markup('\\r','<[=','/\\r/','');

# $[phrase] substitutions
Markup('$[phrase]', '>[=',
  '/\\$\\[(?>([^\\]]+))\\]/e', "NoCache(XL(PSS('$1')))");

# {$var} substitutions
Markup('{$var}', '>$[phrase]',
  '/\\{(!?[-\\w.\\/]*)(\\$\\w+)\\}/e', 
  "htmlspecialchars(PageVar(\$pagename, '$2', '$1'), ENT_NOQUOTES)");

Markup('if', 'fulltext',
  "/\\(:(if[^\n]*?):\\)(.*?)(?=\\(:if[^\n]*?:\\)|$)/sei",
  "CondText(\$pagename,PSS('$1'),PSS('$2'))");

## (:include:)
Markup('include', '>if',
  '/\\(:include\\s+(\\S.*?):\\)/ei',
  "PRR(IncludeText(\$pagename, '$1'))");

## (:redirect:)
Markup('redirect', '<include',
  '/\\(:redirect\\s+(\\S.*?):\\)/ei',
  "RedirectMarkup(\$pagename, PSS('$1'))");

$SaveAttrPatterns['/\\(:(if|include|redirect)(\\s.*?)?:\\)/i'] = ' ';

## GroupHeader/GroupFooter handling
Markup('nogroupheader', '>include',
  '/\\(:nogroupheader:\\)/ei',
  "PZZ(\$GLOBALS['GroupHeaderFmt']='')");
Markup('nogroupfooter', '>include',
  '/\\(:nogroupfooter:\\)/ei',
  "PZZ(\$GLOBALS['GroupFooterFmt']='')");
Markup('groupheader', '>nogroupheader',
  '/\\(:groupheader:\\)/ei',
  "PRR(FmtPageName(\$GLOBALS['GroupHeaderFmt'],\$pagename))");
Markup('groupfooter','>nogroupfooter',
  '/\\(:groupfooter:\\)/ei',
  "PRR(FmtPageName(\$GLOBALS['GroupFooterFmt'],\$pagename))");

## (:nl:)
Markup('nl0','<split',"/([^\n])(?>(?:\\(:nl:\\))+)([^\n])/i","$1\n$2");
Markup('nl1','>nl0',"/\\(:nl:\\)/i",'');

## \\$  (end of line joins)
Markup('\\$','>nl1',"/\\\\(?>(\\\\*))\n/e",
  "str_repeat('<br />',strlen('$1'))");

## Remove one <:vspace> after !headings
Markup('!vspace', '>\\$', "/^(!(?>[^\n]+)\n)<:vspace>/m", '$1');

## (:noheader:),(:nofooter:),(:notitle:)...
Markup('noheader', 'directives',
  '/\\(:noheader:\\)/ei',
  "SetTmplDisplay('PageHeaderFmt',0)");
Markup('nofooter', 'directives',
  '/\\(:nofooter:\\)/ei',
  "SetTmplDisplay('PageFooterFmt',0)");
Markup('notitle', 'directives',
  '/\\(:notitle:\\)/ei',
  "SetTmplDisplay('PageTitleFmt',0)");
Markup('noleft', 'directives',
  '/\\(:noleft:\\)/ei',
  "SetTmplDisplay('PageLeftFmt',0)");
Markup('noright', 'directives',
  '/\\(:noright:\\)/ei',
  "SetTmplDisplay('PageRightFmt',0)");

## (:spacewikiwords:)
Markup('spacewikiwords', 'directives',
  '/\\(:(no)?spacewikiwords:\\)/ei',
  "PZZ(\$GLOBALS['SpaceWikiWords']=('$1'!='no'))");

## (:linkwikiwords:)
Markup('linkwikiwords', 'directives',
  '/\\(:(no)?linkwikiwords:\\)/ei',
  "PZZ(\$GLOBALS['LinkWikiWords']=('$1'!='no'))");

## (:linebreaks:)
Markup('linebreaks', 'directives',
  '/\\(:(no)?linebreaks:\\)/ei',
  "PZZ(\$GLOBALS['HTMLPNewline'] = ('$1'!='no') ? '<br  />' : '')");

## (:messages:)
Markup('messages', 'directives',
  '/^\\(:messages:\\)/ei',
  "'<:block>'.Keep(
    FmtPageName(implode('',(array)\$GLOBALS['MessagesFmt']), \$pagename))");

## (:comment:)
Markup('comment', 'directives', '/\\(:comment .*?:\\)/i', '');

## character entities
Markup('&','directives','/&amp;(?>([A-Za-z0-9]+|#\\d+|#[xX][A-Fa-f0-9]+));/',
  '&$1;');

## (:title:)
Markup('title','>&',
  '/\\(:title\\s(.*?):\\)/ei',
  "PZZ(PCache(\$pagename, 
         array('title' => SetProperty(\$pagename, 'title', PSS('$1')))))");

## (:keywords:), (:description:)
Markup('keywords', '>&', 
  "/\\(:keywords?\\s+(.+?):\\)/ei",
  "PZZ(SetProperty(\$pagename, 'keywords', PSS('$1'), ', '))");
Markup('description', '>&',
  "/\\(:description\\s+(.+?):\\)/ei",
  "PZZ(SetProperty(\$pagename, 'description', PSS('$1'), '\n'))");
$HTMLHeaderFmt['meta'] = 'function:PrintMetaTags';
function PrintMetaTags($pagename, $args) {
  global $PCache;
  foreach(array('keywords', 'description') as $n) {
    foreach((array)@$PCache[$pagename]["=p_$n"] as $v) {
      $v = str_replace("'", '&#039;', $v);
      print "<meta name='$n' content='$v' />\n";
    }
  }
}

#### inline markups ####
## ''emphasis''
Markup("''",'inline',"/''(.*?)''/",'<em>$1</em>');

## '''strong'''
Markup("'''","<''","/'''(.*?)'''/",'<strong>$1</strong>');

## '''''strong emphasis'''''
Markup("'''''","<'''","/'''''(.*?)'''''/",'<strong><em>$1</em></strong>');

## @@code@@
Markup('@@','inline','/@@(.*?)@@/','<code>$1</code>');

## '+big+', '-small-'
Markup("'+","<'''''","/'\\+(.*?)\\+'/",'<big>$1</big>');
Markup("'-","<'''''","/'\\-(.*?)\\-'/",'<small>$1</small>');

## '^superscript^', '_subscript_'
Markup("'^","<'''''","/'\\^(.*?)\\^'/",'<sup>$1</sup>');
Markup("'_","<'''''","/'_(.*?)_'/",'<sub>$1</sub>');

## [+big+], [-small-]
Markup('[+','inline','/\\[(([-+])+)(.*?)\\1\\]/e',
  "'<span style=\'font-size:'.(round(pow(6/5,$2strlen('$1'))*100,0)).'%\'>'.
    PSS('$3</span>')");

## {+ins+}, {-del-}
Markup('{+','inline','/\\{\\+(.*?)\\+\\}/','<ins>$1</ins>');
Markup('{-','inline','/\\{-(.*?)-\\}/','<del>$1</del>');

## [[<<]] (break)
Markup('[[<<]]','inline','/\\[\\[&lt;&lt;\\]\\]/',"<br clear='all' />");

###### Links ######
## [[free links]]
Markup('[[','links',"/(?>\\[\\[\\s*)(.*?)\\]\\]($SuffixPattern)/e",
  "Keep(MakeLink(\$pagename,PSS('$1'),NULL,'$2'),'L')");

## [[!Category]]
SDV($CategoryGroup,'Category');
SDV($LinkCategoryFmt,"<a class='categorylink' href='\$LinkUrl'>\$LinkText</a>");
Markup('[[!','<[[','/\\[\\[!(.*?)\\]\\]/e',
  "Keep(MakeLink(\$pagename,PSS('$CategoryGroup/$1'),NULL,'',\$GLOBALS['LinkCategoryFmt']),'L')");
# This is a temporary workaround for blank category pages.
# It may be removed in a future release (Pm, 2006-01-24)
if (preg_match("/^$CategoryGroup\\./", $pagename)) {
  SDV($DefaultPageTextFmt, '');
  SDV($PageNotFoundHeaderFmt, 'HTTP/1.1 200 Ok');
}

## [[target | text]]
Markup('[[|','<[[',
  "/(?>\\[\\[([^|\\]]*)\\|\\s*)(.*?)\\s*\\]\\]($SuffixPattern)/e",
  "Keep(MakeLink(\$pagename,PSS('$1'),PSS('$2'),'$3'),'L')");

## [[text -> target ]]
Markup('[[->','>[[|',
  "/(?>\\[\\[([^\\]]+?)\\s*-+&gt;\\s*)(.*?)\\]\\]($SuffixPattern)/e",
  "Keep(MakeLink(\$pagename,PSS('$2'),PSS('$1'),'$3'),'L')");

## [[#anchor]]
Markup('[[#','<[[','/(?>\\[\\[#([A-Za-z][-.:\\w]*))\\]\\]/e',
  "Keep(TrackAnchors('$1') ? '' : \"<a name='$1' id='$1'></a>\", 'L')");
function TrackAnchors($x) { global $SeenAnchor; return @$SeenAnchor[$x]++; }

## [[target |#]] reference links
Markup('[[|#', '<[[|',
  "/(?>\\[\\[([^|\\]]+))\\|\\s*#\\s*\\]\\]/e",  
  "Keep(MakeLink(\$pagename,PSS('$1'),'['.++\$MarkupFrame[0]['ref'].']'),'L')");

## [[target |+]] title links
Markup('[[|+', '<[[|',
  "/(?>\\[\\[([^|\\]]+))\\|\\s*\\+\\s*]]/e",
  "Keep(MakeLink(\$pagename, PSS('$1'),
                 PageVar(MakePageName(\$pagename,PSS('$1')), '\$Title')
                ),'L')");

## bare urllinks 
Markup('urllink','>[[',
  "/\\b(?>(\\L))[^\\s$UrlExcludeChars]*[^\\s.,?!$UrlExcludeChars]/e",
  "Keep(MakeLink(\$pagename,'$0','$0'),'L')");

## mailto: links 
Markup('mailto','<urllink',
  "/\\bmailto:([^\\s$UrlExcludeChars]*[^\\s.,?!$UrlExcludeChars])/e",
  "Keep(MakeLink(\$pagename,'$0','$1'),'L')");

## inline images
Markup('img','<urllink',
  "/\\b(?>(\\L))([^\\s$UrlExcludeChars]+$ImgExtPattern)(\"([^\"]*)\")?/e",
  "Keep(\$GLOBALS['LinkFunctions']['$1'](\$pagename,'$1','$2','$4','$1$2',
    \$GLOBALS['ImgTagFmt']),'L')");

## bare wikilinks
Markup('wikilink', '>urllink',
  "/\\b($GroupPattern([\\/.]))?($WikiWordPattern)/e",
  "Keep('<span class=\\'wikiword\\'>'.WikiLink(\$pagename,'$0').'</span>',
        'L')");

## escaped `WikiWords
Markup('`wikiword', '<wikilink',
  "/`(($GroupPattern([\\/.]))?($WikiWordPattern))/e",
  "Keep('$1')");

#### Block markups ####
## Completely blank lines don't do anything.
Markup('blank', '<block', '/^\\s+$/', '');

## process any <:...> markup (after all other block markups)
Markup('^<:','>block','/^(?=\\s*\\S)(<:([^>]+)>)?/e',"Block('$2')");

## unblocked lines w/block markup become anonymous <:block>
Markup('^!<:', '<^<:',
  "/^(?!<:)(?=.*(<\\/?($BlockPattern)\\b)|$KeepToken\\d+B$KeepToken)/",
  '<:block>');

## Lines that begin with displayed images receive their own block.  A
## pipe following the image indicates a "caption" (generates a linebreak).
Markup('^img', 'block',
  "/^((?>(\\s+|%%|%[A-Za-z][-,=:#\\w\\s'\"]*%)*)$KeepToken(\\d+L)$KeepToken)(\\s*\\|\\s?)?(.*)$/e",
  "PSS((strpos(\$GLOBALS['KPV']['$3'],'<img')===false) ? '$0' : 
       '<:block,1><div>$1' . ('$4' ? '<br />' : '') .'$5</div>')");

## Whitespace at the beginning of lines can be used to maintain the
## indent level of a previous list item, or a preformatted text block.
Markup('^ws', '<^img', '/^(\\s+)/e', "WSIndent('$1')");
function WSIndent($i) {
  global $MarkupFrame;
  $icol = strlen($i);
  for($depth = count(@$MarkupFrame[0]['cs']); $depth > 0; $depth--)
    if (@$MarkupFrame[0]['is'][$depth] == $icol) {
      $MarkupFrame[0]['idep'] = $depth;
      $MarkupFrame[0]['icol'] = $icol;
      return '';
    }
  return "<:pre,1>$i";
}

## If the ^ws rule is disabled, then leading whitespace is a
## preformatted text block.
Markup('^ ','block','/^(\\s)/','<:pre,1>$1');

## bullet lists
Markup('^*','block','/^(\\*+)\\s?(\\s*)/','<:ul,$1,$0>$2');

## numbered lists
Markup('^#','block','/^(#+)\\s?(\\s*)/','<:ol,$1,$0>$2');

## indented (->) /hanging indent (-<) text
Markup('^->','block','/^(?>(-+))&gt;\\s?(\\s*)/','<:indent,$1,$1  $2>$2');
Markup('^-<','block','/^(?>(-+))&lt;\\s?(\\s*)/','<:outdent,$1,$1  $2>$2');

## definition lists
Markup('^::','block','/^(:+)(\s*)([^:]+):/','<:dl,$1,$1$2><dt>$2$3</dt><dd>');

## Q: and A:
Markup('^Q:', 'block', '/^Q:(.*)$/', "<:block,1><p class='question'>$1</p>");
Markup('^A:', 'block', '/^A:/', Keep(''));

## tables
## ||cell||, ||!header cell||, ||!caption!||
Markup('^||||', 'block', 
  '/^\\|\\|.*\\|\\|.*$/e',
  "FormatTableRow(PSS('$0'))");
## ||table attributes
Markup('^||','>^||||','/^\\|\\|(.*)$/e',
  "PZZ(\$GLOBALS['BlockMarkups']['table'][0] = PQA(PSS('<table $1>')))
    .'<:block,1>'");

## headings
Markup('^!', 'block',
  '/^(!{1,6})\\s?(.*)$/e',
  "'<:block,1><h'.strlen('$1').PSS('>$2</h').strlen('$1').'>'");

## horiz rule
Markup('^----','>^->','/^----+/','<:block,1><hr />');

#### (:table:) markup (AdvancedTables)

function Cells($name,$attr) {
  global $MarkupFrame;
  $attr = preg_replace('/([a-zA-Z]=)([^\'"]\\S*)/',"\$1'\$2'",$attr);
  $tattr = @$MarkupFrame[0]['tattr'];
  $name = strtolower($name);
  $out = '<:block>';
  if (strncmp($name, 'cell', 4) != 0 || @$MarkupFrame[0]['closeall']['div']) {
    $out .= @$MarkupFrame[0]['closeall']['div']; 
    unset($MarkupFrame[0]['closeall']['div']);
    $out .= @$MarkupFrame[0]['closeall']['table']; 
    unset($MarkupFrame[0]['closeall']['table']);
  }
  if ($name == 'div') {
    $MarkupFrame[0]['closeall']['div'] = "</div>";
    $out .= "<div $attr>";
  }
  if ($name == 'table') $MarkupFrame[0]['tattr'] = $attr;
  if (strncmp($name, 'cell', 4) == 0) {
    if (strpos($attr, "valign=")===false) $attr .= " valign='top'";
    if (!@$MarkupFrame[0]['closeall']['table']) {
       $MarkupFrame[0]['closeall']['table'] = "</td></tr></table>";
       $out .= "<table $tattr><tr><td $attr>";
    } else if ($name == 'cellnr') $out .= "</td></tr><tr><td $attr>";
    else $out .= "</td><td $attr>";
  }
  return $out;
}

Markup('table', '<block',
  '/^\\(:(table|cell|cellnr|tableend|div|divend)(\\s.*?)?:\\)/ie',
  "Cells('$1',PSS('$2'))");
Markup('^>>', '<table',
  '/^&gt;&gt;(.+?)&lt;&lt;(.*)$/',
  '(:div:)%div $1 apply=div%$2 ');
Markup('^>><<', '<^>>',
  '/^&gt;&gt;&lt;&lt;/',
  '(:divend:)');

#### special stuff ####
## (:markup:) for displaying markup examples
function MarkupMarkup($pagename, $text, $opt = '') {
  $MarkupMarkupOpt = array('class' => 'vert');
  $opt = array_merge($MarkupMarkupOpt, ParseArgs($opt));
  $html = MarkupToHTML($pagename, $text, array('escape' => 0));
  if (@$opt['caption']) 
    $caption = str_replace("'", '&#039;', 
                           "<caption>{$opt['caption']}</caption>");
  $class = preg_replace('/[^-\\s\\w]+/', ' ', @$opt['class']);
  if (strpos($class, 'horiz') !== false) 
    { $sep = ''; $pretext = wordwrap($text, 40); } 
  else 
    { $sep = '</tr><tr>'; $pretext = wordwrap($text, 75); }
  return Keep("<table class='markup $class' align='center'>$caption
      <tr><td class='markup1' valign='top'><pre>$pretext</pre></td>$sep<td 
        class='markup2' valign='top'>$html</td></tr></table>");
}

Markup('markup', '<[=',
  "/\\(:markup(\\s+([^\n]*?))?:\\)[^\\S\n]*\\[([=@])(.*?)\\3\\]/sei",
  "MarkupMarkup(\$pagename, PSS('$4'), PSS('$2'))");
Markup('markupend', '>markup',
  "/\\(:markup(\\s+([^\n]*?))?:\\)[^\\S\n]*\n(.*?)\\(:markupend:\\)/sei",
  "MarkupMarkup(\$pagename, PSS('$3'), PSS('$1'))");

$HTMLStylesFmt['markup'] = "
  table.markup { border:2px dotted #ccf; width:90%; }
  td.markup1, td.markup2 { padding-left:10px; padding-right:10px; }
  table.vert td.markup1 { border-bottom:1px solid #ccf; }
  table.horiz td.markup1 { width:23em; border-right:1px solid #ccf; }
  table.markup caption { text-align:left; }
  div.faq p, div.faq pre { margin-left:2em; }
  div.faq p.question { margin:1em 0 0.75em 0; font-weight:bold; }
  ";

#### Special conditions ####
## The code below adds (:if date:) conditions to the markup.
$Conditions['date'] = "CondDate(\$condparm)";

function CondDate($condparm) {
  global $Now;
  NoCache();
  if (!preg_match('/^(.*?)(\\.\\.(.*))?$/', $condparm, $match)) return false;
  if ($match[2]) {
    $t0 = $match[1];  if ($t0 == '') $t0 = '19700101';
    $t1 = $match[3];  if ($t1 == '') $t1 = '20380101';
  } else $t0 = $t1 = $match[1];
  $t0 = preg_replace('/\\D/', '', $t0);
  if (!preg_match('/^(\\d{4})(\\d\\d)(\\d\\d)$/', $t0, $m)) return false;
  $g0 = mktime(0, 0, 0, $m[2], $m[3], $m[1]);
  if ($Now < $g0) return false;

  $t1 = preg_replace('/\\D/', '', $t1);
  $t1++;
  if (!preg_match('/^(\\d{4})(\\d\\d)(\\d\\d)$/', $t1, $m)) return false;
  $g1 = mktime(0, 0, 0, $m[2], $m[3], $m[1]);
  if ($Now >= $g1) return false;
  return true;
}


# This pattern enables the (:encrypt <phrase>:) markup/replace-on-save
# pattern.
SDV($ROSPatterns['/\\(:encrypt\\s+([^\\s:=]+).*?:\\)/e'], "crypt(PSS('$1'))");

