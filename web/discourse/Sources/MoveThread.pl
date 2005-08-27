###############################################################################
# MoveThread.pl                                                               #
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

$movethreadplver = "1 Gold - SP 1.4";

sub MoveThread {
	if((!exists $moderators{$username}) && $settings[7] ne "Administrator") { &fatal_error("$txt{'134'}"); }
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	$boardlist="";
	foreach $curcat (@categories) {
		$curcat =~ s/[\n\r]//g;
		fopen(CAT, "$boardsdir/$curcat.cat");
		@catinfo = <CAT>;
		fclose(CAT);
		$catinfo[1] =~ s/[\n\r]//g;
		$curcatname="$catinfo[0]";
		foreach $curboard (@catinfo) {
			$curboard =~ s/[\n\r]//g;
			fopen(BOARD, "$boardsdir/$curboard.dat");
			@boardinfo = <BOARD>;
			fclose(BOARD);
			chomp @boardinfo;
			$curboardname = $boardinfo[0];
			if($curboard ne "$catinfo[0]" && $curboard ne "$catinfo[1]" && $currentboard ne $curboard) {
				$curboard =~ s/[\n\r]//g;
				$boardlist .= "<option value=\"$curboard\">$curboardname";
			}
		}
	}
	$yymain .= qq~
<table border="0" width="60%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor" cellpadding="4" align="center">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'132'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}" align="center"><font size=2>
    <script language="JavaScript1.2" src="$ubbcjspath" type="text/javascript"></script>
    <form action="$cgi;action=movethread2;thread=$INFO{'thread'}" method="POST" name="move" onSubmit="return submitproc()"><BR>
    <b>$txt{'133'}:</b> <select name="toboard">$boardlist</select>
    <input type=submit value="$txt{'132'}">
    </form>
    </font></td>
  </tr>
</table>
~;
	$yytitle = "$txt{'132'}";
	&template;
	exit;
}

