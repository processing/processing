###############################################################################
# ManageBoards.pl                                                             #
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

$manageboardsplver = "1 Gold - SP 1.4";

sub ManageBoards {
	&is_admin;
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	$yymain .= qq~
<table border="0" align="center" cellspacing="1" cellpadding="4" bgcolor="$color{'bordercolor'}" class="bordercolor" width="90%">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}" colspan="3">
    <img src="$imagesdir/cat.gif" alt="" border="0">
    <font class="text1" color="$color{'titletext'}" size="1"><b>$txt{'41'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}" colspan="3"><BR><font size="1">$txt{'677'}</font><BR><BR></td>
  </tr><tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}" width="*"><font class="text1" color="$color{'titletext'}" size="1"><b>$txt{'20'} / $txt{'782'}</b></font></td>
    <td class="titlebg" bgcolor="$color{'titlebg'}" width="20%"><font class="text1" color="$color{'titletext'}" size="1"><b>$txt{'12'}</b></font></td>
    <td class="titlebg" bgcolor="$color{'titlebg'}" width="20%"><font class="text1" color="$color{'titletext'}" size="1"><b>$txt{'42'}</b></font></td>
  </tr>
~;
	foreach $curcat (@categories) {
		$curcat =~ s/[\n\r]//g;
		fopen(CAT, "$boardsdir/$curcat.cat");
		@catinfo = <CAT>;
		fclose(CAT);
		$curcatname = $catinfo[0];
		$yymain .= qq~
  <tr>
    <td colspan="3" bgcolor="$color{'catbg'}" class="catbg" height="25"><font size="2"><b>$curcatname</b> <a href="$cgi;action=reorderboards;cat=$curcat">($txt{'643'})</a></font></td>
  </tr>
~;
		$curboardnum=0;
		foreach $curboard (@catinfo) {
			if($curboardnum > 1) {
				$curboard =~ s/[\n\r]//g;
				chomp $curboard;
				fopen(BOARD, "$boardsdir/$curboard.dat");
				@boardinfo = <BOARD>;
				fclose(BOARD);
				$curboardname = $boardinfo[0];
				chomp $curboardname;
				$descr = $boardinfo[1];
				$boardinfo[2] =~ s/[\n\r]//g;

				$boardinfo[2] =~ /^\|(.*?)\|$/;
				$multimoderators = $1 or $multimoderators = $boardinfo[2];
				$multimoderators =~ s/\|(\S?)/,$1/g;

				$moderator = "$modprop[1]";
				$yymain .= qq~
  <tr>
    <td bgcolor="$color{'windowbg2'}" class="windowbg2" width="100%" valign="top" colspan="3">
    <form action="$cgi;action=modifyboard" method="POST">
    <table border="0" cellspacing="0" cellpadding="3">
      <tr>
        <td width="10" valign="top"><img src="$imagesdir/board.gif" alt="" border="0"></td>
        <td><font size="2">
        <input type="hidden" name="id" value="$curboard"><input type="hidden" name="cat" value="$curcat">
        <input type="text" name="boardname" value="$curboardname" size="30"><br>
        <textarea name="descr" cols="50" rows="3">$descr</textarea></font></td>
        <td valign="middle" width="20%"><input type="text" name="moderator" value="$multimoderators" size="25"></td>
        <td valign="middle" width="20%"><input type="submit" name="moda" value="$txt{'17'}">
        <input type="submit" name="moda" value="$txt{'31'}"></td>
      </tr>
    </table>
    </form>
    </td>
  </tr>
~;
			}
$curboardnum++;
		}
		$yymain .= qq~
  <tr>
    <td bgcolor="$color{'windowbg'}" class="windowbg" width="100%" valign=top colspan="3">
    <form action="$cgi;action=addboard" method="POST">
    <table border="0" cellspacing="0" cellpadding="3">
      <tr>
        <td colspan="3">
        <table border="0" cellpadding="3" cellspacing="0">
          <tr>
            <td width="10" valign="top"><img src="$imagesdir/board.gif" alt="" border="0"></td>
            <td valign="middle"><font size="2">
            <b>$txt{'43'}: </b><input type=text name="id" value="" size="15"></font></td>
            <td valign="middle"><font size="2">
            <b>$txt{'44'}: </b><input type=text name="boardname" size="30"></font></td>
          </tr>
        </table>
        </td>
      </tr><tr>
        <td valign="bottom"><font size="2"><b>$txt{'782'}:</b><br>
        <textarea name="descr" cols="50" rows="3"></textarea></font></td>
        <td valign="bottom" width="20%"><font size="2">
        <b>$txt{'299'}: </b><input type=text name=moderator value="" size="25">
        <input type=hidden name="cat" value="$curcat"></font></td>
        <td valign="bottom" width="20%"><input type=submit value="$txt{'45'}"></td>
      </tr>
    </table>
    </form>
    </td>
  </tr>
~;
	}
	$yymain .= "</table>";
	$yytitle = "$txt{'41'}";
	&template;
	exit;
}

