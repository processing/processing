<?php if (!defined('PmWiki')) exit();
/*  Copyright 2005-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script provides a number of syndication feed and xml-based 
    metadata options to PmWiki, including Atom, RSS 2.0, RSS 1.0 (RDF), 
    and the Dublin Core Metadata extensions.  This module is typically
    activated from a local configuration file via a line such as

      if ($action == 'atom') include_once("$FarmD/scripts/feeds.php");
      if ($action == 'dc') include_once("$FarmD/scripts/feeds.php");

    When enabled, ?action=atom, ?action=rss, and ?action=rdf produce
    syndication feeds based on any wikitrail contained in the page,
    or, for Category pages, on the pages in the category.  The feeds
    are generated using pagelist, thus one can include parameters such
    as count=, list=, order=, etc. in the url to adjust the feed output.

    ?action=dc will normally generate Dublin Core Metadata for the 
    current page only, but placing a group=, trail=, or link= argument 
    in the url causes it to generate metadata for all pages in the
    associated group, trail, or backlink.

    There are a large number of customizations available, most of which
    are controlled by the $FeedFmt array.  Elements $FeedFmt look like

        $FeedFmt['atom']['feed']['rights'] = 'All Rights Reserved';

    where the first index corresponds to the action (?action=atom),
    the second index indicates a per-feed or per-item element, and
    the third index is the name of the element being generated.
    The above setting would therefore generate a
    "<rights>All Rights Reserved</rights>" in the feed for
    ?action=atom.  If the value of an entry begins with a '<',
    then feeds.php doesn't automatically add the tag around it.
    Elements can also be callable functions which are called to
    generate the appropriate output.

    For example, to set the RSS 2.0 <author> element to the
    value of the last author to modify a page, one can set 
    (in local/config.php):

        $FeedFmt['rss']['item']['author'] = '$LastModifiedBy';

    To use the RSS 2.0 <description> element to contain the
    change summary of the most recent edit, set

        $FeedFmt['rss']['item']['description'] = '$LastModifiedSummary';

    Feeds.php can also be combined with attachments to support
    podcasting via ?action=rss.  Any page such as "PageName"
    that has an mp3 attachment with the same name as the page
    ("PageName.mp3") will have an appropriate <enclosure> element
    in the feed output.  The set of allowed attachments can be
    extended using the $RSSEnclosureFmt array:

        $RSSEnclosureFmt = array('{$Name}.mp3', '{$Name}.mp4');

    References:
      http://www.atomenabled.org/developers/syndication/
      http://dublincore.org/documents/dcmes-xml/
      http://en.wikipedia.org/wiki/Podcasting
*/

## Settings for ?action=atom
SDVA($FeedFmt['atom']['feed'], array(
  '_header' => 'Content-type: text/xml; charset="$Charset"',
  '_start' => '<?xml version="1.0" encoding="$Charset"?'.'>
<feed xmlns="http://www.w3.org/2005/Atom">'."\n",
  '_end' => "</feed>\n",
  'title' => '$WikiTitle',
  'link' => '<link rel="self" href="{$PageUrl}?action=atom" />',
  'id' => '{$PageUrl}?action=atom',
  'updated' => '$FeedISOTime',
  'author' => "<author><name>$WikiTitle</name></author>\n",
  'generator' => '$Version',
  'logo' => '$PageLogoUrl'));
SDVA($FeedFmt['atom']['item'], array(
  '_start' => "<entry>\n",
  'id' => '{$PageUrl}',
  'title' => '{$Title}',
  'updated' => '$ItemISOTime',
  'link' => "<link rel=\"alternate\" href=\"{\$PageUrl}\" />\n",
  'author' => "<author><name>{\$LastModifiedBy}</name></author>\n",
  'summary' => '{$Description}',
  'category' => "<category term=\"\$Category\" />\n",
  '_end' => "</entry>\n"));

