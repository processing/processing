package CGI::Simple::Util;
use strict;
use vars qw( $VERSION @EXPORT_OK @ISA $UTIL );
$VERSION = '1.105';
require Exporter;
@ISA       = qw( Exporter );
@EXPORT_OK = qw(
  rearrange make_attributes expires
  escapeHTML unescapeHTML escape unescape
);

sub rearrange {
    my ( $order, @params ) = @_;
    my ( %pos, @result, %leftover );
    return () unless @params;
    if ( ref $params[0] eq 'HASH' ) {
        @params = %{ $params[0] };
    }
    else {
        return @params unless $params[0] =~ m/^-/;
    }

    # map parameters into positional indices
    my $i = 0;
    for ( @$order ) {
        for ( ref( $_ ) eq 'ARRAY' ? @$_ : $_ ) { $pos{ lc( $_ ) } = $i; }
        $i++;
    }
    $#result = $#$order;    # preextend
    while ( @params ) {
        my $key = lc( shift( @params ) );
        $key =~ s/^\-//;
        if ( exists $pos{$key} ) {
            $result[ $pos{$key} ] = shift( @params );
        }
        else {
            $leftover{$key} = shift( @params );
        }
    }
    push @result, make_attributes( \%leftover, 1 ) if %leftover;
    return @result;
}

sub make_attributes {
    my $attref = shift;
    my $escape = shift || 0;
    return () unless $attref && ref $attref eq 'HASH';
    my @attrib;
    for my $key ( keys %{$attref} ) {
        ( my $mod_key = $key ) =~ s/^-//;    # get rid of initial - if present
        $mod_key = lc $mod_key;              # parameters are lower case
        $mod_key =~ tr/_/-/;                 # use dashes
        my $value = $escape ? escapeHTML( $attref->{$key} ) : $attref->{$key};
        push @attrib, defined $value ? qq/$mod_key="$value"/ : $mod_key;
    }
    return @attrib;
}

# This internal routine creates date strings suitable for use in
# cookies and HTTP headers.  (They differ, unfortunately.)
# Thanks to Mark Fisher for this.
sub expires {
    my ( $time, $format ) = @_;
    $format ||= 'http';
    my @MON  = qw( Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec);
    my @WDAY = qw( Sun Mon Tue Wed Thu Fri Sat );

    # pass through preformatted dates for the sake of expire_calc()
    $time = _expire_calc( $time );
    return $time unless $time =~ /^\d+$/;

    # make HTTP/cookie date string from GMT'ed time
    # (cookies use '-' as date separator, HTTP uses ' ')
    my $sc = $format eq 'cookie' ? '-' : ' ';
    my ( $sec, $min, $hour, $mday, $mon, $year, $wday ) = gmtime( $time );
    $year += 1900;
    return sprintf( "%s, %02d$sc%s$sc%04d %02d:%02d:%02d GMT",
        $WDAY[$wday], $mday, $MON[$mon], $year, $hour, $min, $sec );
}

# This internal routine creates an expires time exactly some number of
# hours from the current time.  It incorporates modifications from Mark Fisher.
# format for time can be in any of the forms...
#   "now"   -- expire immediately
#   "+180s" -- in 180 seconds
#   "+2m"   -- in 2 minutes
#   "+12h"  -- in 12 hours
#   "+1d"   -- in 1 day
#   "+3M"   -- in 3 months
#   "+2y"   -- in 2 years
#   "-3m"   -- 3 minutes ago(!)
# If you don't supply one of these forms, we assume you are specifying
# the date yourself
sub _expire_calc {
    my ( $time ) = @_;
    my %mult = (
        's' => 1,
        'm' => 60,
        'h' => 60 * 60,
        'd' => 60 * 60 * 24,
        'M' => 60 * 60 * 24 * 30,
        'y' => 60 * 60 * 24 * 365
    );
    my $offset;
    if ( !$time or lc $time eq 'now' ) {
        $offset = 0;
    }
    elsif ( $time =~ /^\d+/ ) {
        return $time;
    }
    elsif ( $time =~ /^([+-]?(?:\d+|\d*\.\d*))([mhdMy]?)/ ) {
        $offset = ( $mult{$2} || 1 ) * $1;
    }
    else {
        return $time;
    }
    return ( time + $offset );
}

