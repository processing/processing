###############################################################################
# ManageCats.pl                                                               #
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

$managecatsplver = "1 Gold - SP 1.4";

sub ManageCats {
	&is_admin;
	my $selected = "";
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	# create a list of categories for reorder
	$catlist="";
	foreach $curcat (@categories) {
		$curcat =~ s/[\n\r]//g;
		$catlist="$catlist\n$curcat";
	}
	# create membergroups dropdown
	my $mgroups = qq~<select multiple name="memgroup" size="3">~;
	fopen(FILE, "$vardir/membergroups.txt");
	@groups = <FILE>;
	fclose(FILE);
	for($i = 7; $i < @groups; ++$i ) {
		$groups[$i] =~ s/[\n\r]//g;
		chomp $groups[$i];
		$groups[$i] =~ s~\<~&lt;~g;
		$groups[$i] =~ s~\>~&gt;~g;
		$mgroups .= qq~\n<option>$groups[$i]</option>~;
	}
	$mgroups .= qq~\n</select>~;
	$yymain .= qq~
<table border="0" cellspacing="1" cellpadding="4" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center" width="680">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}" colspan="2">
    <img src="$imagesdir/cat.gif" border="0" alt="">
    <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'52'}</b></font></td>
  </tr><tr>
    <td colspan="2" bgcolor="$color{'catbg'}" class="catbg" height="25"><font size="2"><b>$txt{'54'}</B></font></td>
  </tr><tr>
    <td bgcolor="$color{'windowbg'}" class="windowbg" height="25">
    <BR><font size="1">$txt{'738'}</font><BR><BR></td>
    <td class="windowbg" bgcolor="$color{'windowbg'}" valign=top><font size=2>
    <form action="$cgi;action=modifycatorder" method="POST">
    <textarea name="cats" cols="40" rows="4">$catlist</textarea><br><BR>
    <input type="submit" value="$txt{'54'}">
    </form></font></td>
  </tr><tr>
    <td colspan="2" bgcolor="$color{'catbg'}" class="catbg" height="25"><font size="2">
    <b>$txt{'56'}</b></font></td>
  </tr><tr>
    <td colspan="2" class="windowbg2" bgcolor="$color{'windowbg2'}" valign="top">
    <form action="$cgi;action=createcat" method="POST">
    <table border="0" cellspacing="1" cellpadding="4" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
      <tr>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="top"><font size="2">
        <b>$txt{'43'}:</b></font><br><font size="1">$txt{'671'}</font></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="top"><font size="2">
        <input type="text" size="15" name="catid"></font></td>   </tr><tr>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="top"><font size="2">
        <b>$txt{'44'}:</b></font><br><font size="1">$txt{'672'}</font></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="top"><font size="2">
        <input type="text" size="40" name="catname"></font></td>
      </tr><tr>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="top" width="50%"><font size="2">
        <b>$txt{'57'}:</b></font><br><font size="1">$txt{'673'}</font></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="top" width="50%"><font size="2">
        $mgroups<br><br>
        <input type="submit" value="$txt{'59'}">    
        </font></td>
      </tr>
    </table>
    </form></td>
  </tr>

~;
	foreach $curcat (@categories) {
		$curcat =~ s~[\n\r]~~g;
		chomp $curcat;
		fopen(CAT, "$boardsdir/$curcat.cat");
		@catinfo = <CAT>;
		fclose(CAT);
		$curcatname = $catinfo[0];
		chomp $curcatname;
		(@curcatgroups) = split(/\,/, $catinfo[1]);
		$mgroups = qq~<select multiple name="catgroups" size="3">~;
		for($i = 7; $i < @groups; ++$i) {
			$groups[$i] =~ s~[\n\r]~~g;
			chomp $groups[$i];
			$selected = "";
			foreach $cur (@curcatgroups) {
				$cur =~ s~[\n\r]~~g;
				chomp $cur;
				if($cur eq $groups[$i]) { $selected = " selected"; break;}
			}
			$groups[$i] =~ s~\<~&lt;~g;
			$groups[$i] =~ s~\>~&gt;~g;
			$mgroups .= qq~\n<option$selected>$groups[$i]</option>~;
		}
		$mgroups .= qq~\n</select>~;
		$yymain .= qq~
  <tr>
    <td colspan="2" bgcolor="$color{'catbg'}" class="catbg" height="25"><font size="2"><b>$txt{'43'}: $curcat</B></font></td>
  </tr><tr>
    <td bgcolor="$color{'windowbg'}" class="windowbg" colspan="2">
    <form action="$cgi;action=modifycat" method="POST">
    <table border="0" cellpadding="4" cellspacing="0" width="100%">
      <tr>
        <td valign="middle"><font size="2">
        $txt{'44'}:<BR> <input type="text" value="$curcatname" size="25" name="catname"></font></td>
        <td valign="middle"><font size="2">
        $txt{'57'}:<BR>
        $mgroups
        </font></td>
        <td valign="middle">
        <input type="hidden" name="id" value="$curcat">
        <input type="submit" name="moda" value="$txt{'17'}"> &nbsp;<input type="submit" name="moda" value="$txt{'31'}"></td>
      </tr>
    </table>
    </form>
    </td>
  </tr>
~;
		}
$yymain .= qq~
</table>
~;
	$yytitle = "$txt{'52'}";
	&template;
	exit;
}