sub ReorderBoards {
	&is_admin;
	fopen(FILE, "$boardsdir/$INFO{'cat'}.cat");
	@allboards = <FILE>;
	fclose(FILE);
	$boardlist="";
	$firstline=$allboards[0]; $secondline=$allboards[1];
	$allboards[0]="#EMPTYCATNAME#";	$allboards[1]="#EMPTYGROUPS#";
	foreach $cboard (@allboards) {
		$cboard =~ s/[\n\r]//g;
		if($cboard ne "$allboards[0]" && $cboard ne "$allboards[1]") { $boardlist="$boardlist\n$cboard"; }
	}
	$yymain .= qq~
<table border=0 width="400" cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <img src="$imagesdir/board.gif">
    <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'46'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}" valign=top align="center"><font size=2>
   <form action="$cgi;action=reorderboards2" method="POST">
    <b>$txt{'47'}:</b><br>
    <textarea name="boards" cols="40" rows="4">$boardlist</textarea><br>
    <input type=hidden name="firstline" value="$firstline">
    <input type=hidden name="secondline" value="$secondline">
    <input type=hidden name="cat" value="$INFO{'cat'}">
    <input type=submit value="$txt{'46'}">
    </form>
    </font></td>
  </tr>
</table>
~;
	$yytitle = "$txt{'46'}";
	&template;
	exit;
}

sub ReorderBoards2 {
	&is_admin;
	$FORM{'firstline'} =~ s/[\n\r]//g;
	$FORM{'secondline'} =~ s/[\n\r]//g;
	$FORM{'boards'} =~ s/\r//g;
	$boards = $FORM{'boards'};
	(@boards) = split(/\n/, $boards);
	$theboards = "";
	foreach $cboard (@boards) {
		$cboard =~ s/[\n\r]//g;
		if(-e("$boardsdir/$cboard.dat") && $cboard !~ /\A\n*\Z/) { $theboards .= "$cboard\n"; }
	}
	$theboards =~ s/\A\n*\r*\Z//g;
	fopen(FILE, ">$boardsdir/$FORM{'cat'}.cat");
	print FILE "$FORM{'firstline'}\n";
	print FILE "$FORM{'secondline'}\n";
	print FILE "$theboards";
	fclose(FILE);
	&BoardCatsMake;
	$yySetLocation = qq~$cgi;action=reorderboards;cat=$FORM{'cat'}~;
	&redirectexit;
}

sub ConfRemBoard {
	$yymain .= qq~
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
<tr>
	<td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'31'} - '$FORM{'boardname'}'?</b></font></td>
</tr>
<tr>
	<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
$txt{'617'}<br>
<b><a href="$cgi;action=modifyboard;cat=$FORM{'cat'};id=$FORM{'id'};moda=$txt{'31'}2">$txt{'163'}</a> - <a href="$cgi;action=manageboards">$txt{'164'}</a></b>
</font></td>
</tr>
</table>
~;
	$yytitle = "$txt{'31'} - '$FORM{'boardname'}'?";
	&template;
	exit;

}

sub ModifyBoard {
	&is_admin;
	if($FORM{'moda'} eq "$txt{'17'}") {
		if ($FORM{'moderator'}) { &AllowMod; }
		fopen(FILE, ">$boardsdir/$FORM{'id'}.dat", 1);
		$FORM{'descr'} =~ s/\n/ /g;
		$FORM{'descr'} =~ s/\r//g;
		$FORM{'moderator'} =~ s/(\s*)//g;              # remove all whitespaces
		$FORM{'moderator'} =~ s/(^(\,)+)?((\,)+$)?//;  # remove unnecessary front and back separator
		$FORM{'moderator'} =~ s/(\,)+/\,/g;            # replace multiple separators with one separator

		# replace "," with "|" and remove multiple entries
		my @mod_ary = split(/\,/, $FORM{'moderator'});
		my %seen = ();
		foreach my $mod( @mod_ary) {
			$seen{$mod}++;
		}
		@mod_ary = keys %seen;

		$mods = join( "|", @mod_ary);

		print FILE "$FORM{'boardname'}\n";
		print FILE "$FORM{'descr'}\n";
		print FILE "$mods\n";
		fclose(FILE);

		$yySetLocation = qq~$cgi;action=manageboards~;
		&redirectexit;
	} else {
		if($FORM{'moda'} eq "$txt{'31'}") { &ConfRemBoard; }
		else {
		fopen(FILE, "$boardsdir/$INFO{'cat'}.cat");
		@categories = <FILE>;
		fclose(FILE);
		chomp @categories;
		@categories[0] =~ s/\n//g;
		@categories[1] =~ s/\n//g;
		$newcatlist = "@categories[0]\n@categories[1]\n";
		@categories[0]="#EMPTYCATNAME#"; @categories[1]="#EMPTYGROUPS#";
		foreach $curboard (@categories) {
			$curboard =~ s/\n//g;
			if($curboard ne "$INFO{'id'}" && $curboard ne @categories[0] && $curboard ne @categories[1]) { $newcatlist.="$curboard\n"; }
		}
		fopen(FILE, ">$boardsdir/$INFO{'cat'}.cat", 1);
		print FILE "$newcatlist";
		fclose(FILE);

		$curboard="$INFO{'id'}";
		fopen(BOARDDATA, "$boardsdir/$curboard.txt");
		@messages = <BOARDDATA>;
		fclose(BOARDDATA);
		foreach $curmessage (@messages) {
			($id, $dummy) = split(/\|/, $curmessage);
			unlink("$datadir/$id\.txt");
			unlink("$datadir/$id\.mail");
			unlink("$datadir/$id\.data");
			$yymain .= "$txt{'49'} $id<br>";
			&Sticky_remove($id);
		}
		unlink("$boardsdir/$curboard.dat");
		unlink("$boardsdir/$curboard.txt");
		unlink("$boardsdir/$curboard.ttl");
		unlink("$boardsdir/$curboard.ctb");
		$yymain .= "$txt{'50'}<br>";
		$yymain .= "$txt{'51'}";
		$yymain .= "id $INFO{'id'} - moda $INFO{'moda'} - cat $INFO{'cat'}";
		$yytitle = "$txt{'48'}";
		&template;
		}
	}
	&BoardCatsMake;

	exit;
}

