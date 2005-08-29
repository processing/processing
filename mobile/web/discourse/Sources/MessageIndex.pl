###############################################################################
# MessageIndex.pl                                                             #
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

$messageindexplver = "1 Gold - SP 1.4";

sub MessageIndex {
	my $start = int( $INFO{'start'} ) || 0;
	my( $bdescrip, $counter, $buffer, $pages, $showmods, $mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mstate, $dlp, $threadlength, $threaddate );
	my( @boardinfo, @threads );
	my( $threadcount, $messagecount, $lastposttime, $lastposter ) = &BoardCountGet($currentboard);
	my $maxindex = $INFO{'view'} eq 'all' ? $threadcount : $maxdisplay;

	# Make sure the starting place makes sense.
	if( $start > $threadcount ) { $start = int( $threadcount % $maxindex ) * $maxindex; }
	elsif( $start < 0 ) { $start = 0; }

	# There are three kinds of lies: lies, damned lies, and statistics. 
	# - Mark Twain
	
	# Construct the page links for this board.
	$indexdisplaynum = 3;	# max number of pages to display
	$tmpa = 1;
	$tmpx = int( $threadcount / $maxindex );
	if ($start >= (($indexdisplaynum-1) * $maxindex)) { $startpage = $start - (($indexdisplaynum-1) * $maxindex); $tmpa = int( $startpage / $maxindex ) + 1; }
	if ($threadcount >= $start + ($indexdisplaynum * $maxindex)) { $endpage = $start + ($indexdisplaynum * $maxindex); } else { $endpage = $threadcount }
	if ($startpage > 0) { $pageindex = qq~<a href="$cgi;action=messageindex;start=0">1</a>&nbsp;...&nbsp;~; }
	if ($startpage == $maxindex) { $pageindex = qq~<a href="$cgi;action=messageindex;start=0">1</a>&nbsp;~;}
	for( $counter = $startpage; $counter < $endpage; $counter += $maxindex ) {
		$pageindex .= $start == $counter ? qq~<B>$tmpa</B>&nbsp;~ : qq~<a href="$cgi;action=messageindex;start=$counter">$tmpa</a>&nbsp;~;
		++$tmpa;
	}
	$tmpx = $threadcount - 1 - $maxindex;
	$outerpn = int($tmpx / $maxindex);
	$lastpn = int(($threadcount - 1) / $maxindex) + 1;
	$lastptn = ($lastpn - 1) * $maxindex;
	if ($endpage < $threadcount - $maxindex ) {$pageindexadd = qq~&nbsp;...&nbsp;~;}
	if ($endpage != $threadcount) {$pageindexadd .= qq~&nbsp;<a href="$cgi;action=messageindex;start=$lastptn">$lastpn</a>~;}
	$pageindex .= $pageindexadd;


	# Determine what category we are in.
	fopen(FILE, "$boardsdir/$currentboard.ctb") || &fatal_error("300 $txt{'106'}: $txt{'23'} $currentboard.ctb");
	$cat = <FILE>;
	fclose(FILE);
	fopen(FILE, "$boardsdir/$cat.cat") || &fatal_error("300 $txt{'106'}: $txt{'23'} $cat.cat");
	$currcat = $cat;
	$cat = <FILE>;
	fclose(FILE);

	# Get the board's description
	fopen(FILE, "$boardsdir/$currentboard.dat") || &fatal_error("300 $txt{'106'}: $txt{'23'} $currentboard.dat");
	@boardinfo = <FILE>;
	fclose(FILE);
	chomp @boardinfo;
	$bdescrip = $boardinfo[1];

	# Open and quickly read the current board thread list.
	# Skip past threads until we reach the "page" we want.
	fopen(FILE, "$boardsdir/$currentboard.txt") || &fatal_error("300 $txt{'106'}: $txt{'23'} $currentboard.txt");
	@threadlist = <FILE>;
	$threadcount = $#threadlist;
	@threadlist2 = '';
	fclose(FILE);

	# Sticky Threads
	fopen(FILE, "$boardsdir/sticky.stk") || &fatal_error("300 $txt{'106'}: $txt{'23'} sticky.stk");
	@stickys = <FILE>;
	$stickycount = $#stickys;
	fclose(FILE);
	for ($i=0; $i<=$threadcount; $i++) {
		($mnum) = split( /\|/, $threadlist[$i] );
		$l = 0;
		$is = 0;
		foreach $curnum (@stickys) {
			if ($mnum == $curnum) {
				$is = 1;
				$stickylist{$l} = $threadlist[$i];
				#splice(@threadlist,$i,1);
				last;
			}
			$l++;
		}
		if ($is == 0) { push(@threadlist2,$threadlist[$i]); }
	}
	if (!($threadlist2[0])) { shift(@threadlist2); }

	#now sort the stickys
	@stickylist2 = sort {$b <=> $a } keys %stickylist;
	for ($i=0; $i<=$stickycount; $i++) {
		chomp $stickylist{$i};
		if ($stickylist{$i}) { unshift(@threadlist2,"$stickylist{$i}\n"); }
	}

	$counter = 0;
	$tmpa = '';
	if($counter < $start && ($buffer = $threadlist2[$counter])) {
		$tmpa = $buffer;
		$counter++;
	}
	while($counter < $start && ($buffer = $threadlist2[$counter])) { $counter++; }

	$#threads = $maxindex - 1;
	$curln = $counter;
	$counter = 0;
	while( $counter < $maxindex && ( $buffer = $threadlist2[$curln+$counter] ) ) {
		chomp $buffer;
		$threads[$counter] = $buffer;
		$counter++;
	}
	$#threads = $counter - 1;

	# Let's get the info for the first thread in this forum.
	$tmpa ||= $threads[0];
	($mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mstate) = split( /\|/, $tmpa );

	# Mark current board as seen.
	&dumplog($currentboard);

	# Build a list of the board's moderators.
	if( scalar keys %moderators > 0 ) {
		if( scalar keys %moderators == 1 ) { $showmods = qq~($txt{'298'}: ~; }
		else { $showmods = qq~($txt{'299'}: ~; }
		while( $_ = each(%moderators) ) {
			&FormatUserName($_);
			$showmods .= qq~<a href="$scripturl?action=viewprofile;username=$useraccount{$_}" class="nav">$moderators{$_}</a>, ~;
		}
		$showmods =~ s/, \Z/)/;
	}

	# Load censor list.
	fopen(FILE,"$vardir/censor.txt");
	while( chomp( $buffer = <FILE> ) ) {
		($tmpa,$tmpb) = split(/=/,$buffer);
		push(@censored,[$tmpa,$tmpb]);
	}
	fclose(FILE);

	# Print the header and board info.
	$curboardurl = $curposlinks ? qq~<a href="$cgi" class="nav">$boardname</a>~ : $boardname;
	$yymain .= qq~
<table width="100%" cellpadding="0" cellspacing="0">
  <tr>
    <td><font size="2" class="nav"><b>
    <IMG SRC="$imagesdir/open.gif" BORDER=0>&nbsp;&nbsp;
    <a href="$scripturl" class="nav">$mbname</a><br>
    <IMG SRC="$imagesdir/tline.gif" BORDER=0><IMG SRC="$imagesdir/open.gif" BORDER=0>&nbsp;&nbsp;
    <a href="$scripturl#$currcat" class="nav">$cat</a><br>
    <IMG SRC="$imagesdir/tline2.gif" BORDER=0><IMG SRC="$imagesdir/open.gif" BORDER=0>&nbsp;&nbsp;
    $curboardurl</b> <font size="1">$showmods</font></font></td>
  </tr>
</table>
~;
if($ShowBDescrip && $bdescrip ne "") {
$yymain .= qq~
<table border=0 width="100%" cellspacing="0" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td>
    <table cellpadding="4" cellspacing="1" border="0" width="100%">
      <tr>
        <td align="left" class="catbg" bgcolor="$color{'catbg'}" width="100%">
        <table cellpadding="3" cellspacing="0" width="100%">
          <tr>
            <td width="100%"><font size="1">$bdescrip</font></td>
          </tr>
        </table>
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
        <td align="left" class="catbg" bgcolor="$color{'catbg'}" width="100%">
        <table cellpadding="3" cellspacing="0" width="100%">
          <tr>
            <td><font class="text2"><b>$txt{'139'}:</b> $pageindex</font></td>
	    <td class="catbg" bgcolor="$color{'catbg'}" align=right nowrap><font size=-1>
~;
	if ($username ne 'Guest') {
if($showmarkread) {
	$yymain .= qq~<a href="$cgi;action=markasread">$img{'markboardread'}</a>~;
}
}
	$yymain .= qq~
	    $menusep<a href="$cgi;action=post;title=$txt{'464'}">$img{'newthread'}</a>&nbsp;
	    </font></td>
          </tr>
        </table>
        </td>
      </tr>
    </table>
    </td>
  </tr>
</table>
<table border=0 width="100%" cellspacing="0" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td>
    <table cellpadding="4" cellspacing="1" border="0" width="100%">
      <tr>
        <td class="titlebg" bgcolor="$color{'titlebg'}" width="10%" colspan="2"><font size="2">&nbsp;</font></td>
        <td class="titlebg" bgcolor="$color{'titlebg'}" width="44%"><font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'70'}</b></font></td>
        <td class="titlebg" bgcolor="$color{'titlebg'}" width="14%" align="center"><font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'109'}</b></font></td>
        <td class="titlebg" bgcolor="$color{'titlebg'}" width="4%" align="center"><font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'110'}</b></font></td>
        <td class="titlebg" bgcolor="$color{'titlebg'}" width="4%" align="center"><font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'301'}</b></font></td>
        <td class="titlebg" bgcolor="$color{'titlebg'}" width="24%"><font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'111'}</b></font></td>
      </tr>
