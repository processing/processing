###############################################################################
# LockThread.pl                                                               #
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

$lockthreadplver = "1 Gold - SP 1.4";

sub LockThread {
	if((exists $moderators{$username}) || $settings[7] eq "Administrator") {
		my $threadid = $INFO{'thread'};
		fopen(BOARDFILE, "+<$boardsdir/$currentboard.txt") || &fatal_error("$txt{'23'} $currentboard.txt");
		seek BOARDFILE, 0, 0;
		my $buffer = 1;
		my $found = 0;
		while ($buffer) {
			$offset = tell BOARDFILE;
			$buffer = <BOARDFILE>;
			if($buffer =~ m~\A$threadid\|~o) {
				$found = 1;
				last;
			}
		}
		if ($found) {
			seek BOARDFILE, $offset, 0;
			chomp $buffer;
			my( $mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mstate );
			($mnum, $msub, $mname, $memail, $mdate, $mreplies, $musername, $micon, $mstate) = split(/\|/,$buffer);
			$mstate = $mstate ? 0 : 1;
			if ($mstate == 1) { $yySetLocation = qq~$cgi~; }
			else { $yySetLocation = qq~$cgi;action=display;num=$INFO{'thread'};start=$start;mstate=$mstate~; }
			print BOARDFILE "$mnum|$msub|$mname|$memail|$mdate|$mreplies|$musername|$micon|$mstate\n";
			fclose(BOARDFILE);
		}
	}
	else { &fatal_error("$txt{'93'}"); }
	
	my $start = $INFO{'start'} || 0;

	&redirectexit;
}

1;
