###############################################################################
# InstantMessage.pl                                                           #
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

$instantmessageplver = "1 Gold - SP 1.4";

sub IMIndex {
	if( $username eq 'Guest' ) { &fatal_error($txt{'147'}); }

	fopen(IM, "$memberdir/$username.msg");
	@immessages = <IM>;
	fclose(IM);
	$imbox = $txt{'316'};
	$txt{'412'} =~ s~IMBOX~$imbox~g;
	$img{'im_delete'} =~ s~IMBOX~$imbox~g;
	# Read Membergroups
	fopen(FILE, "$vardir/membergroups.txt");
	@membergroups = <FILE>;
	fclose(FILE);

	&LoadCensorList;	# Load censor list.
	$sender = "im";		# Fix moderator showing in info

	$yymain .= qq~
<script language="JavaScript1.2" src="$ubbcjspath" type="text/javascript"></script>
<table border=0 width=100% cellspacing=0 cellpadding="0">
  <tr>
    <td valign=bottom><font size=2 class="nav"><B>
    <IMG SRC="$imagesdir/open.gif" BORDER=0>&nbsp;&nbsp;
    <A href="$scripturl" class="nav">$mbname</a><br>
    <IMG SRC="$imagesdir/tline.gif" BORDER=0><IMG SRC="$imagesdir/open.gif"  BORDER=0>&nbsp;&nbsp;
    <a href="$cgi;action=im" class="nav">$txt{'144'}</a><br>
    <img src="$imagesdir/tline2.gif" border="0"><img src="$imagesdir/open.gif" border="0">&nbsp;&nbsp;
    $txt{'316'}
    </b></font></td>
    <td align=right valign=bottom><font size=-1>
~;
	if( @immessages ) {
		$yymain .= qq~    <a href="$cgi;action=imremoveall;caller=1" onclick="return confirm('$txt{'412'}')">$img{'im_delete'}</a>$menusep~;
	}

	$yymain .= qq~
    <a href="$cgi;action=imoutbox">$img{'im_outbox'}</a>$menusep<a href="$cgi;action=imsend">$img{'im_new'}</a>$menusep<a href="$cgi;action=im">$img{'im_reload'}</a>$menusep<a href="$cgi;action=imprefs">$img{'im_config'}</a>
    </font></td>
  </tr>
</table>
<table border=0 width="100%" cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}">&nbsp;<b>$txt{'317'}</b></font></td>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'318'}</b></font></td>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'319'}</b></font></td>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}">&nbsp;</font></td>
  </tr>
~;
	unless( @immessages ) {
		$yymain .= qq~
  <tr>
    <td class="windowbg" colspan=4 bgcolor="$color{'windowbg'}"><font size=2>$txt{'151'}</font></td>
  </tr>
~;
	}
	@bgcolors = ( $color{windowbg}, $color{windowbg2} );
	@bgstyles = qw~windowbg windowbg2~;
	$bgcolornum = scalar @bgcolors;
	$bgstylenum = scalar @bgstyles;

	for( $counter = 0; $counter < @immessages; $counter++ ) {
		$windowbg = $bgcolors[($counter % $bgcolornum)];
		$windowcss = $bgstyles[($counter % $bgstylenum)];
		chomp $immessages[$counter];
		($musername, $msub, $mdate, $immessage, $messageid, $mips) = split( /\|/, $immessages[$counter] );
		if( $messageid < 100 ) { $messageid = $counter; }
		if( $msub eq '' ) { $msub = $txt{'24'}; }
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$msub =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		if( $musername ne 'Guest' && ! $yyUDLoaded{$musername} && -e("$memberdir/$musername.dat") ) {
			# If user is not in memory, s/he must be loaded.
			&LoadUserDisplay($musername);
		}
      	$usernamelink = qq~<a href="$scripturl?board=$currentboard;action=viewprofile;username=$useraccount{$musername}"><font size="2">$musername</font></a>~;
		if (!$useraccount{$musername}) {$usernamelink = qq~<font size="2">$musername</font>~;}
		$mydate = &timeformat($mdate);
		$yymain .= qq~
		  <tr>
		    <td class="$windowcss" bgcolor="$windowbg"><font size=2>$mydate</font></td>
		    <td class="$windowcss" bgcolor="$windowbg"><font size=2>$usernamelink</font></td>
		    <td class="$windowcss" bgcolor="$windowbg"><font size=2><a href="#$messageid"><b>$msub</b></a></font></td>
		    <td class="$windowcss" bgcolor="$windowbg"><font size="2"><a href="$cgi;action=imremove;caller=1;id=$messageid" onclick="return confirm('$txt{'739'}')">$img{'im_remove'}</a></font> </td>
		  </tr>
		~;
	}

	if( @immessages ) {
		$yymain .= qq~
</table>\n\n<br>
<table border=0 width="100%" cellspacing=1 cellpadding="4" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
     <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}">&nbsp;<b>$txt{'29'}</b></font></td>
     <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'118'}</b></font></td>
  </tr>