sub ReorderCats {
	&is_admin;
	$FORM{'cats'} =~ s/\r//g;
	$cats = $FORM{'cats'};
	(@cats) = split(/\n/, $cats);
	$thecats = "";
	foreach $ccat (@cats) {
		$ccat =~ s/[\n\r]//g;
		if(-e("$boardsdir/$ccat.cat") && $ccat !~ /\A\n*\Z/) { $thecats .= "$ccat\n"; }
	}
	$thecats =~ s/\A\n*\r*\Z//g;
	fopen(FILE, ">$vardir/cat.txt", 1);
	print FILE "$thecats";
	fclose(FILE);
	$yySetLocation = qq~$cgi;action=managecats~;
	&redirectexit;
}

sub CreateCat {
	&is_admin;
	$catid = $FORM{'catid'};
	&fatal_error("$txt{'240'} $txt{'43'} $txt{'241'}") if($catid !~ /\A[0-9A-Za-z#%+-\.@^_]+\Z/);
	
	# make sure no board already exists with that id
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	foreach $curcat (@categories) {
		$curcat =~ s/[\n\r]//g;
		&fatal_error("$txt{'676'} '$catid' $txt{'675'}") if ($catid eq "$curcat");
	}

	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	fopen(FILE, ">$vardir/cat.txt", 1);
	foreach $curcat (@categories) {
		$curcat =~ s/[\n\r]//g;
		print FILE "$curcat\n";
	}
	$catid =~ s/ /_/g;
	$FORM{'memgroup'} =~ s~\A\s?,\s?~~;
	$FORM{'memgroup'} =~ s~,\s~,~g;
	print FILE "$catid";
	fclose(FILE);
	fopen(FILE, ">$boardsdir/$catid.cat");
	print FILE "$FORM{'catname'}\n";
	print FILE "$FORM{'memgroup'}\n";
	fclose(FILE);
	$yySetLocation = qq~$cgi;action=managecats~;
	&redirectexit;
}

sub ConfRemCat {
	$yymain .= qq~
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'31'} - '$FORM{'catname'}'?</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
    $txt{'736'}<br>
    <b><a href="$cgi;action=modifycat;id=$FORM{'id'};moda=$txt{'31'}2">$txt{'163'}</a> - <a href="$cgi;action=manageboards">$txt{'164'}</a></b>
    </font></td>
  </tr>
</table>
~;
	$yytitle = "$txt{'31'} - '$FORM{'catname'}'?";
	&template;
	exit;
}

sub ModifyCat {
	&is_admin;
	if($FORM{'moda'} eq "$txt{'17'}") {
		fopen(FILE, "$boardsdir/$FORM{'id'}.cat");
		@catinfo = <FILE>;
		fclose(FILE);
		fopen(FILE, ">$boardsdir/$FORM{'id'}.cat", 1);
		$FORM{'catgroups'} =~ s~\A\s?,\s?~~;
		$FORM{'catgroups'} =~ s~,\s~,~g;
		print FILE "$FORM{'catname'}\n";
		print FILE "$FORM{'catgroups'}\n";
		for ($i = 2; $i < @catinfo; $i++) {
			chomp @catinfo;
			$catinfo =~ s/[\n\r]//g;
			print FILE "$catinfo[$i]\n";
		}
		fclose(FILE);
		$yySetLocation = qq~$cgi;action=managecats~;
		&redirectexit;
	} else {
		if($FORM{'moda'} eq "$txt{'31'}") { &ConfRemCat; }
		else {
		fopen(FILE, "$vardir/cat.txt");
		@categories = <FILE>;
		fclose(FILE);
		$newcatlist="";
		foreach $curcat (@categories) {
			$curcat =~ s/[\n\r]//g;
			if($curcat ne "$INFO{'id'}") { $newcatlist="$newcatlist$curcat\n"; }
		}
		fopen(FILE, ">$vardir/cat.txt", 1);
		print FILE "$newcatlist";
		fclose(FILE);
		$curcat = "$INFO{'id'}";

		fopen(CAT, "$boardsdir/$curcat.cat") || &fatal_error("$txt{'23'} $curcat.cat");
		@catinfo = <CAT>;
		fclose(CAT);
		$curcatname="$catinfo[0]";
		foreach $curboard (@catinfo) {
			$curboard =~ s/[\n\r]//g;
			if($curboard ne "$catinfo[0]") {
				fopen(BOARDDATA, "$boardsdir/$curboard.txt");
				@messages = <BOARDDATA>;
				fclose(BOARDDATA);
				foreach $curmessage (@messages) {
					($id, $dummy) = split(/\|/, $curmessage);
					unlink("$datadir/$id.txt");
					unlink("$datadir/$id.mail");
					unlink("$datadir/$id.data");
					$yymain .= "$txt{'49'} $id<br>";
					&Sticky_remove($id);
				}
			}
			unlink("$boardsdir/$curboard.txt");
			unlink("$boardsdir/$curboard.mail");
			unlink("$boardsdir/$curboard.dat");
			unlink("$boardsdir/$curboard.ctb");
			unlink("$boardsdir/$curboard.ttl");
			$yymain .= "$txt{'50'}<br>";
		}
		unlink("$boardsdir/$curcat.cat");
		$yymain .= "$txt{'61'}<br>";
		$yymain .= "$txt{'51'}";
		$yytitle = "$txt{'60'}";
		&template;
		exit;
		}
	}
}

1;