sub escapeHTML {
    my ( $escape, $text ) = @_;
    return undef unless defined $escape;
    $escape =~ s/&/&amp;/g;
    $escape =~ s/"/&quot;/g;
    $escape =~ s/</&lt;/g;
    $escape =~ s/>/&gt;/g;

    # these next optional escapes make text look the same when rendered in HTML
    if ( $text ) {
        $escape =~ s/\t/    /g;                          # tabs to 4 spaces
        $escape =~ s/( {2,})/"&nbsp;" x length $1/eg;    # whitespace escapes
        $escape =~ s/\n/<br>\n/g;                        # newlines to <br>
    }
    return $escape;
}

sub unescapeHTML {
    my ( $unescape ) = @_;
    return undef unless defined( $unescape );
    my $latin = $UTIL->{'charset'} =~ /^(?:ISO-8859-1|WINDOWS-1252)$/i;
    my $ebcdic = $UTIL->{'ebcdic'};

    # credit to Randal Schwartz for original version of this
    $unescape =~ s[&(.*?);]{
        local $_ = $1;
        /^amp$/i           ? "&" :
        /^quot$/i          ? '"' :
        /^gt$/i            ? ">" :
        /^lt$/i            ? "<" :
        /^#(\d+)$/         && $latin  ? chr($1) :
        /^#(\d+)$/         && $ebcdic ? chr($UTIL->{'a2e'}->[$1]) :
        /^#x([0-9a-f]+)$/i && $latin  ? chr(hex($1)) :
        /^#x([0-9a-f]+)$/i && $ebcdic ? chr($UTIL->{'a2e'}->[hex $1]) :
        $_
    }gex;
    return $unescape;
}

# URL-encode data
sub escape {
    my ( $toencode ) = @_;
    return undef unless defined $toencode;
    if ( $UTIL->{'ebcdic'} ) {
        $toencode
          =~ s/([^a-zA-Z0-9_.-])/uc sprintf "%%%02x", $UTIL->{'e2a'}->[ord $1]/eg;
    }
    else {
        $toencode =~ s/([^a-zA-Z0-9_.-])/uc sprintf "%%%02x", ord $1 /eg;
    }
    return $toencode;
}

# unescape URL-encoded data
sub unescape {
    my ( $todecode ) = @_;
    return undef unless defined $todecode;
    $todecode =~ tr/+/ /;
    if ( $UTIL->{'ebcdic'} ) {
        $todecode =~ s/%([0-9a-fA-F]{2})/chr $UTIL->{'a2e'}->[hex $1]/ge;
    }
    else {
        $todecode =~ s/%(?:([0-9a-fA-F]{2})|u([0-9a-fA-F]{4}))/
        defined($1)? chr hex($1) : utf8_chr(hex($2))/ge;
    }
    return $todecode;
}

sub utf8_chr ($) {
    my $c = shift;
    if ( $c < 0x80 ) {
        return sprintf( "%c", $c );
    }
    elsif ( $c < 0x800 ) {
        return sprintf( "%c%c", 0xc0 | ( $c >> 6 ), 0x80 | ( $c & 0x3f ) );
    }
    elsif ( $c < 0x10000 ) {
        return sprintf( "%c%c%c",
            0xe0 | ( $c >> 12 ),
            0x80 | ( ( $c >> 6 ) & 0x3f ),
            0x80 | ( $c & 0x3f ) );
    }
    elsif ( $c < 0x200000 ) {
        return sprintf( "%c%c%c%c",
            0xf0 | ( $c >> 18 ),
            0x80 | ( ( $c >> 12 ) & 0x3f ),
            0x80 | ( ( $c >> 6 ) & 0x3f ),
            0x80 | ( $c & 0x3f ) );
    }
    elsif ( $c < 0x4000000 ) {
        return sprintf( "%c%c%c%c%c",
            0xf8 | ( $c >> 24 ),
            0x80 | ( ( $c >> 18 ) & 0x3f ),
            0x80 | ( ( $c >> 12 ) & 0x3f ),
            0x80 | ( ( $c >> 6 ) & 0x3f ),
            0x80 | ( $c & 0x3f ) );

    }
    elsif ( $c < 0x80000000 ) {
        return sprintf(
            "%c%c%c%c%c%c",
            0xfc | ( $c >> 30 ),             # was 0xfe patch Thomas L. Shinnick
            0x80 | ( ( $c >> 24 ) & 0x3f ),
            0x80 | ( ( $c >> 18 ) & 0x3f ),
            0x80 | ( ( $c >> 12 ) & 0x3f ),
            0x80 | ( ( $c >> 6 ) & 0x3f ),
            0x80 | ( $c & 0x3f )
        );
    }
    else {
        return utf8( 0xfffd );
    }
}