~;
	}

	for( $counter = 0; $counter < @immessages; $counter++ ) {
		$windowbg = $bgcolors[($counter % $bgcolornum)];
		$windowcss = $bgstyles[($counter % $bgstylenum)];
		($musername, $msub, $mdate, $immessage, $messageid) = split( /\|/, $immessages[$counter] );
		if( $messageid < 100 ) { $messageid = $counter; }
		if( $msub eq '' ) { $msub = $txt{'24'}; }
		$mydate = &timeformat($mdate);
		if( $musername ne 'Guest' && ! $yyUDLoaded{$musername} && -e("$memberdir/$musername.dat") ) {
			# If user is not in memory, s/he must be loaded.
			&LoadUserDisplay($musername);
		}
		if( $yyUDLoaded{$musername} ) {
			@userprofile = @{$userprofile{$musername}};
			$displayname = $userprofile[1];
			$star = $memberstar{$musername};
			$memberinfo = $memberinfo{$musername};
			$icqad = $icqad{$musername};
			$yimon = $yimon{$musername};
			$usernamelink = qq~<a href="$scripturl?board=$currentboard;action=viewprofile;username=$useraccount{$musername}"><font size="2"><b>$userprofile[1]</b></font></a>~;
			$profbutton = $profilebutton && $musername ne 'Guest' ? qq~$menusep<a href="$scripturl?action=viewprofile;username=$useraccount{$musername}">$img{'viewprofile'}</a>~ : '';
			$postinfo = qq~$txt{'26'}: $userprofile[6]<br>~;
			$memail = $userprofile[2];
		}
		if (!$useraccount{$musername}) {$usernamelink=qq~<font  size="2"><B>$musername</B></font>~; $memberinfo ="<b>$txt{'783'}</b>"; $displayname = $musername;}
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$immessage =~ s~\Q$tmpa\E~$tmpb~gi;
			$msub =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		$message = $immessage; # put the message back in the proper variable for doing ubbc
		&wrap;
		if($enable_ubbc) { if(!$yyYaBBCloaded) { require "$sourcedir/YaBBC.pl"; } &DoUBBC; }
		&wrap2;
		$yymain .= qq~
  <tr>
    <td class="$windowcss" bgcolor="$windowbg" width="160" valign="top" height="100%">
    $usernamelink<br>
    <font size="1">$memberinfo<br>
    ~;
	if ($useraccount{$musername}) {
    $yymain .= qq~
    $star<br><br>
    $userprofile[13]$userprofile[12]
    <BR>$userprofile[8] $icqad &nbsp; $userprofile[10] $yimon &nbsp; $userprofile[9]<BR>
    <BR>$userprofile[11]
    $postinfo
    ~;
    }
    $yymain .= qq~
    </font></td>
    <td class="$windowcss" bgcolor="$windowbg" valign=top>
    <table border="0" cellspacing="0" cellpadding="3" width="100%" height="100%" align="center" bgcolor="$color{'bordercolor'}" class="bordercolor">
      <tr class="$windowcss" bgcolor="$windowbg" height="10" width="100%">
        <td class="$windowcss" bgcolor="$windowbg"><a name="$messageid"><font size="1">&nbsp;<b>$msub</b></font></a></td>
        <td class="$windowcss" bgcolor="$windowbg" align="right"><font size="1"><b>$txt{'30'}:</b> $mydate</font></td>
      </tr><tr height="*">
        <td colspan="2" class="$windowcss" bgcolor="$windowbg" height="100%">
        <hr width="100%" size="1" class="hr">
        <font size=2>$message</font>
        </td>
      </tr><TR height="10">
        <td colspan="2" class="$windowcss" bgcolor="$windowbg" height="10">
        <font size=2>$userprofile[5]</font>
        <hr width="100%" size="1" class="hr">
        </td>
      </tr><tr height="10" width="100%">
	<td class="$windowcss" bgcolor="$windowbg" height="10">
        <font size=2>
~;
if ($useraccount{$musername}) {
if ($userprofile[19] ne "checked" || $settings[7] eq "Administrator" || $allow_hide_email ne 1) {
$yymain .= qq~
	$userprofile[4]<a href="mailto:$memail">$img{'email'}</a>$profbutton
~;
} else {
$yymain .= qq~
	$userprofile[4]$profbutton
~;
}
}
$yymain .= qq~
        </font></td>
        <td class="$windowcss" bgcolor="$windowbg" height="10" align="right"><font size=2>
~;
		if ($useraccount{$musername}) {$yymain .= qq~<a href="$cgi;action=imsend;caller=1;num=$counter;quote=1;to=$useraccount{$musername}">$img{'replyquote'}</a>$menusep<a href="$cgi;action=imsend;caller=1;num=$counter;reply=1;to=$useraccount{$musername}">$img{'reply'}</a>~;}
$yymain .= qq~
		$menusep<a href="$cgi;action=imremove;caller=1;id=$messageid" onclick="return confirm('$txt{'739'}')">$img{'im_remove'}</a>
        </font></td>
      </tr>
    </table>
    </td>
  </tr>
~;
	}
	$yymain .= qq~
