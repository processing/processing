###############################################################################
# Search.pl                                                                   #
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

$searchplver = "1 Gold - SP 1.4";

sub plushSearch1 {
	my( @categories, %cat, $curcat, %catname, %cataccess, %catboards, $openmemgr, @membergroups, $tmpa, %openmemgr, $curboard, @threads, @boardinfo, $counter );
	@categories = ();
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	&LoadCensorList;	# Load Censor List

	$searchpageurl = $curposlinks ? qq~<a href="$scripturl?action=search" class="nav">$txt{'182'}</a>~ : $txt{'182'};
	$yymain .= qq~
<script language="JavaScript1.2" type="text/javascript">
<!-- Begin
function changeBox(cbox) {
  box = eval(cbox);
  box.checked = !box.checked;
}
function checkAll() {
  for (var i = 0; i < document.searchform.elements.length; i++) {
  	if(document.searchform.elements[i].name != "subfield" && document.searchform.elements[i].name != "msgfield") {
    		document.searchform.elements[i].checked = true;
    	}
  }
}
function uncheckAll() {
  for (var i = 0; i < document.searchform.elements.length; i++) {
  	if(document.searchform.elements[i].name != "subfield" && document.searchform.elements[i].name != "msgfield") {
    		document.searchform.elements[i].checked = false;
    	}
  }
}
//-->
</script>
<table width="80%" align="center" border="0" cellpadding="2" cellspacing="0">
  <tr>
    <td><font size=2 class="nav"><img src="$imagesdir/open.gif" BORDER=0>&nbsp;&nbsp;<b><a href="$scripturl" class="nav">$mbname</a></b><br><img src="$imagesdir/tline.gif" BORDER=0><img src="$imagesdir/open.gif" border=0>&nbsp;&nbsp;<b>$searchpageurl</b></font></td>
  </tr>
</table>
<table border="0" width="80%" cellspacing="1" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
    <td bgcolor="$color{'bordercolor'}" class="bordercolor">
    <form action="$scripturl?action=search2" method="post" name="searchform">
    <table class="titlebg" bgcolor="$color{'titlebg'}" width="100%">
      <tr>
        <td class="titlebg" bgcolor="$color{'titlebg'}"><img src="$imagesdir/search.gif" alt=""> <font size="2" class="text1" color="$color{'titletext'}">$txt{'183'}</font></td>
      </tr>
    </table>
    </td>
  </tr><tr>
    <td bgcolor="$color{'bordercolor'}" class="bordercolor">
    <table width="100%" cellpadding="4" cellspacing="0" bgcolor="$color{'windowbg'}" class="windowbg">
      <tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><b>$txt{'582'}:</b></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
        <input type=text size=30 name="search">&nbsp;
        <select name="searchtype">
         <option value="allwords" selected>$txt{'343'}</option>
         <option value="anywords">$txt{'344'}</option>
         <option value="asphrase">$txt{'345'}</option>
        </select>
        </font></td>
      </tr><tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><b>$txt{'583'}:</B></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
        <input type=text size=30 name="userspec">&nbsp;
        <select name="userkind">
         <option value="any">$txt{'577'}</option>
         <option value="starter">$txt{'186'}</option>
         <option value="poster">$txt{'187'}</option>
         <option value="noguests" selected>$txt{'346'}</option>
         <option value="onlyguests">$txt{'572'}</option>
        </select>
        </font></td>
      </tr><tr>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="top"><font size=2><b>$txt{'189'}:</B></font></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2>
        <table border="0" cellpadding="1" cellspacing="0">
          <tr>
~;
	$counter = 1;
	foreach $curcat (@categories) {
		chomp $curcat;
		fopen(FILE, "$boardsdir/$curcat.cat");
		$catname{$curcat} = <FILE>;
		chomp $catname{$curcat};
		$cataccess{$curcat} = <FILE>;
		chomp $cataccess{$curcat};
		@{$catboards{$curcat}} = <FILE>;
		fclose(FILE);
		$openmemgr{$curcat} = 0;
		@membergroups = split( /,/, $cataccess{$curcat} );
		foreach $tmpa (@membergroups) {
			if( $tmpa eq $settings[7]) { $openmemgr{$curcat} = 1; last; }
		}
		if( ! $cataccess{$curcat} || $settings[7] eq 'Administrator' ) {
			$openmemgr{$curcat} = 1;
		}
		unless( $openmemgr{$curcat} ) { next; }
		boardcheck: foreach $curboard (@{$catboards{$curcat}}) {
			chomp $curboard;
			fopen(FILE, "$boardsdir/$curboard.dat");
			@boardinfo = <FILE>;
			fclose(FILE);
			chomp @boardinfo;
			@{$boardinfo{$curboard}} = @boardinfo;
			$cat{$curboard} = $curcat;
			$yymain .= qq~<td width="50%"><font size="2"><input type=checkbox name="brd$counter" value="$curboard" checked><span style="cursor:hand;" onClick="changeBox('document.searchform.brd$counter')">$boardinfo[0]</span></font></td>~;
			unless( $counter % 2 ) { $yymain .= qq~</tr><tr>~; }
			++$counter;
		}
	}
	$yymain .= qq~
          </tr>
        </table><BR>
        <INPUT TYPE="checkbox" ONCLICK="if (this.checked) checkAll(); else uncheckAll();" checked><font size="2"><i>$txt{'737'}</i></font>
        </font></td>
      </tr><tr>

        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><b>$txt{'573'}:</B></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
        <table border="0" cellpadding="4" cellspacing="0">
          <tr>
            <td><font size="2"><input type=checkbox name=subfield value=on checked><span style="cursor:hand;" onClick="changeBox('document.searchform.subfield')">$txt{'70'}</span></font></td>
            <td><font size="2"><input type=checkbox name=msgfield value=on checked><span style="cursor:hand;" onClick="changeBox('document.searchform.msgfield')">$txt{'72'}</span></font></td>
          </tr>
        </table>
        </font></td>
      </tr><tr>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2><B>$txt{'575'} $txt{'574'}:</B></font></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2>
        <input type=text name=minripe value=0 size=3 maxlength=5> $txt{'578'} +
        <input type=text name=minage value=0 size=5 maxlength=5> $txt{'579'}
        </font></td>
      </tr><tr>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2><B>$txt{'576'} $txt{'574'}:</B></font></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2>
        <input type=text name=maxripe value=0 size=3 maxlength=5> $txt{'578'} +
        <input type=text name=maxage value=7 size=5 maxlength=5> $txt{'579'}
        </font></td>
      </tr><tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><b>$txt{'191'}</B></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
        <input type=text size=5 name=numberreturned maxlength=5 value=25></font></td>
      </tr><tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}" colspan="2" align="center"><font size="2">
        <input type=hidden name=action value=dosearch>
        <input type="submit" name="submit" value="$txt{'182'}"><BR><BR>
        </font></td>
      </tr>
    </table>
    </td>
  </tr>
