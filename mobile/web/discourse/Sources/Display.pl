###############################################################################
# Display.pl                                                                  #
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

$displayplver = "1 Gold - SP 1.4";

sub Display {
	my $viewnum = $INFO{'num'};
	if( $viewnum =~ /\D/ ) { &fatal_error($txt{'337'}); }
	if( $currentboard eq '' ) { &fatal_error($txt{'1'}); }
	$maxmessagedisplay ||= 10;
	my($buffer,$views,$lastposter,$moderators,$counter,$counterwords,$pageindex,$msubthread,$mnum,$mstate,$mdate,$msub,$mname,$memail,$mreplies,$musername,$micon,$noposting,$threadclass,$notify,$max,$start,$bgcolornum,$windowbg,$mattach,$mip,$mlm,$mlmb,$lastmodified,$postinfo,$star,$sendm,$topicdate);
	my(@userprofile,@messages,@bgcolors);

	# Determine what category we are in.
	fopen(FILE, "$boardsdir/$currentboard.ctb") || &fatal_error("300 $txt{'106'}: $txt{'23'} $currentboard.ctb");
	$curcat = <FILE>;
	fclose(FILE);
	#$curcat = $cat;
	fopen(FILE, "$boardsdir/$curcat.cat") || &fatal_error("300 $txt{'106'}: $txt{'23'} $cat.cat");
	$cat = <FILE>;
	fclose(FILE);

	# Load the membergroups list.
	fopen(FILE, "$vardir/membergroups.txt") || &fatal_error("100 $txt{'106'}: $txt{'23'} membergroups.txt");
	@membergroups = <FILE>;
	fclose(FILE);

	# Mark current thread as read.
	($mnum,$tmpa,$tmpa,$tmpa,$mdate) = split(/\|/,$yyThreadLine);
	&dumplog($mnum,$date);

	# Add 1 to the number of views of this thread.
	if(fopen(FILE, "$datadir/$viewnum.data")) {
		$tmpa = <FILE>;
		fclose(FILE);
	}
	elsif( -e "$datadir/$viewnum.data" ) { &fatal_error("102 $txt{'106'}: $txt{'23'} $viewnum.data"); }
	else { $tmpa = '0'; }
	($tmpa, $tmpb) = split( /\|/, $tmpa );
	$tmpa++;
	fopen(FILE, "+>$datadir/$viewnum.data") || &fatal_error("103 $txt{'106'}: $txt{'23'} $viewnum.data");
	print FILE qq~$tmpa|$tmpb~;
	fclose(FILE);
	$views = $tmpa - 1;

	# Check to make sure this thread isn't locked.
	($mnum,$msubthread,$mname,$memail,$mdate,$mreplies,$musername,$micon,$mstate) = split( /\|/, $yyThreadLine );
	$noposting = $viewnum eq $mnum && $mstate == 1 ? 1 : 0;

	# Get the class of this thread, based on lock status and number of replies.
	$replybutton = qq~<a href="$cgi;action=post;num=$viewnum;title=$txt{'116'};start=$start">$img{'reply'}</a>~;
	$threadclass = 'thread';
	if( $mstate == 1 ) {
		$threadclass = 'locked';
		$replybutton = "";
	}
	elsif( $mreplies > 24 ) { $threadclass = 'veryhotthread'; }
	elsif( $mreplies > 14 ) { $threadclass = 'hotthread'; }
	elsif( $mstate == 0 ) { $threadclass = 'thread'; }
	fopen(FILE, "$boardsdir/sticky.stk") || &fatal_error("300 $txt{'106'}: $txt{'23'} sticky.stk");
	@stickys = <FILE>;
	fclose(FILE);
	foreach $curnum (@stickys) {
		if ($mnum == $curnum) {
			if($threadclass eq 'locked') { $threadclass = 'stickylock'; }
			else { $threadclass = 'sticky'; }
		}
	}

	&LoadCensorList;	# Load Censor List

	# Build a list of this board's moderators.
	if( scalar keys %moderators > 0 ) {
		if( scalar keys %moderators == 1 ) { $showmods = qq~($txt{'298'}: ~; }
		else { $showmods = qq~($txt{'299'}: ~; }
		while( $_ = each(%moderators) ) {
			&FormatUserName($_);
			$showmods .= qq~<a href="$scripturl?action=viewprofile;username=$useraccount{$_}" class="nav">$moderators{$_}</a>, ~;
		}
		$showmods =~ s/, \Z/)/;
	}

	if($enable_notification) {
		my $startnum = $start || '0';
		$notify = qq~$menusep<a href="$cgi;action=notify;thread=$viewnum;start=$startnum">$img{'notify'}</a>~;
	}
	&jumpto;	# create the jumpto list

	# Build the page links list.
	$postdisplaynum = 3;	# max number of pages to display
	$max = $mreplies + 1;
	$start = $INFO{'start'} || 0;
	$start = $start > $mreplies ? $mreplies : $start;
	$start = ( int( $start / $maxmessagedisplay ) ) * $maxmessagedisplay;
	$tmpa = 1;
	$tmpx = int( $max / $maxmessagedisplay );
	if ($start >= (($postdisplaynum-1) * $maxmessagedisplay)) { $startpage = $start - (($postdisplaynum-1) * $maxmessagedisplay); $tmpa = int( $startpage / $maxmessagedisplay ) + 1; }
	if ($max >= $start + ($postdisplaynum * $maxmessagedisplay)) { $endpage = $start + ($postdisplaynum * $maxmessagedisplay); } else { $endpage = $max }
	if ($startpage > 0) { $pageindex = qq~<a href="$cgi;action=display;num=$viewnum;start=0">1</a>&nbsp;...&nbsp;~; }
	if ($startpage == $maxmessagedisplay) { $pageindex = qq~<a href="$cgi;action=display;num=$viewnum;start=0">1</a>&nbsp;~;}
	for( $counter = $startpage; $counter < $endpage; $counter += $maxmessagedisplay ) {
		$pageindex .= $start == $counter ? qq~<b>$tmpa</b>&nbsp;~ : qq~<a href="$cgi;action=display;num=$viewnum;start=$counter">$tmpa</a>&nbsp;~;
		$tmpa++;
	}
	$tmpx = $max - $maxmessagedisplay;
	$outerpn = int($tmpx / $maxmessagedisplay) + 0;
	$lastpn = int($mreplies / $maxmessagedisplay) + 1;
	$lastptn = ($lastpn - 1) * $maxmessagedisplay;
	if ($endpage < $max - ($maxmessagedisplay) ) {$pageindexadd = qq~&nbsp;...&nbsp;~;}
	if ($endpage != $max) {$pageindexadd .= qq~&nbsp;<a href="$cgi;action=display;num=$viewnum;start=$lastptn">$lastpn</a>~;}
	$pageindex .= $pageindexadd;

	foreach (@censored) {
		($tmpa,$tmpb) = @{$_};
		$msubthread =~ s~\Q$tmpa\E~$tmpb~gi;
	}
	$curthreadurl = $curposlinks ? qq~<a href="$cgi;action=display;num=$viewnum" class="nav">$msubthread</a>~ : $msubthread;

	# Create next/prev links
	fopen(LISTS, "$boardsdir/$INFO{'board'}.txt");
	@boardtopics = <LISTS>;
	seek LISTS, 0, 0;
	my $found;
	my $name = $INFO{'num'};
	my $bcount = 0;
	$CurrentPosition = -1;
	while($ThreadNum = <LISTS>) {
		++$CurrentPosition;
		$boardtopics[$bcount] = $ThreadNum;
		$bcount++;
		if ($ThreadNum =~ m/\A$name/o) { $found = 1; last; }
	}
	fclose(LISTS);
	$previous = $boardtopics[$CurrentPosition-1];
	$next = $boardtopics[$CurrentPosition+1];
	@prevthread = split(/\|/, $previous);
	$goprevious = $prevthread[0];
	@nextthread = split(/\|/, $next);
	$gonext = $nextthread[0];
	@getlastthread = @boardtopics;
	$lastthread = pop(@getlastthread);
	@lasttopic = split(/\|/, $lastthread);
	$endthread2 = $lasttopic[0];
	if($found) {
		$prevtopic = "$cgi;action=display;num=$goprevious";
		$nexttopic = "$cgi;action=display;num=$gonext";
		$endthread = "$cgi;action=display;num=$endthread2";
	}
	if( $endthread eq $prevtopic && $gonext eq "") { $nav = qq~&#171; $txt{'766'} | $txt{'766'} &#187;~; }
	if($endthread eq $prevtopic && $gonext ne "") { $nav = qq~&#171; $txt{'766'} | <a href="$nexttopic">$txt{'767'}</a> &#187;~; }
	if ( $endthread ne $prevtopic && $gonext eq "") { $nav = qq~&#171; <a href="$prevtopic">$txt{'768'}</a> | $txt{'766'} &#187;~; }
	if ($endthread ne $prevtopic && $gonext ne "") { $nav = qq~&#171; <a href="$prevtopic">$txt{'768'}</a> | <a href="$nexttopic">$txt{'767'}</a> &#187;~; 	}
	$yymain .= qq~
<script language="JavaScript1.2" src="$ubbcjspath" type="text/javascript"></script>
<table width="100%" cellpadding="0" cellspacing="0">
  <tr>
    <td valign=bottom colspan="2"><font size="2" class="nav"><B>
    <img src="$imagesdir/open.gif" border="0" alt="">&nbsp;&nbsp;
    <a href="$scripturl" class="nav">$mbname</a><br>
    <img src="$imagesdir/tline.gif" border="0" alt=""><IMG SRC="$imagesdir/open.gif" border="0" alt="">&nbsp;&nbsp;
    <a href="$scripturl#$curcat" class="nav">$cat</a><br>
    <img src="$imagesdir/tline2.gif" border="0" alt=""><IMG SRC="$imagesdir/open.gif" border="0" alt="">&nbsp;&nbsp;
    <a href="$cgi" class="nav">$boardname</a> </b><font size="1">$showmods</font><b><br>
    <img SRC="$imagesdir/tline3.gif" border="0" alt=""><IMG SRC="$imagesdir/open.gif" border="0" alt="">&nbsp;&nbsp;
    $curthreadurl</b></font></td>
    <td valign="bottom" align="right"><font size="1">$nav</font></td>
  </tr>
</table>
<table border="0" width="100%" cellspacing="0" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td>
    <table cellpadding="4" cellspacing="1" border="0" width="100%">
      <tr>
        <td align="left" class="catbg" bgcolor="$color{'catbg'}" width="100%">
        <table cellpadding="3" cellspacing="0" width="100%">
          <tr>
            <td><font size="2"><b>$txt{'139'}:</b> $pageindex</font></td>
            <td class="catbg" bgcolor="$color{'catbg'}" align="right"><font size="2">
            $replybutton$notify$menusep
            <a href="$cgi;action=sendtopic;topic=$viewnum">$img{'sendtopic'}</a>$menusep
            <a href="$cgi;action=print;num=$viewnum" target="_blank">$img{'print'}</a>
            </font></td>
          </tr>
        </table>
        </td>
      </tr>
    </table>
    </td>
  </tr>
</table>
<table cellpadding="0" cellspacing="0" border="0" width="100%" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
    <td>
    <table cellpadding="3" cellspacing="1" border="0" width="100%">
      <tr>
        <td valign="middle" align="left" width="20%" bgcolor="$color{'titlebg'}" class="titlebg">
        <font size=2 class="text1" color="$color{'titletext'}">&nbsp;<img src="$imagesdir/$threadclass.gif" alt="">
        &nbsp;<b>$txt{'29'}</b></font></td>
        <td valign="middle" align="left" bgcolor="$color{'titlebg'}" class="titlebg" width="80%">
        <font size=2 class="text1" color="$color{'titletext'}"><b>&nbsp;$txt{'118'}: $msubthread</b> &nbsp;($txt{'641'} $views $txt{'642'})</font></td>
      </tr>
    </table>
    </td>
  </tr>
</table>
~;

	# Load background color list.
	@bgcolors = ( $color{windowbg}, $color{windowbg2} );
	$bgcolornum = scalar @bgcolors;
	@cssvalues = ( "windowbg","windowbg2" );
	$cssnum = scalar @bgcolors;

	if(!$MenuType) { $sm = 1; }
	$counter = 0;

	fopen(FILE,"$datadir/$viewnum.txt") || &fatal_error("104 $txt{'106'}: $txt{'23'} $viewnum.txt");

	# Skip past the posts in this thread until we reach $start.
	while($counter < $start && ($buffer = <FILE>)) { $counter++; }

	$#messages = $maxmessagedisplay - 1;
	for($counter = 0; $counter < $maxmessagedisplay && ($buffer = <FILE>); $counter++) {
		$messages[$counter] = $buffer;
	}
	fclose(FILE);
	$#messages = $counter - 1;
	$counter = $start;

	# For each post in this thread:
	foreach (@messages) {
		$windowbg = $bgcolors[($counter % $bgcolornum)];
		$css = $cssvalues[($counter % $cssnum)];
		chomp;
		($msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $postmessage, $ns, $mlm, $mlmb) = split(/[\|]/, $_);
		# Should we show "last modified by?"
		if( $mlm && $showmodify && $mlm ne "" && $mlmb ne "") {
			$mlm = &timeformat($mlm);
			&LoadUser($mlmb);
			$mlmb = $userprofile{$mlmb}->[1] || $mlmb || $txt{'470'};
			$lastmodified = qq~&#171; <i>$txt{'211'}: $mlm $txt{'525'} $mlmb</i> &#187;~;
		}
		else {
			$mlm = '-';
			$lastmodified = '';
		}
		$msub ||= $txt{'24'};
		$messdate = &timeformat($mdate);
		$mip = $settings[7] eq 'Administrator' ? $mip : "$txt{'511'}";
		$sendm = '';

		# If the user isn't a guest, load his/her info.
		if($musername ne 'Guest' && ! $yyUDLoaded{$musername} && -e("$memberdir/$musername.dat") ) {
			&LoadUserDisplay($musername);	# If user is not in memory, s/he must be loaded.
		}
		if($yyUDLoaded{$musername}) {
			@userprofile = @{$userprofile{$musername}};
			$displayname = $userprofile[1];
			$star = $memberstar{$musername};
			$memberinfo = $memberinfo{$musername};
			$memberinfo =~ s~\n~~g;
			$icqad = $icqad{$musername};
			$yimon = $yimon{$musername};
			if($username ne 'Guest') {
				# Allow instant message sending if current user is a member.
				$sendm = qq~$menusep<a href="$cgi;action=imsend;to=$useraccount{$musername}">$img{'message_sm'}</a>~;
			}
			$usernamelink = qq~<a href="$scripturl?board=$currentboard;action=viewprofile;username=$useraccount{$musername}"><font size="2"><b>$userprofile[1]</b></font></a>~;
			$postinfo = qq~$txt{'26'}: $userprofile[6]<br>~;
			$memail = $userprofile[2];
		}
		else {
			$musername = "Guest";
			$star = '';
			$memberinfo = "$txt{'28'}";
			$icqad = '';
			$yimon = '';
			$usernamelink = qq~<font size="2"><b>$mname</b></font>~;
			$postinfo = '';
			@userprofile = ();
			$displayname = $mname;
		}
		# Censor the subject and message.
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$postmessage =~ s~\Q$tmpa\E~$tmpb~gi;
			$msub =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		# Run UBBC interpreter on the message.
		$message = $postmessage; # put the message back into the proper variable to do ubbc on it
		&wrap;
		if($enable_ubbc) { if(!$yyYaBBCloaded) { require "$sourcedir/YaBBC.pl"; } &DoUBBC; }
		&wrap2;
		$profbutton = $profilebutton && $musername ne 'Guest' ? qq~<a href="$scripturl?action=viewprofile;username=$useraccount{$musername}">$img{'viewprofile_sm'}</a>$menusep~ : '';
		if($counter != 0) { $counterwords = "$txt{'146'} #$counter"; }
		else { $counterwords = ""; }
		# Print the post and user info for the poster.
		$yymain .= qq~
<table cellpadding="0" cellspacing="0" border="0" width="100%" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
    <td>
    <table cellpadding="3" cellspacing="1" border="0" width="100%">
      <tr>
        <td bgcolor="$windowbg" class="$css">
        <a name="$counter"></a>
        <table width="100%" cellpadding="4" cellspacing="0" class="$css" bgcolor="$windowbg">
          <tr>
            <td class="$css" bgcolor="$windowbg" valign="top" width="20%" rowspan="2"><font size="1">
            $usernamelink<br>
            $memberinfo<br>~;
	if($musername ne "Guest") {
		$yymain .= qq~
            $star<BR><BR>
            $userprofile[13]$userprofile[12]
            <BR>$userprofile[8] $icqad &nbsp; $userprofile[10] $yimon &nbsp; $userprofile[9]<BR>
~;
	}
	if($musername eq "Guest") {
		$yymain .= qq~
            <BR><a href="mailto:$memail">$img{'email_sm'}</a><BR><BR>
~;
	}
	elsif ($userprofile[19] ne "checked" || $settings[7] eq "Administrator" || $allow_hide_email ne 1) {
		$yymain .= qq~
            $profbutton$userprofile[4] <a href="mailto:$memail">$img{'email_sm'}</a>$sendm<BR><BR>
~;
	} else {
		$yymain .= qq~
    $profbutton$userprofile[4]$sendm<BR><BR>
~;
	}
	$yymain .= qq~
            $userprofile[11]
            $postinfo
            </font></td>
            <td class="$css" bgcolor="$windowbg" valign="top" width="80%" height="100%">
            <table width="100%" border="0">
              <tr>
                <td align="left" valign="middle"><img src="$imagesdir/$micon.gif" alt=""></td>
                <td align="left" valign="middle">
                <font size="2"><B>$msub</b></font><BR>
                <font size="1">&#171; <B>$counterwords $txt{'30'}:</B> $messdate &#187;</font></td>
                <td align="right" valign="bottom" nowrap height="20">
                <font size=-1>
~;
if ($mstate != 1)
{
$yymain .= qq~
                <a href="$cgi;action=post;num=$viewnum;quote=$counter;title=$txt{'116'};start=$start">$img{'replyquote'}</a>$menusep<a href="$cgi;action=modify;message=$counter;thread=$viewnum">$img{'modify'}</a>
~;
	if(exists $moderators{$username} || $settings[7] eq 'Administrator' || $username eq $musername) {
		$yymain .= qq~  $menusep<a href="$cgi;action=modify2;thread=$viewnum;id=$counter;d=1" onclick="return confirm('$txt{'739'}')">$img{'delete'}</a>~;
	}
}
	$yymain .= qq~
                </font></td>
              </tr>
            </table>
            <hr width="100%" size="1" class="hr">
            <font size="2">
            $message
            </font>
            </td>
          </tr><tr>
            <td class="$css" bgcolor="$windowbg" valign="bottom">
            <table width="100%" border="0">
              <tr>
                <td align="left"><font size="1">$lastmodified</font></td>
                <td align="right"><font size="1"><img src="$imagesdir/ip.gif" alt="" border="0"> $mip</font></td>
              </tr>
            </table>
            <font size="1">
            $userprofile[5]
            </font></td>
          </tr>
        </table>
        </td>
      </tr>
    </table>
    </td>
  </tr>
</table>~;
		$counter++;
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
            <td><font size=2><b>$txt{'139'}:</b> $pageindex</font></td>
            <td class="catbg" bgcolor="$color{'catbg'}" align=right><font size=2>
            $replybutton$notify$menusep
            <a href="$cgi;action=sendtopic;topic=$viewnum">$img{'sendtopic'}</a>$menusep
            <a href="$cgi;action=print;num=$viewnum" target="_blank">$img{'print'}</a>
            </font></td>
          </tr>
        </table>
        </td>
      </tr>
    </table>
    </td>
  </tr>
</table><br>
<table border="0" width="100%" cellpadding="0" cellspacing="0">~;
	  $yymain .= qq~
  <tr>
    <td align="left" valign="top" colspan="2">~;
	if(exists $moderators{$username} || $settings[7] eq 'Administrator') {
		$yymain .= qq~<font size="1"><b>$img{'admin_func'}</b>
        &nbsp;<a href="$cgi;action=movethread;thread=$viewnum">$img{'admin_move'}</a>$menusep
        <a href="$cgi;action=removethread;thread=$viewnum" onclick="return confirm('$txt{'162'}')">$img{'admin_rem'}</a>$menusep
        <a href="$cgi;action=lock;thread=$viewnum">$img{'admin_lock'}</a>$menusep
        <a href="$cgi;action=sticky;thread=$viewnum">$img{'admin_sticky'}</a>
</font>~;
	}
	$yymain .= qq~
    </td>
    <td valign="top" height="18" align="right"><font size="1">$selecthtml</font></td>
  </tr><tr>
    <td colspan="3" valign="top" align="right"><font size="1">$nav</font></td>
  </tr>
</table>
~;
	$yytitle = $msubthread;
	&template;
	exit;
}

1;