</table>
~;
	$yytitle = $txt{'143'};
	&template;
	exit;
}

sub IMOutbox {
	if ($username eq 'Guest') { &fatal_error($txt{'147'}); }
	$imbox = $txt{'320'};
	$txt{'412'} =~ s~IMBOX~$imbox~g;
	$img{'im_delete'} =~ s~IMBOX~$imbox~g;
	# Read all messages
	fopen(FILE, "$memberdir/$username.outbox");
	@ommessages = <FILE>;
	fclose(FILE);
	$sender = "im";		# Fix moderator showing in info
	# Read Membergroups
	fopen(FILE, "$vardir/membergroups.txt");
	@membergroups = <FILE>;
	fclose(FILE);
	&LoadCensorList;	# Load censor list.

	$yymain .= qq~
<script language="JavaScript1.2" src="$ubbcjspath" type="text/javascript"></script>
<table border=0 width=100% cellspacing=0>
  <tr>
    <td valign=bottom><font size=2 class="nav"><B>
    <img src="$imagesdir/open.gif" border=0>&nbsp;&nbsp;
    <a href="$scripturl" class="nav">$mbname</a><br>
    <img src="$imagesdir/tline.gif" border=0><img src="$imagesdir/open.gif"  BORDER=0>&nbsp;&nbsp;
    <a href="$cgi;action=im" class="nav">$txt{'144'}</a><br>
    <img src="$imagesdir/tline2.gif" border="0"><img src="$imagesdir/open.gif" border="0">&nbsp;&nbsp;
    $txt{'320'}
    </b></font></td>
    <td align=right valign=bottom><font size=-1>
~;
	if( @ommessages ) {
		$yymain .= qq~    <a href="$cgi;action=imremoveall;caller=2" onclick="return confirm('$txt{'412'}')">$img{'im_delete'}</a>$menusep~;
	}
	$yymain .= qq~
    <a href="$cgi;action=im">$img{'im_inbox'}</a>$menusep<a href="$cgi;action=imsend">$img{'im_new'}</a>$menusep<a href="$cgi;action=im">$img{'im_reload'}</a>$menusep<a href="$cgi;action=imprefs">$img{'im_config'}</a>
    </font></td>
  </tr>
</table>
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size="2" class="text1" color="$color{'titletext'}">&nbsp;<b>$txt{'317'}</b></font></td>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'324'}</b></font></td>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'319'}</b></font></td>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}">&nbsp;</font></td>
  </tr>
