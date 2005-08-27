###############################################################################
# Security.pl                                                                 #
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

$securityplver = "1 Gold - SP 1.4";

sub is_admin {
	if($settings[7] ne 'Administrator') { &fatal_error($txt{'1'}); }
}

sub is_admin2 {
	if($settings[7] ne 'Administrator') { &fatal_error($txt{'134'}); }
}

sub banning {
	# IP BANNING
	fopen(BAN, "$vardir/ban.txt" );
	@entries = <BAN>;
	fclose(BAN);
	foreach $ban_ip (@entries) {
   		chomp $ban_ip;
   		$str_len = length($ban_ip);
   		$comp_ip = substr($user_ip,0,$str_len);
   		if ($comp_ip eq $ban_ip) {
      			fopen(LOG, ">>$vardir/ban_log.txt" );
      			print LOG "$user_ip\n";
     			fclose(LOG);
     			$username = "Guest";
     			&fatal_error("$txt{'678'}$txt{'430'}!");
      			exit;
      		}
	}
	# EMAIL BANNING
	if ($username ne 'Guest') {
	fopen(BAN, "$vardir/ban_email.txt" );
	@entries = <BAN>;
	fclose(BAN);
	foreach $ban_email (@entries) {
		chomp $ban_email;
   		if (lc $ban_email eq lc $settings[2]) {
      			fopen(LOG, ">>$vardir/ban_log.txt" );
      			print LOG "$ban_email ($user_ip)\n";
     			fclose(LOG);
     			$username = "Guest";
      			&fatal_error("$txt{'678'}$txt{'430'}!");
      			exit;
      		}
	}
	# USERNAME BANNING
	if ($username ne 'Guest') {
	fopen(BAN, "$vardir/ban_memname.txt" );
	@entries = <BAN>;
	fclose(BAN);
	foreach $ban_memname (@entries) {
		chomp $ban_memname;
   		if (lc $ban_memname eq lc $settings[1]) {
      			fopen(LOG, ">>$vardir/ban_log.txt" );
      			print LOG "$ban_memname ($user_ip)\n";
     			fclose(LOG);
     			$username = "Guest";
      			&fatal_error("$txt{'678'}$txt{'430'}!");
      			exit;
                        }
      		}
	}
	}
}

sub CheckIcon {
	# Check the icon so HTML cannot be exploited.
	# Do it in 3 unless's because 1 is too long :D
	$icon =~ s~[^A-Za-z]~~g;
	$icon =~ s~\\~~g;
	$icon =~ s~\/~~g;
	unless($icon eq "xx" || $icon eq "thumbup" || $icon eq "thumbdown" || $icon eq "exclamation") {
		unless($icon eq "question" || $icon eq "lamp" || $icon eq "smiley" || $icon eq "angry") {
			unless($icon eq "cheesy" || $icon eq "laugh" || $icon eq "sad" || $icon eq "wink") {
				$icon = "xx";
			}
		}
	}
}

1;