## Settings for ?action=dc
SDVA($FeedFmt['dc']['feed'], array(
  '_header' => 'Content-type: text/xml; charset="$Charset"',
  '_start' => '<?xml version="1.0" encoding="$Charset"?'.'>
<!DOCTYPE rdf:RDF PUBLIC "-//DUBLIN CORE//DCMES DTD 2002/07/31//EN"
    "http://dublincore.org/documents/2002/07/31/dcmes-xml/dcmes-xml-dtd.dtd">
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:dc="http://purl.org/dc/elements/1.1/">'."\n",
  '_end' => "</rdf:RDF>\n"));
SDVA($FeedFmt['dc']['item'], array(
  '_start' => "<rdf:Description rdf:about=\"{\$PageUrl}\">\n",
  'dc:title' => '{$Title}',
  'dc:identifier' => '{$PageUrl}',
  'dc:date' => '$ItemISOTime',
  'dc:type' => 'Text',
  'dc:format' => 'text/html',
  'dc:description' => '{$Description}',
  'dc:subject' => "<dc:subject>\$Category</dc:subject>\n",
  'dc:publisher' => '$WikiTitle',
  'dc:author' => '{$LastModifiedBy}',
  '_end' => "</rdf:Description>\n"));

## RSS 2.0 settings for ?action=rss
SDVA($FeedFmt['rss']['feed'], array(
  '_header' => 'Content-type: text/xml; charset="$Charset"',
  '_start' => '<?xml version="1.0" encoding="$Charset"?'.'>
<rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/">
<channel>'."\n",
  '_end' => "</channel>\n</rss>\n",
  'title' => '$WikiTitle | {$Group} / {$Title}',
  'link' => '{$PageUrl}?action=rss',
  'description' => '{$Group}.{$Title}',
  'lastBuildDate' => '$FeedRSSTime'));
SDVA($FeedFmt['rss']['item'], array(
  '_start' => "<item>\n",
  '_end' => "</item>\n",
  'title' => '{$Group} / {$Title}',
  'link' => '{$PageUrl}',
  'description' => '{$Description}',
  'dc:contributor' => '{$LastModifiedBy}',
  'dc:date' => '$ItemISOTime',
  'pubDate' => '$ItemRSSTime',
  'enclosure' => 'RSSEnclosure'));

## RDF 1.0, for ?action=rdf
SDVA($FeedFmt['rdf']['feed'], array(
  '_header' => 'Content-type: text/xml; charset="$Charset"',
  '_start' => '<?xml version="1.0" encoding="$Charset"?'.'>
<rdf:RDF xmlns="http://purl.org/rss/1.0/"
         xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:dc="http://purl.org/dc/elements/1.1/">
  <channel rdf:about="{$PageUrl}?action=rdf">'."\n",
  'title' => '$WikiTitle | {$Group} / {$Title}',
  'link' => '{$PageUrl}?action=rdf',
  'description' => '{$Group}.{$Title}',
  'dc:date' => '$FeedISOTime',
  'items' => "<items>\n<rdf:Seq>\n\$FeedRDFSeq</rdf:Seq>\n</items>\n",
  '_items' => "</channel>\n",
  '_end' => "</rdf:RDF>\n"));
SDVA($FeedFmt['rdf']['item'], array(
  '_start' => "<item rdf:about=\"{\$PageUrl}\">\n",
  '_end' => "</item>\n",
  'title' => '$WikiTitle | {$Group} / {$Title}',
  'link' => '{$PageUrl}',
  'description' => '{$Description}',
  'dc:date' => '$ItemISOTime'));
  
foreach(array_keys($FeedFmt) as $k) {
  SDV($HandleActions[$k], 'HandleFeed');
  SDV($HandleAuth[$k], 'read');
}

