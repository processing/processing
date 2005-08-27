#!/usr/bin/perl --


###############################################################################
# YaBB.pl                                                                     #
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

### Version Info ###
$YaBBversion = '1 Gold - SP 1.4';
$YaBBplver = '1 Gold - SP 1.4';

if( $ENV{'SERVER_SOFTWARE'} =~ /IIS/ ) {
	$yyIIS = 1;
	$0 =~ m~(.*)(\\|/)~;
	$yypath = $1;
	$yypath =~ s~\\~/~g;
	chdir($yypath);
	push(@INC,$yypath);
}

### Requirements and Errors ###
require "Settings.pl";
require "$language";
require "$sourcedir/Subs.pl";
require "$sourcedir/Load.pl";
require "$sourcedir/Security.pl";

# Those who write software only for pay should go hurt some other field.
# - Erik Naggum

&LoadCookie;		# Load the user's cookie (or set to guest)
&LoadUserSettings;	# Load user settings
&banning;		# Check for banned people
&WriteLog;		# Write to the log
&LoadIMs;		# Load IM's
if($currentboard ne "") { &LoadBoard; }		# Load board information

$SIG{__WARN__} = sub { &fatal_error( @_ ); };
eval { &yymain; };
if ($@) { &fatal_error("Untrapped Error:<BR>$@"); }

sub yymain {
#### Choose what to do based on the form action ####
if ($maintenance == 1 && $action eq 'login2') { require "$sourcedir/LogInOut.pl"; &Login2; }
if ($maintenance == 1 && $settings[7] ne 'Administrator') { require "$sourcedir/Maintenance.pl"; &InMaintenance; }
### Guest can do the very few following actions.
if($username eq 'Guest' && $guestaccess == 0) {
	if(!(($action eq 'login') || ($action eq 'login2') || ($action eq 'register') || ($action eq 'register2') || ($action eq 'reminder') || ($action eq 'reminder2'))) { &KickGuest; }
}

if ($action ne "") { 
	require "$sourcedir/SubList.pl"; 
	if ($director{$action}) { 
		@act = split(/&/,$director{$action}); 
		$aa = $act[1]; 
		require "$sourcedir/$act[0]"; &$aa; 
	} else { require "$sourcedir/BoardIndex.pl"; &BoardIndex; } 
}
elsif($currentboard eq "") { require "$sourcedir/BoardIndex.pl"; &BoardIndex; }
else { require "$sourcedir/MessageIndex.pl"; &MessageIndex; }

exit;
}