~;

	# Begin printing the message index for current board.
	$counter = $start;
	foreach( @threads ) {
		($mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mstate) = split( /\|/, $_ );
		# Set thread class depending on locked status and number of replies.
		$threadclass = 'thread';
		if( $mstate == 1 ) { $threadclass = 'locked'; }
		elsif( $mreplies > 24 ) { $threadclass = 'veryhotthread'; }
		elsif( $mreplies > 14 ) { $threadclass = 'hotthread'; }
		elsif( $mstate == 0 ) { $threadclass = 'thread'; }
		foreach $curnum (@stickys) {
			if ($mnum == $curnum) {
				if($threadclass eq 'locked') { $threadclass = 'stickylock'; }
				else { $threadclass = 'sticky'; }
			}
		}

		# Decide if thread should have the "NEW" indicator next to it.
		# Do this by reading the user's log for last read time on thread,
		# and compare to the last post time on the thread.
		$dlp = &getlog($mnum);
		$threaddate = stringtotime($mdate);
		if( $max_log_days_old && $dlp < $threaddate && $username ne 'Guest' && &getlog("$currentboard--mark") < $threaddate ) {
			$new = qq~<img src="$imagesdir/new.gif" alt="$txt{'302'}">~;
		}
		else { $new = ''; }

		# Load the current nickname of the account name of the thread starter.
		if( $musername ne 'Guest' && -e "$memberdir/$musername.dat" ) {
			&LoadUser($musername);
			$mname = $userprofile{$musername}->[1] || $mname || $txt{'470'};
			$mname = qq~<a href="$scripturl?action=viewprofile;username=$useraccount{$musername}">$mname</a>~;
		}
		else {
			$mname ||= $txt{'470'};
		}

		# Censor the subject of the thread.
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$msub =~ s~\Q$tmpa\E~$tmpb~gi;
		}

		# Decide how many pages the thread should have.
		$threadlength = $mreplies + 1;
		$pages = '';
		if( $threadlength > $maxmessagedisplay ) {
			$tmpa = 1;
			for( $tmpb = 0; $tmpb < $threadlength; $tmpb += $maxmessagedisplay ) {
				$pages .= qq~<a href="$cgi;action=display;num=$mnum;start=$tmpb#$tmpb">$tmpa</a>\n~;
				++$tmpa;
			}
			$pages =~ s/\n\Z//;
			$pages = qq~<BR><font size="1">&#171; $txt{'139'} $pages &#187;</font>~;
		}

		if( fopen(FILE, "$datadir/$mnum.data") ) {
			$tmpa = <FILE>;
			fclose(FILE);
		}
		elsif( -e "$datadir/$mnum.data" ) {
			&fatal_error("301 $txt{'106'}: $txt{'23'} $mnum.data");
		}
		else {
			$tmpa = '0';
		}


		($views, $lastposter) = split(/\|/, $tmpa);
		if( $lastposter =~ m~\AGuest-(.*)~ ) {
			$lastposter = $1;
		}
		else {
			unless( $lastposter eq $txt{'470'} ) {
				$lastposterid = $lastposter;
				&LoadUser($lastposterid);
				if($userprofile{$lastposter}->[1]) { $lastposter = qq~<a href="$scripturl?action=viewprofile;username=$lastposterid">$userprofile{$lastposter}->[1]</a>~; }
			}
			&LoadUser($lastposterid);
		}
		$lastpostername = $lastposter || $txt{'470'};
		$views = $views ? $views - 1 : 0;

		# Print the thread info.
		$mydate = &timeformat($mdate);
		$yymain .= qq~
      <tr>
        <td class="windowbg2" valign="middle" align="center" width="6%" bgcolor="$color{'windowbg2'}"><img src="$imagesdir/$threadclass.gif"></td>
        <td class="windowbg2" valign="middle" align="center" width="4%" bgcolor="$color{'windowbg2'}"><img src="$imagesdir/$micon.gif" alt="" border="0" align="middle"></td>
        <td class="windowbg" valign="middle" width="44%" bgcolor="$color{'windowbg'}"><font size="2">$new <a href="$cgi;action=display;num=$mnum"><b>$msub</b></a> $pages</font></td>
        <td class="windowbg2" valign="middle" align="center" width="14%" bgcolor="$color{'windowbg2'}"><font size="1">$mname</font></td>
        <td class="windowbg" valign="middle" width="4%" align="center" bgcolor="$color{'windowbg'}"><font size="2">$mreplies</font></td>
        <td class="windowbg" valign="middle" width="4%" align="center" bgcolor="$color{'windowbg'}"><font size="2">$views</font></td>
        <td class="windowbg2" valign="middle" width="24%" bgcolor="$color{'windowbg2'}"><font size="1">$mydate<br>$txt{'525'} $lastpostername</font></td>
      </tr>