function HandleFeed($pagename, $auth = 'read') {
  global $FeedFmt, $action, $PCache, $FmtV, $ISOTimeFmt, $RSSTimeFmt,
    $FeedOpt, $FeedDescPatterns, $CategoryGroup, $EntitiesTable;
  SDV($ISOTimeFmt, '%Y-%m-%dT%H:%M:%SZ');
  SDV($RSSTimeFmt, 'D, d M Y H:i:s \G\M\T');
  SDV($FeedDescPatterns, 
    array('/<[^>]*$/' => ' ', '/\\w+$/' => '', '/<[^>]+>/' => ''));
  SDVA($FeedPageListOpt, array());
  SDVA($FeedCategoryOpt, array('link' => $pagename));
  SDVA($FeedTrailOpt, array('trail' => $pagename, 'count' => 10));

  $f = $FeedFmt[$action];
  $page = RetrieveAuthPage($pagename, $auth, true, READPAGE_CURRENT);
  if (!$page) Abort("?cannot generate feed");
  $feedtime = $page['time'];

  # determine list of pages to display
  if (@($_REQUEST['trail'] || $_REQUEST['group'] || $_REQUEST['link'] 
        || $_REQUEST['name'])) 
    $opt = $FeedPageListOpt;
  else if (preg_match("/^$CategoryGroup\\./", $pagename)) 
    $opt = $FeedCategoryOpt;
  else if ($action != 'dc') $opt = $FeedTrailOpt;
  else { 
    PCache($pagename, $page); 
    $pagelist = array($pagename); 
  }
  if (!$pagelist) {
    $opt = array_merge($opt, @$_REQUEST);
    $pagelist = MakePageList($pagename, $opt, 0);
  }

  # process list of pages in feed
  $rdfseq = '';
  $pl = array();
  foreach($pagelist as $pn) {
    if (!PageExists($pn)) continue;
    if (!isset($PCache[$pn]['time']))
      PCache($pn, ReadPage($pn, READPAGE_CURRENT));
    $page = & $PCache[$pn];
    $pl[] = $pn;
    if (@$opt['count'] && count($pl) >= $opt['count']) break;
    $rdfseq .= FmtPageName("<rdf:li resource=\"{\$PageUrl}\" />\n", $pn);
    if ($page['time'] > $feedtime) $feedtime = $page['time'];
  }
  $pagelist = $pl;

  $FmtV['$FeedRDFSeq'] = $rdfseq;
  $FmtV['$FeedISOTime'] = gmstrftime($ISOTimeFmt, $feedtime);
  $FmtV['$FeedRSSTime'] = gmdate($RSSTimeFmt, $feedtime);
  # format start of feed
  $out = FmtPageName($f['feed']['_start'], $pagename);

  # format feed elements
  foreach($f['feed'] as $k => $v) {
    if ($k{0} == '_' || !$v) continue;
    $x = FmtPageName($v, $pagename);
    if (!$x) continue;
    $out .= ($v{0} == '<') ? $x : "<$k>$x</$k>\n";
  }

  # format items in feed
  if (@$f['feed']['_items']) 
    $out .= FmtPageName($f['feed']['_items'], $pagename);
  foreach($pagelist as $pn) {
    $page = &$PCache[$pn];
    $FmtV['$ItemDesc'] = @$page['description'];
    $FmtV['$ItemISOTime'] = gmstrftime($ISOTimeFmt, $page['time']);
    $FmtV['$ItemRSSTime'] = gmdate($RSSTimeFmt, $page['time']);

    $out .= FmtPageName($f['item']['_start'], $pn);
    foreach((array)@$f['item'] as $k => $v) {
      if ($k{0} == '_' || !$v) continue;
      if (is_callable($v)) { $out .= $v($pn, $page, $k); continue; }
      if (strpos($v, '$LastModifiedBy') !== false && !@$page['author']) 
        continue;
      if (strpos($v, '$Category') !== false) {
        if (preg_match_all("/(?<=^|,)$CategoryGroup\\.([^,]+)/", 
                           @$page['targets'], $match)) {
          foreach($match[1] as $c) {
            $FmtV['$Category'] = $c;
            $out .= FmtPageName($v, $pn);
          }
        }
        continue;
      }
      $x = FmtPageName($v, $pn);
      if (!$x) continue;
      $out .= ($v{0} == '<') ? $x : "<$k>$x</$k>\n";
    }
    $out .= FmtPageName($f['item']['_end'], $pn);
  } 
  $out .= FmtPageName($f['feed']['_end'], $pagename);
  foreach((array)@$f['feed']['_header'] as $fmt)
    header(FmtPageName($fmt, $pagename));
  print str_replace(array_keys($EntitiesTable),
                    array_values($EntitiesTable), $out);
}

