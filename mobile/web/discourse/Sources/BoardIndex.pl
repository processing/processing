###############################################################################
# BoardIndex.pl                                                               #
###############################################################################
#                                                                             #
# YaBB: Yet another Bulletin Board                                            #
# Open-Source Community Software for Webmasters                               #
#                                                                             #
# Version:        YaBB 1 Gold - SP 1.4                                        #
# Released:       December 2001; Updated November 25, 2004                    #
# Distributed by: http://www.yabbforum.com                                    #
#                                                                             #
# =========================================================================== #
#                                                                             #
# Copyright (c) 2000-2004 YaBB (www.yabbforum.com) - All Rights Reserved.     #
#                                                                             #
# Software by:  The YaBB Development Team                                     #
#               with assistance from the YaBB community.                      #
# Sponsored by: Xnull Internet Media, Inc. - http://www.ximinc.com            #
#               Your source for web hosting, web design, and domains.         #
#                                                                             #
###############################################################################

$boardindexplver = "1 Gold - SP 1.4";

sub BoardIndex {
	# Open the file with all categories
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	$yyCatsLoaded = 1;
	my($memcount, $latestmember) = &MembershipGet;
	$totalm = 0;
	$totalt = 0;
	foreach $curcat (@categories) {
		chomp $curcat;
		fopen(FILE, "$boardsdir/$curcat.cat");
		$catname{$curcat} = <FILE>;
		chomp $catname{$curcat};
		$cataccess{$curcat} = <FILE>;
		chomp $cataccess{$curcat};
		@{$catboards{$curcat}} = <FILE>;
		fclose(FILE);
		@membergroups = split( /,/, $cataccess{$curcat} );
		$openmemgr{$curcat} = 0;
		foreach $tmpa (@membergroups) { if($tmpa eq $settings[7]) { $openmemgr{$curcat} = 1; last; } }
		if(!$cataccess{$curcat} || $settings[7] eq 'Administrator') { $openmemgr{$curcat} = 1; }
		unless($openmemgr{$curcat}) { next; }
		foreach $curboard (@{$catboards{$curcat}}) {
			chomp $curboard;
			( $threadcount, $messagecount, $lastposttime, $lastposter ) = &BoardCountGet($curboard);

			$lastposttime{$curboard} = $lastposttime eq 'N/A' || ! $lastposttime ? $txt{'470'} : &timeformat($lastposttime);
			$lastpostrealtime{$curboard} = $lastposttime eq 'N/A' || ! $lastposttime ? '' : $lastposttime;
			if( $lastposter =~ m~\AGuest-(.*)~ ) {
				$lastposter = $1;
				$lastposterguest{$curboard} = 1;
			}
			$lastposter{$curboard} = $lastposter eq 'N/A' || ! $lastposter ? $txt{'470'} : $lastposter;
			$messagecount{$curboard} = $messagecount || 0;
			$threadcount{$curboard} = $threadcount || 0;
			$totalm += $messagecount;
			$totalt += $threadcount;
		}
	}

	$curforumurl = $curposlinks ? qq~<a href="$scripturl" class="nav">$mbname</a>~ : $mbname;
	$yymain .= qq~
<P align="left"><font size="2" class="nav"><IMG SRC="$imagesdir/open.gif" BORDER="0" alt=""> <b>$curforumurl</b></font>~;
	if($shownewsfader == 1) {
		if(!$fadedelay) { $fadedelay = 5000; }
		$yymain .= qq~
<table border=0 width="100%" cellspacing="0" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td>
    <table cellpadding="4" cellspacing="1" border="0" width="100%">
      <tr>
        <td bgcolor="$color{'titlebg'}" class="titlebg" align="center"><font class="text1" color="$color{'titletext'}" size="2"><b>$txt{'102'}</b></font></td>
      </tr><tr>
        <td bgcolor="$color{'windowbg2'}" class="windowbg2" valign="middle" align="center" height="60">
<script language="JavaScript1.2" type="text/javascript">
<!--
var delay = $fadertime
var bcolor = "$color{'windowbg2'}"
var tcolor = "$color{'fadertext'}"
var fcontent = new Array()
begintag = '<font size="2"><B>'
~;
fopen(FILE, "$vardir/news.txt");
@newsmessages = <FILE>;
fclose(FILE);
for($i=0; $i < @newsmessages; $i++) {
	$newsmessages[$i] =~ s/\n|\r//g;
	if($i != 0){ $yymain .= qq~\n~; }
	$message = $newsmessages[$i];
	if($enable_ubbc) { if(!$yyYaBBCloaded) { require "$sourcedir/YaBBC.pl"; } &DoUBBC; }
	$message =~ s/\"/\\\"/g;
	$yymain .= qq~fcontent[$i] = "$message"~;
}
$yymain .= qq~
closetag = '</b></font>'
// -->
</script>
<script language="JavaScript1.2" type="text/javascript" src="$faderpath"></script>
<script language="JavaScript1.2" type="text/javascript">
<!--
if (navigator.appVersion.substring(0,1) < 5 && navigator.appName == "Netscape") {
   var fwidth = screen.availWidth / 2;
   var bwidth = screen.availWidth / 4;
   document.write('<ilayer id="fscrollerns" width='+fwidth+' height=35 left='+bwidth+' top=0><layer id="fscrollerns_sub" width='+fwidth+' height=35 left=0 top=0></layer></ilayer>');
   window.onload = fade;
}
else if (navigator.userAgent.search(/Opera/) != -1 || (navigator.platform != "Win32" && navigator.userAgent.indexOf('Gecko') == -1)) {
   document.open();
   document.write('<div id="fscroller" style="width:90%; height:15px; padding:2px">');
   for(i=0; i < fcontent.length; ++i) {
      document.write(begintag+fcontent[i]+closetag+'<br>');
   }
   document.write('</div>');
   document.close();
   window.onload = fade;
}
else {
   document.write('<div id="fscroller" style="width:90% height:15px; padding:2px"></div>');
   window.onload = fade;
}
// -->
</script>
        </td>
      </tr>
    </table>
    </td>
  </tr>
</table>
~;
	}
	$yymain .= qq~
<table border=0 width="100%" cellspacing="0" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td>
    <table cellpadding="4" cellspacing="1" border="0" width="100%">
      <tr>
        <td class="titlebg" bgcolor="$color{'titlebg'}" colspan="2"><font class="text1" color="$color{'titletext'}" size="2"><b>$txt{'20'}</b></font></td>
        <td class="titlebg" bgcolor="$color{'titlebg'}" width="1%" align="center"><font class="text1" color="$color{'titletext'}" size="2"><b>$txt{'330'}</b></font></td>
        <td class="titlebg" bgcolor="$color{'titlebg'}" width="1%" align="center"><font class="text1" color="$color{'titletext'}" size="2"><b>$txt{'21'}</b></font></td>
        <td class="titlebg" bgcolor="$color{'titlebg'}" width="24%" align="center"><font class="text1" color="$color{'titletext'}" size="2"><b>$txt{'22'}</b></font></td>
      </tr>~;

	foreach $curcat (@categories) {
		unless( $openmemgr{$curcat} ) { next; }
		$yymain .= qq~<tr>
        <td colspan="5" class="catbg" bgcolor="$color{'catbg'}" height="18"><a name="$curcat"> <font size=2><b>$catname{$curcat}</b></font></a></td>
      </tr>~;
		foreach $curboard (@{$catboards{$curcat}}) {
			chomp $curboard;
			fopen(FILE, "$boardsdir/$curboard.dat");
			$curboardname = <FILE>;
			chomp $curboardname;
			$curboarddescr = <FILE>;
			chomp $curboarddescr;
			$curboardmods = <FILE>;
			chomp $curboardmods;
			fclose(FILE);
			%moderators = ();
			foreach $curuser (split(/\|/, $curboardmods)) {
				&LoadUser($curuser);
				$moderators{$curuser} = $userprofile{$curuser}->[1];

			}
			$showmods = '';
			if(scalar keys %moderators == 1) { $showmods = qq~$txt{'298'}: ~; }
			elsif(scalar keys %moderators != 0) { $showmods = qq~$txt{'299'}: ~; }
			while($tmpa = each(%moderators)) {
				&FormatUserName($tmpa);
				$showmods .= qq~<a href="$scripturl?action=viewprofile;username=$useraccount{$tmpa}">$moderators{$tmpa}</a>, ~;
			}
			$showmods =~ s/, \Z//;
			if($showmods eq "") { $showmods = qq~$txt{'298'}: $txt{'470'}~; }
			$dlp = &getlog($curboard);
			if( $max_log_days_old && $lastposttime{$curboard} ne $txt{'470'} && $username ne 'Guest' && $dlp < stringtotime( $lastpostrealtime{$curboard} ) ) {
				$new = qq~<img src="$imagesdir/on.gif" alt="$txt{'333'}" border="0">~;
			}
			else { $new = qq~<img src="$imagesdir/off.gif" alt="$txt{'334'}" border="0">~; }
			$lastposter = $lastposter{$curboard};
			unless( $lastposterguest{$curboard} || $lastposter{$curboard} eq $txt{'470'} ) {
				$lastposterid = $lastposter;
				&LoadUser($lastposterid);
				if($userprofile{$lastposter}->[1]) { $lastposter = qq~<a href="$scripturl?action=viewprofile;username=$lastposterid">$userprofile{$lastposter}->[1]</a>~; }
			}
			$lastposter ||= $txt{'470'};
			$lastposttime ||= $txt{'470'};
			$yymain .= qq~<tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}" width="8%" align="center" valign="top">$new</td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" align="left" width="66%">
        <font size="2"><a name="$curboard" href="$scripturl?board=$curboard"><b>$curboardname</b></a></font>
        <br><font size="1">$curboarddescr<BR>
        <i>$showmods</i></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}" valign="middle" align="center" width="1%"><font size="2">$threadcount{$curboard}</font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}" valign="middle" align="center" width="1%"><font size="2">$messagecount{$curboard}</font></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="middle" width="24%"><font size="1">$lastposttime{$curboard}<BR>$txt{'525'} $lastposter</font></td>
      </tr>~;
		}
	}
	my $checkadded = 0;
	$guests = 0;
	$users = '';
	$numusers = 0;
	fopen(FILE, "$vardir/log.txt");
	@entries = <FILE>;
	fclose(FILE);
	foreach $curentry (@entries) {
		chomp $curentry;
		($name, $value) = split(/\|/, $curentry);
		if($name) {
			if(!$yyUDLoaded{$musername}) { &LoadUser($name); }
			if(exists $userprofile{$name}) {
				$numusers++;
				$users .= qq~ <a href="$scripturl?action=viewprofile;username=$useraccount{$name}">$userprofile{$name}->[1]</a><font size="1">,</font> \n~;
			}
			else { $guests++; }
		}
	}
	$users =~ s~<font size="1">,</font> \n\Z~~;
	if($username ne 'Guest') {
		$ims = @immessages;
		$ims = qq~<BR>$txt{'796'} <a href="$scripturl?action=im"><B>$ims</B></a>~;
		$yymain .= qq~<tr>
        <td class="catbg" bgcolor="$color{'catbg'}" colspan="6" align="center">
        <table cellpadding="0" border="0" cellspacing="0" width="100%">
          <tr>
            <td align="left"> <font size="1">
            <img src="$imagesdir/on.gif" border=0 alt="$txt{'333'}"> $txt{'333'}&nbsp;&nbsp;
            <img src="$imagesdir/off.gif" border=0 alt="$txt{'334'}"> $txt{'334'}</font></td>
            <td align="right"><font size=1>&nbsp;
~;
	if($showmarkread) { $yymain .= qq~<a href="$scripturl?action=markallasread">$img{'markallread'}</a>~; }
	$yymain .= qq~
            </font></td>
          </tr>
        </table>
        </td>
      </tr>~;
	}
	$yymain .= qq~
    </table>
    </td>
  </tr>
