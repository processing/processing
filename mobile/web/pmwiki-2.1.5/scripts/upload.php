<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script adds upload capabilities to PmWiki.  Uploads can be
    enabled by setting
        $EnableUpload = 1;
    in config.php.  In addition, an upload password must be set, as
    the default is to lock uploads.  In some configurations it may also
    be necessary to set values for $UploadDir and $UploadUrlFmt,
    especially if any form of URL rewriting is being performed.
    See the PmWiki.UploadsAdmin page for more information.
*/

## $EnableUploadOverwrite determines if we allow previously uploaded
## files to be overwritten.
SDV($EnableUploadOverwrite,1);

## $UploadExts contains the list of file extensions we're willing to
## accept, along with the Content-Type: value appropriate for each.
SDVA($UploadExts,array(
  'gif' => 'image/gif', 'jpg' => 'image/jpeg', 'jpeg' => 'image/jpeg',
  'png' => 'image/png', 'bmp' => 'image/bmp', 'ico' => 'image/x-icon',
  'wbmp' => 'image/vnd.wap.wbmp',
  'mp3' => 'audio/mpeg', 'au' => 'audio/basic', 'wav' => 'audio/x-wav',
  'mpg' => 'video/mpeg', 'mpeg' => 'video/mpeg',
  'mov' => 'video/quicktime', 'qt' => 'video/quicktime',
  'wmf' => 'text/plain', 'avi' => 'video/x-msvideo',
  'zip' => 'application/zip', 
  'gz' => 'application/x-gzip', 'tgz' => 'application/x-gzip',
  'rpm' => 'application/x-rpm', 
  'hqx' => 'application/mac-binhex40', 'sit' => 'application/x-stuffit',
  'doc' => 'application/msword', 'ppt' => 'application/vnd.ms-powerpoint',
  'xls' => 'application/vnd.ms-excel', 'mdb' => 'text/plain',
  'exe' => 'application/octet-stream',
  'pdf' => 'application/pdf', 'psd' => 'text/plain', 
  'ps' => 'application/postscript', 'ai' => 'application/postscript',
  'eps' => 'application/postscript',
  'htm' => 'text/html', 'html' => 'text/html', 'css' => 'text/css', 
  'fla' => 'application/x-shockwave-flash', 
  'swf' => 'application/x-shockwave-flash',
  'txt' => 'text/plain', 'rtf' => 'application/rtf', 
  'tex' => 'application/x-tex', 'dvi' => 'application/x-dvi',
  '' => 'text/plain'));

SDV($UploadMaxSize,50000);
SDV($UploadPrefixQuota,0);
SDV($UploadDirQuota,0);
foreach($UploadExts as $k=>$v) 
  if (!isset($UploadExtSize[$k])) $UploadExtSize[$k]=$UploadMaxSize;

SDV($UploadDir,'uploads');
SDV($UploadPrefixFmt,'/$Group');
SDV($UploadFileFmt,"$UploadDir$UploadPrefixFmt");
$v = preg_replace('#^/(.*/)#', '', $UploadDir);
SDV($UploadUrlFmt,preg_replace('#/[^/]*$#', "/$v", $PubDirUrl, 1));
SDV($LinkUploadCreateFmt, "<a class='createlinktext' href='\$LinkUpload'>\$LinkText</a><a class='createlink' href='\$LinkUpload'>&nbsp;&Delta;</a>");

SDV($PageUploadFmt,array("
  <div id='wikiupload'>
  <h2 class='wikiaction'>$[Attachments for] {\$FullName}</h2>
  <h3>\$UploadResult</h3>
  <form enctype='multipart/form-data' action='{\$PageUrl}' method='post'>
  <input type='hidden' name='n' value='{\$FullName}' />
  <input type='hidden' name='action' value='postupload' />
  <table border='0'>
    <tr><td align='right'>$[File to upload:]</td><td><input
      name='uploadfile' type='file' /></td></tr>
    <tr><td align='right'>$[Name attachment as:]</td>
      <td><input type='text' name='upname' value='\$UploadName' /><input 
        type='submit' value=' $[Upload] ' /><br />
        </td></tr></table></form></div>",
  'wiki:$[{$SiteGroup}/UploadQuickReference]'));
