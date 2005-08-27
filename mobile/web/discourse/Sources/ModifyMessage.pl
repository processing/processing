###############################################################################
# ModifyMessage.pl                                                            #
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

$modifymessageplver = "1 Gold - SP 1.4";

sub ModifyMessage {
	if($username eq 'Guest') { &fatal_error($txt{'223'}); }
	if( $currentboard eq '' ) { &fatal_error($txt{'1'}); }
	my( $mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mstate, @messages, $curmessage, $msubject, $mattach, $mip, $mmessage, $mns, $mlm, $mlmb);
	$threadid = $INFO{'thread'};
	$postid = int( $INFO{'message'} );

	($mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mstate) = split(/\|/,$yyThreadLine);
	if( $mstate == 1 ) {
		&fatal_error($txt{'90'});
	}

	fopen(FILE, "$datadir/$threadid.txt") || &fatal_error("$txt{'23'} $threadid.txt");
	@messages = <FILE>;
	fclose(FILE);

	$curmessage = $messages[$postid];
	chomp $curmessage;
	($sub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip,  $message, $mns, $mlm, $mlmb) = split(/\|/,$messages[$postid]);

	if($musername ne $username && (!exists $moderators{$username}) && $settings[7] ne 'Administrator' ) {
		&fatal_error($txt{'67'});
	}

	$lastmod = $mlm ? &timeformat($mlm) : '-';
	$nscheck = $mns ? ' checked' : '';

	$lastmod = qq~
<tr>
	<td valign=top width="23%"><font size=2><b>$txt{'211'}:</b></font></td>
	<td><font size="2">$lastmod</font></td>
</tr>
~;
	$icon = $micon;
	if($icon eq "xx") { $ic1 = " selected"; }
	elsif($icon eq "thumbup") { $ic2 = " selected"; }
	elsif($icon eq "thumbdown") { $ic3 = " selected"; }
	elsif($icon eq "exclamation") { $ic4 = " selected"; }
	elsif($icon eq "question") { $ic5 = " selected"; }
	elsif($icon eq "lamp") { $ic6 = " selected"; }
	elsif($icon eq "smiley") { $ic7 = " selected"; }
	elsif($icon eq "angry") { $ic8 = " selected"; }
	elsif($icon eq "cheesy") { $ic9 = " selected"; }
	elsif($icon eq "laugh") { $ic10 = " selected"; }
	elsif($icon eq "sad") { $ic11 = " selected"; }
	elsif($icon eq "wink") { $ic12 = " selected"; }

	$message =~ s/<br>/\n/ig;

	$submittxt = "$txt{'10'}";
	$destination = "modify2";
	$is_preview = 0;
	$post = "postmodify";
	$preview = "previewmodify";
	require "$sourcedir/Post.pl";
	$yytitle = "$txt{'66'}";
	$settofield = "message";
	&Postpage;
	&template;
	exit;
}