~;
		++$counter;
	}

	$yymain .= qq~
    </table>
    </td>
  </tr>
</table>
<table border=0 width="100%" cellspacing="0" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td>
    <table cellpadding="4" cellspacing="1" border="0" width="100%">
      <tr>
        <td align="left" class="catbg" bgcolor="$color{'catbg'}" width="100%">
        <table cellpadding="3" cellspacing="0" width="100%">
          <tr>
            <td><font class="text2"><b>$txt{'139'}:</b> $pageindex</font></td>
	    <td class="catbg" bgcolor="$color{'catbg'}" align=right nowrap><font size="2">
~;
	if ($username ne 'Guest') {
	if($showmarkread) { $yymain .= qq~<a href="$cgi;action=markasread">$img{'markboardread'}</a>~; }
}
	&jumpto;
	$yymain .= qq~
	    $menusep<a href="$cgi;action=post;title=$txt{'464'}">$img{'newthread'}</a>&nbsp;
	    </font></td>
	  </tr>
        </table>
        </td>
      </tr>
    </table>
    </td>
  </tr>
</table><br>
<table cellpadding=0 cellspacing=0 width="100%">
  <tr>
	<td align="left" valign="middle"><font size="1">
	<img src="$imagesdir/thread.gif"> $txt{'457'}<BR>
	<img src="$imagesdir/hotthread.gif"> $txt{'454'}<BR>
	<img src="$imagesdir/veryhotthread.gif"> $txt{'455'}
	</font></td><td align="left" valign="middle"><font size="1">
	<img src="$imagesdir/locked.gif"> $txt{'456'}<BR>
	<img src="$imagesdir/sticky.gif"> $txt{'779'}<BR>
	<img src="$imagesdir/stickylock.gif"> $txt{'780'}
	</font></td>
	<td align="right" valign="middle"><font size="1">$selecthtml</font></td>
  </tr>
</table>
~;
	$yytitle = $boardname;
	&template;
	exit;
}

sub MarkRead {
	# Mark all threads in this board as read.
	&dumplog("$currentboard--mark");
	$yySetLocation = qq~$scripturl~;
	&redirectexit;
}

1;