# We need to define a number of things about the operating environment so
# we do this on first initialization and store the results in in an object
BEGIN {

    $UTIL = new CGI::Simple::Util;    # initialize our $UTIL object

    sub new {
        my $class = shift;
        $class = ref( $class ) || $class;
        my $self = {};
        bless $self, $class;
        $self->init;
        return $self;
    }

    sub init {
        my $self = shift;
        $self->charset;
        $self->os;
        $self->ebcdic;
    }

    sub charset {
        my ( $self, $charset ) = @_;
        $self->{'charset'} = $charset if $charset;
        $self->{'charset'}
          ||= 'ISO-8859-1';    # set to the safe ISO-8859-1 if not defined
        return $self->{'charset'};
    }

    sub os {
        my ( $self, $OS ) = @_;
        $self->{'os'} = $OS if $OS;    # allow value to be set manually
        $OS = $self->{'os'};
        unless ( $OS ) {
            unless ( $OS = $^O ) {
                require Config;
                $OS = $Config::Config{'osname'};
            }
            if ( $OS =~ /Win/i ) {
                $OS = 'WINDOWS';
            }
            elsif ( $OS =~ /vms/i ) {
                $OS = 'VMS';
            }
            elsif ( $OS =~ /bsdos/i ) {
                $OS = 'UNIX';
            }
            elsif ( $OS =~ /dos/i ) {
                $OS = 'DOS';
            }
            elsif ( $OS =~ /^MacOS$/i ) {
                $OS = 'MACINTOSH';
            }
            elsif ( $OS =~ /os2/i ) {
                $OS = 'OS2';
            }
            else {
                $OS = 'UNIX';
            }
        }
        return $self->{'os'} = $OS;
    }

    sub ebcdic {
        my $self = shift;
        return $self->{'ebcdic'} if exists $self->{'ebcdic'};
        $self->{'ebcdic'} = "\t" ne "\011" ? 1 : 0;
        if ( $self->{'ebcdic'} ) {

            # (ord('^') == 95) for codepage 1047 as on os390, vmesa
            my @A2E = (
                0,   1,   2,   3,   55,  45,  46,  47,  22,  5,   21,  11,
                12,  13,  14,  15,  16,  17,  18,  19,  60,  61,  50,  38,
                24,  25,  63,  39,  28,  29,  30,  31,  64,  90,  127, 123,
                91,  108, 80,  125, 77,  93,  92,  78,  107, 96,  75,  97,
                240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 122, 94,
                76,  126, 110, 111, 124, 193, 194, 195, 196, 197, 198, 199,
                200, 201, 209, 210, 211, 212, 213, 214, 215, 216, 217, 226,
                227, 228, 229, 230, 231, 232, 233, 173, 224, 189, 95,  109,
                121, 129, 130, 131, 132, 133, 134, 135, 136, 137, 145, 146,
                147, 148, 149, 150, 151, 152, 153, 162, 163, 164, 165, 166,
                167, 168, 169, 192, 79,  208, 161, 7,   32,  33,  34,  35,
                36,  37,  6,   23,  40,  41,  42,  43,  44,  9,   10,  27,
                48,  49,  26,  51,  52,  53,  54,  8,   56,  57,  58,  59,
                4,   20,  62,  255, 65,  170, 74,  177, 159, 178, 106, 181,
                187, 180, 154, 138, 176, 202, 175, 188, 144, 143, 234, 250,
                190, 160, 182, 179, 157, 218, 155, 139, 183, 184, 185, 171,
                100, 101, 98,  102, 99,  103, 158, 104, 116, 113, 114, 115,
                120, 117, 118, 119, 172, 105, 237, 238, 235, 239, 236, 191,
                128, 253, 254, 251, 252, 186, 174, 89,  68,  69,  66,  70,
                67,  71,  156, 72,  84,  81,  82,  83,  88,  85,  86,  87,
                140, 73,  205, 206, 203, 207, 204, 225, 112, 221, 222, 219,
                220, 141, 142, 223
            );
            my @E2A = (
                0,   1,   2,   3,   156, 9,   134, 127, 151, 141, 142, 11,
                12,  13,  14,  15,  16,  17,  18,  19,  157, 10,  8,   135,
                24,  25,  146, 143, 28,  29,  30,  31,  128, 129, 130, 131,
                132, 133, 23,  27,  136, 137, 138, 139, 140, 5,   6,   7,
                144, 145, 22,  147, 148, 149, 150, 4,   152, 153, 154, 155,
                20,  21,  158, 26,  32,  160, 226, 228, 224, 225, 227, 229,
                231, 241, 162, 46,  60,  40,  43,  124, 38,  233, 234, 235,
                232, 237, 238, 239, 236, 223, 33,  36,  42,  41,  59,  94,
                45,  47,  194, 196, 192, 193, 195, 197, 199, 209, 166, 44,
                37,  95,  62,  63,  248, 201, 202, 203, 200, 205, 206, 207,
                204, 96,  58,  35,  64,  39,  61,  34,  216, 97,  98,  99,
                100, 101, 102, 103, 104, 105, 171, 187, 240, 253, 254, 177,
                176, 106, 107, 108, 109, 110, 111, 112, 113, 114, 170, 186,
                230, 184, 198, 164, 181, 126, 115, 116, 117, 118, 119, 120,
                121, 122, 161, 191, 208, 91,  222, 174, 172, 163, 165, 183,
                169, 167, 182, 188, 189, 190, 221, 168, 175, 93,  180, 215,
                123, 65,  66,  67,  68,  69,  70,  71,  72,  73,  173, 244,
                246, 242, 243, 245, 125, 74,  75,  76,  77,  78,  79,  80,
                81,  82,  185, 251, 252, 249, 250, 255, 92,  247, 83,  84,
                85,  86,  87,  88,  89,  90,  178, 212, 214, 210, 211, 213,
                48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  179, 219,
                220, 217, 218, 159
            );
            if ( ord( '^' ) == 106 )
            {    # as in the BS2000 posix-bc coded character set
                $A2E[91]  = 187;
                $A2E[92]  = 188;
                $A2E[94]  = 106;
                $A2E[96]  = 74;
                $A2E[123] = 251;
                $A2E[125] = 253;
                $A2E[126] = 255;
                $A2E[159] = 95;
                $A2E[162] = 176;
                $A2E[166] = 208;
                $A2E[168] = 121;
                $A2E[172] = 186;
                $A2E[175] = 161;
                $A2E[217] = 224;
                $A2E[219] = 221;
                $A2E[221] = 173;
                $A2E[249] = 192;

                $E2A[74]  = 96;
                $E2A[95]  = 159;
                $E2A[106] = 94;
                $E2A[121] = 168;
                $E2A[161] = 175;
                $E2A[173] = 221;
                $E2A[176] = 162;
                $E2A[186] = 172;
                $E2A[187] = 91;
                $E2A[188] = 92;
                $E2A[192] = 249;
                $E2A[208] = 166;
                $E2A[221] = 219;
                $E2A[224] = 217;
                $E2A[251] = 123;
                $E2A[253] = 125;
                $E2A[255] = 126;
            }
            elsif ( ord( '^' ) == 176 ) {    # as in codepage 037 on os400
                $A2E[10]  = 37;
                $A2E[91]  = 186;
                $A2E[93]  = 187;
                $A2E[94]  = 176;
                $A2E[133] = 21;
                $A2E[168] = 189;
                $A2E[172] = 95;
                $A2E[221] = 173;

                $E2A[21]  = 133;
                $E2A[37]  = 10;
                $E2A[95]  = 172;
                $E2A[173] = 221;
                $E2A[176] = 94;
                $E2A[186] = 91;
                $E2A[187] = 93;
                $E2A[189] = 168;
            }
            $self->{'a2e'} = \@A2E;
            $self->{'e2a'} = \@E2A;
        }
    }
}

1;

__END__

=head1 NAME

CGI::Simple::Util - Internal utilities used by CGI::Simple module

=head1 SYNOPSIS

    $escaped     = escapeHTML('In HTML you need to escape < > " and & chars');
    $unescaped   = unescapeHTML('&lt;&gt;&quot;&amp;');
    $url_encoded = escape($string);
    $decoded     = unescape($url_encoded);

=head1 DESCRIPTION

CGI::Simple::Util contains essentially non public subroutines used by
CGI::Simple. There are HTML and URL escape and unescape routines that may
be of some use.

An internal object is used to store a number of system specific details to
enable the escape routines to be accurate.

=head1 AUTHOR INFORMATION

Original version copyright 1995-1998, Lincoln D. Stein.  All rights reserved.
Originally copyright 2001 Dr James Freeman E<lt>jfreeman@tassie.net.auE<gt>
This release by Andy Armstrong <andy@hexten.net>

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

Address bug reports and comments to: andy@hexten.net

=head1 SEE ALSO

L<CGI::Simple>

=cut