## RSSEnclosure is called in ?action=rss to generate <enclosure>
## tags for any pages that have an attached "PageName.mp3" file.
## The set of attachments to enclose is given by $RSSEnclosureFmt.
function RSSEnclosure($pagename, &$page, $k) {
  global $RSSEnclosureFmt, $UploadFileFmt, $UploadExts;
  if (!function_exists('MakeUploadName')) return '';
  SDV($RSSEnclosureFmt, array('{$Name}.mp3'));
  $encl = '';
  foreach((array)$RSSEnclosureFmt as $fmt) {
    $path = FmtPageName($fmt, $pagename);
    $upname = MakeUploadName($pagename, $path);
    $filepath = FmtPageName("$UploadFileFmt/$upname", $pagename);
    if (file_exists($filepath)) {
      $length = filesize($filepath);
      $type = @$UploadExts[preg_replace('/.*\\./', '', $filepath)];
      $url = LinkUpload($pagename, 'Attach:', $path, '', '', '$LinkUrl');
      $encl .= "<$k url='$url' length='$length' type='$type' />";
    }
  }
  return $encl;
}

## Since most feeds don't understand html character entities, we
## convert the common ones to their numeric form here.
SDVA($EntitiesTable, array(
  # entities defined in "http://www.w3.org/TR/xhtml1/DTD/xhtml-lat1.ent"
  '&nbsp;' => '&#160;', 
  '&iexcl;' => '&#161;', 
  '&cent;' => '&#162;', 
  '&pound;' => '&#163;', 
  '&curren;' => '&#164;', 
  '&yen;' => '&#165;', 
  '&brvbar;' => '&#166;', 
  '&sect;' => '&#167;', 
  '&uml;' => '&#168;', 
  '&copy;' => '&#169;', 
  '&ordf;' => '&#170;', 
  '&laquo;' => '&#171;', 
  '&not;' => '&#172;', 
  '&shy;' => '&#173;', 
  '&reg;' => '&#174;', 
  '&macr;' => '&#175;', 
  '&deg;' => '&#176;', 
  '&plusmn;' => '&#177;', 
  '&sup2;' => '&#178;', 
  '&sup3;' => '&#179;', 
  '&acute;' => '&#180;', 
  '&micro;' => '&#181;', 
  '&para;' => '&#182;', 
  '&middot;' => '&#183;', 
  '&cedil;' => '&#184;', 
  '&sup1;' => '&#185;', 
  '&ordm;' => '&#186;', 
  '&raquo;' => '&#187;', 
  '&frac14;' => '&#188;', 
  '&frac12;' => '&#189;', 
  '&frac34;' => '&#190;', 
  '&iquest;' => '&#191;', 
  '&Agrave;' => '&#192;', 
  '&Aacute;' => '&#193;', 
  '&Acirc;' => '&#194;', 
  '&Atilde;' => '&#195;', 
  '&Auml;' => '&#196;', 
  '&Aring;' => '&#197;', 
  '&AElig;' => '&#198;', 
  '&Ccedil;' => '&#199;', 
  '&Egrave;' => '&#200;', 
  '&Eacute;' => '&#201;', 
  '&Ecirc;' => '&#202;', 
  '&Euml;' => '&#203;', 
  '&Igrave;' => '&#204;', 
  '&Iacute;' => '&#205;', 
  '&Icirc;' => '&#206;', 
  '&Iuml;' => '&#207;', 
  '&ETH;' => '&#208;', 
  '&Ntilde;' => '&#209;', 
  '&Ograve;' => '&#210;', 
  '&Oacute;' => '&#211;', 
  '&Ocirc;' => '&#212;', 
  '&Otilde;' => '&#213;', 
  '&Ouml;' => '&#214;', 
  '&times;' => '&#215;', 
  '&Oslash;' => '&#216;', 
  '&Ugrave;' => '&#217;', 
  '&Uacute;' => '&#218;', 
  '&Ucirc;' => '&#219;', 
  '&Uuml;' => '&#220;', 
  '&Yacute;' => '&#221;', 
  '&THORN;' => '&#222;', 
  '&szlig;' => '&#223;', 
  '&agrave;' => '&#224;', 
  '&aacute;' => '&#225;', 
  '&acirc;' => '&#226;', 
  '&atilde;' => '&#227;', 
  '&auml;' => '&#228;', 
  '&aring;' => '&#229;', 
  '&aelig;' => '&#230;', 
  '&ccedil;' => '&#231;', 
  '&egrave;' => '&#232;', 
  '&eacute;' => '&#233;', 
  '&ecirc;' => '&#234;', 
  '&euml;' => '&#235;', 
  '&igrave;' => '&#236;', 
  '&iacute;' => '&#237;', 
  '&icirc;' => '&#238;', 
  '&iuml;' => '&#239;', 
  '&eth;' => '&#240;', 
  '&ntilde;' => '&#241;', 
  '&ograve;' => '&#242;', 
  '&oacute;' => '&#243;', 
  '&ocirc;' => '&#244;', 
  '&otilde;' => '&#245;', 
  '&ouml;' => '&#246;', 
  '&divide;' => '&#247;', 
  '&oslash;' => '&#248;', 
  '&ugrave;' => '&#249;', 
  '&uacute;' => '&#250;', 
  '&ucirc;' => '&#251;', 
  '&uuml;' => '&#252;', 
  '&yacute;' => '&#253;', 
  '&thorn;' => '&#254;', 
  '&yuml;' => '&#255;', 
  # entities defined in "http://www.w3.org/TR/xhtml1/DTD/xhtml-special.ent"
  '&quot;' => '&#34;', 
  #'&amp;' => '&#38;#38;', 
  #'&lt;' => '&#38;#60;', 
  #'&gt;' => '&#62;', 
  '&apos;' => '&#39;', 
  '&OElig;' => '&#338;', 
  '&oelig;' => '&#339;', 
  '&Scaron;' => '&#352;', 
  '&scaron;' => '&#353;', 
  '&Yuml;' => '&#376;', 
  '&circ;' => '&#710;', 
  '&tilde;' => '&#732;', 
  '&ensp;' => '&#8194;', 
  '&emsp;' => '&#8195;', 
  '&thinsp;' => '&#8201;', 
  '&zwnj;' => '&#8204;', 
  '&zwj;' => '&#8205;', 
  '&lrm;' => '&#8206;', 
  '&rlm;' => '&#8207;', 
  '&ndash;' => '&#8211;', 
  '&mdash;' => '&#8212;', 
  '&lsquo;' => '&#8216;', 
  '&rsquo;' => '&#8217;', 
  '&sbquo;' => '&#8218;', 
  '&ldquo;' => '&#8220;', 
  '&rdquo;' => '&#8221;', 
  '&bdquo;' => '&#8222;', 
  '&dagger;' => '&#8224;', 
  '&Dagger;' => '&#8225;', 
  '&permil;' => '&#8240;', 
  '&lsaquo;' => '&#8249;', 
  '&rsaquo;' => '&#8250;', 
  '&euro;' => '&#8364;', 
  # entities defined in "http://www.w3.org/TR/xhtml1/DTD/xhtml-symbol.ent"
  '&fnof;' => '&#402;', 
  '&Alpha;' => '&#913;', 
  '&Beta;' => '&#914;', 
  '&Gamma;' => '&#915;', 
  '&Delta;' => '&#916;', 
  '&Epsilon;' => '&#917;', 
  '&Zeta;' => '&#918;', 
  '&Eta;' => '&#919;', 
  '&Theta;' => '&#920;', 
  '&Iota;' => '&#921;', 
  '&Kappa;' => '&#922;', 
  '&Lambda;' => '&#923;', 
  '&Mu;' => '&#924;', 
  '&Nu;' => '&#925;', 
  '&Xi;' => '&#926;', 
  '&Omicron;' => '&#927;', 
  '&Pi;' => '&#928;', 
  '&Rho;' => '&#929;', 
  '&Sigma;' => '&#931;', 
  '&Tau;' => '&#932;', 
  '&Upsilon;' => '&#933;', 
  '&Phi;' => '&#934;', 
  '&Chi;' => '&#935;', 
  '&Psi;' => '&#936;', 
  '&Omega;' => '&#937;', 
  '&alpha;' => '&#945;', 
  '&beta;' => '&#946;', 
  '&gamma;' => '&#947;', 
  '&delta;' => '&#948;', 
  '&epsilon;' => '&#949;', 
  '&zeta;' => '&#950;', 
  '&eta;' => '&#951;', 
  '&theta;' => '&#952;', 
  '&iota;' => '&#953;', 
  '&kappa;' => '&#954;', 
  '&lambda;' => '&#955;', 
  '&mu;' => '&#956;', 
  '&nu;' => '&#957;', 
  '&xi;' => '&#958;', 
  '&omicron;' => '&#959;', 
  '&pi;' => '&#960;', 
  '&rho;' => '&#961;', 
  '&sigmaf;' => '&#962;', 
  '&sigma;' => '&#963;', 
  '&tau;' => '&#964;', 
  '&upsilon;' => '&#965;', 
  '&phi;' => '&#966;', 
  '&chi;' => '&#967;', 
  '&psi;' => '&#968;', 
  '&omega;' => '&#969;', 
  '&thetasym;' => '&#977;', 
  '&upsih;' => '&#978;', 
  '&piv;' => '&#982;', 
  '&bull;' => '&#8226;', 
  '&hellip;' => '&#8230;', 
  '&prime;' => '&#8242;', 
  '&Prime;' => '&#8243;', 
  '&oline;' => '&#8254;', 
  '&frasl;' => '&#8260;', 
  '&weierp;' => '&#8472;', 
  '&image;' => '&#8465;', 
  '&real;' => '&#8476;', 
  '&trade;' => '&#8482;', 
  '&alefsym;' => '&#8501;', 
  '&larr;' => '&#8592;', 
  '&uarr;' => '&#8593;', 
  '&rarr;' => '&#8594;', 
  '&darr;' => '&#8595;', 
  '&harr;' => '&#8596;', 
  '&crarr;' => '&#8629;', 
  '&lArr;' => '&#8656;', 
  '&uArr;' => '&#8657;', 
  '&rArr;' => '&#8658;', 
  '&dArr;' => '&#8659;', 
  '&hArr;' => '&#8660;', 
  '&forall;' => '&#8704;', 
  '&part;' => '&#8706;', 
  '&exist;' => '&#8707;', 
  '&empty;' => '&#8709;', 
  '&nabla;' => '&#8711;', 
  '&isin;' => '&#8712;', 
  '&notin;' => '&#8713;', 
  '&ni;' => '&#8715;', 
  '&prod;' => '&#8719;', 
  '&sum;' => '&#8721;', 
  '&minus;' => '&#8722;', 
  '&lowast;' => '&#8727;', 
  '&radic;' => '&#8730;', 
  '&prop;' => '&#8733;', 
  '&infin;' => '&#8734;', 
  '&ang;' => '&#8736;', 
  '&and;' => '&#8743;', 
  '&or;' => '&#8744;', 
  '&cap;' => '&#8745;', 
  '&cup;' => '&#8746;', 
  '&int;' => '&#8747;', 
  '&there4;' => '&#8756;', 
  '&sim;' => '&#8764;', 
  '&cong;' => '&#8773;', 
  '&asymp;' => '&#8776;', 
  '&ne;' => '&#8800;', 
  '&equiv;' => '&#8801;', 
  '&le;' => '&#8804;', 
  '&ge;' => '&#8805;', 
  '&sub;' => '&#8834;', 
  '&sup;' => '&#8835;', 
  '&nsub;' => '&#8836;', 
  '&sube;' => '&#8838;', 
  '&supe;' => '&#8839;', 
  '&oplus;' => '&#8853;', 
  '&otimes;' => '&#8855;', 
  '&perp;' => '&#8869;', 
  '&sdot;' => '&#8901;', 
  '&lceil;' => '&#8968;', 
  '&rceil;' => '&#8969;', 
  '&lfloor;' => '&#8970;', 
  '&rfloor;' => '&#8971;', 
  '&lang;' => '&#9001;', 
  '&rang;' => '&#9002;', 
  '&loz;' => '&#9674;', 
  '&spades;' => '&#9824;', 
  '&clubs;' => '&#9827;', 
  '&hearts;' => '&#9829;', 
  '&diams;' => '&#9830;'));