XLSDV('en',array(
  'ULsuccess' => 'successfully uploaded',
  'ULbadname' => 'invalid attachment name',
  'ULbadtype' => '\'$upext\' is not an allowed file extension',
  'ULtoobig' => 'file is larger than maximum allowed by webserver',
  'ULtoobigext' => 'file is larger than allowed maximum of $upmax
     bytes for \'$upext\' files',
  'ULpartial' => 'incomplete file received',
  'ULnofile' => 'no file uploaded',
  'ULexists' => 'file with that name already exists',
  'ULpquota' => 'group quota exceeded',
  'ULtquota' => 'upload quota exceeded'));
SDV($PageAttributes['passwdupload'],'$[Set new upload password:]');
SDV($DefaultPasswords['upload'],'*');
SDV($AuthCascade['upload'], 'read');

Markup('attachlist', '<block', 
  '/\\(:attachlist\\s*(.*?):\\)/ei',
  "Keep('<ul>'.FmtUploadList('$pagename',PSS('$1')).'</ul>')");
SDV($GUIButtons['attach'], array(220, 'Attach:', '', '$[file.ext]',
  '$GUIButtonDirUrlFmt/attach.gif"$[Attach file]"'));
SDV($LinkFunctions['Attach:'], 'LinkUpload');
SDV($IMap['Attach:'], '$1');
SDVA($HandleActions, array('upload' => 'HandleUpload',
  'postupload' => 'HandlePostUpload',
  'download' => 'HandleDownload'));
SDVA($HandleAuth, array('upload' => 'upload',
  'postupload' => 'upload',
  'download' => 'read'));
SDV($UploadVerifyFunction, 'UploadVerifyBasic');

function MakeUploadName($pagename,$x) {
  global $UploadNameChars;
  SDV($UploadNameChars, "-\\w. ");
  $x = preg_replace("/[^$UploadNameChars]/", '', $x);
  $x = preg_replace('/\\.[^.]*$/e', "strtolower('$0')", $x);
  $x = preg_replace('/^[^[:alnum:]]+/', '', $x);
  return preg_replace('/[^[:alnum:]_]+$/', '', $x);
}

function LinkUpload($pagename, $imap, $path, $title, $txt, $fmt=NULL) {
  global $FmtV, $UploadFileFmt, $LinkUploadCreateFmt, $UploadUrlFmt,
    $UploadPrefixFmt, $EnableDirectDownload;
  if (preg_match('!^(.*)/([^/]+)$!', $path, $match)) {
    $pagename = MakePageName($pagename, $match[1]);
    $path = $match[2];
  }
  $upname = MakeUploadName($pagename, $path);
  $filepath = FmtPageName("$UploadFileFmt/$upname", $pagename);
  $FmtV['$LinkUpload'] = 
    FmtPageName("\$PageUrl?action=upload&amp;upname=$upname", $pagename);
  $FmtV['$LinkText'] = $txt;
  if (!file_exists($filepath)) 
    return FmtPageName($LinkUploadCreateFmt, $pagename);
  $path = PUE(FmtPageName(IsEnabled($EnableDirectDownload, 1) 
                            ? "$UploadUrlFmt$UploadPrefixFmt/$upname"
                            : "{\$PageUrl}?action=download&amp;upname=$upname",
                          $pagename));
  return LinkIMap($pagename, $imap, $path, $title, $txt, $fmt);
}