~;
	# Display Message if there are no Messages in Users Outbox
	unless( @ommessages ) {
		$yymain .= qq~
  <tr>
    <td class="windowbg" colspan=4 bgcolor="$color{'windowbg'}"><font size=2>$txt{'151'}</font></td>
  </tr>
~;
	}

	@bgcolors = ( $color{windowbg}, $color{windowbg2} );
	@bgstyles = qw~windowbg windowbg2~;
	$bgcolornum = scalar @bgcolors;
	$bgstylenum = scalar @bgstyles;

	for( $counter = 0; $counter < @ommessages; $counter++ ) {
		$windowbg = $bgcolors[($counter % $bgcolornum)];
		$windowcss = $bgstyles[($counter % $bgstylenum)];
		chomp $ommessages[$counter];
		($musername, $msub, $mdate, $immessage, $messageid, $mips) = split( /\|/, $ommessages[$counter] );
		if( $messageid < 100 ) { $messageid = $counter; }
		if( $msub eq '' ) { $msub = $txt{'24'}; }
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$msub =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		if( $musername ne 'Guest' && ! $yyUDLoaded{$musername} && -e("$memberdir/$musername.dat") ) {
			# If user is not in memory, s/he must be loaded.
			&LoadUserDisplay($musername);
		}
      	$usernamelink = qq~<a href="$scripturl?board=$currentboard;action=viewprofile;username=$useraccount{$musername}"><font size="2">$musername</font></a>~;
		if (!$useraccount{$musername}) {$usernamelink = qq~<font size="2">$musername</font>~;}
		$mydate = &timeformat($mdate);
		$yymain .= qq~
  <tr>
    <td class="$windowcss" bgcolor="$windowbg"><font size=2>$mydate</font></td>
    <td class="$windowcss" bgcolor="$windowbg"><font size=2>$usernamelink</font></td><td class="$windowcss" bgcolor="$windowbg"><font size=2><a href="#$messageid"><b>$msub</b></a></font></td>
    <td class="$windowcss" bgcolor="$windowbg"><font size="2"><a href="$cgi;action=imremove;caller=2;id=$messageid" onclick="return confirm('$txt{'739'}')">$img{'im_remove'}</a></font></td>
  </tr>
~;
	}

	# Output all messages
	if( @ommessages ) {
	$yymain .= qq~
</table>
<br>
<table border="0" width="100%" cellspacing="1" cellpadding="4" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}">&nbsp;<b>$txt{'535'}</b></font></td>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'118'}</b></font></td>
  </tr>