</table>
<BR><BR>
<table border=0 width="100%" cellspacing="0" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td>
    <table cellpadding="4" cellspacing="1" border="0" width="100%">
      <tr>
        <td bgcolor="$color{'titlebg'}" class="titlebg" align="center" colspan="2">
        <font class="text1" color="$color{'titletext'}" size="2"><b>$txt{'685'}</b></font></td>
      </tr>~;
	$yymain .= qq~<tr>
        <td class="catbg" bgcolor="$color{'catbg'}" colspan="2"> <font size="2" class="catbg"><b>$txt{'200'}</b></font></td>
      </tr><tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}" width="20" valign="middle" align="center"><img src="$imagesdir/info.gif" border="0" alt=""></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="top" align="center"><font size="1">
        <table width="98%" cellpadding="3" align="center">
          <tr>
            <td valign="top" align="left" width="60%"><font size="1">
            $txt{'490'} <B>$totalt</B> &nbsp; - &nbsp; $txt{'489'} <B>$totalm</B>
	    ~;
	if($Show_RecentBar) {
		require "$sourcedir/Recent.pl";
		&LastPost;
	}
	$yymain .= qq~
	    </font></td>
	    <td valign="top" align="left" width="40%"><font size="1">
	    $txt{'488'} <a href="$scripturl?action=mlall"><B>$memcount</B></a>
	    ~;
	if ($showlatestmember) {
		&LoadUser($latestmember);
		$yymain .= qq~<BR>$txt{'201'}: <a href="$scripturl?action=viewprofile;username=$useraccount{$latestmember}"><b>$userprofile{$latestmember}->[1]</b></a>~;
	}
	$yymain .= qq~
	    $ims
	    </font></td>
	  </tr>
	</table>
	</font></td>
      </tr><tr>
        <td class="catbg" bgcolor="$color{'catbg'}" colspan="2"> <font size="2" class="catbg"><b>$txt{'158'}</b></font></td>
      </tr><tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}" width="20" valign="middle" align="center"><img src="$imagesdir/online.gif" border="0" alt=""></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=1>
        <table width="98%" cellpadding="3" align="center">
          <tr>
            <td valign="top" align="left"><font size="1">
            $guests $txt{'141'}, $numusers $txt{'142'}<BR>$users
            </font></td>
          </tr>
        </table>
        </font></td>
      </tr>~;

	if($username eq 'Guest') {
		require "$sourcedir/LogInOut.pl";
		$sharedLogin_title="$txt{'34'} <small><a href=\"$cgi;action=reminder\">($txt{'315'})</a></small>";
		&sharedLogin;
	}

	$yymain .= qq~
    </table>
    </td>
  </tr>
</table>
~;
	$yytitle = "$txt{'18'}";
	&template;
	exit;
}

sub MarkAllRead {
	fopen(FILE, "$vardir/cat.txt");
	my @categories = <FILE>;
	fclose(FILE);
	my( $curcat, $curcatname, $curcataccess, @catboards, @membergroups, $openmemgr, $curboard );
	foreach $curcat (@categories) {
		chomp $curcat;
		fopen(FILE, "$boardsdir/$curcat.cat");
		$curcatname = <FILE>;
		$curcataccess = <FILE>;
		chomp $curcatname;
		chomp $curcataccess;
		@catboards = <FILE>;
		fclose(FILE);
		@membergroups = split( /,/, $curcataccess );
		$openmemgr = 0;
		foreach (@membergroups) {
			if( $_ eq $settings[7]) { $openmemgr = 1; last; }
		}
		if(!$curcataccess || $settings[7] eq 'Administrator') { $openmemgr = 1; }
		unless( $openmemgr ) { next; }
		foreach $curboard (@catboards) {
			chomp $curboard;
			&modlog("$curboard--mark");
			&modlog($curboard);
		}
	}
	&dumplog;
	&BoardIndex;
}

1;