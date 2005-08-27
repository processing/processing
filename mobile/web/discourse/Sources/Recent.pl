###############################################################################
# Recent.pl                                                                   #
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

$recentplver = "1 Gold - SP 1.4";

sub LastPost {
	# easiest way to add check if cat.txt was loaded in Recent.pl or BoardIndex.pl before
        # but not the best. Next releases should have for each file a function in load.pl to read the data
        # and check on each call if the data was already loaded
	if( ! $yyCatsLoaded) {
		fopen(FILE, "$vardir/cat.txt");
		@categories = <FILE>;
		fclose(FILE);
	}

	# Load Censor List
	&LoadCensorList;

	%data= ();
	foreach $curcat (@categories) {
		$curcat =~ s/[\n\r]//g;
		fopen(CAT, "$boardsdir/$curcat.cat");
		$curcatname = <CAT>;
		$curcataccess = <CAT>;
		@catboards = <CAT>;
		fclose(CAT);
		chomp $curcatname;
		chomp $curcataccess;
		%membergroups = ();
		foreach(split(/\,/,$curcataccess)) {
			$membergroups{$_} = $_;
		}
		if($curcataccess) {
			if($settings[7] ne 'Administrator' && !exists $membergroups{$settings[7]}) { next; }
		}
		foreach $curboard (@catboards ) {
			chomp $curboard;
			fopen(BOARDDATA, "$boardsdir/$curboard.txt");
			$message = <BOARDDATA>;
			fclose(BOARDDATA);

			($mnum, $msub, $dummy, $dummy, $datetime, $mreplies) = split(/\|/, $message);
			$mydatetime = &timeformat($datetime);

			foreach (@censored) {
				($tmpa,$tmpb) = @{$_};
				$message =~ s~\Q$tmpa\E~$tmpb~gi;
				$msub =~ s~\Q$tmpa\E~$tmpb~gi;
			}
			if($recentsender eq "admin") {
				$post = qq~"<a href="$scripturl?board=$curboard;action=display;num=$mnum;start=$mreplies#$mreplies">$msub</a>" &#171; $mydatetime &#187;\n~;
			} else {
				$post = qq~<br>$txt{'235'}: <a href="$cgi$curboard;action=display;num=$mnum;start=$mreplies#$mreplies"><b>$msub</b></a> ($mydatetime)<br>$txt{'791'} <a href="$scripturl?action=recent">$txt{'792'}</a> $txt{'793'}~;
			}
			$totaltime = stringtotime($datetime);
			$data{$totaltime}= $post;
		}
	}

	@num = sort {$b <=> $a } keys %data;
	$yymain .= "$data{$num[0]}";
}