sub ModifyMessage2 {
	if($username eq 'Guest') { &fatal_error($txt{'223'}); }
	if( $FORM{'previewmodify'} ) {
		require "$sourcedir/Post.pl";
		&Preview;
	}
	my( $deletepost, $threadid, $postid, @messages, $msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $mmessage, $mns, $mlm, $mlmb, $tnum, $tsub, $tname, $temail, $tdate, $treplies, $tusername, $ticon, $tstate, @threads, $tmpa, $tmpb, $tnum2, $tdate2, $newlastposttime, $newlastposter, $lastpostid, $views, $name, $email, $subject, $message, $ns, $postkilled, $threadkilled );
	$deletepost = $INFO{'d'} eq '1';
	if($deletepost) {
		$threadid = $INFO{'thread'};
		$postid = $INFO{'id'};
	}
	else {
		$threadid = $FORM{'threadid'};
		$postid = $FORM{'postid'};
	}

	fopen(FILE, "$datadir/$threadid.txt") || &fatal_error("$txt{'23'} $threadid.txt");
	@messages = <FILE>;
	fclose(FILE);

	# Make sure the user is allowed to edit this post.
	if( $postid >= 0 && $postid < @messages ) {
		chomp $messages[$postid];
		($msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $mmessage, $mns, $mlm, $mlmb) = split( /\|/, $messages[$postid]);
		if($INFO{'d'} eq 1) {
			unless($musername eq $username || exists $moderators{$username} || $settings[7] eq 'Administrator') {
				&fatal_error("$txt{'73'}");
			}
		}
		else {
			unless($musername eq $username || exists $moderators{$username} || $settings[7] eq 'Administrator') {
				&fatal_error("$txt{'67'}");
			}
		}
	}
	else { &fatal_error("$txt{'580'} $postid"); }

	# Look for the current thread in the current board.
	fopen(FILE, "$boardsdir/$currentboard.txt");
	@threads = <FILE>;
	fclose(FILE);
	($tnum, $tsub, $tname, $temail, $tdate, $treplies, $tusername, $ticon, $tstate) = split( /\|/, $yyThreadLine );
	$threadnum = $yyThreadPosition;
	if( $tstate == 1 ) {
		&fatal_error($txt{'90'});
	}

	if( $deletepost ) {
		if( $musername ne 'Guest' ) {
			fopen(FILE, "$memberdir/$musername.dat");
			@userprofile = <FILE>;
			fclose(FILE);
			chomp $userprofile[6];
			if ($userprofile[6] > 0)
			{
				--$userprofile[6];
				$userprofile[6] .= "\n";
				fopen(FILE, ">$memberdir/$musername.dat", 1);
				print FILE @userprofile;
				fclose(FILE);
			}
		}
		--$treplies;
		$postkilled = 1;
		if( $treplies < 0 ) {
			# If the post is the only one in the thread, # then delete the thread as well.
			$threads[$threadnum] = '';
			$threadkilled = 1;
			unlink("$datadir/$tnum.txt");
			unlink("$datadir/$tnum.mail");
			unlink("$datadir/$tnum.data");
			&Sticky_remove($tnum);
			
			if( $threadnum == 0 ) {
				($tnum2, $tmpa, $tmpa, $tmpa, $tdate2) = split( /\|/, $threads[1] );
				if( $tnum2 ) {
					$newlastposttime = $tdate2;
					fopen(FILE, "$datadir/$tnum2.data");
					$tmpa = <FILE>;
					fclose(FILE);
					($views,$newlastposter) = split(/\|/, $tmpa);
				}
				else {
					$newlastposttime = 'N/A';
					$newlastposter = 'N/A';
				}
			}
		}
		else {
			# If the post is not the only one in the thread...
			if( $postid == $#messages ) {
				($tmpa,$tmpa,$tmpa,$tdate) = split( /\|/, $messages[$postid-1] );
			}
			elsif( $postid == 0 ) {
				$_ = $messages[1];
				chomp;
				($msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $mmessage, $mns, $mlm, $mlmb) = split( /\|/, $_ );
				$tsub = $msub;
				$ticon = $micon;
			}
			$threads[$threadnum] = qq~$tnum|$tsub|$tname|$temail|$tdate|$treplies|$tusername|$ticon|$tstate\n~;
			if( $postid == $#messages ) {
				# If the post is the last one in the thread, then make sure
				# all the lastposter/lastposttime stuff is correct for the thread.
				$lastpostid = $postid - 1;
				$_ = $messages[$lastpostid];
				chomp;
				($msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $mmessage, $mns, $mlm, $mlmb) = split( /\|/, $_ );

				# Changed from->if( $threadnum == 0 ) {
				$newlastposttime = $mdate;
				if($musername ne "Guest") { $newlastposter = $musername; }
				else { $newlastposter = $mname; }

				fopen(FILE, "$datadir/$tnum.data");
				$tmpa = <FILE>;
				fclose(FILE);
				($views,$tmpa) = split(/\|/, $tmpa);
				fopen(FILE, "+>$datadir/$tnum.data");
				print FILE qq~$views|$newlastposter~;
				fclose(FILE);
			}
			$messages[$postid] = '';
			# Save thread without the deleted post.
			fopen(FILE, ">$datadir/$threadid.txt", 1) || &fatal_error("$txt{'23'} $threadid.txt");
			print FILE @messages;
			fclose(FILE);
		}
	}
	else {
		# If the post is to be modified...
		$name = $FORM{'name'};
		$email = $FORM{'email'};
		$subject = $FORM{'subject'};
		$message = $FORM{'message'};
		$icon = $FORM{'icon'};
		$ns = $FORM{'ns'};
		&CheckIcon;

		&fatal_error($txt{'78'}) unless($message);
		if (length($message)>$MaxMessLen) { &fatal_error($txt{'499'}); }

		&ToHTML($name);
		$email =~ s/\|//g;
		&ToHTML($email);
		&fatal_error($txt{'77'}) unless($subject && $subject !~ m~\A[\s_.,]+\Z~ );
		$message =~ s/\cM//g;
		$message =~ s~\[([^\]]{0,30})\n([^\]]{0,30})\]~\[$1$2\]~g;
		$message =~ s~\[/([^\]]{0,30})\n([^\]]{0,30})\]~\[/$1$2\]~g;
		$message =~ s~(\w+://[^<>\s\n\"\]\[]+)\n([^<>\s\n\"\]\[]+)~$1\n$2~g;
		&ToHTML($message);
		$message =~ s/\t/ \&nbsp; \&nbsp; \&nbsp;/g;
		$message =~ s/\n/<br>/g;
		if( $postid == 0 ) {
			$tsub = $subject;
			$ticon = $icon;
		}
		$threads[$threadnum] = qq~$tnum|$tsub|$tname|$temail|$tdate|$treplies|$tusername|$ticon|$tstate\n~;
		if ( $mip =~ /$user_ip/) {$useredit_ip = $mip;}
		else {$useredit_ip = "$mip $user_ip";}
		$messages[$postid] = qq~$subject|$mname|$memail|$mdate|$musername|$icon|0|$useredit_ip|$message|$ns|$date|$username\n~;
		fopen(FILE, ">$datadir/$threadid.txt", 1) || &fatal_error("$txt{'23'} $threadid.txt");
		print FILE @messages;
		fclose(FILE);
	}
	# Save the current board.
	fopen(FILE, "+<$boardsdir/$currentboard.txt", 1) || &fatal_error("$txt{'23'} $currentboard.txt");
		seek FILE, 0, 0;
		my @buffer = <FILE>;
		truncate FILE, 0;
		for ($a = 0; $a < @buffer; $a++) {
			if ( $buffer[$a] =~ m~\A$threadid\|~o ) { $buffer[$a] = $threads[$threadnum]; last; }
		}
		seek FILE, 0, 0;
		print FILE @buffer;
	fclose(FILE);

	if( $postkilled ) {
		# If post was killed, update the current board.
		my( $threadcount, $messagecount, $lastposttime, $lastposter ) = &BoardCountGet($currentboard);
		--$messagecount;
		if( $threadkilled ) {
			--$threadcount;
		}
		&BoardCountSet( $currentboard, $threadcount, $messagecount, $newlastposttime || $lastposttime, $newlastposter || $lastposter );
	}

	&dumplog($currentboard);
	if( $threadkilled ) {
		$yySetLocation = qq~$cgi~;
		&redirectexit;
	}
	else {
		# Let's figure out what page number to show
		my $pageindex = int($postid / $maxmessagedisplay);
		my $start = $pageindex * $maxmessagedisplay;
		$yySetLocation = qq~$cgi;action=display;num=$threadid;start=$start#$postid~;
		&redirectexit;
	}
}

1;