function HandleUpload($pagename, $auth = 'upload') {
  global $FmtV,$UploadExtMax,
    $HandleUploadFmt,$PageStartFmt,$PageEndFmt,$PageUploadFmt;
  $page = RetrieveAuthPage($pagename, $auth, true, READPAGE_CURRENT);
  if (!$page) Abort("?cannot upload to $pagename");
  PCache($pagename,$page);
  $FmtV['$UploadName'] = MakeUploadName($pagename,@$_REQUEST['upname']);
  $upresult = @$_REQUEST['upresult'];
  $uprname = @$_REQUEST['uprname'];
  $FmtV['$upext'] = @$_REQUEST['upext'];
  $FmtV['$upmax'] = @$_REQUEST['upmax'];
  $FmtV['$UploadResult'] = ($upresult) ?
    FmtPageName("<i>$uprname</i>: $[UL$upresult]",$pagename) : '';
  SDV($HandleUploadFmt,array(&$PageStartFmt,&$PageUploadFmt,&$PageEndFmt));
  PrintFmt($pagename,$HandleUploadFmt);
}

function HandleDownload($pagename, $auth = 'read') {
  global $UploadFileFmt, $UploadExts, $DownloadDisposition;
  SDV($DownloadDisposition, "inline");
  $page = RetrieveAuthPage($pagename, $auth, true, READPAGE_CURRENT);
  if (!$page) Abort("?cannot read $pagename");
  $upname = MakeUploadName($pagename, @$_REQUEST['upname']);
  $filepath = FmtPageName("$UploadFileFmt/$upname", $pagename);
  if (!$upname || !file_exists($filepath)) {
    header("HTTP/1.0 404 Not Found");
    Abort("?requested file not found");
    exit();
  }
  preg_match('/\\.([^.]+)$/',$filepath,$match); 
  if ($UploadExts[@$match[1]]) 
    header("Content-Type: {$UploadExts[@$match[1]]}");
  header("Content-Length: ".filesize($filepath));
  header("Content-disposition: $DownloadDisposition; filename=$upname");
  $fp = fopen($filepath, "r");
  if ($fp) {
    while (!feof($fp)) echo fread($fp, 4096);
    fclose($fp);
  }
  exit();
}  
 
function HandlePostUpload($pagename, $auth = 'upload') {
  global $UploadVerifyFunction, $UploadFileFmt, $LastModFile, 
    $EnableUploadVersions, $Now;
  $page = RetrieveAuthPage($pagename, $auth, true, READPAGE_CURRENT);
  if (!$page) Abort("?cannot upload to $pagename");
  $uploadfile = $_FILES['uploadfile'];
  $upname = $_REQUEST['upname'];
  if ($upname=='') $upname=$uploadfile['name'];
  $upname = MakeUploadName($pagename,$upname);
  if (!function_exists($UploadVerifyFunction))
    Abort('?no UploadVerifyFunction available');
  $filepath = FmtPageName("$UploadFileFmt/$upname",$pagename);
  $result = $UploadVerifyFunction($pagename,$uploadfile,$filepath);
  if ($result=='') {
    $filedir = preg_replace('#/[^/]*$#','',$filepath);
    mkdirp($filedir);
    if (IsEnabled($EnableUploadVersions, 0))
      @rename($filepath, "$filepath,$Now");
    if (!move_uploaded_file($uploadfile['tmp_name'],$filepath))
      { Abort("?cannot move uploaded file to $filepath"); return; }
    fixperms($filepath,0444);
    if ($LastModFile) { touch($LastModFile); fixperms($LastModFile); }
    $result = "upresult=success";
  }
  Redirect($pagename,"{\$PageUrl}?action=upload&uprname=$upname&$result");
}