sub RecentPosts {
	my $display = 10;
	my( @memset, @categories, %data, %cat, $numfound, $oldestfound, $curcat, %catname, %cataccess, %catboards, $openmemgr, @membergroups, %openmemgr, $curboard, @threads, @boardinfo, $i, $c, @messages, $tnum, $tsub, $tname, $temail, $tdate, $treplies, $tusername, $ticon, $tstate, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $mns, $mtime, $counter, $board, $notify );

	@categories = ();
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);

	$oldestfound = stringtotime("01/10/37 $txt{'107'} 00:00:00");

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
			fopen(FILE, "$boardsdir/$curboard.txt");
			@threads = <FILE>;
			fclose(FILE);

			fopen(FILE, "$boardsdir/$curboard.dat");
			@boardinfo = <FILE>;
			fclose(FILE);
			foreach (@boardinfo) {
				chomp;
			}
			@{$boardinfo{$curboard}} = @boardinfo;
			$cat{$curboard} = $curcat;

			for ($i = 0; $i < @threads; $i++) {
				chomp $threads[$i];
				($tnum, $tsub, $tname, $temail, $tdate, $treplies, $tusername, $ticon, $tstate) = split( /\|/, $threads[$i] );
				fopen(FILE, "$datadir/$tnum.txt") || next;
				while( <FILE> ) { $message = $_; }
				# get only the last post for this thread.
				fclose(FILE);

				chomp $message;
				if( $message ) {
					($msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $message, $mns) = split(/\|/,$message);
					$mtime = stringtotime($mdate);
					if( $numfound >= $display && $mtime <= $oldestfound ) {
						next;
					}
					else {
						$data{$mtime} = [$curboard, $tnum, $treplies, $tusername, $tname, $msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $message, $mns, $tstate];
						if( $mtime < $oldestfound ) { $oldestfound = $mtime; }
						++$numfound;
					}
				}
			}
		}
	}

	@messages = sort {$b <=> $a } keys %data;
	if( @messages ) {
		if( @messages > $display ) { $#messages = $display - 1; }
		$counter = 1;
		# Load Censor List
		&LoadCensorList;
	}
	else {
		$yymain .= qq~<hr class="hr"><b>$txt{'170'}</b><hr>~;
	}
	for( $i = 0; $i < @messages; $i++ ) {
		($board, $tnum, $c, $tusername, $tname, $msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $message, $mns, $tstate) = @{ $data{$messages[$i]} };
		$displayname = $mname;
		if( $tusername ne 'Guest' ) {
			&LoadUser($tusername);
			$tname = exists $userprofile{$tusername} ? $userprofile{$tusername}->[1] : $tname;
			$tname ||= $txt{'470'};
			$tname = qq~<a href="$scripturl?action=viewprofile;username=$useraccount{$tusername}" alt="$txt{'27'}: $musername">$tname</a>~;
		}
		if( $musername ne 'Guest' ) {
			&LoadUser($musername);
			$mname = exists $userprofile{$musername} ? $userprofile{$musername}->[1] : $mname;
			$mname ||= $txt{'470'};
			$mname = qq~<a href="$scripturl?action=viewprofile;username=$useraccount{$musername}" alt="$txt{'27'}: $musername">$mname</a>~;
		}
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$message =~ s~\Q$tmpa\E~$tmpb~gi;
			$msub =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		&wrap;
		if($enable_ubbc) { $ns = $mns; if(!$yyYaBBCloaded) { require "$sourcedir/YaBBC.pl"; } &DoUBBC; }
		&wrap2;
		if($enable_notification) { $notify = qq~$menusep<a href="$scripturl?board=$board;action=notify;thread=$tnum;start=$startnum">$img{'notify'}</a>~; }
		$mdate = &timeformat($mdate);
		$yymain .= qq~
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td align="left" bgcolor="$color{'titlebg'}" class="titlebg"><font class="text1" color="$color{'titletext'}" size="2">&nbsp;$counter&nbsp;</font></td>
    <td width="75%" bgcolor="$color{'titlebg'}" class="titlebg"><font class="text1" color="$color{'titletext'}" size="2">&nbsp;$catname{$cat{$board}} / $boardinfo{$board}->[0] / <a href="$scripturl?board=$board;action=display;num=$tnum;start=$c#$c"><u><font class="text1" color="$color{'titletext'}" size="2">$msub</font></a></font></u></td>
    <td align="right" bgcolor="$color{'titlebg'}" class="titlebg" nowrap>&nbsp;<font class="text1" color="$color{'titletext'}" size="2">$mdate&nbsp;</font></td>
  </tr><tr>
    <td colspan="3" bgcolor="$color{'catbg'}" class="catbg"><font class="catbg" size="2">$txt{'109'} $tname | $txt{'22'} $txt{'525'} $mname</font></td>
  </tr><tr height="80">
    <td colspan="3" bgcolor="$color{'windowbg2'}" class="windowbg2" valign=top><font size="2">$message</font></td>
  </tr><tr>
    <td colspan="3" bgcolor="$color{'catbg'}" class="catbg"><font size="2">&nbsp;
~;
if ($tstate != 1)
{
	$yymain .= qq~<a href="$scripturl?board=$board;action=post;num=$tnum;start=$c;title=Post+reply">$img{'reply'}</a>$menusep<a href="$scripturl?board=$board;action=post;num=$tnum;quote=$c;title=Post+reply">$img{'replyquote'}</a>$notify~;
}
$yymain .= qq~
    </font></td>
  </tr>
</table><br>
~;
		++$counter;
	}

	$yymain .= qq~
<font size="1"><a href="$cgi">$txt{'236'}</a> $txt{'237'}<br></font>
~;
	$yytitle = $txt{'214'};
	&template;
	exit;
}

1;