~;
	}

	for( $counter = 0; $counter < @ommessages; $counter++ ) {
		$windowbg = $bgcolors[($counter % $bgcolornum)];
		$windowcss = $bgstyles[($counter % $bgstylenum)];
		($musername, $msub, $mdate, $immessage, $messageid) = split( /\|/, $ommessages[$counter] );
		if( $messageid < 100 ) { $messageid = $counter; }
		if( $msub eq '' ) { $msub = $txt{'24'}; }
		$mydate = &timeformat($mdate);
		if( $musername ne 'Guest' && ! $yyUDLoaded{$musername} && -e("$memberdir/$musername.dat") ) {
			# If user is not in memory, s/he must be loaded.
			&LoadUserDisplay($musername);
		}
		if( $yyUDLoaded{$musername} ) {
			@userprofile = @{$userprofile{$musername}};
			$star = $memberstar{$musername};
			$memberinfo = $memberinfo{$musername};
			$icqad = $icqad{$musername};
			$yimon = $yimon{$musername};
			$usernamelink = qq~<a href="$scripturl?board=$currentboard;action=viewprofile;username=$useraccount{$musername}"><font size="2"><B>$userprofile[1]</b></font></a>~;
			$profbutton = $profilebutton && $musername ne 'Guest' ? qq~$menusep<a href="$scripturl?action=viewprofile;username=$useraccount{$musername}">$img{'viewprofile'}</a>~ : '';
			$postinfo = qq~$txt{'26'}: $userprofile[6]<br>~;
			$memail = $userprofile[2];
		}
		if (!$useraccount{$musername}) {$usernamelink=qq~<font size="2"><B>$musername</B></font>~; $memberinfo ="<b>$txt{'783'}</b>"; $displayname = $musername;}
		$message = $immessage; # put the message back in the proper variable for doing ubbc
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$message =~ s~\Q$tmpa\E~$tmpb~gi;
			$msub =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		&wrap;
		if($enable_ubbc) { if(!$yyYaBBCloaded) { require "$sourcedir/YaBBC.pl"; } &DoUBBC; }
		&wrap2;
		$yymain .= qq~
  <tr>
    <td class="$windowcss" bgcolor="$windowbg" width="160" valign="top" height="100%">
    $usernamelink<br>
    <font size="1">$memberinfo<br>
    ~;
	if ($useraccount{$musername}) {
    $yymain .= qq~
    $star<br><br>
    $userprofile[13]$userprofile[12]
    <br>$userprofile[8] $icqad &nbsp; $userprofile[10] $yimon &nbsp; $userprofile[9]<br>
    <br>$userprofile[11]
    $postinfo
    ~;
    }
    $yymain .= qq~
    </font></td>
    <td class="$windowcss" bgcolor="$windowbg" valign=top>
    <table border="0" cellspacing="0" cellpadding="3" width="100%" height="100%" align="center" bgcolor="$color{'bordercolor'}" class="bordercolor">
      <tr class="$windowcss" bgcolor="$windowbg" height="10" width="100%">
        <td class="$windowcss" bgcolor="$windowbg"><font size="1"><a name="$messageid">&nbsp;<b>$msub</b></a></font></td>
        <td class="$windowcss" bgcolor="$windowbg" align="right"><font size="1"><b>$txt{'30'}:</b> $mydate</font></td>
      </tr><tr height="100%">
        <td colspan="2" class="$windowcss" bgcolor="$windowbg" height="100%">
        <hr width="100%" size="1" class="hr">
        <font size=2>$message</font>
        </td>
      </tr><TR height="10">
        <td colspan="2" class="$windowcss" bgcolor="$windowbg" height="10">
        <font size=2>$userprofile[5]</font>
        <hr width="100%" size="1" class="hr">
        </td>
      </tr><tr height="10" width="100%">
	<td class="$windowcss" bgcolor="$windowbg" height="10">
        <font size=2>
~;
if ($useraccount{$musername}) {
if ($userprofile[19] ne "checked" || $settings[7] eq "Administrator" || $allow_hide_email ne 1) {
$yymain .= qq~
	$userprofile[4]<a href="mailto:$memail">$img{'email'}</a>$profbutton
~;
} else {
$yymain .= qq~
	$userprofile[4]$profbutton
~;
}
}
$yymain .= qq~
        </font></td>
        <td class="$windowcss" bgcolor="$windowbg" align="right" height="10"><font size=2>
~;
		if ($useraccount{$musername}) {$yymain .= qq~<a href="$cgi;action=imsend;caller=2;num=$counter;quote=1;to=$useraccount{$musername}">$img{'replyquote'}</a>$menusep<a href="$cgi;action=imsend;caller=2;num=$counter;reply=1;to=$useraccount{$musername}">$img{'reply'}</a>~;}
$yymain .= qq~
        $menusep<a href="$cgi;action=imremove;caller=2;id=$messageid" onclick="return confirm('$txt{'739'}')">$img{'im_remove'}</a>
        </font></td>
      </tr>
    </table>
    </td>
  </tr>
~;
;
	}
	$yymain .= qq~
</table>
~;
	$yytitle = $txt{'143'};
	&template;
	exit;
}