sub CreateBoard {
	&is_admin;
	$id = $FORM{'id'};
	&fatal_error("$txt{'240'} $txt{'43'} $txt{'241'}") if($id !~ /\A[0-9A-Za-z#%+-\.@^_]+\Z/);

	# make sure no board already exists with that id
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	foreach $curcat (@categories) {
		$curcat =~ s/[\n\r]//g;
		fopen(CAT, "$boardsdir/$curcat.cat");
		@boards = <CAT>;
		fclose(CAT);
		foreach $curboard (@boards) {
			$curboard =~ s/[\n\r]//g;
			if($curboard ne "$boards[0]" && $curboard ne "$boards[1]") {
			&fatal_error("$txt{'674'} '$id' $txt{'675'}") if ($id eq "$curboard");
			}
		}
	}


	if ($FORM{'moderator'}) { &AllowMod; }
	fopen(FILE, "$boardsdir/$FORM{'cat'}.cat");
	@categories = <FILE>;
	fclose(FILE);
	fopen(FILE, ">$boardsdir/$FORM{'cat'}.cat", 1);
	foreach $curboard (@categories) {
		$curboard =~ s/[\n\r]//g;
		print FILE "$curboard\n";
	}
	$id =~ s/ /_/g;
	print FILE "$id";
	fclose(FILE);

	fopen(FILE, ">$boardsdir/$id.dat");
	$FORM{'descr'} =~ s/\n/ /g;
	$FORM{'descr'} =~ s/\r//g;
	$FORM{'moderator'} =~ s/(\s*)//g;              # remove all whitespaces
	$FORM{'moderator'} =~ s/(^(\,)+)?((\,)+$)?//;  # remove unnecessary front and back separator
	$FORM{'moderator'} =~ s/(\,)+/\,/g;            # replace multiple separators with one separator

	# replace "," with "|" and remove multiple entries
	my @mod_ary = split(/\,/, $FORM{'moderator'});
	my %seen = ();
	foreach my $mod( @mod_ary) {
		$seen{$mod}++;
	}
	@mod_ary = keys %seen;

	$mods = join( "|", @mod_ary);

	print FILE "$FORM{'boardname'}\n";
	print FILE "$FORM{'descr'}\n";
	print FILE "$mods\n";
	fclose(FILE);
	fopen(FILE, ">$boardsdir/$id.txt");
	print FILE '';
	fclose(FILE);

	&BoardCatsMake;
	$yySetLocation = qq~$cgi;action=manageboards~;
	&redirectexit;
}

sub AllowMod { 
	$FORM{'moderator'} =~ s/\s*,\s*/,/g;
	fopen(FILE, "$boardsdir/$FORM{'cat'}.cat");
	@catinfo = <FILE>;
	fclose(FILE); 
	(@curcatgroups) = split(/\,/, $catinfo[1]);
	$text=""; 
	foreach $curuser (split(/\,/, $FORM{'moderator'})) { 
		if(-e "$memberdir/$curuser.dat") { 
			fopen(FILE, "$memberdir/$curuser.dat");
			@memsettings=<FILE>;
			fclose(FILE); 
			$memsettings[7] =~ s~[\n\r]~~g;
			chomp $memsettings[7]; 
			$notfound = 0; 
			foreach $cur (@curcatgroups) { 
				$cur =~ s~[\n\r]~~g;
				chomp $cur; 
				if ($cur ne '') { 
					if ($memsettings[7] ne $cur && $memsettings[7] ne "Administrator") { $notfound = 1; }
					else { $notfound = 0; last;} 
				} 
			} 
			if ($notfound eq 1) { $text.= qq~$curuser $txt{'797'} "$FORM{'boardname'}" $txt{'798'} ~; } 
		} else { $text .=  qq~$curuser $txt{'799'} ~; } 
	} 
	if ($text ne "") {&fatal_error($text);} 
}

1;