sub MoveThread2 {
	if((!exists $moderators{$username}) && $settings[7] ne "Administrator") { &fatal_error("$txt{'134'}"); }
	my( $thread, $toboard, @threads, $a, $mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mattach, $checknum, $mobilenum, $mobiledate, $mobileposts, $threadcount, $messagecount, $lastposttime, $lastposter, $mnuma, $tmpa, $mtime, $mobiletime );
	$thread=$INFO{'thread'};
	if( $thread =~ /\D/ ) { &fatal_error($txt{'337'}); }
	$toboard = $FORM{'toboard'};
	if ($toboard =~ m~/~){ &fatal_error($txt{'224'}); }
	if ($toboard =~ m~\\~){ &fatal_error($txt{'225'}); }
	fopen(FILE, "$datadir/$thread.txt") || &fatal_error("$txt{'23'} $thread.txt");
	@messages = <FILE>;
	fclose(FILE);
	fopen(FILE, "$boardsdir/$currentboard.txt") || &fatal_error("$txt{'23'} $currentboard.txt");
	@oldthreads = <FILE>;
	fclose(FILE);
	
	($mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mattach) = split(/\|/,$yyThreadLine);
	$checknum = $yyThreadPosition + 1;
	$mobilenum = $mnum;
	$mobiledate = $mdate;
	$mobiletime = stringtotime($mdate);
	$mobileposts = $mreplies + 1;

	unless( $checknum ) {
		&fatal_error(qq~$txt{'472'}: $thread $currentboard -&gt; $toboard~);
	}
	( $threadcount, $messagecount, $lastposttime, $lastposter ) = &BoardCountGet($currentboard);
	$messagecount -= $mreplies;
	if( $checknum == 1 ) {
		$_ = $oldthreads[1];
		chomp;
		($mnuma, $tmpa, $tmpa, $tmpa, $lastposttime) = split(/\|/, $_);
		if( $mnuma ) {
			fopen(FILE, "$datadir/$mnuma.data");
			$tmpa = <FILE>;
			fclose(FILE);
			($tmpa, $lastposter) = split(/\|/, $tmpa);
		}
		else {
			$lastposttime = 'N/A';
			$lastposter = 'N/A';
		}
	}

	$newthreadid = time;
	$i=0;
	if (-e "$datadir/$newthreadid.txt") {
		while (-e "$datadir/$newthreadid$i.txt") { ++$i; }
		$newthreadid="$newthreadid$i";
	}

	fopen(FILE, "$boardsdir/$toboard.txt") || &fatal_error("209 $txt{'106'}: $txt{'23'} $boardsdir/$toboard.txt");
	@boardlist = <FILE>;
	fclose(FILE);
	fopen(FILE, "$boardsdir/$currentboard.txt") || &fatal_error("$txt{'23'} $currentboard.txt");
	@oldthreads2 = <FILE>;
	fclose(FILE);
	chomp $oldthreads2[$yyThreadPosition];
	@newthreadinfo = split(/\|/, $oldthreads2[$yyThreadPosition]);
	$newthreadinfo[0] = "$newthreadid";
	$oldthreads2[$yyThreadPosition] = join("|", @newthreadinfo)."\n";
	fopen(FILE, "+<$boardsdir/$toboard.txt", 1) || &fatal_error("210 $txt{'106'}: $txt{'23'} $toboard.txt");
		seek FILE, 0, 0;
		my @buffer = <FILE>;
		truncate FILE, 0;
		seek FILE, 0, 0;
		print FILE $oldthreads2[$yyThreadPosition];
		print FILE @buffer;
	fclose(FILE);
	opendir (MMD,$datadir) || &fatal_error("$fatxt{'19'} $datadir!");
	@files = readdir(MMD);
	closedir(MMD);
		foreach $files(@files) {	
			if ($files =~ /\A$thread\.(.+)/igo) {
				fopen(FILE, "$datadir/$thread.$1");
				@fileinfo = <FILE>;
				fclose(FILE);
				fopen(FILE, ">$datadir/$newthreadid.$1");
				print FILE @fileinfo;
				fclose(FILE);
				if ($1 eq 'mail' && -e "$datadir/$thread.mail") { unlink("$datadir/$thread.mail"); }
			}
		}

	fopen(FILE, "$boardsdir/$toboard.dat", 1) || &fatal_error("210 $txt{'106'}: $txt{'23'} $toboard.dat");
	@toboardinfo = <FILE>;
	fclose(FILE);
	chomp $oldthreads[$yyThreadPosition];
	chomp $toboardinfo[0];
	@oldthreadlock = split(/\|/, $oldthreads[$yyThreadPosition]);
	$oldthreadlock[1] = "$txt{'758'}: ".$oldthreadlock[1]; #changes subject in message index to "Moved: Subject"
	$oldthreadlock[8] = "1";
	$oldthreadlock[5] = "0";
	$oldthreads[$yyThreadPosition] = join("|", @oldthreadlock)."\n";
	fopen(FILE, "$memberdir/$username.dat", 1) || &fatal_error("210 $txt{'106'}: $txt{'23'} $username.dat");
	@userinfo = <FILE>;
	fclose(FILE);
	chomp $userinfo[1];
	chomp $messages[0];
	@movedthread = split(/\|/, $messages[0]);
	$movedthread[8] = "$txt{'160'} [link=$scripturl?board=$toboard;action=display;num=$newthreadid;start=0]"."$toboardinfo[0]"."[/link] $txt{'525'} $userinfo[1].";
	$movedpost = join("|", @movedthread);
	fopen(FILE, "+<$boardsdir/$currentboard.txt", 1) || &fatal_error("$txt{'23'} $currentboard.txt");
		seek FILE, 0, 0;
		my @buffer = <FILE>;
		truncate FILE, 0;
		for ($a = 0; $a < @buffer; $a++) {
			if ( $buffer[$a] =~ m~\A$mnum\|~o ) { $buffer[$a] = $oldthreads[$yyThreadPosition]; last; }
		}
		seek FILE, 0, 0;
		print FILE @buffer;
	fclose(FILE);
	fopen(FILE, ">$datadir/$thread.txt") || &fatal_error("$txt{'23'} $thread.txt");
	print FILE "$movedpost\n";
	fclose(FILE);

	&BoardCountSet( $currentboard, $threadcount, $messagecount, $lastposttime, $lastposter );
	fopen(FILE, "$boardsdir/$toboard.txt") || &fatal_error("$txt{'23'} $toboard.txt");
	@threads = <FILE>;
	fclose(FILE);

	for ($a = 0; $a < @threads; $a++) {
		$_ = $threads[$a];
		chomp;
		($mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mattach) = split(/\|/,$_);
		$mtime = stringtotime($mdate);
		if ($mobiletime >= $mtime) { last; }
	}

	( $threadcount, $messagecount, $lastposttime, $lastposter ) = &BoardCountGet($toboard);
	++$threadcount;
	$messagecount += $mobileposts;
	if( $a == 0 ) {
		if( $mobilenum ) {
			fopen(FILE, "$datadir/$mobilenum.data");
			$tmpa = <FILE>;
			fclose(FILE);
			($tmpa, $lastposter) = split(/\|/, $tmpa);
			$lastposttime = $mobiledate;
		}
		else {
			$lastposttime = 'N/A';
			$lastposter = 'N/A';
		}
	}
	$threads[$a] = "$yyThreadLine\n$threads[$a]";
	&BoardCountSet( $toboard, $threadcount, $messagecount, $lastposttime, $lastposter );

	$yySetLocation = qq~$scripturl?board=$toboard;action=display;num=$newthreadid;start=0~;
	&redirectexit;
}

1;