</table>
</form>
<script language="JavaScript"> <!--
	document.searchform.search.focus();
//--> </script>
~;
	$yytitle = $txt{'183'};
	&template;
	exit;
}

sub plushSearch2 {
	my $minripe = $FORM{'minripe'} || 0;
	my $minage = $FORM{'minage'} || 0;
	my $maxripe = $FORM{'maxripe'} || 0;
	my $maxage = $FORM{'maxage'} || 7;
	my $display = $FORM{'numberreturned'} || 25;
	if( $minripe =~ /\D/ ) { &fatal_error($txt{'337'}); }
	if( $minage =~ /\D/ ) { &fatal_error($txt{'337'}); }
	if( $maxripe =~ /\D/ ) { &fatal_error($txt{'337'}); }
	if( $maxage =~ /\D/ ) { &fatal_error($txt{'337'}); }
	if( $display =~ /\D/ ) { &fatal_error($txt{'337'}); }

	my $userkind = $FORM{'userkind'};
	my $userspec = $FORM{'userspec'};
	if( $userkind eq 'starter' ) { $userkind = 1; }
	elsif( $userkind eq 'poster' ) { $userkind = 2; }
	elsif( $userkind eq 'noguests' ) { $userkind = 3; }
	elsif( $userkind eq 'onlyguests' ) { $userkind = 4; }
	else { $userkind = 0; $userspec = ''; }
	if ($userspec =~ m~/~){ &fatal_error($txt{'224'}); }
	if ($userspec =~ m~\\~){ &fatal_error($txt{'225'}); }
	$userspec =~ s/\A\s+//;
	$userspec =~ s/\s+\Z//;
	$userspec =~ s/[^0-9A-Za-z#%+,-\.@^_]//g;

	my $searchtype = $FORM{'searchtype'};
	my $search = $FORM{'search'};
	if( $searchtype eq 'anywords' ) { $searchtype = 2; }
	elsif( $searchtype eq 'asphrase' ) { $searchtype = 3; }
	else { $searchtype = 1; }
	if ($search eq ""){ &fatal_error($txt{'754'}); }
	if ($search =~ m~/~){ &fatal_error($txt{'397'}); }
	if ($search =~ m~\\~){ &fatal_error($txt{'397'}); }
	if ($search =~ /\AIs UBB Good\?\Z/i) { &fatal_error("Many llamas have pondered this question for ages. They each came up with logical answers to this question, each being quite different. The consensus of their answers: UBB is a decent piece of software made by a large company. They, however, lack a strong supporting community, charge a lot for their software and the employees are very biased towards their own products. And so, once again, let it be written into the books that<BR><center><a href=\"http://yabb.xnull.com\"><img src=\"http://yabb.xnull.com/images/9870.gif\" alt=\"\" border=0></a><BR>YaBB is the greatest community software there ever was!</center>"); }

	my $searchsubject = $FORM{'subfield'} eq 'on';
	my $searchmessage = $FORM{'msgfield'} eq 'on';

	$search =~ s/\A\s+//;
	$search =~ s/\s+\Z//;
	&ToHTML($search);
	$search =~ s/\t/ \&nbsp; \&nbsp; \&nbsp;/g;
	$search =~ s/\cM//g;
	$search =~ s/\n/<br>/g;
	if( $searchtype != 3 ) { @search = split( /\s+/, $search ); }
	else { @search = ( $search ); }

	my( $curboard, @threads, $curthread, $tnum, $tsub, $tname, $temail, $tdate, $treplies, $tusername, $ticon, $tstate, $ttime, @messages, $curpost, $mtime, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $mns, $subfound, $msgfound, $numfound, %data, $i, $board, $curcat, @categories, %catname, %cataccess, %openmemgr, @membergroups, %cats, @boardinfo, %boardinfo, @boards, $counter, $msgnum );
	my $curtime = time + (3600*$settings[17]);
	my $mintime = $curtime - (($minage*86400)+($minripe*3600) - 1);
	my $maxtime = $curtime - (($maxage*86400)+($maxripe*3600) + 1);
	my $oldestfound = stringtotime("01/10/37 $txt{'107'} 00:00:00");

	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	foreach $curcat (@categories) {
		chomp $curcat;
		fopen(FILE, "$boardsdir/$curcat.cat");
		$catname{$curcat} = <FILE>;
		chomp $catname{$curcat};
		$cataccess{$curcat} = <FILE>;
		chomp $cataccess{$curcat};
		@{$catboards{$curcat}} = <FILE>;
		fclose(FILE);
		$openmemgr{$curcat} = 0;
		@membergroups = split( /,/, $cataccess{$curcat} );
		foreach $tmpa (@membergroups) {
			if( $tmpa eq $settings[7]) { $openmemgr{$curcat} = 1; last; }
		}
		if( ! $cataccess{$curcat} || $settings[7] eq 'Administrator' ) {
			$openmemgr{$curcat} = 1;
		}
		unless( $openmemgr{$curcat} ) { next; }
		foreach $curboard (@{$catboards{$curcat}}) {
			chomp $curboard;
			fopen(FILE, "$boardsdir/$curboard.dat");
			@boardinfo = <FILE>;
			fclose(FILE);
			chomp @boardinfo;
			@{$boardinfo{$curboard}} = @boardinfo;
			$cat{$curboard} = $curcat;
		}
	}

	$counter = 1;
	while( $_ = each(%FORM) ) {
		unless( $_ =~ m~\Abrd(\d+)\Z~ ) { next; }
		$_ = $FORM{$_};
		if ($_ =~ m~/~){ &fatal_error($txt{'397'}); }
		if ($_ =~ m~\\~){ &fatal_error($txt{'397'}); }
		if( $cat{$_} ) { push( @boards, $_ ); }
		++$counter;
	}
	boardcheck: foreach $curboard (@boards) {
		fopen(FILE, "$boardsdir/$curboard.txt") || next;
		@threads = <FILE>;
		fclose(FILE);
#$yymain .= qq~<ol>Beginning search in board $curboard<ol>~;
		threadcheck: foreach $curthread (@threads) {
			chomp $curthread;
#$yymain .= qq~</ol>Beginning search in topic $curthread.<ol>~;
			($tnum, $tsub, $tname, $temail, $tdate, $treplies, $tusername, $ticon, $tstate) = split( /\|/, $curthread );
			if( $userkind == 1 ) {
				if( $tusername eq 'Guest' ) {
					if( $tname !~ m~\A\Q$userspec\E\Z~i ) { next threadcheck; }
				}
				else {
					if( $tusername !~ m~\A\Q$userspec\E\Z~i ) { next threadcheck; }
				}
			}
			$ttime = stringtotime($tdate);
			unless( $ttime < $mintime ) { next threadcheck; }
			unless( $ttime > $maxtime ) { next threadcheck; }
			fopen(FILE, "$datadir/$tnum.txt") || next;
			@messages = <FILE>;
			fclose(FILE);

			postcheck: for( $msgnum = 0; $msgnum < @messages; $msgnum++ ) {
				$curpost = $messages[$msgnum];
				chomp $curpost;
#$yymain .= qq~Beginning search in post $curpost.<br>~;
				($msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $message, $mns) = split(/\|/,$curpost);
				$mtime = stringtotime($mdate);
				unless( $mtime < $mintime ) { next postcheck; }
				if( $numfound >= $display && $mtime <= $oldestfound ) { next postcheck; }
				if( $musername eq 'Guest' ) {
					if( $userkind == 3 || ( $userkind == 2 && $mname !~ m~\A\Q$userspec\E\Z~i ) ) { next postcheck; }
				}
				else {
					if( $userkind == 4 || ( $userkind == 2 && $musername !~ m~\A\Q$userspec\E\Z~i ) ) { next postcheck; }
				}
#$yymain .= qq~<ol>Performing search in post $curpost.</ol>~;
				if( $searchsubject ) {
					if( $searchtype == 2 ) {
						$subfound = 0;
						foreach( @search ) {
							if( $msub =~ m~\Q$_\E~i ) { $subfound = 1; last; }
						}
					}
					else {
						$subfound = 1;
						foreach( @search ) {
							if( $msub !~ m~\Q$_\E~i ) { $subfound = 0; last; }
						}
					}
				}
				if( $searchmessage && ! $subfound ) {
					if( $searchtype == 2 ) {
						$msgfound = 0;
						foreach( @search ) {
							if( $message =~ m~\Q$_\E~i ) { $msgfound = 1; last; }
						}
					}
					else {
						$msgfound = 1;
						foreach( @search ) {
							if( $message !~ m~\Q$_\E~i ) { $msgfound = 0; last; }
						}
					}
				}
				unless( $msgfound || $subfound ) { next postcheck; }
				if( $subfound ) {
					foreach( @search ) {
						$msub =~ s~(\Q$_\E)~<font class="text1" color="titletext" size="3"><b>$_</b></font>~ig;
					}
				}
				$data{$mtime} = [$curboard, $tnum, $msgnum, $tusername, $tname, $msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $message, $mns, $tstate];
				if( $mtime < $oldestfound ) { $oldestfound = $mtime; }
				++$numfound;
			}
		}
	}

	@messages = sort {$b <=> $a } keys %data;
	if( @messages ) {
		if( @messages > $display ) { $#messages = $display - 1; }
		$counter = 1;
		&LoadCensorList;	# Load Censor List
	}
	else { $yymain .= qq~<hr class="hr"><b>$txt{'170'}</b><hr>~; }
	for( $i = 0; $i < @messages; $i++ ) {
		($board, $tnum, $msgnum, $tusername, $tname, $msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $message, $mns, $tstate) = @{ $data{$messages[$i]} };
		$mdate = &timeformat($mdate);
		$displayname = $mname;
		if( $tusername ne 'Guest' ) {
			if( &LoadUser($tusername) ) { $tname = "<a href=\"$cgi;action=viewprofile;username=$tusername\">$userprofile{$tusername}->[1]</a>"; }
		}
		if( $musername ne 'Guest' ) {
			if( &LoadUser($musername) ) { $mname = "<a href=\"$cgi;action=viewprofile;username=$musername\">$userprofile{$musername}->[1]</a>"; }
		}
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$message =~ s~\Q$tmpa\E~$tmpb~gi;
			$msub =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		$sender = "search";	# for efficiency we let bolding be done in the wrap function
		&wrap;
		if($enable_ubbc) { $ns = $mns; if(!$yyYaBBCloaded) { require "$sourcedir/YaBBC.pl"; } &DoUBBC; }
		&wrap2;

		if($enable_notification) {
			$notify = qq~$menusep<a href="$scripturl?board=$board;action=notify;thread=$tnum;start=$msgnum">$img{'notify'}</a>~;
		}
		$message =~ s/--mot--/\<b\>/g;
		$message =~ s/--finmot--/\<\/b>/g;
		$message =~ s/&#45;&#45;mot&#45;&#45;/\<b\>/g;
		$message =~ s/&#45;&#45;finmot&#45;&#45;/\<\/b>/g;
		$yymain .= qq~
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}">
  <tr>
    <td align="left" bgcolor="$color{'titlebg'}" class="titlebg"><font class="text1" color="$color{'titletext'}" size="2">&nbsp;$counter&nbsp;</font></td>
    <td width="75%" bgcolor="$color{'titlebg'}" class="titlebg"><font class="text1" color="$color{'titletext'}" size="2">&nbsp;$catname{$cat{$board}} / 
    $boardinfo{$board}->[0] / <a href="$scripturl?board=$board;action=display;num=$tnum;start=$msgnum#$msgnum"><u><font class="text1" color="$color{'titletext'}" size="2">$msub</font></u></a>
    </font></td>
    <td align="right" bgcolor="$color{'titlebg'}" class="titlebg"><nobr>&nbsp;<font class="text1" color="$color{'titletext'}" size="2">$mdate&nbsp;</font></nobr></td>
  </tr><tr>
    <td colspan="3" bgcolor="$color{'catbg'}" class="catbg"><font class="catbg" size="2">$txt{'109'} $tname | $txt{'105'} $txt{'525'} $mname</font></td>
  </tr><tr height="80">
    <td colspan="3" bgcolor="$color{'windowbg2'}" class="windowbg2" valign=top><font size="2">$message</font></td>
  </tr><tr>
    <td colspan="3" bgcolor="$color{'catbg'}" class="catbg"><font size="2">&nbsp;
~;
if ($tstate != 1)
{
    $yymain .= qq~<a href="$scripturl?board=$board;action=post;num=$tnum;start=$msgnum;title=Post+reply">$img{'reply'}</a>$menusep<a href="$scripturl?board=$board;action=post;num=$tnum;quote=$msgnum;title=Post+reply">$img{'replyquote'}</a>$notify~;
}
$yymain .= qq~
    </font></td>
  </tr>
</table><br>
~;
		++$counter;
	}

	$yymain .= qq~
$txt{'167'}<hr class="hr">
<font size="1"><a href="$cgi">$txt{'236'}</a> $txt{'237'}<br></font>~;
	$yytitle = $txt{'166'};
	&template;
	exit;
}

1;