sub IMPost {
	if($username eq 'Guest') { &fatal_error($txt{'147'}); }
	my( @messages, $mdate, $mip, $mmessage);
	if($INFO{'num'} ne "") {
		if($INFO{'caller'} == 1) { fopen(FILE, "$memberdir/$username.msg"); }
		else { fopen(FILE, "$memberdir/$username.outbox"); }
		@messages = <FILE>;
		fclose(FILE);

		($mfrom, $sub, $mdate, $mmessage, $mip) = split(/\|/,$messages[$INFO{'num'}]);
		$sub =~ s/Re: //g;

		if($INFO{'quote'} == 1) {
			$message = $mmessage;
			$message =~ s/<br>/\n/g;
			$message =~ s/\[quote\](\S+?)\[\/quote\]//isg;
			$message =~ s/\[(\S+?)\]//isg;
			$message = "\[quote\]$message\[/quote\]\n";
			if ($message =~ /\#nosmileys/isg) {$message =~ s/\#nosmileys//isg; $nscheck="checked";}
			$sub = "Re: $sub";
		}
		if($INFO{'reply'} == 1) { $sub = "Re: $sub";}
	}

	if ($sub eq "") { $sub = "$txt{'24'}"; }

	$yymain .= qq~
<table border=0 width="90%" cellpadding="3" align="center" cellspacing=0>
  <tr>
    <td valign=bottom><font size=2 class="nav"><B>
    <IMG SRC="$imagesdir/open.gif" BORDER=0>&nbsp;&nbsp;
    <A href="$scripturl" class="nav">$mbname</A><br>
    <img src="$imagesdir/tline.gif" BORDER=0><img src="$imagesdir/open.gif" border="0">&nbsp;&nbsp;
    <a href="$cgi;action=im" class="nav">$txt{'144'}</A><br>
    <img src="$imagesdir/tline2.gif" BORDER=0><img src="$imagesdir/open.gif" border="0">&nbsp;&nbsp;
    $txt{'321'}
    </B></font></td>
    <td align=right valign=bottom><font size=-1>
    <a href="$cgi;action=im">$img{'im_inbox'}</a>$menusep<a href="$cgi;action=imoutbox">$img{'im_outbox'}</a>$menusep<a href="$cgi;action=im">$img{'im_reload'}</a>$menusep<a href="$cgi;action=imprefs">$img{'im_config'}</a>
    </font></td>
  </tr>
</table>
~;
	$submittxt = "$txt{'148'}";
	$destination = "imsend2";
	$is_preview = 0;
	$post = "imsend";
	$preview = "previewim";
	$icon = "xx";
	require "$sourcedir/Post.pl";
	$yytitle = $txt{'148'};
	&Postpage;
	&doshowims;
	&template;
	exit;
}

sub IMPost2
{
if($username eq 'Guest') { &fatal_error($txt{'147'}); }
my( @imconfig, @ignore, $igname, $messageid, $subject, $message, @recipient, $ignored );

$subject = $FORM{'subject'};
$subject =~ s/\A\s+//;
$subject =~ s/\s+\Z//;
$message = $FORM{'message'};
if (length($message)>$MaxMessLen) { &fatal_error("$txt{'499'}"); }

&fatal_error("$txt{'752'}") unless($FORM{'to'});
&fatal_error("$txt{'77'}") unless($subject);
&fatal_error("$txt{'78'}") unless($message);

$mmessage = $message;
$msubject = $subject;

&ToHTML($message);
$message =~ s/\t/ \&nbsp; \&nbsp; \&nbsp;/g;
$message =~ s/\cM//g;
$message =~ s/\n/<br>/g;

if ($FORM{'ns'} eq "NS") {$message .= "#nosmileys";}

if( $FORM{'previewim'} ) {
	require "$sourcedir/Post.pl";
	&Preview;
}

@multiple = split(/,/, $FORM{'to'});
foreach $db (@multiple) {
	chomp $db;
	$ignored = 0;

	$db =~ s/\A\s+//;
	$db =~ s/\s+\Z//;
	$db =~ s/[^0-9A-Za-z#%+,-\.@^_]//g;

	# Check Ignore-List
	if (-e("$memberdir/$db.imconfig")) {
		fopen(FILE, "$memberdir/$db.imconfig");
		@imconfig = <FILE>;
		fclose(FILE);

		# Build Ignore-List
		$imconfig[0] =~ s/[\n\r]//g;
		$imconfig[1] =~ s/[\n\r]//g;

		@ignore = split(/\|/,$imconfig[0]);

		# If User is on Recipient's Ignore-List, show Error Message
		foreach $igname (@ignore) {
			#adds ignored user's name to array which error list will be built from later
			if ($igname eq $username) {push(@nouser, $db); $ignored = 1;}
			if ($igname eq "*") {push(@nouser, "$txt{'761'} $db $txt{'762'};"); $ignored = 1;}
		}

	}

	if (!(-e("$memberdir/$db.dat"))) {
		#adds invalid user's name to array which error list will be built from later
		push(@nouser, $db);
		$ignored = 1;
	}

	if(!$ignored) {
	# Create unique Message ID = Time & ProccessID
	$messageid = $^T.$$;

	# Add message to outbox
	if(-e("$memberdir/$username.outbox")) { fopen(FILE, ">>$memberdir/$username.outbox", 1); }
	else { fopen(FILE, ">$memberdir/$username.outbox", 1); }
	print FILE "$db|$subject|$date|$message|$messageid|$user_ip\n";
	fclose(FILE);

	unless ($sendedto =~ /\#$db\#/) {
		# Send message to user
		fopen(FILE, ">>$memberdir/$db.msg");
		print FILE "$username|$subject|$date|$message|$messageid|$user_ip\n";
		fclose(FILE);
	}
	$sendedto.="#$db#";

	# Send notification
	if ($imconfig[1]==1) {
		fopen(FILE, "$memberdir/$db.dat");
		@recipient = <FILE>;
		fclose(FILE);
		$mydate = &timeformat($date);
		$recipient[2] =~ s/[\n\r]//g; # get email address
		if ($recipient[2] ne "") {
			$fromname = $settings[1];
			$txt{'561'} =~ s~SUBJECT~$msubject~g;
			$txt{'561'} =~ s~SENDER~$fromname~g;
			$txt{'561'} =~ s~DATE~$mydate~g;
			$txt{'562'} =~ s~SUBJECT~$msubject~g;
			$txt{'562'} =~ s~MESSAGE~$mmessage~g;
			$txt{'562'} =~ s~SENDER~$fromname~g;
			$txt{'562'} =~ s~DATE~$mydate~g;
			&sendmail($recipient[2],$txt{'561'},$txt{'562'});
		}
	}
	}
}  #end foreach loop
#if there were invalid usernames in the recipient list, these names are listed after all valid users have been IMed
if (@nouser) {
	$badusers = join(" $txt{'763'} ", @nouser);
	$badusers =~ s/; $txt{'763'}/;/;
	&fatal_error("$badusers $txt{'747'}");
}

$yySetLocation = qq~$cgi;action=im~;
&redirectexit;
}

sub IMRemove
{
	if($username eq 'Guest') { &fatal_error($txt{'147'}); }
	my( @messages, $a, $musername, $msub, $mdate, $mmessage, $messageid, $mip );
	if ($INFO{'caller'} == 1) { fopen(FILE, "$memberdir/$username.msg"); }
	elsif ($INFO{'caller'} == 2) { fopen(FILE, "$memberdir/$username.outbox"); }
	@messages = <FILE>;
	fclose(FILE);

	if ($INFO{'caller'} == 1) { fopen(FILE, ">$memberdir/$username.msg", 1); }
	elsif ($INFO{'caller'} == 2) { fopen(FILE, ">$memberdir/$username.outbox", 1); }

	for ($a = 0; $a < @messages; $a++) {
		chomp $messages[$a];
		# ONLY delete MSG with correct ID
		($musername, $msub, $mdate, $mmessage, $messageid, $mip) = split(/\|/,$messages[$a]);

		# If Message-ID is < 100, user has used the old IM before
		if ($messageid < 100 ) {
			if($a ne $INFO{'id'}) { print FILE "$messages[$a]\n"; }
		} else {
	 		if($messageid ne "$INFO{'id'}") { print FILE "$messages[$a]\n"; }
      		}
   	}

   	fclose(FILE);
	my $redirect = $INFO{'caller'} == 1 ? 'im' : 'imoutbox';
	$yySetLocation = qq~$cgi;action=$redirect~;
	&redirectexit;

}

sub IMPreferences {
	if ($username eq 'Guest') { &fatal_error($txt{'147'}); }
	my( @imconfig, $sel0, $sel1 );
	if (-e("$memberdir/$username.imconfig")) {
		fopen(FILE, "$memberdir/$username.imconfig");
		@imconfig = <FILE>;
		fclose(FILE);
	}

	$imconfig[0] =~ s/[\n\r]//g;
	$imconfig[0] =~ s/\|/\n/g;
	$imconfig[1] =~ s/[\n\r]//g;

	if ($imconfig[1]) {
		$sel0='';
		$sel1=' selected';
	} else {
		$sel0=' selected';
		$sel1='';
	}
	$yymain .= qq~
<table border=0 width=100% cellspacing=0>
<tr>
	<td valign=bottom><font size=2 class="nav"><B>
	<img src="$imagesdir/open.gif" border="0">&nbsp;&nbsp;
	<a href="$scripturl" class="nav">$mbname</a>
	<br>
	<img src="$imagesdir/tline.gif" border="0"><img src="$imagesdir/open.gif" border="0">&nbsp;&nbsp;
	<a href="$cgi;action=im" class="nav">$txt{'144'}</A>
	<br>
	<img src="$imagesdir/tline2.gif" border="0"><img src="$imagesdir/open.gif" border="0">&nbsp;&nbsp;
	$txt{'323'}
	</b></font></td>
<td align=right valign=bottom>
~;
	$yymain .= qq~
<a href="$cgi;action=im">$img{'im_inbox'}</a> <a href="$cgi;action=imoutbox">$img{'im_outbox'}</a> <a href="$cgi;action=imsend">$img{'im_new'}</a> <a href="$cgi;action=im">$img{'im_reload'}</a>
</td>
</tr>
</table>
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
 <tr>
  <td class="titlebg" bgcolor="$color{'titlebg'}"><font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'323'}</b></font></td>
 </tr>
 <tr>
  <td class="windowbg" bgcolor="$color{'windowbg'}">
   <form action="$cgi;action=imprefs2" method=post>
    <table border=0>
     <tr>
      <td valign=top>
       <font size=2><b>$txt{'325'}:</b></font><br><font size="1">$txt{'326'}</font>
      </td>
      <td>
       <font size=2><textarea name=ignore rows=10 cols=50 wrap=virtual>$imconfig[0]</textarea></font>
      </td>
     </tr>
     <tr>
      <td valign=top>
       <font size=2><b>$txt{'327'}:</b></font>
      </td>
      <td>
       <font size=2>
	<select name="notify">
	 <option value="0"$sel0>$txt{'164'}
	 <option value="1"$sel1>$txt{'163'}
	</select>
       </font>
      </td>
     </tr>
     <tr>
      <td>
      	&nbsp;
      </td>
      <td>
       <input type=submit value="$txt{'328'}">
       <input type=reset value="$txt{'278'}">
      </td>
     </tr>
    </table>
   </form>
  </td>
 </tr>
</table>
~;
	$yytitle = "$txt{'323'}: $txt{'144'}";
	&template;
	exit;
}

sub IMPreferences2 {
	if($username eq 'Guest') { &fatal_error($txt{'147'}); }
	my( $ignorelist, $notify );
	$ignorelist = "$FORM{'ignore'}";
	$notify = "$FORM{'notify'}";

	$ignorelist =~ s~\A\n\s*~~;
	$ignorelist =~ s~\s*\n\Z~~;
	$ignorelist =~ s~\n\s*\n~\n~g;
	$ignorelist =~ s~[\n\r]~\|~g;
	$ignorelist =~ s~\|\|~\|~g;
	$notify =~ s~[\n\r]~~g;

	fopen(FILE, "+>$memberdir/$username.imconfig");
	print FILE "$ignorelist\n$notify\n";
	fclose(FILE);
	$yySetLocation = qq~$cgi;action=imprefs~;
	&redirectexit;
}

sub KillAll {
	if($username eq 'Guest') { &fatal_error($txt{'147'}); }
	if ($INFO{'caller'} == 1) {
		unlink("$memberdir/$username.msg");
		$redirect = 'im';
	} elsif ($INFO{'caller'} == 2) {
		unlink("$memberdir/$username.outbox");
		$redirect = 'imoutbox';
	}
	$yySetLocation = qq~$cgi;action=$redirect~;
	&redirectexit;
}

sub doshowims {
	my $tempdate;
	if($INFO{'num'} ne "") {
		chomp $immessages[$INFO{'num'}];
		($musername, $msub, $mdate, $message, $messageid, $mips) = split( /\|/, $immessages[$INFO{'num'}]);
		# Load Censor List
		&LoadCensorList;
		$yymain .= qq~
	<BR><BR>
	<table cellspacing=1cellpadding=0 width="90%" align="center" bgcolor="$color{'bordercolor'}" class="bordercolor"><tr><td>
	<table class="windowbg" cellspacing="1" cellpadding="2" width="100%" align="center" bgcolor="$color{'windowbg'}">
	<tr><td class="titlebg" bgcolor="$color{'titlebg'}" colspan="2"><font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'319'}: $msub</b></td></tr>
	~;
		$tempdate = &timeformat($mdate);
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$message =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		&wrap;
		if($enable_ubbc) { if(!$yyYaBBCloaded) { require "$sourcedir/YaBBC.pl"; } &DoUBBC; }
		&wrap2;
		$yymain .= qq~
	<tr><td align=left class="catbg"><font size="1">$txt{'318'}: $musername</font></td><td class="catbg" align=right><font size="1">$txt{'30'}: $tempdate</font></td></tr>
	<tr><td class="windowbg2" colspan=2 bgcolor="$color{'windowbg2'}"><font size="1">$message</font></td></tr></table></td></tr></table>\n
	~;

	}
}

1;