function UploadVerifyBasic($pagename,$uploadfile,$filepath) {
  global $EnableUploadOverwrite,$UploadExtSize,$UploadPrefixQuota,
    $UploadDirQuota,$UploadDir;
  if (!$EnableUploadOverwrite && file_exists($filepath)) 
    return 'upresult=exists';
  preg_match('/\\.([^.\\/]+)$/',$filepath,$match); $ext=@$match[1];
  $maxsize = $UploadExtSize[$ext];
  if ($maxsize<=0) return "upresult=badtype&upext=$ext";
  if ($uploadfile['size']>$maxsize) 
    return "upresult=toobigext&upext=$ext&upmax=$maxsize";
  switch (@$uploadfile['error']) {
    case 1: return 'upresult=toobig';
    case 2: return 'upresult=toobig';
    case 3: return 'upresult=partial';
    case 4: return 'upresult=nofile';
  }
  if (!is_uploaded_file($uploadfile['tmp_name'])) return 'upresult=nofile';
  $filedir = preg_replace('#/[^/]*$#','',$filepath);
  if ($UploadPrefixQuota && 
      (dirsize($filedir)-@filesize($filepath)+$uploadfile['size']) >
        $UploadPrefixQuota) return 'upresult=pquota';
  if ($UploadDirQuota && 
      (dirsize($UploadDir)-@filesize($filepath)+$uploadfile['size']) >
        $UploadDirQuota) return 'upresult=tquota';
  return '';
}

function dirsize($dir) {
  $size = 0;
  $dirp = @opendir($dir);
  if (!$dirp) return 0;
  while (($file=readdir($dirp)) !== false) {
    if ($file[0]=='.') continue;
    if (is_dir("$dir/$file")) $size+=dirsize("$dir/$file");
    else $size+=filesize("$dir/$file");
  }
  closedir($dirp);
  return $size;
}

function FmtUploadList($pagename, $args) {
  global $UploadDir, $UploadPrefixFmt, $UploadUrlFmt, $EnableUploadOverwrite,
    $TimeFmt, $EnableDirectDownload;

  $opt = ParseArgs($args);
  if (@$opt[''][0]) $pagename = MakePageName($pagename, $opt[''][0]);
  if (@$opt['ext']) 
    $matchext = '/\\.(' 
      . implode('|', preg_split('/\\W+/', $opt['ext'], -1, PREG_SPLIT_NO_EMPTY))
      . ')$/i';

  $uploaddir = FmtPageName("$UploadDir$UploadPrefixFmt", $pagename);
  $uploadurl = FmtPageName(IsEnabled($EnableDirectDownload, 1) 
                          ? "$UploadUrlFmt$UploadPrefixFmt/"
                          : "\$PageUrl?action=download&amp;upname=",
                      $pagename);

  $dirp = @opendir($uploaddir);
  if (!$dirp) return '';
  $filelist = array();
  while (($file=readdir($dirp)) !== false) {
    if ($file{0} == '.') continue;
    if (@$matchext && !preg_match(@$matchext, $file)) continue;
    $filelist[$file] = $file;
  }
  closedir($dirp);
  $out = array();
  natcasesort($filelist);
  $overwrite = '';
  foreach($filelist as $file=>$x) {
    $name = PUE("$uploadurl$file");
    $stat = stat("$uploaddir/$file");
    if ($EnableUploadOverwrite) 
      $overwrite = FmtPageName("<a class='createlink'
        href='\$PageUrl?action=upload&amp;upname=$file'>&nbsp;&Delta;</a>", 
        $pagename);
    $out[] = "<li> <a href='$name'>$file</a>$overwrite ... ".
      number_format($stat['size']) . " bytes ... " . 
      strftime($TimeFmt, $stat['mtime']) . "</li>";
  }
  return implode("\n",$out);
}

# this adds (:if [!]attachments:) to the markup
$Conditions['attachments'] = "AttachExist(\$pagename)";
function AttachExist($pagename) {
  global $UploadDir, $UploadPrefixFmt;
  $uploaddir = FmtPageName("$UploadDir$UploadPrefixFmt", $pagename);
  $count = 0;
  $dirp = @opendir($uploaddir);
  if ($dirp) {
    while (($file = readdir($dirp)) !== false) 
      if ($file{0} != '.') $count++;
    closedir($dirp);
  }
  return $count;
}


