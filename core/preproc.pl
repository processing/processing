#!/usr/bin/perl

# PGraphics subclasses PImage.. so first get methods from bimage
open(F, "PImage.java") || die $!;
@contents = <F>;
close(F);

# next slurp methods from PGraphics
open(F, "PGraphics.java") || die $!;
#@contents = <F>;
foreach $line (<F>) {
    # can't remember perl right now.. there must be a better way
    @contents[$#contents++] = $line;
}
close(F);


open(APPLET, "PApplet.java") || die $!;
@applet = <APPLET>;
close(APPLET);


$insert = 'public functions for processing.core';

# an improved version of this would only rewrite if changes made
open(OUT, ">PApplet.new") || die $!;
foreach $line (@applet) {
    print OUT $line;
    last if ($line =~ /$insert/);
}

#open(INTF, ">PMethods.java") || die $!;
#print INTF "package processing.core;\n\n\n\n";
#print INTF "// this file is auto-generated. no touchy-touchy.\n\n";
#print INTF "public interface PMethods {\n";

$comments = 0;

while ($line = shift(@contents)) {
    $decl = "";

    if ($line =~ /\/\*/) {
        $comments++;
    }
    if ($line =~ /\*\//) {
        $comments--;
    }
    next if ($comments > 0);

    $got_something = 0;  # so it's ugly, back off
    $got_static = 0;
    $got_interface = 0;

    if ($line =~ /^\s*public ([\w\[\]]+) [a-zA-z_]+\(.*$/) {
        $got_something = 1;
	$got_interface = 1;
    } elsif ($line =~ /^\s*public final ([\w\[\]]+) [a-zA-z_]+\(.*$/) {
        $got_something = 1;
    } elsif ($line =~ /^\s*static public ([\w\[\]]+) [a-zA-z_]+\(.*$/) {
        $got_something = 1;
	$got_static = 1;
    }
    # if function is marked "// ignore" then, uh, ignore it.
    if (($got_something == 1) && ($line =~ /\/\/ ignore/)) {
	$got_something = 0;
    }
    #if ($line =~ /^\s*public (\w+) [a-zA-z_]+\(.*$/) {
    if ($got_something == 1) {
        if ($1 ne 'void') {
            $returns = 'return';
        } else {
            $returns = '';
        }
        print OUT "\n\n$line";

	if ($got_interface == 1) {
	    $iline = $line;
	    $iline =~ s/ \{/\;/;
#	    print INTF "\n$iline";
	}

        $decl .= $line;
        while (!($line =~ /\)/)) {
            $line = shift (@contents);
            $decl .= $line;
            print OUT $line;

	    if ($got_interface == 1) {
		$iline = $line;
		$iline =~ s/ \{/\;/;
#		print INTF $iline;
	    }
        }

        $decl =~ /\s(\S+)\(/;
        $decl_name = $1;
	if ($got_static == 1) {
	    print OUT "    $returns PGraphics.${decl_name}(";
	} else {
	    print OUT "    $returns g.${decl_name}(";
	}

        $decl =~ s/\s+/ /g; # smush onto a single line
        $decl =~ s/^.*\(//;
        $decl =~ s/\).*$//;

        $prev = 0;
        @parts = split(', ', $decl);
        foreach $part (@parts) {
            ($the_type, $the_arg) = split(' ', $part);
            $the_arg =~ s/[\[\]]//g;
            if ($prev != 0) {
                print OUT ", ";
            }
            print OUT "${the_arg}";
            $prev = 1;
        }
        print OUT ");\n";

        print OUT "  }\n"; #\n";
    }
}
print OUT "}\n";
#print INTF "}\n";

close(OUT);
#close(INTF);

$oldguy = join(' ', @applet);

open(NEWGUY, "PApplet.new") || die $!;
@newbie = <NEWGUY>;
$newguy = join(' ', @newbie);
close(NEWGUY);

if ($oldguy ne $newguy) {
    # replace him
    print "updating PApplet with PGraphics api changes\n";
    `mv PApplet.new PApplet.java`;
} else {
    # just kill the new guy
    #print "no changes to applet\n";
    `rm PApplet.new`;
}

# then do the actual building
#print `perl buzz.pl '${JIKES} -target 1.1 -bootclasspath $extra_classes +D -d classes' $video_flag $sonic_flag $serial_flag $opengl_flag $illustrator_flag $jdk13_flag *.java`;
