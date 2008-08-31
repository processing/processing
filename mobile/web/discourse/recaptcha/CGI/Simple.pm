package CGI::Simple;

require 5.004;

# this module is both strict (and warnings) compliant, but they are only used
# in testing as they add an unnecessary compile time overhead in production.
use strict;
use warnings;
use Carp;

use vars qw(
  $VERSION $USE_CGI_PM_DEFAULTS $DISABLE_UPLOADS $POST_MAX
  $NO_UNDEF_PARAMS $USE_PARAM_SEMICOLONS $HEADERS_ONCE
  $NPH $DEBUG $NO_NULL $FATAL *in
);

$VERSION = "1.105";

# you can hard code the global variable settings here if you want.
# warning - do not delete the unless defined $VAR part unless you
# want to permanently remove the ability to change the variable.
sub _initialize_globals {

    # set this to 1 to use CGI.pm default global settings
    $USE_CGI_PM_DEFAULTS = 0
      unless defined $USE_CGI_PM_DEFAULTS;

    # see if user wants old CGI.pm defaults
    if ( $USE_CGI_PM_DEFAULTS ) {
        _use_cgi_pm_global_settings();
        return;
    }

    # no file uploads by default, set to 0 to enable uploads
    $DISABLE_UPLOADS = 1
      unless defined $DISABLE_UPLOADS;

    # use a post max of 100K, set to -1 for no limits
    $POST_MAX = 102_400
      unless defined $POST_MAX;

    # set to 1 to not include undefined params parsed from query string
    $NO_UNDEF_PARAMS = 0
      unless defined $NO_UNDEF_PARAMS;

    # separate the name=value pairs with ; rather than &
    $USE_PARAM_SEMICOLONS = 0
      unless defined $USE_PARAM_SEMICOLONS;

    # only print headers once
    $HEADERS_ONCE = 0
      unless defined $HEADERS_ONCE;

    # Set this to 1 to enable NPH scripts
    $NPH = 0
      unless defined $NPH;

    # 0 => no debug, 1 => from @ARGV,  2 => from STDIN
    $DEBUG = 0
      unless defined $DEBUG;

    # filter out null bytes in param - value pairs
    $NO_NULL = 1
      unless defined $NO_NULL;

    # set behavior when cgi_err() called -1 => silent, 0 => carp, 1 => croak
    $FATAL = -1
      unless defined $FATAL;
}

# I happen to disagree with many of the default global settings in CGI.pm
# This sub is called if you set $CGI::Simple::USE_CGI_PM_GLOBALS = 1; or
# invoke the '-default' pragma via a use CGI::Simple qw(-default);
sub _use_cgi_pm_global_settings {
    $USE_CGI_PM_DEFAULTS  = 1;
    $DISABLE_UPLOADS      = 0 unless defined $DISABLE_UPLOADS;
    $POST_MAX             = -1 unless defined $POST_MAX;
    $NO_UNDEF_PARAMS      = 0 unless defined $NO_UNDEF_PARAMS;
    $USE_PARAM_SEMICOLONS = 1 unless defined $USE_PARAM_SEMICOLONS;
    $HEADERS_ONCE         = 0 unless defined $HEADERS_ONCE;
    $NPH                  = 0 unless defined $NPH;
    $DEBUG                = 1 unless defined $DEBUG;
    $NO_NULL              = 0 unless defined $NO_NULL;
    $FATAL                = -1 unless defined $FATAL;
}

# this is called by new, we will never directly reference the globals again
sub _store_globals {
    my $self = shift;

    $self->{'.globals'}->{'DISABLE_UPLOADS'}      = $DISABLE_UPLOADS;
    $self->{'.globals'}->{'POST_MAX'}             = $POST_MAX;
    $self->{'.globals'}->{'NO_UNDEF_PARAMS'}      = $NO_UNDEF_PARAMS;
    $self->{'.globals'}->{'USE_PARAM_SEMICOLONS'} = $USE_PARAM_SEMICOLONS;
    $self->{'.globals'}->{'HEADERS_ONCE'}         = $HEADERS_ONCE;
    $self->{'.globals'}->{'NPH'}                  = $NPH;
    $self->{'.globals'}->{'DEBUG'}                = $DEBUG;
    $self->{'.globals'}->{'NO_NULL'}              = $NO_NULL;
    $self->{'.globals'}->{'FATAL'}                = $FATAL;
    $self->{'.globals'}->{'USE_CGI_PM_DEFAULTS'}  = $USE_CGI_PM_DEFAULTS;
}

# use the automatic calling of the import sub to set our pragmas. CGI.pm compat
sub import {
    my ( $self, @args ) = @_;

    # arguments supplied in the 'use CGI::Simple [ARGS];' will now be in @args
    foreach ( @args ) {
        $USE_CGI_PM_DEFAULTS = 1, next if m/^-default/i;
        $DISABLE_UPLOADS     = 1, next if m/^-no.?upload/i;
        $DISABLE_UPLOADS     = 0, next if m/^-upload/i;
        $HEADERS_ONCE        = 1, next if m/^-unique.?header/i;
        $NPH                 = 1, next if m/^-nph/i;
        $DEBUG               = 0, next if m/^-no.?debug/i;
        $DEBUG = defined $1 ? $1 : 2, next if m/^-debug(\d)?/i;
        $USE_PARAM_SEMICOLONS = 1, next if m/^-newstyle.?url/i;
        $USE_PARAM_SEMICOLONS = 0, next if m/^-oldstyle.?url/i;
        $NO_UNDEF_PARAMS      = 1, next if m/^-no.?undef.?param/i;
        $FATAL                = 0, next if m/^-carp/i;
        $FATAL                = 1, next if m/^-croak/i;
        croak "Pragma '$_' is not defined in CGI::Simple\n";
    }
}

# used in CGI.pm .t files
sub _reset_globals {
    _use_cgi_pm_global_settings();
}

binmode STDIN;
binmode STDOUT;

# use correct encoding conversion to handle non ASCII char sets.
# we import and install the complex routines only if we have to.
BEGIN {

    sub url_decode {
        my ( $self, $decode ) = @_;
        return () unless defined $decode;
        $decode =~ tr/+/ /;
        $decode =~ s/%([a-fA-F0-9]{2})/ pack "C", hex $1 /eg;
        return $decode;
    }

    sub url_encode {
        my ( $self, $encode ) = @_;
        return () unless defined $encode;
        $encode =~ s/([^A-Za-z0-9\-_.!~*'() ])/ uc sprintf "%%%02x",ord $1 /eg;
        $encode =~ tr/ /+/;
        return $encode;
    }

    if ( "\t" ne "\011" ) {
        eval { require CGI::Simple::Util };
        if ( $@ ) {
            croak
              "Your server is using not using ASCII, you must install CGI::Simple::Util, error: $@";
        }

        # hack the symbol table and replace simple encode/decode subs
        *CGI::Simple::url_encode = sub { CGI::Simple::Util::escape( $_[1] ) };
        *CGI::Simple::url_decode = sub { CGI::Simple::Util::unescape( $_[1] ) };
    }
}

################ The Guts ################

sub new {
    my ( $class, $init ) = @_;
    $class = ref( $class ) || $class;
    my $self = {};
    bless $self, $class;
    if ( $self->_mod_perl ) {
        if ( $init ) {
            $self->{'.mod_perl_request'} = $init;
            undef $init;    # otherwise _initialize takes the wrong path
        }
        $self->_initialize_mod_perl();
    }
    $self->_initialize_globals;
    $self->_store_globals;
    $self->_initialize( $init );
    return $self;
}

sub _mod_perl {
    return (
        exists $ENV{MOD_PERL}
          or (  $ENV{GATEWAY_INTERFACE}
            and $ENV{GATEWAY_INTERFACE} =~ m{^CGI-Perl/} )
    );
}

# Return the global request object under mod_perl. If you use mod_perl 2
# and you don't set PerlOptions +GlobalRequest then the request must be
# passed in to the new() method.
sub _mod_perl_request {
    my $self = shift;

    my $mp = $self->{'.mod_perl'};

    return unless $mp;

    my $req = $self->{'.mod_perl_request'};
    return $req if $req;

    $self->{'.mod_perl_request'} = do {
        if ( $mp == 2 ) {
            Apache2::RequestUtil->request;
        }
        else {
            Apache->request;
        }
    };
}

sub _initialize_mod_perl {
    my ( $self ) = @_;

    eval "require mod_perl";

    if ( defined $mod_perl::VERSION ) {

        if ( $mod_perl::VERSION >= 2.00 ) {
            $self->{'.mod_perl'} = 2;

            require Apache2::RequestRec;
            require Apache2::RequestUtil;
            require APR::Pool;

            my $r = $self->_mod_perl_request();

            if ( defined $r ) {
                $r->subprocess_env unless exists $ENV{REQUEST_METHOD};
                $r->pool->cleanup_register(
                    \&CGI::Simple::_initialize_globals );
            }
        }
        else {
            $self->{'.mod_perl'} = 1;

            require Apache;

            my $r = $self->_mod_perl_request();

            if ( defined $r ) {
                $r->register_cleanup( \&CGI::Simple::_initialize_globals );
            }
        }
    }
}

sub _initialize {
    my ( $self, $init ) = @_;

    if ( !defined $init ) {

        # initialize from QUERY_STRING, STDIN or @ARGV
        $self->_read_parse();
    }
    elsif ( ( ref $init ) =~ m/HASH/i ) {

        # initialize from param hash
        for my $param ( keys %{$init} ) {
            $self->_add_param( $param, $init->{$param} );
        }
    }

    # chromatic's blessed GLOB patch
    # elsif ( (ref $init) =~ m/GLOB/i ) { # initialize from a file
    elsif ( UNIVERSAL::isa( $init, 'GLOB' ) ) {    # initialize from a file
        $self->_init_from_file( $init );
    }
    elsif ( ( ref $init ) eq 'CGI::Simple' ) {

        # initialize from a CGI::Simple object
        require Data::Dumper;

        # avoid problems with strict when Data::Dumper returns $VAR1
        my $VAR1;
        my $clone = eval( Data::Dumper::Dumper( $init ) );
        if ( $@ ) {
            $self->cgi_error( "Can't clone CGI::Simple object: $@" );
        }
        else {
            $_[0] = $clone;
        }
    }
    else {
        $self->_parse_params( $init );    # initialize from a query string
    }
}

sub _internal_read($\$;$) {
    my ( $self, $buffer, $len ) = @_;
    $len = 4096 if !defined $len;
    if ( $self->{'.mod_perl'} ) {
        my $r = $self->_mod_perl_request();
        $r->read( $$buffer, $len );
    }
    else {
        read( STDIN, $$buffer, $len );
    }
}

sub _read_parse {
    my $self   = shift;
    my $data   = '';
    my $type   = $ENV{'CONTENT_TYPE'} || 'No CONTENT_TYPE received';
    my $length = $ENV{'CONTENT_LENGTH'} || 0;
    my $method = $ENV{'REQUEST_METHOD'} || 'No REQUEST_METHOD received';

    # first check POST_MAX Steve Purkis pointed out the previous bug
    if (    ( $method eq 'POST' or $method eq "PUT" )
        and $self->{'.globals'}->{'POST_MAX'} != -1
        and $length > $self->{'.globals'}->{'POST_MAX'} ) {
        $self->cgi_error(
            "413 Request entity too large: $length bytes on STDIN exceeds \$POST_MAX!"
        );

        # silently discard data ??? better to just close the socket ???
        while ( $length > 0 ) {
            last unless _internal_read( $self, my $buffer );
            $length -= length( $buffer );
        }

        return;
    }

    if ( $length and $type =~ m|^multipart/form-data|i ) {
        my $got_length = $self->_parse_multipart;
        if ( $length != $got_length ) {
            $self->cgi_error(
                "500 Bad read on multipart/form-data! wanted $length, got $got_length"
            );
        }

        return;
    }
    elsif ( $method eq 'POST' or $method eq 'PUT' ) {
        if ( $length ) {

            # we may not get all the data we want with a single read on large
            # POSTs as it may not be here yet! Credit Jason Luther for patch
            # CGI.pm < 2.99 suffers from same bug
            _internal_read( $self, $data, $length );
            while ( length( $data ) < $length ) {
                last unless _internal_read( $self, my $buffer );
                $data .= $buffer;
            }

            unless ( $length == length $data ) {
                $self->cgi_error( "500 Bad read on POST! wanted $length, got "
                      . length( $data ) );
                return;
            }

            if ( $type !~ m|^application/x-www-form-urlencoded| ) {
                $self->_add_param( $method . "DATA", $data );
            }
            else {
                $self->_parse_params( $data );
            }
        }
    }
    elsif ( $method eq 'GET' or $method eq 'HEAD' ) {
        $data
          = $self->{'.mod_perl'}
          ? $self->_mod_perl_request()->args()
          : $ENV{'QUERY_STRING'}
          || $ENV{'REDIRECT_QUERY_STRING'}
          || '';
        $self->_parse_params( $data );
    }
    else {
        unless ($self->{'.globals'}->{'DEBUG'}
            and $data = $self->read_from_cmdline() ) {
            $self->cgi_error( "400 Unknown method $method" );
            return;
        }

        unless ( $data ) {

    # I liked this reporting but CGI.pm does not behave like this so
    # out it goes......
    # $self->cgi_error("400 No data received via method: $method, type: $type");
            return;
        }

        $self->_parse_params( $data );
    }
}

sub _parse_params {
    my ( $self, $data ) = @_;
    return () unless defined $data;
    unless ( $data =~ /[&=;]/ ) {
        $self->{'keywords'} = [ $self->_parse_keywordlist( $data ) ];
        return;
    }
    my @pairs = split /[&;]/, $data;
    for my $pair ( @pairs ) {
        my ( $param, $value ) = split /=/, $pair, 2;
        next unless defined $param;
        $value = '' unless defined $value;
        $self->_add_param( $self->url_decode( $param ),
            $self->url_decode( $value ) );
    }
}

sub _add_param {
    my ( $self, $param, $value, $overwrite ) = @_;
    return () unless defined $param and defined $value;
    $param =~ tr/\000//d if $self->{'.globals'}->{'NO_NULL'};
    @{ $self->{$param} } = () if $overwrite;
    @{ $self->{$param} } = () unless exists $self->{$param};
    my @values = ref $value ? @{$value} : ( $value );
    for my $value ( @values ) {
        next
          if $value eq ''
          and $self->{'.globals'}->{'NO_UNDEF_PARAMS'};
        $value =~ tr/\000//d if $self->{'.globals'}->{'NO_NULL'};
        push @{ $self->{$param} }, $value;
        unless ( $self->{'.fieldnames'}->{$param} ) {
            push @{ $self->{'.parameters'} }, $param;
            $self->{'.fieldnames'}->{$param}++;
        }
    }
    return scalar @values;    # for compatibility with CGI.pm request.t
}

sub _parse_keywordlist {
    my ( $self, $data ) = @_;
    return () unless defined $data;
    $data = $self->url_decode( $data );
    $data =~ tr/\000//d if $self->{'.globals'}->{'NO_NULL'};
    my @keywords = split /\s+/, $data;
    return @keywords;
}

sub _parse_multipart {
    my $self = shift;

    # TODO: See 14838. We /could/ have a heuristic here for the case
    # where no boundary is supplied.

    my ( $boundary ) = $ENV{'CONTENT_TYPE'} =~ /boundary=\"?([^\";,]+)\"?/;
    unless ( $boundary ) {
        $self->cgi_error( '400 No boundary supplied for multipart/form-data' );
        return 0;
    }

    # BUG: IE 3.01 on the Macintosh uses just the boundary, forgetting the --
    $boundary = '--' . $boundary
      unless exists $ENV{'HTTP_USER_AGENT'}
      && $ENV{'HTTP_USER_AGENT'} =~ m/MSIE\s+3\.0[12];\s*Mac/i;

    $boundary = quotemeta $boundary;
    my $got_data = 0;
    my $data     = '';
    my $length   = $ENV{'CONTENT_LENGTH'} || 0;
    my $CRLF     = $self->crlf;

    READ:

    while ( $got_data < $length ) {
        last READ unless _internal_read( $self, my $buffer );
        $data .= $buffer;
        $got_data += length $buffer;

        BOUNDARY:

        while ( $data =~ m/^$boundary$CRLF/ ) {
            ## TAB and high ascii chars are definitivelly allowed in headers.
            ## Not accepting them in the following regex prevents the upload of
            ## files with filenames like "España.txt".
            # next READ unless $data =~ m/^([\040-\176$CRLF]+?$CRLF$CRLF)/o;
            next READ
              unless $data =~ m/^([\x20-\x7E\x80-\xFF\x09$CRLF]+?$CRLF$CRLF)/o;
            my $header = $1;
            ( my $unfold = $1 ) =~ s/$CRLF\s+/ /og;
            my ( $param ) = $unfold =~ m/form-data;\s+name="?([^\";]*)"?/;
            my ( $filename )
              = $unfold =~ m/name="?\Q$param\E"?;\s+filename="?([^\"]*)"?/;
            if ( defined $filename ) {
                my ( $mime ) = $unfold =~ m/Content-Type:\s+([-\w\/]+)/io;
                $data =~ s/^\Q$header\E//;
                ( $got_data, $data, my $fh, my $size )
                  = $self->_save_tmpfile( $boundary, $filename, $got_data,
                    $data );
                $self->_add_param( $param, $filename );
                $self->{'.upload_fields'}->{$param} = $filename;
                $self->{'.filehandles'}->{$filename} = $fh if $fh;
                $self->{'.tmpfiles'}->{$filename}
                  = { 'size' => $size, 'mime' => $mime }
                  if $size;
                next BOUNDARY;
            }
            next READ
              unless $data =~ s/^\Q$header\E(.*?)$CRLF(?=$boundary)//s;
            $self->_add_param( $param, $1 );
        }
        unless ( $data =~ m/^$boundary/ ) {
            ## In a perfect world, $data should always begin with $boundary.
            ## But sometimes, IE5 prepends garbage boundaries into POST(ed) data.
            ## Then, $data does not start with $boundary and the previous block
            ## never gets executed. The following fix attempts to remove those
            ## extra boundaries from readed $data and restart boundary parsing.
            ## Note about performance: with well formed data, previous check is
            ## executed (generally) only once, when $data value is "$boundary--"
            ## at end of parsing.
            goto BOUNDARY if ( $data =~ s/.*?$CRLF(?=$boundary$CRLF)//s );
        }
    }
    return $got_data;
}

sub _save_tmpfile {
    my ( $self, $boundary, $filename, $got_data, $data ) = @_;
    my $fh;
    my $CRLF      = $self->crlf;
    my $length    = $ENV{'CONTENT_LENGTH'} || 0;
    my $file_size = 0;
    if ( $self->{'.globals'}->{'DISABLE_UPLOADS'} ) {
        $self->cgi_error( "405 Not Allowed - File uploads are disabled" );
    }
    elsif ( $filename ) {
        eval { require IO::File };
        $self->cgi_error( "500 IO::File is not available $@" ) if $@;
        $fh = new_tmpfile IO::File;
        $self->cgi_error( "500 IO::File can't create new temp_file" )
          unless $fh;
    }

    # read in data until closing boundary found. buffer to catch split boundary
    # we do this regardless of whether we save the file or not to read the file
    # data from STDIN. if either uploads are disabled or no file has been sent
    # $fh will be undef so only do file stuff if $fh is true using $fh && syntax
    $fh && binmode $fh;
    while ( $got_data < $length ) {

        my $buffer = $data;
        last unless _internal_read( $self, $data );

        # fixed hanging bug if browser terminates upload part way through
        # thanks to Brandon Black
        unless ( $data ) {
            $self->cgi_error(
                '400 Malformed multipart, no terminating boundary' );
            undef $fh;
            return $got_data;
        }

        $got_data += length $data;
        if ( "$buffer$data" =~ m/$boundary/ ) {
            $data = $buffer . $data;
            last;
        }

        # we do not have partial boundary so print to file if valid $fh
        $fh && print $fh $buffer;
        $file_size += length $buffer;
    }
    $data =~ s/^(.*?)$CRLF(?=$boundary)//s;
    $fh && print $fh $1;    # print remainder of file if valid $fh
    $file_size += length $1;
    return $got_data, $data, $fh, $file_size;
}

# Define the CRLF sequence.  You can't use a simple "\r\n" because of system
# specific 'features'. On EBCDIC systems "\t" ne "\011" as the don't use ASCII
sub crlf {
    my ( $self, $CRLF ) = @_;
    $self->{'.crlf'} = $CRLF if $CRLF;    # allow value to be set manually
    unless ( $self->{'.crlf'} ) {
        my $OS = $^O
          || do { require Config; $Config::Config{'osname'} };
        $self->{'.crlf'}
          = ( $OS =~ m/VMS/i ) ? "\n"
          : ( "\t" ne "\011" ) ? "\r\n"
          :                      "\015\012";
    }
    return $self->{'.crlf'};
}

################ The Core Methods ################

sub param {
    my ( $self, $param, @p ) = @_;
    unless ( defined $param ) {    # return list of all params
        my @params = $self->{'.parameters'} ? @{ $self->{'.parameters'} } : ();
        return @params;
    }
    unless ( @p ) {                # return values for $param
        return () unless exists $self->{$param};
        return wantarray ? @{ $self->{$param} } : $self->{$param}->[0];
    }
    if ( $param =~ m/^-name$/i and @p == 1 ) {
        return () unless exists $self->{ $p[0] };
        return wantarray ? @{ $self->{ $p[0] } } : $self->{ $p[0] }->[0];
    }

    # set values using -name=>'foo',-value=>'bar' syntax.
    # also allows for $q->param( 'foo', 'some', 'new', 'values' ) syntax
    ( $param, undef, @p ) = @p
      if $param =~ m/^-name$/i;    # undef represents -value token
    $self->_add_param( $param, ( ref $p[0] eq 'ARRAY' ? $p[0] : [@p] ),
        'overwrite' );
    return wantarray ? @{ $self->{$param} } : $self->{$param}->[0];
}

#1;

###############   The following methods only loaded on demand   ###############
###############  Move commonly used methods above the __DATA__  ###############
############### token if you are into recreational optimization ###############
###############  You can not use Selfloader and the __DATA__    ###############
###############   token under mod_perl, so comment token out    ###############

#__DATA__

# a new method that provides access to a new internal routine. Useage:
# $q->add_param( $param, $value, $overwrite )
# $param must be a plain scalar
# $value may be either a scalar or an array ref
# if $overwrite is a true value $param will be overwritten with new values.
sub add_param {
    _add_param( @_ );
}

sub param_fetch {
    my ( $self, $param, @p ) = @_;
    $param = ( defined $param and $param =~ m/^-name$/i ) ? $p[0] : $param;
    return undef unless defined $param;
    $self->_add_param( $param, [] ) unless exists $self->{$param};
    return $self->{$param};
}

# Return a parameter in the QUERY_STRING, regardless of whether a POST or GET
sub url_param {
    my ( $self, $param ) = @_;
    return () unless $ENV{'QUERY_STRING'};
    $self->{'.url_param'} = {};
    bless $self->{'.url_param'}, 'CGI::Simple';
    $self->{'.url_param'}->_parse_params( $ENV{'QUERY_STRING'} );
    return $self->{'.url_param'}->param( $param );
}

sub keywords {
    my ( $self, @values ) = @_;
    $self->{'keywords'} = ref $values[0] eq 'ARRAY' ? $values[0] : [@values]
      if @values;
    my @result = defined( $self->{'keywords'} ) ? @{ $self->{'keywords'} } : ();
    return @result;
}

sub Vars {
    my $self = shift;
    $self->{'.sep'} = shift || $self->{'.sep'} || "\0";
    my ( %hash, %tied );
    for my $param ( $self->param ) {
        $hash{$param} = join $self->{'.sep'}, $self->param( $param );
    }
    tie %tied, "CGI::Simple", $self;
    return wantarray ? %hash : \%tied;
}

sub TIEHASH { $_[1] ? $_[1] : new $_[0] }

sub STORE {
    my ( $q, $p, $v ) = @_;
    $q->param( $p, split $q->{'.sep'}, $v );
}

sub FETCH {
    my ( $q, $p ) = @_;
    ref $q->{$p} eq "ARRAY" ? join $q->{'.sep'}, @{ $q->{$p} } : $q->{$p};
}
sub FIRSTKEY { my $a = scalar keys %{ $_[0] }; each %{ $_[0] } }
sub NEXTKEY { each %{ $_[0] } }
sub EXISTS  { exists $_[0]->{ $_[1] } }
sub DELETE  { $_[0]->delete( $_[1] ) }
sub CLEAR   { %{ $_[0] } = () }

sub append {
    my ( $self, $param, @p ) = @_;
    return () unless defined $param;

    # set values using $q->append(-name=>'foo',-value=>'bar') syntax
    # also allows for $q->append( 'foo', 'some', 'new', 'values' ) syntax
    ( $param, undef, @p ) = @p
      if $param =~ m/^-name$/i;    # undef represents -value token
    $self->_add_param( $param,
        ( ( defined $p[0] and ref $p[0] ) ? $p[0] : [@p] ) );
    return $self->param( $param );
}

sub delete {
    my ( $self, $param ) = @_;
    return () unless defined $param;
    $param
      = $param =~ m/^-name$/i
      ? shift
      : $param;                    # allow delete(-name=>'foo') syntax
    return undef unless defined $self->{$param};
    delete $self->{$param};
    delete $self->{'.fieldnames'}->{$param};
    $self->{'.parameters'}
      = [ grep { $_ ne $param } @{ $self->{'.parameters'} } ];
}

sub Delete { CGI::Simple::delete( @_ ) }    # for method style interface

sub delete_all {
    my $self = shift;
    undef %{$self};
    $self->_store_globals;
}

sub Delete_all { $_[0]->delete_all }        # as used by CGI.pm

sub upload {
    my ( $self, $filename, $writefile ) = @_;
    unless ( $filename ) {
        $self->cgi_error( "No filename submitted for upload to $writefile" )
          if $writefile;
        return $self->{'.filehandles'}
          ? keys %{ $self->{'.filehandles'} }
          : ();
    }
    unless ( $ENV{'CONTENT_TYPE'} =~ m|^multipart/form-data|i ) {
        $self->cgi_error(
            'Oops! File uploads only work if you specify ENCTYPE="multipart/form-data" in your <FORM> tag'
        );
        return undef;
    }
    my $fh = $self->{'.filehandles'}->{$filename};

    # allow use of upload fieldname to get filehandle
    # this has limitation that in the event of duplicate
    # upload field names there can only be one filehandle
    # which will point to the last upload file
    # access by filename does not suffer from this issue.
    $fh = $self->{'.filehandles'}->{ $self->{'.upload_fields'}->{$filename} }
      if !$fh and defined $self->{'.upload_fields'}->{$filename};

    if ( $fh ) {
        seek $fh, 0, 0;    # get ready for reading
        return $fh unless $writefile;
        my $buffer;
        unless ( open OUT, ">$writefile" ) {
            $self->cgi_error( "500 Can't write to $writefile: $!\n" );
            return undef;
        }
        binmode OUT;
        binmode $fh;
        print OUT $buffer while read( $fh, $buffer, 4096 );
        close OUT;
        $self->{'.filehandles'}->{$filename} = undef;
        undef $fh;
        return 1;
    }
    else {
        $self->cgi_error(
            "No filehandle for '$filename'. Are uploads enabled (\$DISABLE_UPLOADS = 0)? Is \$POST_MAX big enough?"
        );
        return undef;
    }
}

sub upload_fieldnames {
    my ( $self ) = @_;
    return wantarray
      ? ( keys %{ $self->{'.upload_fields'} } )
      : [ keys %{ $self->{'.upload_fields'} } ];
}

# return the file size of an uploaded file
sub upload_info {
    my ( $self, $filename, $info ) = @_;
    unless ( $ENV{'CONTENT_TYPE'} =~ m|^multipart/form-data|i ) {
        $self->cgi_error(
            'Oops! File uploads only work if you specify ENCTYPE="multipart/form-data" in your <FORM> tag'
        );
        return undef;
    }
    return keys %{ $self->{'.tmpfiles'} } unless $filename;
    return $self->{'.tmpfiles'}->{$filename}->{'mime'}
      if $info =~ /mime/i;
    return $self->{'.tmpfiles'}->{$filename}->{'size'};
}

sub uploadInfo { &upload_info }    # alias for CGI.pm compatibility

# return all params/values in object as a query string suitable for 'GET'
sub query_string {
    my $self = shift;
    my @pairs;
    for my $param ( $self->param ) {
        for my $value ( $self->param( $param ) ) {
            next unless defined $value;
            push @pairs,
              $self->url_encode( $param ) . '=' . $self->url_encode( $value );
        }
    }
    return join $self->{'.globals'}->{'USE_PARAM_SEMICOLONS'} ? ';' : '&',
      @pairs;
}

# new method that will add QUERY_STRING data to our CGI::Simple object
# if the REQUEST_METHOD was 'POST'
sub parse_query_string {
    my $self = shift;
    $self->_parse_params( $ENV{'QUERY_STRING'} )
      if defined $ENV{'QUERY_STRING'}
      and $ENV{'REQUEST_METHOD'} eq 'POST';
}

################   Save and Restore params from file    ###############

sub _init_from_file {
    my ( $self, $fh ) = @_;
    local $/ = "\n";
    while ( my $pair = <$fh> ) {
        chomp $pair;
        return if $pair eq '=';
        $self->_parse_params( $pair );
    }
}

sub save {
    my ( $self, $fh ) = @_;
    local ( $,, $\ ) = ( '', '' );
    unless ( $fh and fileno $fh ) {
        $self->cgi_error( 'Invalid filehandle' );
        return undef;
    }
    for my $param ( $self->param ) {
        for my $value ( $self->param( $param ) ) {
            ;
            print $fh $self->url_encode( $param ), '=',
              $self->url_encode( $value ), "\n";
        }
    }
    print $fh "=\n";
}

sub save_parameters { save( @_ ) }    # CGI.pm alias for save

################ Miscellaneous Methods ################

sub parse_keywordlist { _parse_keywordlist( @_ ) }    # CGI.pm compatibility

sub escapeHTML {
    my ( $self, $escape, $newlinestoo ) = @_;
    require CGI::Simple::Util;
    $escape = CGI::Simple::Util::escapeHTML( $escape );
    $escape =~ s/([\012\015])/'&#'.(ord $1).';'/eg if $newlinestoo;
    return $escape;
}

sub unescapeHTML {
    require CGI::Simple::Util;
    return CGI::Simple::Util::unescapeHTML( $_[1] );
}

sub put { my $self = shift; $self->print( @_ ) }    # send output to browser

sub print {
    shift;
    CORE::print( @_ );
}    # print to standard output (for overriding in mod_perl)

################# Cookie Methods ################

sub cookie {
    my ( $self, @params ) = @_;
    require CGI::Simple::Cookie;
    require CGI::Simple::Util;
    my ( $name, $value, $path, $domain, $secure, $expires )
      = CGI::Simple::Util::rearrange(
        [
            'NAME', [ 'VALUE', 'VALUES' ], 'PATH', 'DOMAIN', 'SECURE', 'EXPIRES'
        ],
        @params
      );

    # retrieve the value of the cookie, if no value is supplied
    unless ( defined( $value ) ) {
        $self->{'.cookies'} = CGI::Simple::Cookie->fetch
          unless $self->{'.cookies'};
        return () unless $self->{'.cookies'};

        # if no name is supplied, then retrieve the names of all our cookies.
        return keys %{ $self->{'.cookies'} } unless $name;

        # return the value of the cookie
        return
          exists $self->{'.cookies'}->{$name}
          ? $self->{'.cookies'}->{$name}->value
          : ();
    }

    # If we get here, we're creating a new cookie
    return undef unless $name;    # this is an error
    @params = ();
    push @params, '-name'    => $name;
    push @params, '-value'   => $value;
    push @params, '-domain'  => $domain if $domain;
    push @params, '-path'    => $path if $path;
    push @params, '-expires' => $expires if $expires;
    push @params, '-secure'  => $secure if $secure;
    return CGI::Simple::Cookie->new( @params );
}

sub raw_cookie {
    my ( $self, $key ) = @_;
    if ( defined $key ) {
        unless ( $self->{'.raw_cookies'} ) {
            require CGI::Simple::Cookie;
            $self->{'.raw_cookies'} = CGI::Simple::Cookie->raw_fetch;
        }
        return $self->{'.raw_cookies'}->{$key} || ();
    }
    return $ENV{'HTTP_COOKIE'} || $ENV{'COOKIE'} || '';
}

################# Header Methods ################

sub header {
    my ( $self, @params ) = @_;
    require CGI::Simple::Util;
    my @header;
    return undef
      if $self->{'.header_printed'}++
      and $self->{'.globals'}->{'HEADERS_ONCE'};
    my (
        $type, $status,  $cookie,     $target, $expires,
        $nph,  $charset, $attachment, $p3p,    @other
      )
      = CGI::Simple::Util::rearrange(
        [
            [ 'TYPE',   'CONTENT_TYPE', 'CONTENT-TYPE' ], 'STATUS',
            [ 'COOKIE', 'COOKIES',      'SET-COOKIE' ],   'TARGET',
            'EXPIRES', 'NPH',
            'CHARSET', 'ATTACHMENT',
            'P3P'
        ],
        @params
      );
    $nph ||= $self->{'.globals'}->{'NPH'};
    $charset = $self->charset( $charset )
      ;    # get charset (and set new charset if supplied)
     # rearrange() was designed for the HTML portion, so we need to fix it up a little.

    for ( @other ) {

        # Don't use \s because of perl bug 21951
        next
          unless my ( $header, $value ) = /([^ \r\n\t=]+)=\"?(.+?)\"?$/;
        ( $_ = $header )
          =~ s/^(\w)(.*)/"\u$1\L$2" . ': '.$self->unescapeHTML($value)/e;
    }
    $type ||= 'text/html' unless defined $type;
    $type .= "; charset=$charset"
      if $type
      and $type =~ m!^text/!
      and $type !~ /\bcharset\b/;
    my $protocol = $ENV{SERVER_PROTOCOL} || 'HTTP/1.0';
    push @header, $protocol . ' ' . ( $status || '200 OK' ) if $nph;
    push @header, "Server: " . server_software() if $nph;
    push @header, "Status: $status"              if $status;
    push @header, "Window-Target: $target"       if $target;

    if ( $p3p ) {
        $p3p = join ' ', @$p3p if ref( $p3p ) eq 'ARRAY';
        push( @header, qq(P3P: policyref="/w3c/p3p.xml", CP="$p3p") );
    }

    # push all the cookies -- there may be several
    if ( $cookie ) {
        my @cookie = ref $cookie eq 'ARRAY' ? @{$cookie} : $cookie;
        for my $cookie ( @cookie ) {
            my $cs
              = ref $cookie eq 'CGI::Simple::Cookie'
              ? $cookie->as_string
              : $cookie;
            push @header, "Set-Cookie: $cs" if $cs;
        }
    }

    # if the user indicates an expiration time, then we need both an Expires
    # and a Date header (so that the browser is using OUR clock)
    $expires = 'now'
      if $self->no_cache;    # encourage no caching via expires now
    push @header, "Expires: " . CGI::Simple::Util::expires( $expires, 'http' )
      if $expires;
    push @header, "Date: " . CGI::Simple::Util::expires( 0, 'http' )
      if defined $expires || $cookie || $nph;
    push @header, "Pragma: no-cache" if $self->cache or $self->no_cache;
    push @header, "Content-Disposition: attachment; filename=\"$attachment\""
      if $attachment;
    push @header, @other;
    push @header, "Content-Type: $type" if $type;
    my $CRLF = $self->crlf;
    my $header = join $CRLF, @header;
    $header .= $CRLF . $CRLF;    # add the statutory two CRLFs

    if ( $self->{'.mod_perl'} and not $nph ) {
        my $r = $self->_mod_perl_request();
        $r->send_cgi_header( $header );
        return '';
    }
    return $header;
}

# Control whether header() will produce the no-cache Pragma directive.
sub cache {
    my ( $self, $value ) = @_;
    $self->{'.cache'} = $value if defined $value;
    return $self->{'.cache'};
}

# Control whether header() will produce expires now + the no-cache Pragma.
sub no_cache {
    my ( $self, $value ) = @_;
    $self->{'.no_cache'} = $value if defined $value;
    return $self->{'.no_cache'};
}

sub redirect {
    my ( $self, @params ) = @_;
    require CGI::Simple::Util;
    my ( $url, $target, $cookie, $nph, @other ) = CGI::Simple::Util::rearrange(
        [
            [ 'LOCATION', 'URI',       'URL' ], 'TARGET',
            [ 'COOKIE',   'COOKIES' ], 'NPH'
        ],
        @params
    );
    $url ||= $self->self_url;
    my @o;
    for ( @other ) { tr/\"//d; push @o, split "=", $_, 2; }
    unshift @o,
      '-Status'   => '302 Moved',
      '-Location' => $url,
      '-nph'      => $nph;
    unshift @o, '-Target' => $target if $target;
    unshift @o, '-Cookie' => $cookie if $cookie;
    unshift @o, '-Type'   => '';
    my @unescaped;
    unshift( @unescaped, '-Cookie' => $cookie ) if $cookie;
    return $self->header( ( map { $self->unescapeHTML( $_ ) } @o ),
        @unescaped );
}

################# Server Push Methods #################
# Return a Content-Type: style header for server-push
# This has to be NPH, and it is advisable to set $| = 1
# Credit to Ed Jordan <ed@fidalgo.net> and
# Andrew Benham <adsb@bigfoot.com> for this section

sub multipart_init {
    my ( $self, @p ) = @_;
    use CGI::Simple::Util qw(rearrange);
    my ( $boundary, @other ) = rearrange( ['BOUNDARY'], @p );
    $boundary = $boundary || '------- =_aaaaaaaaaa0';
    my $CRLF = $self->crlf;    # get CRLF sequence
    my $warning
      = "WARNING: YOUR BROWSER DOESN'T SUPPORT THIS SERVER-PUSH TECHNOLOGY.";
    $self->{'.separator'}       = "$CRLF--$boundary$CRLF";
    $self->{'.final_separator'} = "$CRLF--$boundary--$CRLF$warning$CRLF";
    my $type = 'multipart/x-mixed-replace;boundary="' . $boundary . '"';
    return $self->header(
        -nph  => 1,
        -type => $type,
        map { split "=", $_, 2 } @other
      )
      . $warning
      . $self->multipart_end;
}

sub multipart_start {
    my ( $self, @p ) = @_;
    use CGI::Simple::Util qw(rearrange);
    my ( $type, @other ) = rearrange( ['TYPE'], @p );
    foreach ( @other ) {    # fix return from rearange
        next unless my ( $header, $value ) = /([^\s=]+)=\"?(.+?)\"?$/;
        $_ = ucfirst( lc $header ) . ': ' . unescapeHTML( 1, $value );
    }
    $type = $type || 'text/html';
    my @header = ( "Content-Type: $type" );
    push @header, @other;
    my $CRLF = $self->crlf;    # get CRLF sequence
    return ( join $CRLF, @header ) . $CRLF . $CRLF;
}

sub multipart_end { return $_[0]->{'.separator'} }

sub multipart_final { return $_[0]->{'.final_separator'} }

################# Debugging Methods ################

sub read_from_cmdline {
    my @words;
    if ( $_[0]->{'.globals'}->{'DEBUG'} == 1 and @ARGV ) {
        @words = @ARGV;
    }
    elsif ( $_[0]->{'.globals'}->{'DEBUG'} == 2 ) {
        require "shellwords.pl";
        print "(offline mode: enter name=value pairs on standard input)\n";
        chomp( my @lines = <STDIN> );
        @words = &shellwords( join " ", @lines );
    }
    else {
        return '';
    }
    @words = map { s/\\=/%3D/g; s/\\&/%26/g; $_ } @words;
    return "@words" =~ m/=/ ? join '&', @words : join '+', @words;
}

sub Dump {
    require Data::Dumper;    # short and sweet way of doing it
    ( my $dump = Data::Dumper::Dumper( @_ ) )
      =~ tr/\000/0/;         # remove null bytes cgi-lib.pl
    return '<pre>' . escapeHTML( 1, $dump ) . '</pre>';
}

sub as_string { Dump( @_ ) }    # CGI.pm alias for Dump()

sub cgi_error {
    my ( $self, $err ) = @_;
    if ( $err ) {
        $self->{'.cgi_error'} = $err;
            $self->{'.globals'}->{'FATAL'} == 1 ? croak $err
          : $self->{'.globals'}->{'FATAL'} == 0 ? carp $err
          :                                       return $err;
    }
    return $self->{'.cgi_error'};
}

################# cgi-lib.pl Compatibility Methods #################
# Lightly GOLFED but the original functionality remains. You can call
# them using either: # $q->MethodName or CGI::Simple::MethodName

sub _shift_if_ref { shift if ref $_[0] eq 'CGI::Simple' }

sub ReadParse {
    my $q = &_shift_if_ref || new CGI::Simple;
    my $pkg = caller();
    no strict 'refs';
    *in
      = @_
      ? $_[0]
      : *{"${pkg}::in"};    # set *in to passed glob or export *in
    %in = $q->Vars;
    $in{'CGI'} = $q;
    return scalar %in;
}

sub SplitParam {
    &_shift_if_ref;
    defined $_[0]
      && ( wantarray ? split "\0", $_[0] : ( split "\0", $_[0] )[0] );
}

sub MethGet { request_method() eq 'GET' }

sub MethPost { request_method() eq 'POST' }

sub MyBaseUrl {
    local $^W = 0;
    'http://'
      . server_name()
      . ( server_port() != 80 ? ':' . server_port() : '' )
      . script_name();
}

sub MyURL { MyBaseUrl() }

sub MyFullUrl {
    local $^W = 0;
    MyBaseUrl()
      . $ENV{'PATH_INFO'}
      . ( $ENV{'QUERY_STRING'} ? "?$ENV{'QUERY_STRING'}" : '' );
}

sub PrintHeader {
    ref $_[0] ? $_[0]->header() : "Content-Type: text/html\n\n";
}

sub HtmlTop {
    &_shift_if_ref;
    "<html>\n<head>\n<title>$_[0]</title>\n</head>\n<body>\n<h1>$_[0]</h1>\n";
}

sub HtmlBot { "</body>\n</html>\n" }

sub PrintVariables { &_shift_if_ref; &Dump }

sub PrintEnv { &Dump( \%ENV ) }

sub CgiDie { CgiError( @_ ); die @_ }

sub CgiError {
    &_shift_if_ref;
    @_
      = @_
      ? @_
      : ( "Error: script " . MyFullUrl() . " encountered fatal error\n" );
    print PrintHeader(), HtmlTop( shift ), ( map { "<p>$_</p>\n" } @_ ),
      HtmlBot();
}

################ Accessor Methods ################

sub version { $VERSION }

sub nph {
    $_[0]->{'.globals'}->{'NPH'} = $_[1] if defined $_[1];
    return $_[0]->{'.globals'}->{'NPH'};
}

sub all_parameters { $_[0]->param }

sub charset {
    require CGI::Simple::Util;
    $CGI::Simple::Util::UTIL->charset( $_[1] );
}

sub globals {
    my ( $self, $global, $value ) = @_;
    return keys %{ $self->{'.globals'} } unless $global;
    $self->{'.globals'}->{$global} = $value if defined $value;
    return $self->{'.globals'}->{$global};
}

sub auth_type         { $ENV{'AUTH_TYPE'} }
sub content_length    { $ENV{'CONTENT_LENGTH'} }
sub content_type      { $ENV{'CONTENT_TYPE'} }
sub document_root     { $ENV{'DOCUMENT_ROOT'} }
sub gateway_interface { $ENV{'GATEWAY_INTERFACE'} }
sub path_translated   { $ENV{'PATH_TRANSLATED'} }
sub referer           { $ENV{'HTTP_REFERER'} }
sub remote_addr       { $ENV{'REMOTE_ADDR'} || '127.0.0.1' }

sub remote_host {
    $ENV{'REMOTE_HOST'} || $ENV{'REMOTE_ADDR'} || 'localhost';
}

sub remote_ident   { $ENV{'REMOTE_IDENT'} }
sub remote_user    { $ENV{'REMOTE_USER'} }
sub request_method { $ENV{'REQUEST_METHOD'} }
sub script_name    { $ENV{'SCRIPT_NAME'} || $0 || '' }
sub server_name     { $ENV{'SERVER_NAME'}     || 'localhost' }
sub server_port     { $ENV{'SERVER_PORT'}     || 80 }
sub server_protocol { $ENV{'SERVER_PROTOCOL'} || 'HTTP/1.0' }
sub server_software { $ENV{'SERVER_SOFTWARE'} || 'cmdline' }

sub user_name {
    $ENV{'HTTP_FROM'} || $ENV{'REMOTE_IDENT'} || $ENV{'REMOTE_USER'};
}

sub user_agent {
    my ( $self, $match ) = @_;
    return $match
      ? $ENV{'HTTP_USER_AGENT'} =~ /\Q$match\E/i
      : $ENV{'HTTP_USER_AGENT'};
}

sub virtual_host {
    my $vh = $ENV{'HTTP_HOST'} || $ENV{'SERVER_NAME'};
    $vh =~ s/:\d+$//;    # get rid of port number
    return $vh;
}

sub path_info {
    my ( $self, $info ) = @_;
    if ( defined $info ) {
        $info = "/$info" if $info !~ m|^/|;
        $self->{'.path_info'} = $info;
    }
    elsif ( !defined( $self->{'.path_info'} ) ) {
        $self->{'.path_info'}
          = defined( $ENV{'PATH_INFO'} ) ? $ENV{'PATH_INFO'} : '';

        # hack to fix broken path info in IIS source CGI.pm
        $self->{'.path_info'} =~ s/^\Q$ENV{'SCRIPT_NAME'}\E//
          if defined( $ENV{'SERVER_SOFTWARE'} )
          && $ENV{'SERVER_SOFTWARE'} =~ /IIS/;
    }
    return $self->{'.path_info'};
}

sub accept {
    my ( $self, $search ) = @_;
    my %prefs;
    for my $accept ( split ',', $ENV{'HTTP_ACCEPT'} ) {
        ( my $pref ) = $accept =~ m|q=([\d\.]+)|;
        ( my $type ) = $accept =~ m|(\S+/[^;]+)|;
        next unless $type;
        $prefs{$type} = $pref || 1;
    }
    return keys %prefs unless $search;
    return $prefs{$search} if $prefs{$search};

    # Didn't get it, so try pattern matching.
    for my $pref ( keys %prefs ) {
        next unless $pref =~ m/\*/;    # not a pattern match
        ( my $pat = $pref ) =~ s/([^\w*])/\\$1/g;    # escape meta characters
        $pat =~ s/\*/.*/g;                           # turn it into a pattern
        return $prefs{$pref} if $search =~ /$pat/;
    }
}

sub Accept { my $self = shift; $self->accept( @_ ) }

sub http {
    my ( $self, $parameter ) = @_;
    if ( defined $parameter ) {
        ( $parameter = uc $parameter ) =~ tr/-/_/;
        return $ENV{$parameter} if $parameter =~ m/^HTTP/;
        return $ENV{"HTTP_$parameter"} if $parameter;
    }
    return grep { /^HTTP/ } keys %ENV;
}

sub https {
    my ( $self, $parameter ) = @_;
    return $ENV{'HTTPS'} unless $parameter;
    ( $parameter = uc $parameter ) =~ tr/-/_/;
    return $ENV{$parameter} if $parameter =~ /^HTTPS/;
    return $ENV{"HTTPS_$parameter"};
}

sub protocol {
    local ( $^W ) = 0;
    my $self = shift;
    return 'https' if uc $ENV{'HTTPS'} eq 'ON';
    return 'https' if $self->server_port == 443;
    my ( $protocol, $version ) = split '/', $self->server_protocol;
    return lc $protocol;
}

sub url {
    my ( $self, @p ) = @_;
    use CGI::Simple::Util 'rearrange';
    my ( $relative, $absolute, $full, $path_info, $query, $base ) = rearrange(
        [
            'RELATIVE', 'ABSOLUTE', 'FULL',
            [ 'PATH',  'PATH_INFO' ],
            [ 'QUERY', 'QUERY_STRING' ], 'BASE'
        ],
        @p
    );
    my $url;
    $full++ if $base || !( $relative || $absolute );
    my $path        = $self->path_info;
    my $script_name = $self->script_name;
    if ( $full ) {
        my $protocol = $self->protocol();
        $url = "$protocol://";
        my $vh = $self->http( 'host' );
        if ( $vh ) {
            $url .= $vh;
        }
        else {
            $url .= server_name();
            my $port = $self->server_port;
            $url .= ":" . $port
              unless ( lc( $protocol ) eq 'http' && $port == 80 )
              or ( lc( $protocol ) eq 'https' && $port == 443 );
        }
        return $url if $base;
        $url .= $script_name;
    }
    elsif ( $relative ) {
        ( $url ) = $script_name =~ m!([^/]+)$!;
    }
    elsif ( $absolute ) {
        $url = $script_name;
    }
    $url .= $path if $path_info and defined $path;
    $url .= "?" . $self->query_string if $query and $self->query_string;
    $url = '' unless defined $url;
    $url =~ s/([^a-zA-Z0-9_.%;&?\/\\:+=~-])/uc sprintf("%%%02x",ord($1))/eg;
    return $url;
}

sub self_url {
    my ( $self, @params ) = @_;
    return $self->url(
        '-path_info' => 1,
        '-query'     => 1,
        '-full'      => 1,
        @params
    );
}

sub state { self_url( @_ ) }    # CGI.pm synonym routine

1;

=head1 NAME

CGI::Simple - A Simple totally OO CGI interface that is CGI.pm compliant

=head1 VERSION

This document describes CGI::Simple version 1.105.

=head1 SYNOPSIS

    use CGI::Simple;
    $CGI::Simple::POST_MAX = 1024;       # max upload via post default 100kB
    $CGI::Simple::DISABLE_UPLOADS = 0;   # enable uploads

    $q = new CGI::Simple;
    $q = new CGI::Simple( { 'foo'=>'1', 'bar'=>[2,3,4] } );
    $q = new CGI::Simple( 'foo=1&bar=2&bar=3&bar=4' );
    $q = new CGI::Simple( \*FILEHANDLE );

    $q->save( \*FILEHANDLE );   # save current object to a file as used by new

    @params = $q->param;        # return all param names as a list
    $value = $q->param('foo');  # return the first value supplied for 'foo'
    @values = $q->param('foo'); # return all values supplied for foo

    %fields   = $q->Vars;      # returns untied key value pair hash
    $hash_ref = $q->Vars;      # or as a hash ref
    %fields   = $q->Vars("|"); # packs multiple values with "|" rather than "\0";

    @keywords = $q->keywords;  # return all keywords as a list

    $q->param( 'foo', 'some', 'new', 'values' );      # set new 'foo' values
    $q->param( -name=>'foo', -value=>'bar' );
    $q->param( -name=>'foo', -value=>['bar','baz'] );

    $q->param( 'foo', 'some', 'new', 'values' );      # append values to 'foo'
    $q->append( -name=>'foo', -value=>'bar' );
    $q->append( -name=>'foo', -value=>['some', 'new', 'values'] );

    $q->delete('foo'); # delete param 'foo' and all its values
    $q->delete_all;    # delete everything

    <INPUT TYPE="file" NAME="upload_file" SIZE="42">

    $files    = $q->upload()                # number of files uploaded
    @files    = $q->upload();               # names of all uploaded files
    $filename = $q->param('upload_file')    # filename of uploaded file
    $mime     = $q->upload_info($filename,'mime'); # MIME type of uploaded file
    $size     = $q->upload_info($filename,'size'); # size of uploaded file

    my $fh = $q->upload($filename);         # get filehandle to read from
    while ( read( $fh, $buffer, 1024 ) ) { ... }

    # short and sweet upload
    $ok = $q->upload( $q->param('upload_file'), '/path/to/write/file.name' );
    print "Uploaded ".$q->param('upload_file')." and wrote it OK!" if $ok;

    $decoded    = $q->url_decode($encoded);
    $encoded    = $q->url_encode($unencoded);
    $escaped    = $q->escapeHTML('<>"&');
    $unescaped  = $q->unescapeHTML('&lt;&gt;&quot;&amp;');

    $qs = $q->query_string; # get all data in $q as a query string OK for GET

    $q->no_cache(1);        # set Pragma: no-cache + expires
    print $q->header();     # print a simple header
    # get a complex header
    $header = $q->header(   -type       => 'image/gif'
                            -nph        => 1,
                            -status     => '402 Payment required',
                            -expires    =>'+24h',
                            -cookie     => $cookie,
                            -charset    => 'utf-7',
                            -attachment => 'foo.gif',
                            -Cost       => '$2.00'
                        );
    # a p3p header (OK for redirect use as well)
    $header = $q->header( -p3p => 'policyref="http://somesite.com/P3P/PolicyReferences.xml' );

    @cookies = $q->cookie();        # get names of all available cookies
    $value   = $q->cookie('foo')    # get first value of cookie 'foo'
    @value   = $q->cookie('foo')    # get all values of cookie 'foo'
    # get a cookie formatted for header() method
    $cookie  = $q->cookie(  -name    => 'Password',
                            -values  => ['superuser','god','my dog woofie'],
                            -expires => '+3d',
                            -domain  => '.nowhere.com',
                            -path    => '/cgi-bin/database',
                            -secure  => 1
                         );
    print $q->header( -cookie=>$cookie );       # set cookie

    print $q->redirect('http://go.away.now');   # print a redirect header

    dienice( $q->cgi_error ) if $q->cgi_error;

=head1 DESCRIPTION

CGI::Simple provides a relatively lightweight drop in replacement for CGI.pm.
It shares an identical OO interface to CGI.pm for parameter parsing, file
upload, cookie handling and header generation. This module is entirely object
oriented, however a complete functional interface is available by using the
CGI::Simple::Standard module.

Essentially everything in CGI.pm that relates to the CGI (not HTML) side of
things is available. There are even a few new methods and additions to old
ones! If you are interested in what has gone on under the hood see the
Compatibility with CGI.pm section at the end.

In practical testing this module loads and runs about twice as fast as CGI.pm
depending on the precise task.

=head1 CALLING CGI::Simple ROUTINES USING THE OBJECT INTERFACE

Here is a very brief rundown on how you use the interface. Full details
follow.

=head2 First you need to initialize an object

Before you can call a CGI::Simple method you must create a CGI::Simple object.
You do that by using the module and then calling the new() constructor:

    use CGI::Simple;
    my $q = new CGI::Simple;

It is traditional to call your object $q for query or perhaps $cgi.

=head2 Next you call methods on that object

Once you have your object you can call methods on it using the -> arrow
syntax For example to get the names of all the parameters passed to your
script you would just write:

    @names = $q->param();

Many methods are sensitive to the context in which you call them. In the
example above the B<param()> method returns a list of all the parameter names
when called without any arguments.

When you call B<param('arg')> with a single argument it assumes you want
to get the value(s) associated with that argument (parameter). If you ask
for an array it gives you an array of all the values associated with it's
argument:

    @values = $q->param('foo');  # get all the values for 'foo'

whereas if you ask for a scalar like this:

    $value = $q->param('foo');   # get only the first value for 'foo'

then it returns only the first value (if more than one value for
'foo' exists).

Most CGI::Simple routines accept several arguments, sometimes as many as
10 optional ones!  To simplify this interface, all routines use a named
argument calling style that looks like this:

    print $q->header( -type=>'image/gif', -expires=>'+3d' );

Each argument name is preceded by a dash.  Neither case nor order
matters in the argument list.  -type, -Type, and -TYPE are all
acceptable.

Several routines are commonly called with just one argument.  In the
case of these routines you can provide the single argument without an
argument name.  B<header()> happens to be one of these routines.  In this
case, the single argument is the document type.

   print $q->header('text/html');

Sometimes methods expect a scalar, sometimes a reference to an
array, and sometimes a reference to a hash.  Often, you can pass any
type of argument and the routine will do whatever is most appropriate.
For example, the B<param()> method can be used to set a CGI parameter to a
single or a multi-valued value.  The two cases are shown below:

   $q->param(-name=>'veggie',-value=>'tomato');
   $q->param(-name=>'veggie',-value=>['tomato','tomahto','potato','potahto']);

=head1 CALLING CGI::Simple ROUTINES USING THE FUNCTION INTERFACE

For convenience a functional interface is provided by the
CGI::Simple::Standard module. This hides the OO details from you and allows
you to simply call methods. You may either use AUTOLOADING of methods or
import specific method sets into you namespace. Here are the first few
examples again using the function interface.

    use CGI::Simple::Standard qw(-autoload);
    @names  = param();
    @values = param('foo');
    $value  = param('foo');
    print header(-type=>'image/gif',-expires=>'+3d');
    print header('text/html');

Yes that's it. Not a $q-> in sight. You just use the module and select
how/which methods to load. You then just call the methods you want exactly
as before but without the $q-> notation.

When (if) you read the following docs and are using the functional interface
just pretend the $q-> is not there.

=head2 Selecting which methods to load

When you use the functional interface Perl needs to be able to find the
functions you call. The simplest way of doing this is to use autoloading as
shown above. When you use CGI::Simple::Standard with the '-autoload' pragma
it exports a single AUTOLOAD sub into you namespace. Every time you call a
non existent function AUTOLOAD is called and will load the required
function and install it in your namespace. Thus only the AUTOLOAD sub and
those functions you specifically call will be imported.

Alternatively CGI::Simple::Standard provides a range of function sets you can
import or you can just select exactly what you want. You do this using the
familiar

    use CGI::Simple::Standard qw( :func_set  some_func);

notation. This will import the ':func_set' function set and the specific
function 'some_func'.

=head2 To Autoload or not to Autoload, that is the question.

If you do not have a AUTOLOAD sub in you script it is generally best to use
the '-autoload' option. Under autoload you can use any method you want but
only import and compile those functions you actually use.

If you do not use autoload you must specify what functions to import. You can
only use functions that you have imported. For comvenience functions are
grouped into related sets. If you choose to import one or more ':func_set'
you may have potential namespace collisions so check out the docs to see
what gets imported. Using the ':all' tag is pretty slack but it is there
if you want. Full details of the function sets are provided in the
CGI::Simple::Standard docs

If you just want say the param and header methods just load these two.

    use CGI::Simple::Standard qw(param header);

=head2 Setting globals using the functional interface

Where you see global variables being set using the syntax:

    $CGI::Simple::DEBUG = 1;

You use exactly the same syntax when using CGI::Simple::Standard.

=cut

################ The Core Methods ################

=head1 THE CORE METHODS

=head2 new() Creating a new query object

The first step in using CGI::Simple is to create a new query object using
the B<new()> constructor:

     $q = new CGI::Simple;

This will parse the input (from both POST and GET methods) and store
it into an object called $q.

If you provide a file handle to the B<new()> method, it will read
parameters from the file (or STDIN, or whatever).

     open FH, "test.in" or die $!;
     $q = new CGI::Simple(\*FH);

     open $fh, "test.in" or die $!;
     $q = new CGI::Simple($fh);

The file should be a series of newline delimited TAG=VALUE pairs.
Conveniently, this type of file is created by the B<save()> method
(see below). Multiple records can be saved and restored.
IO::File objects work fine.

If you are using the function-oriented interface provided by
CGI::Simple::Standard and want to initialize from a file handle,
the way to do this is with B<restore_parameters()>.  This will (re)initialize
the default CGI::Simple object from the indicated file handle.

    restore_parameters(\*FH);

In fact for all intents and purposes B<restore_parameters()> is identical
to B<new()> Note that B<restore_parameters()> does not exist in
CGI::Simple itself so you can't use it.

You can also initialize the query object from an associative array
reference:

    $q = new CGI::Simple( { 'dinosaur' => 'barney',
                            'song'     => 'I love you',
                            'friends'  => [qw/Jessica George Nancy/] }
                        );

or from a properly formatted, URL-escaped query string:

    $q = new CGI::Simple( 'dinosaur=barney&color=purple' );

or from a previously existing CGI::Simple object (this generates an identical clone
including all global variable settings, etc that are stored in the object):

    $old_query = new CGI::Simple;
    $new_query = new CGI::Simple($old_query);

To create an empty query, initialize it from an empty string or hash:

    $empty_query = new CGI::Simple("");

       -or-

    $empty_query = new CGI::Simple({});

=head2 keywords() Fetching a list of keywords from a query

    @keywords = $q->keywords;

If the script was invoked as the result of an <ISINDEX> search, the
parsed keywords can be obtained as an array using the B<keywords()> method.

=head2 param() Fetching the names of all parameters passed to your script

    @names = $q->param;

If the script was invoked with a parameter list
(e.g. "name1=value1&name2=value2&name3=value3"), the B<param()> method
will return the parameter names as a list.  If the script was invoked
as an <ISINDEX> script and contains a string without ampersands
(e.g. "value1+value2+value3") , there will be a single parameter named
"keywords" containing the "+"-delimited keywords.

NOTE: The array of parameter names returned will
be in the same order as they were submitted by the browser.
Usually this order is the same as the order in which the
parameters are defined in the form (however, this isn't part
of the spec, and so isn't guaranteed).

=head2 param() Fetching the value or values of a simple named parameter

    @values = $q->param('foo');

          -or-

    $value = $q->param('foo');

Pass the B<param()> method a single argument to fetch the value of the
named parameter. If the parameter is multi-valued (e.g. from multiple
selections in a scrolling list), you can ask to receive an array.  Otherwise
the method will return a single value.

If a value is not given in the query string, as in the queries
"name1=&name2=" or "name1&name2", it will be returned by default
as an empty string. If you set the global variable:

    $CGI::Simple::NO_UNDEF_PARAMS = 1;

Then value-less parameters will be ignored, and will not exist in the
query object. If you try to access them via param you will get an undef
return value.

=head2 param() Setting the values of a named parameter

    $q->param('foo','an','array','of','values');

This sets the value for the named parameter 'foo' to an array of
values.  This is one way to change the value of a field.

B<param()> also recognizes a named parameter style of calling described
in more detail later:

    $q->param(-name=>'foo',-values=>['an','array','of','values']);

                  -or-

    $q->param(-name=>'foo',-value=>'the value');

=head2 param() Retrieving non-application/x-www-form-urlencoded data

If POSTed or PUTed data is not of type application/x-www-form-urlencoded or multipart/form-data, 
then the data will not be processed, but instead be returned as-is in a parameter named POSTDATA
or PUTDATA.  To retrieve it, use code like this:

    my $data = $q->param( 'POSTDATA' );

                  -or-

    my $data = $q->param( 'PUTDATA' );

(If you don't know what the preceding means, don't worry about it.  It only affects people trying
to use CGI::Simple for REST webservices)

=head2 add_param() Setting the values of a named parameter

You nay also use the new method B<add_param> to add parameters. This is an
alias to the _add_param() internal method that actually does all the work.
You can call it like this:

    $q->add_param('foo', 'new');
    $q->add_param('foo', [1,2,3,4,5]);
    $q->add_param( 'foo', 'bar', 'overwrite' );

The first argument is the parameter, the second the value or an array ref
of values and the optional third argument sets overwrite mode. If the third
argument is absent of false the values will be appended. If true the values
will overwrite any existing ones

=head2 append() Appending values to a named parameter

   $q->append(-name=>'foo',-values=>['yet','more','values']);

This adds a value or list of values to the named parameter.  The
values are appended to the end of the parameter if it already exists.
Otherwise the parameter is created.  Note that this method only
recognizes the named argument calling syntax.

=head2 import_names() Importing all parameters into a namespace.

This method was silly, non OO and has been deleted. You can get all the params
as a hash using B<Vars> or via all the other accessors.

=head2 delete() Deleting a parameter completely

    $q->delete('foo');

This completely clears a parameter. If you are using the function call
interface, use B<Delete()> instead to avoid conflicts with Perl's
built-in delete operator.

If you are using the function call interface, use B<Delete()> instead to
avoid conflicts with Perl's built-in delete operator.

=head2 delete_all() Deleting all parameters

    $q->delete_all();

This clears the CGI::Simple object completely. For CGI.pm compatibility
B<Delete_all()> is provided however there is no reason to use this in the
function call interface other than symmetry.

For CGI.pm compatibility B<Delete_all()> is provided as an alias for
B<delete_all> however there is no reason to use this, even in the
function call interface.

=head2 param_fetch() Direct access to the parameter list

This method is provided for CGI.pm compatibility only. It returns an
array ref to the values associated with a named param. It is deprecated.

=head2 Vars() Fetching the entire parameter list as a hash

    $params = $q->Vars;  # as a tied hash ref
    print $params->{'address'};
    @foo = split "\0", $params->{'foo'};

    %params = $q->Vars;  # as a plain hash
    print $params{'address'};
    @foo = split "\0", $params{'foo'};

    %params = $q->Vars(','); # specifying a different separator than "\0"
    @foo = split ',', $params{'foo'};

Many people want to fetch the entire parameter list as a hash in which
the keys are the names of the CGI parameters, and the values are the
parameters' values.  The B<Vars()> method does this.

Called in a scalar context, it returns the parameter list as a tied
hash reference. Because this hash ref is tied changing a key/value
changes the underlying CGI::Simple object.

Called in a list context, it returns the parameter list as an ordinary hash.
Changing this hash will not change the underlying CGI::Simple object

When using B<Vars()>, the thing you must watch out for are multi-valued CGI
parameters.  Because a hash cannot distinguish between scalar and
list context, multi-valued parameters will be returned as a packed
string, separated by the "\0" (null) character.  You must split this
packed string in order to get at the individual values.  This is the
convention introduced long ago by Steve Brenner in his cgi-lib.pl
module for Perl version 4.

You can change the character used to do the multiple value packing by passing
it to B<Vars()> as an argument as shown.

=head2 url_param() Access the QUERY_STRING regardless of 'GET' or 'POST'

The B<url_param()> method makes the QUERY_STRING data available regardless
of whether the REQUEST_METHOD was 'GET' or 'POST'. You can do anything
with B<url_param> that you can do with B<param()>, however the data set
is completely independent.

Technically what happens if you use this method is that the QUERY_STRING data
is parsed into a new CGI::Simple object which is stored within the current
object. B<url_param> then just calls B<param()> on this new object.

=head2 parse_query_string() Add QUERY_STRING data to 'POST' requests

When the REQUEST_METHOD is 'POST' the default behavior is to ignore
name/value pairs or keywords in the $ENV{'QUERY_STRING'}. You can override
this by calling B<parse_query_string()> which will add the QUERY_STRING data to
the data already in our CGI::Simple object if the REQUEST_METHOD was 'POST'

    $q = new CGI::Simple;
    $q->parse_query_string;  # add $ENV{'QUERY_STRING'} data to our $q object

If the REQUEST_METHOD was 'GET' then the QUERY_STRING will already be
stored in our object so B<parse_query_string> will be ignored.

This is a new method in CGI::Simple that is not available in CGI.pm

=head2 save() Saving the state of an object to file

    $q->save(\*FILEHANDLE)

This will write the current state of the form to the provided
filehandle.  You can read it back in by providing a filehandle
to the B<new()> method.

The format of the saved file is:

    NAME1=VALUE1
    NAME1=VALUE1'
    NAME2=VALUE2
    NAME3=VALUE3
    =

Both name and value are URL escaped.  Multi-valued CGI parameters are
represented as repeated names.  A session record is delimited by a
single = symbol.  You can write out multiple records and read them
back in with several calls to B<new()>.

    open FH, "test.in" or die $!;
    $q1 = new CGI::Simple(\*FH);  # get the first record
    $q2 = new CGI::Simple(\*FH);  # get the next record

Note: If you wish to use this method from the function-oriented (non-OO)
interface, the exported name for this method is B<save_parameters()>.
Also if you want to initialize from a file handle, the way to do this is
with B<restore_parameters()>.  This will (re)initialize
the default CGI::Simple object from the indicated file handle.

    restore_parameters(\*FH);

=cut

################ Uploading Files ###################

=head1 FILE UPLOADS

File uploads are easy with CGI::Simple. You use the B<upload()> method.
Assuming you have the following in your HTML:

    <FORM
     METHOD="POST"
     ACTION="http://somewhere.com/cgi-bin/script.cgi"
     ENCTYPE="multipart/form-data">
        <INPUT TYPE="file" NAME="upload_file1" SIZE="42">
        <INPUT TYPE="file" NAME="upload_file2" SIZE="42">
    </FORM>

Note that the ENCTYPE is "multipart/form-data". You must specify this or the
browser will default to "application/x-www-form-urlencoded" which will result
in no files being uploaded although on the surface things will appear OK.

When the user submits this form any supplied files will be spooled onto disk
and saved in temporary files. These files will be deleted when your script.cgi
exits so if you want to keep them you will need to proceed as follows.

=head2 upload() The key file upload method

The B<upload()> method is quite versatile. If you call B<upload()> without
any arguments it will return a list of uploaded files in list context and
the number of uploaded files in scalar context.

    $number_of_files = $q->upload;
    @list_of_files   = $q->upload;

Having established that you have uploaded files available you can get the
browser supplied filename using B<param()> like this:

    $filename1 = $q->param('upload_file1');

You can then get a filehandle to read from by calling B<upload()> and
supplying this filename as an argument. Warning: do not modify the
value you get from B<param()> in any way - you don't need to untaint it.

    $fh = $q->upload( $filename1 );

Now to save the file you would just do something like:

    $save_path = '/path/to/write/file.name';
    open FH, ">$save_path" or die "Oops $!\n";
    binmode FH;
    print FH $buffer while read( $fh, $buffer, 4096 );
    close FH;

By utilizing a new feature of the upload method this process can be
simplified to:

    $ok = $q->upload( $q->param('upload_file1'), '/path/to/write/file.name' );
    if ($ok) {
        print "Uploaded and wrote file OK!";
    } else {
        print $q->cgi_error();
    }

As you can see upload will accept an optional second argument and will write
the file to this file path. It will return 1 for success and undef if it
fails. If it fails you can get the error from B<cgi_error>

You can also use just the fieldname as an argument to upload ie:

    $fh = $q->upload( 'upload_field_name' );

    or

    $ok = $q->upload( 'upload_field_name', '/path/to/write/file.name' );

BUT there is a catch. If you have multiple upload fields, all called
'upload_field_name' then you will only get the last uploaded file from
these fields.

=head2 upload_info() Get the details about uploaded files

The B<upload_info()> method is a new method. Called without arguments it
returns the number of uploaded files in scalar context and the names of
those files in list context.

    $number_of_upload_files   = $q->upload_info();
    @filenames_of_all_uploads = $q->upload_info();

You can get the MIME type of an uploaded file like this:

    $mime = $q->upload_info( $filename1, 'mime' );

If you want to know how big a file is before you copy it you can get that
information from B<uploadInfo> which will return the file size in bytes.

    $file_size = $q->upload_info( $filename1, 'size' );

The size attribute is optional as this is the default value returned.

Note: The old CGI.pm B<uploadInfo()> method has been deleted.

=head2 $POST_MAX and $DISABLE_UPLOADS

CGI.pm has a default setting that allows infinite size file uploads by
default. In contrast file uploads are disabled by default in CGI::Simple
to discourage Denial of Service attacks. You must enable them before you
expect file uploads to work.

When file uploads are disabled the file name and file size details will
still be available from B<param()> and B<upload_info> respectively but
the upload filehandle returned by B<upload()> will be undefined - not
surprising as the underlying temp file will not exist either.

You can enable uploads using the '-upload' pragma. You do this by specifying
this in you use statement:

    use CGI::Simple qw(-upload);

Alternatively you can enable uploads via the $DISABLE_UPLOADS global like this:

    use CGI::Simple;
    $CGI::Simple::DISABLE_UPLOADS = 0;
    $q = new CGI::Simple;

If you wish to set $DISABLE_UPLOADS you must do this *after* the
use statement and *before* the new constructor call as shown above.

The maximum acceptable data via post is capped at 102_400kB rather than
infinity which is the CGI.pm default. This should be ample for most tasks
but you can set this to whatever you want using the $POST_MAX global.

    use CGI::Simple;
    $CGI::Simple::DISABLE_UPLOADS = 0;      # enable uploads
    $CGI::Simple::POST_MAX = 1_048_576;     # allow 1MB uploads
    $q = new CGI::Simple;

If you set to -1 infinite size uploads will be permitted, which is the CGI.pm
default.

    $CGI::Simple::POST_MAX = -1;            # infinite size upload

Alternatively you can specify all the CGI.pm default values which allow file
uploads of infinite size in one easy step by specifying the '-default' pragma
in your use statement.

    use CGI::Simple qw( -default ..... );

=head2 binmode() and Win32

If you are using CGI::Simple be sure to call B<binmode()> on any handle that
you create to write the uploaded file to disk. Calling B<binmode()> will do
no harm on other systems anyway.

=cut

################ Miscellaneous Methods ################

=head1 MISCELANEOUS METHODS

=head2 escapeHTML() Escaping HTML special characters

In HTML the < > " and & chars have special meaning and need to be
escaped to &lt; &gt; &quot; and &amp; respectively.

    $escaped = $q->escapeHTML( $string );

    $escaped = $q->escapeHTML( $string, 'new_lines_too' );

If the optional second argument is supplied then newlines will be escaped to.

=head2 unescapeHTML() Unescape HTML special characters

This performs the reverse of B<escapeHTML()>.

    $unescaped = $q->unescapeHTML( $HTML_escaped_string );

=head2 url_decode() Decode a URL encoded string

This method will correctly decode a url encoded string.

    $decoded = $q->url_decode( $encoded );

=head2 url_encode() URL encode a string

This method will correctly URL encode a string.

    $encoded = $q->url_encode( $string );

=head2 parse_keywordlist() Parse a supplied keyword list

    @keywords = $q->parse_keywordlist( $keyword_list );

This method returns a list of keywords, correctly URL escaped and split out
of the supplied string

=head2 put() Send output to browser

CGI.pm alias for print. $q->put('Hello World!') will print the usual

=head2 print() Send output to browser

CGI.pm alias for print. $q->print('Hello World!') will print the usual

=cut

################# Cookie Methods ################

=head1 HTTP COOKIES

Netscape browsers versions 1.1 and higher, and all versions of
Internet Explorer, support a so-called "cookie" designed to help
maintain state within a browser session.  CGI.pm has several methods
that support cookies.

A cookie is a name=value pair much like the named parameters in a CGI
query string.  CGI scripts create one or more cookies and send
them to the browser in the HTTP header.  The browser maintains a list
of cookies that belong to a particular Web server, and returns them
to the CGI script during subsequent interactions.

In addition to the required name=value pair, each cookie has several
optional attributes:

=over 4

=item 1. an expiration time

This is a time/date string (in a special GMT format) that indicates
when a cookie expires.  The cookie will be saved and returned to your
script until this expiration date is reached if the user exits
the browser and restarts it.  If an expiration date isn't specified, the cookie
will remain active until the user quits the browser.

=item 2. a domain

This is a partial or complete domain name for which the cookie is
valid.  The browser will return the cookie to any host that matches
the partial domain name.  For example, if you specify a domain name
of ".capricorn.com", then the browser will return the cookie to
Web servers running on any of the machines "www.capricorn.com",
"www2.capricorn.com", "feckless.capricorn.com", etc.  Domain names
must contain at least two periods to prevent attempts to match
on top level domains like ".edu".  If no domain is specified, then
the browser will only return the cookie to servers on the host the
cookie originated from.

=item 3. a path

If you provide a cookie path attribute, the browser will check it
against your script's URL before returning the cookie.  For example,
if you specify the path "/cgi-bin", then the cookie will be returned
to each of the scripts "/cgi-bin/tally.pl", "/cgi-bin/order.pl",
and "/cgi-bin/customer_service/complain.pl", but not to the script
"/cgi-private/site_admin.pl".  By default, path is set to "/", which
causes the cookie to be sent to any CGI script on your site.

=item 4. a "secure" flag

If the "secure" attribute is set, the cookie will only be sent to your
script if the CGI request is occurring on a secure channel, such as SSL.

=back

=head2 cookie() A simple access method to cookies

The interface to HTTP cookies is the B<cookie()> method:

    $cookie = $q->cookie( -name      => 'sessionID',
                          -value     => 'xyzzy',
                          -expires   => '+1h',
                          -path      => '/cgi-bin/database',
                          -domain    => '.capricorn.org',
                          -secure    => 1
                         );
    print $q->header(-cookie=>$cookie);

B<cookie()> creates a new cookie.  Its parameters include:

=over 4

=item B<-name>

The name of the cookie (required).  This can be any string at all.
Although browsers limit their cookie names to non-whitespace
alphanumeric characters, CGI.pm removes this restriction by escaping
and unescaping cookies behind the scenes.

=item B<-value>

The value of the cookie.  This can be any scalar value,
array reference, or even associative array reference.  For example,
you can store an entire associative array into a cookie this way:

    $cookie=$q->cookie( -name   => 'family information',
                        -value  => \%childrens_ages );

=item B<-path>

The optional partial path for which this cookie will be valid, as described
above.

=item B<-domain>

The optional partial domain for which this cookie will be valid, as described
above.

=item B<-expires>

The optional expiration date for this cookie.  The format is as described
in the section on the B<header()> method:

    "+1h"  one hour from now

=item B<-secure>

If set to true, this cookie will only be used within a secure
SSL session.

=back

The cookie created by B<cookie()> must be incorporated into the HTTP
header within the string returned by the B<header()> method:

    print $q->header(-cookie=>$my_cookie);

To create multiple cookies, give B<header()> an array reference:

    $cookie1 = $q->cookie( -name  => 'riddle_name',
                           -value => "The Sphynx's Question"
                         );
    $cookie2 = $q->cookie( -name  => 'answers',
                           -value => \%answers
                         );
    print $q->header( -cookie => [ $cookie1, $cookie2 ] );

To retrieve a cookie, request it by name by calling B<cookie()> method
without the B<-value> parameter:

    use CGI::Simple;
    $q = new CGI::Simple;
    $riddle  = $q->cookie('riddle_name');
    %answers = $q->cookie('answers');

Cookies created with a single scalar value, such as the "riddle_name"
cookie, will be returned in that form.  Cookies with array and hash
values can also be retrieved.

The cookie and CGI::Simple  namespaces are separate.  If you have a parameter
named 'answers' and a cookie named 'answers', the values retrieved by
B<param()> and B<cookie()> are independent of each other.  However, it's
simple to turn a CGI parameter into a cookie, and vice-versa:

    # turn a CGI parameter into a cookie
    $c = $q->cookie( -name=>'answers', -value=>[$q->param('answers')] );
    # vice-versa
    $q->param( -name=>'answers', -value=>[$q->cookie('answers')] );

=head2 raw_cookie()

Returns the HTTP_COOKIE variable, an HTTP extension implemented by
Netscape browsers version 1.1 and higher, and all versions of Internet
Explorer.  Cookies have a special format, and this method call just
returns the raw form (?cookie dough).  See B<cookie()> for ways of
setting and retrieving cooked cookies.

Called with no parameters, B<raw_cookie()> returns the packed cookie
structure.  You can separate it into individual cookies by splitting
on the character sequence "; ".  Called with the name of a cookie,
retrieves the B<unescaped> form of the cookie.  You can use the
regular B<cookie()> method to get the names, or use the raw_fetch()
method from the CGI::Simmple::Cookie module.

=cut

################# Header Methods ################

=head1 CREATING HTTP HEADERS

Normally the first thing you will do in any CGI script is print out an
HTTP header.  This tells the browser what type of document to expect,
and gives other optional information, such as the language, expiration
date, and whether to cache the document.  The header can also be
manipulated for special purposes, such as server push and pay per view
pages.

=head2 header() Create simple or complex HTTP headers

    print $q->header;

         -or-

    print $q->header('image/gif');

         -or-

    print $q->header('text/html','204 No response');

         -or-

    print $q->header( -type       => 'image/gif',
                      -nph        => 1,
                      -status     => '402 Payment required',
                      -expires    => '+3d',
                      -cookie     => $cookie,
                      -charset    => 'utf-7',
                      -attachment => 'foo.gif',
                      -Cost       => '$2.00'
                    );

B<header()> returns the Content-type: header.  You can provide your own
MIME type if you choose, otherwise it defaults to text/html.  An
optional second parameter specifies the status code and a human-readable
message.  For example, you can specify 204, "No response" to create a
script that tells the browser to do nothing at all.

The last example shows the named argument style for passing arguments
to the CGI methods using named parameters.  Recognized parameters are
B<-type>, B<-status>, B<-cookie>, B<-target>, B<-expires>, B<-nph>,
B<-charset> and B<-attachment>.  Any other named parameters will be
stripped of their initial hyphens and turned into header fields, allowing
you to specify any HTTP header you desire.

For example, you can produce non-standard HTTP header fields by providing
them as named arguments:

  print $q->header( -type            => 'text/html',
                    -nph             => 1,
                    -cost            => 'Three smackers',
                    -annoyance_level => 'high',
                    -complaints_to   => 'bit bucket'
                  );

This will produce the following non-standard HTTP header:

    HTTP/1.0 200 OK
    Cost: Three smackers
    Annoyance-level: high
    Complaints-to: bit bucket
    Content-type: text/html

Note that underscores are translated automatically into hyphens. This feature
allows you to keep up with the rapidly changing HTTP "standards".

The B<-type> is a key element that tell the browser how to display your
document. The default is 'text/html'. Common types are:

    text/html
    text/plain
    image/gif
    image/jpg
    image/png
    application/octet-stream

The B<-status> code is the HTTP response code. The default is 200 OK. Common
status codes are:

    200 OK
    204 No Response
    301 Moved Permanently
    302 Found
    303 See Other
    307 Temporary Redirect
    400 Bad Request
    401 Unauthorized
    403 Forbidden
    404 Not Found
    405 Not Allowed
    408 Request Timed Out
    500 Internal Server Error
    503 Service Unavailable
    504 Gateway Timed Out

The B<-expires> parameter lets you indicate to a browser and proxy server
how long to cache pages for. When you specify an absolute or relative
expiration interval with this parameter, some browsers and proxy servers
will cache the script's output until the indicated expiration date.
The following forms are all valid for the -expires field:

    +30s                                30 seconds from now
    +10m                                ten minutes from now
    +1h                                 one hour from now
    -1d                                 yesterday (i.e. "ASAP!")
    now                                 immediately
    +3M                                 in three months
    +10y                                in ten years time
    Thursday, 25-Apr-1999 00:40:33 GMT  at the indicated time & date

The B<-cookie> parameter generates a header that tells the browser to provide
a "magic cookie" during all subsequent transactions with your script.
Netscape cookies have a special format that includes interesting attributes
such as expiration time.  Use the B<cookie()> method to create and retrieve
session cookies.

The B<-target> is for frames use

The B<-nph> parameter, if set to a true value, will issue the correct
headers to work with a NPH (no-parse-header) script.  This is important
to use with certain servers that expect all their scripts to be NPH.

The B<-charset> parameter can be used to control the character set
sent to the browser.  If not provided, defaults to ISO-8859-1.  As a
side effect, this sets the charset() method as well.

The B<-attachment> parameter can be used to turn the page into an
attachment.  Instead of displaying the page, some browsers will prompt
the user to save it to disk.  The value of the argument is the
suggested name for the saved file.  In order for this to work, you may
have to set the B<-type> to 'application/octet-stream'.

=head2 no_cache() Preventing browser caching of scripts

Most browsers will not cache the output from CGI scripts. Every time
the browser reloads the page, the script is invoked anew. However some
browsers do cache pages. You can discourage this behavior using the
B<no_cache()> function.

    $q->no_cache(1); # turn caching off by sending appropriate headers
    $q->no_cache(1); # do not send cache related headers.

    $q->no_cache(1);
    print header (-type=>'image/gif', -nph=>1);

    This will produce a header like the following:

    HTTP/1.0 200 OK
    Server: Apache - accept no substitutes
    Expires: Thu, 15 Nov 2001 03:37:50 GMT
    Date: Thu, 15 Nov 2001 03:37:50 GMT
    Pragma: no-cache
    Content-Type: image/gif

Both the Pragma: no-cache header field and an Expires header that corresponds
to the current time (ie now) will be sent.

=head2 cache() Preventing browser caching of scripts

The somewhat ill named B<cache()> method is a legacy from CGI.pm. It operates
the same as the new B<no_cache()> method. The difference is/was that when set
it results only in the Pragma: no-cache line being printed.
Expires time data is not sent.

=head2 redirect() Generating a redirection header

    print $q->redirect('http://somewhere.else/in/movie/land');

Sometimes you don't want to produce a document yourself, but simply
redirect the browser elsewhere, perhaps choosing a URL based on the
time of day or the identity of the user.

The B<redirect()> function redirects the browser to a different URL.  If
you use redirection like this, you should B<not> print out a header as
well.

One hint I can offer is that relative links may not work correctly
when you generate a redirection to another document on your site.
This is due to a well-intentioned optimization that some servers use.
The solution to this is to use the full URL (including the http: part)
of the document you are redirecting to.

You can also use named arguments:

    print $q->redirect( -uri=>'http://somewhere.else/in/movie/land',
                        -nph=>1
                      );

The B<-nph> parameter, if set to a true value, will issue the correct
headers to work with a NPH (no-parse-header) script.  This is important
to use with certain servers, such as Microsoft ones, which
expect all their scripts to be NPH.

=cut

=head1 PRAGMAS

There are a number of pragmas that you can specify in your use CGI::Simple
statement. Pragmas, which are always preceded by a hyphen, change the way
that CGI::Simple functions in various ways. You can generally achieve
exactly the same results by setting the underlying $GLOBAL_VARIABLES.

For example the '-upload' pargma will enable file uploads:

    use CGI::Simple qw(-upload);

In CGI::Simple::Standard Pragmas, function sets , and individual functions
can all be imported in the same use() line.  For example, the following
use statement imports the standard set of functions and enables debugging
mode (pragma -debug):

    use CGI::Simple::Standard qw(:standard -debug);

The current list of pragmas is as follows:

=over 4

=item -no_undef_params

If a value is not given in the query string, as in the queries
"name1=&name2=" or "name1&name2", by default it will be returned
as an empty string.

If you specify the '-no_undef_params' pragma then CGI::Simple ignores
parameters with no values and they will not appear in the query object.

=item -nph

This makes CGI.pm produce a header appropriate for an NPH (no
parsed header) script.  You may need to do other things as well
to tell the server that the script is NPH.  See the discussion
of NPH scripts below.

=item -newstyle_urls

Separate the name=value pairs in CGI parameter query strings with
semicolons rather than ampersands.  For example:

    ?name=fred;age=24;favorite_color=3

Semicolon-delimited query strings are always accepted, but will not be
emitted by self_url() and query_string() unless the -newstyle_urls
pragma is specified.

=item -oldstyle_urls

Separate the name=value pairs in CGI parameter query strings with
ampersands rather than semicolons.  This is the default.

    ?name=fred&age=24&favorite_color=3

=item -autoload

This is only available for CGI::Simple::Standard and uses AUTOLOAD to
load functions on demand. See the CGI::Simple::Standard docs for details.

=item -no_debug

This turns off the command-line processing features. This is the default.

=item -debug1 and debug2

This turns on debugging.  At debug level 1 CGI::Simple will read arguments
from the command-line. At debug level 2 CGI.pm will produce the prompt
"(offline mode: enter name=value pairs on standard input)" and wait for
input on STDIN. If no number is specified then a debug level of 2 is used.

See the section on debugging for more details.

=item -default

This sets the default global values for CGI.pm which will enable infinite
size file uploads, and specify the '-newstyle_urls' and '-debug1' pragmas

=item -no_upload

Disable uploads - the default setting

=item - upload

Enable uploads - the CGI.pm default

=item -unique_header

Only allows headers to be generated once per script invocation

=item -carp

Carp when B<cgi_error()> called, default is to do nothing

=item -croak

Croak when B<cgi_error()> called, default is to do nothing

=back

=cut

############### NPH Scripts ################

=head1 USING NPH SCRIPTS

NPH, or "no-parsed-header", scripts bypass the server completely by
sending the complete HTTP header directly to the browser.  This has
slight performance benefits, but is of most use for taking advantage
of HTTP extensions that are not directly supported by your server,
such as server push and PICS headers.

Servers use a variety of conventions for designating CGI scripts as
NPH.  Many Unix servers look at the beginning of the script's name for
the prefix "nph-".  The Macintosh WebSTAR server and Microsoft's
Internet Information Server, in contrast, try to decide whether a
program is an NPH script by examining the first line of script output.

CGI.pm supports NPH scripts with a special NPH mode.  When in this
mode, CGI.pm will output the necessary extra header information when
the B<header()> and B<redirect()> methods are called. You can set NPH mode
in any of the following ways:

=over 4

=item In the B<use> statement

Simply add the "-nph" pragma to the use:

    use CGI::Simple qw(-nph)

=item By calling the B<nph()> method:

Call B<nph()> with a non-zero parameter at any point after using CGI.pm in your program.

    $q->nph(1)

=item By using B<-nph> parameters

in the B<header()> and B<redirect()>  statements:

    print $q->header(-nph=>1);

=back

The Microsoft Internet Information Server requires NPH mode.
CGI::Simple will automatically detect when the script is
running under IIS and put itself into this mode.  You do not need to
do this manually, although it won't hurt anything if you do.  However,
note that if you have applied Service Pack 6, much of the
functionality of NPH scripts, including the ability to redirect while
setting a cookie, b<do not work at all> on IIS without a special patch
from Microsoft.  See
http://support.microsoft.com/support/kb/articles/Q280/3/41.ASP:
Non-Parsed Headers Stripped From CGI Applications That Have nph-
Prefix in Name.

=cut

################# Server Push Methods #################

=head1 SERVER PUSH

CGI.pm provides four simple functions for producing multipart
documents of the type needed to implement server push.  These
functions were graciously provided by Ed Jordan <ed@fidalgo.net> with
additions from Andrew Benham <adsb@bigfoot.com>

You are also advised to put the script into NPH mode and to set $| to
1 to avoid buffering problems.

Only Netscape Navigator supports server push.
Internet Explorer browsers do not.

Here is a simple script that demonstrates server push:

    #!/usr/local/bin/perl
    use CGI::Simple::Standard qw/:push -nph/;
    $| = 1;
    print multipart_init(-boundary=>'----here we go!');
    foreach (0 .. 4) {
        print multipart_start(-type=>'text/plain'),
        "The current time is ",scalar(localtime),"\n";
        if ($_ < 4) {
            print multipart_end;
        }
        else {
            print multipart_final;
        }
        sleep 1;
    }

This script initializes server push by calling B<multipart_init()>.
It then enters a loop in which it begins a new multipart section by
calling B<multipart_start()>, prints the current local time,
and ends a multipart section with B<multipart_end()>.  It then sleeps
a second, and begins again. On the final iteration, it ends the
multipart section with B<multipart_final()> rather than with
B<multipart_end()>.

=head2 multipart_init() Initialize the multipart system

    multipart_init(-boundary=>$boundary);

Initialize the multipart system.  The -boundary argument specifies
what MIME boundary string to use to separate parts of the document.
If not provided, CGI.pm chooses a reasonable boundary for you.

=head2 multipart_start() Start a new part of the multipart document

    multipart_start(-type=>$type)

Start a new part of the multipart document using the specified MIME
type.  If not specified, text/html is assumed.

=head2 multipart_end() End a multipart part

    multipart_end()

End a part.  You must remember to call B<multipart_end()> once for each
B<multipart_start()>, except at the end of the last part of the multipart
document when B<multipart_final()> should be called instead of
B<multipart_end()>.

=head2 multipart_final()

    multipart_final()

End all parts.  You should call B<multipart_final()> rather than
B<multipart_end()> at the end of the last part of the multipart document.

=head2 CGI::Push

Users interested in server push applications should also have a look
at the B<CGI::Push> module.

=cut

################# Debugging Methods ################

=head1 DEBUGGING

If you are running the script from the command line or in the perl
debugger, you can pass the script a list of keywords or
parameter=value pairs on the command line or from standard input (you
don't have to worry about tricking your script into reading from
environment variables).  Before you do this you will need to change the
debug level from the default level of 0 (no debug) to either 1 if you
want to debug from @ARGV (the command line) of 2 if you want to debug from
STDIN. You can do this using the debug pragma like this:

    use CGI::Simple qw(-debug2);  # set debug to level 2 => from STDIN

        or this:

    $CGI::Simple::DEBUG = 1;      # set debug to level 1 => from @ARGV

At debug level 1 you can pass keywords and name=value pairs like this:

    your_script.pl keyword1 keyword2 keyword3

        or this:

    your_script.pl keyword1+keyword2+keyword3

        or this:

    your_script.pl name1=value1 name2=value2

        or this:

    your_script.pl name1=value1&name2=value2

At debug level 2 you can feed newline-delimited name=value
pairs to the script on standard input. You will be presented
with the following prompt:

    (offline mode: enter name=value pairs on standard input)

You end the input with your system dependent end of file character.
You should try ^Z ^X ^D and ^C if all else fails. The ^ means hold down
the [Ctrl] button while you press the other key.

When debugging, you can use quotes and backslashes to escape
characters in the familiar shell manner, letting you place
spaces and other funny characters in your parameter=value
pairs:

    your_script.pl "name1='I am a long value'" "name2=two\ words"

=head2 Dump() Dumping the current object details

The B<Dump()> method produces a string consisting of all the
query's object attributes formatted nicely as a nested list.  This dump
includes the name/value pairs and a number of other details. This is useful
for debugging purposes:

    print $q->Dump

The actual result of this is HTML escaped formatted text wrapped in <pre> tags
so if you send it straight to the browser it produces something that looks
like:

    $VAR1 = bless( {
         '.parameters' => [
                            'name',
                            'color'
                          ],
         '.globals' => {
                         'FATAL' => -1,
                         'DEBUG' => 0,
                         'NO_NULL' => 1,
                         'POST_MAX' => 102400,
                         'USE_CGI_PM_DEFAULTS' => 0,
                         'HEADERS_ONCE' => 0,
                         'NPH' => 0,
                         'DISABLE_UPLOADS' => 1,
                         'NO_UNDEF_PARAMS' => 0,
                         'USE_PARAM_SEMICOLONS' => 0
                       },
         '.fieldnames' => {
                            'color' => '1',
                            'name' => '1'
                          },
         '.mod_perl' => '',
         'color' => [
                      'red',
                      'green',
                      'blue'
                    ],
         'name' => [
                     'JaPh,'
                   ]
        }, 'CGI::Simple' );

You may recognize this as valid Perl syntax (which it is) and/or the output
from Data::Dumper (also true). This is the actual guts of how the information
is stored in the query object. All the internal params start with a . char

Alternatively you can dump your object and the current environment using:

    print $q->Dump(\%ENV);

=head2 PrintEnv() Dumping the environment

You can get a similar browser friendly dump of the current %ENV hash using:

    print $q->PrintEnv;

This will produce something like (in the browser):

    $VAR1 = {
          'QUERY_STRING' => 'name=JaPh%2C&color=red&color=green&color=blue',
          'CONTENT_TYPE' => 'application/x-www-form-urlencoded',
          'REGRESSION_TEST' => 'simple.t.pl',
          'VIM' => 'C:\\WINDOWS\\Desktop\\vim',
          'HTTP_REFERER' => 'xxx.sex.com',
          'HTTP_USER_AGENT' => 'LWP',
          'HTTP_ACCEPT' => 'text/html;q=1, image/gif;q=0.42, */*;q=0.001',
          'REMOTE_HOST' => 'localhost',
          'HTTP_HOST' => 'the.restaurant.at.the.end.of.the.universe',
          'GATEWAY_INTERFACE' => 'bleeding edge',
          'REMOTE_IDENT' => 'None of your damn business',
          'SCRIPT_NAME' => '/cgi-bin/foo.cgi',
          'SERVER_NAME' => 'nowhere.com',
          'HTTP_COOKIE' => '',
          'CONTENT_LENGTH' => '42',
          'HTTPS_A' => 'A',
          'HTTP_FROM' => 'spammer@nowhere.com',
          'HTTPS_B' => 'B',
          'SERVER_PROTOCOL' => 'HTTP/1.0',
          'PATH_TRANSLATED' => '/usr/local/somewhere/else',
          'SERVER_SOFTWARE' => 'Apache - accept no substitutes',
          'PATH_INFO' => '/somewhere/else',
          'REMOTE_USER' => 'Just another Perl hacker,',
          'REMOTE_ADDR' => '127.0.0.1',
          'HTTPS' => 'ON',
          'DOCUMENT_ROOT' => '/vs/www/foo',
          'REQUEST_METHOD' => 'GET',
          'REDIRECT_QUERY_STRING' => '',
          'AUTH_TYPE' => 'PGP MD5 DES rot13',
          'COOKIE' => 'foo=a%20phrase; bar=yes%2C%20a%20phrase&;I%20say;',
          'SERVER_PORT' => '8080'
        };


=head2 cgi_error() Retrieving CGI::Simple error messages

Errors can occur while processing user input, particularly when
processing uploaded files.  When these errors occur, CGI::Simple will stop
processing and return an empty parameter list.  You can test for
the existence and nature of errors using the B<cgi_error()> function.
The error messages are formatted as HTTP status codes. You can either
incorporate the error text into an HTML page, or use it as the value
of the HTTP status:

    my $error = $q->cgi_error;
    if ($error) {
        print $q->header(-status=>$error);
        print "<H2>$error</H2>;
      exit;
    }

=cut

############### Accessor Methods ################

=head1 ACCESSOR METHODS

=head2 version() Get the CGI::Simple version info

    $version = $q->version();

The B<version()> method returns the value of $VERSION

=head2 nph() Enable/disable NPH (Non Parsed Header) mode

    $q->nph(1);  # enable NPH mode
    $q->nph(0);  # disable NPH mode

The B<nph()> method enables and disables NPH headers. See the NPH section.

=head2 all_parameters() Get the names/values of all parameters

    @all_parameters = $q->all_parameters();

The B<all_parameters()> method is an alias for B<param()>

=head2 charset() Get/set the current character set.

    $charset = $q->charset(); # get current charset
    $q->charset('utf-42');    # set the charset

The B<charset()> method gets the current charset value if no argument is
supplied or sets it if an argument is supplied.

=head2 crlf() Get the system specific line ending sequence

    $crlf = $q->crlf();

The B<crlf()> method returns the system specific line ending sequence.

=head2 globals() Get/set the value of the remaining global variables

    $globals = $q->globals('FATAL');     # get the current value of $FATAL
    $globals = $q->globals('FATAL', 1 ); # set croak mode on cgi_error()

The B<globals()> method gets/sets the values of the global variables after the
script has been invoked. For globals like $POST_MAX and $DISABLE_UPLOADS this
makes no difference as they must be set prior to calling the new constructor
but there might be reason the change the value of others.

=head2 auth_type() Get the current authorization/verification method

    $auth_type = $q->auth_type();

The B<auth_type()> method returns the value of $ENV{'AUTH_TYPE'} which should
contain the authorization/verification method in use for this script, if any.

=head2 content_length() Get the content length submitted in a POST

    $content_length = $q->content_length();

The B<content_length()> method returns the value of $ENV{'AUTH_TYPE'}

=head2 content_type() Get the content_type of data submitted in a POST

    $content_type = $q->content_type();

The B<content_type()> method returns the content_type of data submitted in
a POST, generally 'multipart/form-data' or
'application/x-www-form-urlencoded' as supplied in $ENV{'CONTENT_TYPE'}

=head2 document_root() Get the document root

    $document_root = $q->document_root();

The B<document_root()> method returns the value of $ENV{'DOCUMENT_ROOT'}

=head2 gateway_interface() Get the gateway interface

    $gateway_interface = $q->gateway_interface();

The B<gateway_interface()> method returns the value of
$ENV{'GATEWAY_INTERFACE'}

=head2 path_translated() Get the value of path translated

    $path_translated = $q->path_translated();

The B<path_translated()> method returns the value of $ENV{'PATH_TRANSLATED'}

=head2 referer() Spy on your users

    $referer = $q->referer();

The B<referer()> method returns the value of $ENV{'REFERER'} This will return
the URL of the page the browser was viewing prior to fetching your script.
Not available for all browsers.

=head2 remote_addr() Get the remote address

    $remote_addr = $q->remote_addr();

The B<remote_addr()> method returns the value of $ENV{'REMOTE_ADDR'} or
127.0.0.1 (localhost) if this is not defined.

=head2 remote_host() Get a value for remote host

    $remote_host = $q->remote_host();

The B<remote_host()> method returns the value of $ENV{'REMOTE_HOST'} if it is
defined. If this is not defined it returns $ENV{'REMOTE_ADDR'} If this is not
defined it returns 'localhost'

=head2 remote_ident() Get the remote identity

    $remote_ident = $q->remote_ident();

The B<remote_ident()> method returns the value of $ENV{'REMOTE_IDENT'}

=head2 remote_user() Get the remote user

    $remote_user = $q->remote_user();

The B<remote_user()> method returns the authorization/verification name used
for user verification, if this script is protected. The value comes from
$ENV{'REMOTE_USER'}

=head2 request_method() Get the request method

    $request_method = $q->request_method();

The B<request_method()> method returns the method used to access your
script, usually one of 'POST', 'GET' or 'HEAD' as supplied by
$ENV{'REQUEST_METHOD'}

=head2 script_name() Get the script name

    $script_name = $q->script_name();

The B<script_name()> method returns the value of $ENV{'SCRIPT_NAME'} if it is
defined. Otherwise it returns Perl's script name from $0. Failing this it
returns a null string ''

=head2 server_name() Get the server name

    $server_name = $q->server_name();

The B<server_name()> method returns the value of $ENV{'SERVER_NAME'} if defined
or 'localhost' otherwise

=head2 server_port() Get the port the server is listening on

    $server_port = $q->server_port();

The B<server_port()> method returns the value $ENV{'SERVER_PORT'} if defined or
80 if not.

=head2 server_protocol() Get the current server protocol

    $server_protocol = $q->server_protocol();

The B<server_protocol()> method returns the value of $ENV{'SERVER_PROTOCOL'} if
defined or 'HTTP/1.0' otherwise

=head2 server_software() Get the server software

    $server_software = $q->server_software();

The B<server_software()> method returns the value $ENV{'SERVER_SOFTWARE'} or
'cmdline' If the server software is IIS it formats your hard drive, installs
Linux, FTPs to www.apache.org, installs Apache, and then restores your system
from tape. Well maybe not, but it's a nice thought.

=head2 user_name() Get a value for the user name.

    $user_name = $q->user_name();

Attempt to obtain the remote user's name, using a variety of different
techniques.  This only works with older browsers such as Mosaic.
Newer browsers do not report the user name for privacy reasons!

Technically the B<user_name()> method returns the value of $ENV{'HTTP_FROM'}
or failing that $ENV{'REMOTE_IDENT'} or as a last choice $ENV{'REMOTE_USER'}

=head2 user_agent() Get the users browser type

    $ua = $q->user_agent();          # return the user agent
    $ok = $q->user_agent('mozilla'); # return true if user agent 'mozilla'

The B<user_agent()> method returns the value of $ENV{'HTTP_USER_AGENT'}  when
called without an argument or true or false if the $ENV{'HTTP_USER_AGENT'}
matches the passed argument. The matching is case insensitive and partial.

=head2 virtual_host() Get the virtual host

    $virtual_host = $q->virtual_host();

The B<virtual_host()> method returns the value of  $ENV{'HTTP_HOST'} if defined
or $ENV{'SERVER_NAME'} as a default. Port numbers are removed.

=head2 path_info() Get any extra path info set to the script

    $path_info = $q->path_info();

The B<path_info()> method returns additional path information from the script
URL. E.G. fetching /cgi-bin/your_script/additional/stuff will result in
$q->path_info() returning "/additional/stuff".

NOTE: The Microsoft Internet Information Server
is broken with respect to additional path information.  If
you use the Perl DLL library, the IIS server will attempt to
execute the additional path information as a Perl script.
If you use the ordinary file associations mapping, the
path information will be present in the environment,
but incorrect.  The best thing to do is to avoid using additional
path information in CGI scripts destined for use with IIS.

=head2 Accept() Get the browser MIME types

    $Accept = $q->Accept();

The B<Accept()> method returns a list of MIME types that the remote browser
accepts. If you give this method a single argument corresponding to a
MIME type, as in $q->Accept('text/html'), it will return a floating point
value corresponding to the browser's preference for this type from 0.0
(don't want) to 1.0.  Glob types (e.g. text/*) in the browser's accept
list are handled correctly.

=head2 accept() Alias for Accept()

    $accept = $q->accept();

The B<accept()> Method is an alias for Accept()

=head2 http() Get a range of HTTP related information

    $http = $q->http();

Called with no arguments the B<http()> method returns the list of HTTP or HTTPS
environment variables, including such things as HTTP_USER_AGENT,
HTTP_ACCEPT_LANGUAGE, and HTTP_ACCEPT_CHARSET, corresponding to the
like-named HTTP header fields in the request. Called with the name of
an HTTP header field, returns its value.  Capitalization and the use
of hyphens versus underscores are not significant.

For example, all three of these examples are equivalent:

   $requested_language = $q->http('Accept-language');
   $requested_language = $q->http('Accept_language');
   $requested_language = $q->http('HTTP_ACCEPT_LANGUAGE');

=head2 https() Get a range of HTTPS related information

    $https = $q->https();

The B<https()> method is similar to the http() method except that when called
without an argument it returns the value of $ENV{'HTTPS'} which will be
true if a HTTPS connection is in use and false otherwise.

=head2 protocol() Get the current protocol

    $protocol = $q->protocol();

The B<protocol()> method returns 'https' if a HTTPS connection is in use or the
B<server_protocol()> minus version numbers ('http') otherwise.

=head2 url() Return the script's URL in several formats

    $full_url      = $q->url();
    $full_url      = $q->url(-full=>1);
    $relative_url  = $q->url(-relative=>1);
    $absolute_url  = $q->url(-absolute=>1);
    $url_with_path = $q->url(-path_info=>1);
    $url_with_path_and_query = $q->url(-path_info=>1,-query=>1);
    $netloc        = $q->url(-base => 1);

B<url()> returns the script's URL in a variety of formats.  Called
without any arguments, it returns the full form of the URL, including
host name and port number

    http://your.host.com/path/to/script.cgi

You can modify this format with the following named arguments:

=over 4

=item B<-absolute>

If true, produce an absolute URL, e.g.

    /path/to/script.cgi

=item B<-relative>

Produce a relative URL.  This is useful if you want to reinvoke your
script with different parameters. For example:

    script.cgi

=item B<-full>

Produce the full URL, exactly as if called without any arguments.
This overrides the -relative and -absolute arguments.

=item B<-path> (B<-path_info>)

Append the additional path information to the URL.  This can be
combined with B<-full>, B<-absolute> or B<-relative>.  B<-path_info>
is provided as a synonym.

=item B<-query> (B<-query_string>)

Append the query string to the URL.  This can be combined with
B<-full>, B<-absolute> or B<-relative>.  B<-query_string> is provided
as a synonym.

=item B<-base>

Generate just the protocol and net location, as in http://www.foo.com:8000

=back

=head2 self_url() Get the scripts complete URL

    $self_url = $q->self_url();

The B<self_url()> method returns the value of:

   $self->url( '-path_info'=>1, '-query'=>1, '-full'=>1 );

=head2 state() Alias for self_url()

    $state = $q->state();

The B<state()> method is an alias for self_url()

=cut

################# cgi-lib.pl Compatibility Methods #################

=head1 COMPATIBILITY WITH cgi-lib.pl 2.18

To make it easier to port existing programs that use cgi-lib.pl all
the subs within cgi-lib.pl are available in CGI::Simple.  Using the
functional interface of CGI::Simple::Standard porting is
as easy as:

    OLD VERSION
        require "cgi-lib.pl";
        &ReadParse;
        print "The value of the antique is $in{'antique'}.\n";

    NEW VERSION
        use CGI::Simple::Standard qw(:cgi-lib);
        &ReadParse;
        print "The value of the antique is $in{'antique'}.\n";

CGI:Simple's B<ReadParse()> routine creates a variable named %in,
which can be accessed to obtain the query variables.  Like
ReadParse, you can also provide your own variable via a glob. Infrequently
used features of B<ReadParse()>, such as the creation of @in and $in
variables, are not supported.

You can also use the OO interface of CGI::Simple and call B<ReadParse()> and
other cgi-lib.pl functions like this:

    &CGI::Simple::ReadParse;       # get hash values in %in

    my $q = new CGI::Simple;
    $q->ReadParse();                # same thing

    CGI::Simple::ReadParse(*field); # get hash values in %field function style

    my $q = new CGI::Simple;
    $q->ReadParse(*field);          # same thing

Once you use B<ReadParse()> under the functional interface , you can retrieve
the query object itself this way if needed:

    $q = $in{'CGI'};

Either way it allows you to start using the more interesting features
of CGI.pm without rewriting your old scripts from scratch.

Unlike CGI.pm all the cgi-lib.pl functions from Version 2.18 are supported:

    ReadParse()
    SplitParam()
    MethGet()
    MethPost()
    MyBaseUrl()
    MyURL()
    MyFullUrl()
    PrintHeader()
    HtmlTop()
    HtmlBot()
    PrintVariables()
    PrintEnv()
    CgiDie()
    CgiError()

=cut

############### Compatibility with mod_perl ################

=head1 COMPATIBILITY WITH mod_perl

This module uses Selfloader and the __DATA__ token to ensure that only code
that is used gets complied. This optimises performance but means that it
will not work under mod_perl in its default configuration. To configure it
to run under mod perl you would need to remove two lines from the module.

    use Selfloader;

    ....

    __DATA__

With these two lines gone the entire module will load and compile at mod_perl
startup. CGI::Simple's pure OO methods return data significantly faster than
CGI.pm's OO methods

=cut

############### Compatibility with CGI.pm ################

=head1 COMPATIBILITY WITH CGI.pm

I has long been suggested that the CGI and HTML parts of CGI.pm should be
split into separate modules (even the author suggests this!), CGI::Simple
represents the realization of this and contains the complete CGI side of
CGI.pm. Code-wise it weighs in at a little under 30% of the size of CGI.pm at
a little under 1000 lines. It uses SelfLoader and only compiles the first 350
lines. Other routines are loaded on first use. Internally around half the
code is new although the method interfaces remain unchanged.

A great deal of care has been taken to ensure that the interface remains
unchanged although a few tweaks have been made. The test suite is extensive
and includes all the CGI.pm test scripts as well as a series of new test
scripts. You may like to have a look at /t/concur.t which makes 160 tests
of CGI::Simple and CGI in parallel and compares the results to ensure they
are identical. This is the case as of CGI.pm 2.78.

You can't make an omelet without breaking eggs. A large number of methods
and global variables have been deleted as detailed below. Some pragmas are
also gone. In the tarball there is a script B</misc/check.pl> that will check if
a script seems to be using any of these now non existent methods, globals or
pragmas. You call it like this:

    perl check.pl <files>

If it finds any likely candidates it will print a line with the line number,
problem method/global and the complete line. For example here is some output
from running the script on CGI.pm:

    ...
    3162: Problem:'$CGI::OS'   local($CRLF) = "\015\012" if $CGI::OS eq 'VMS';
    3165: Problem:'fillBuffer' $self->fillBuffer($FILLUNIT);
    ....

=head1 DIFFERENCES FROM CGI.pm

CGI::Simple is strict and warnings compliant. SelfLoader is used to load only
the required code. You can easily optimize code loading simply by moving the
__DATA__ token. Commonly called methods should go above the token and will
be compiled at compile time (on load). Uncommonly used methods go below the
__DATA__ token and will only be compiled as required at runtime when the
method is actually called.

As well as using SelfLoader to load the non core methods, Simple.pm uses
IO::File to supply anonymous temp files for file uploads and Data::Dumper
for cloning objects and dumping data.  These modules are all part of the
standard Perl distribution.

There are 4 modules in this distribution:

    CGI/Simple.pm           supplies all the core code.
    CGI/Simple/Cookie.pm    supplies the cookie handling functions.
    CGI/Simple/Util.pm      supplies a variety of utility functions
    CGI/Simple/Standard.pm  supplies a functional interface for Simple.pm

Simple.pm is the core module that provide all the essential functionality.
Cookie.pm is a shortened rehash of the CGI.pm module of the same name
which supplies the required cookie functionality. Util.pm has been recoded to
use an internal object for data storage and supplies rarely needed non core
functions and/or functions needed for the HTML side of things. Standard.pm is
a wrapper module that supplies a complete functional interface to the OO
back end supplied by CGI::Simple.

Although a serious attempt has been made to keep the interface identical,
some minor changes and tweaks have been made. They will likely be
insignificant to most users but here are the gory details.

=head2 Globals Variables

The list of global variables has been pruned by 75%. Here is the complete
list of the global variables used:

    $VERSION = "0.01";
    # set this to 1 to use CGI.pm default global settings
    $USE_CGI_PM_DEFAULTS = 0 unless defined $USE_CGI_PM_DEFAULTS;
    # see if user wants old  CGI.pm defaults
    do{ _use_cgi_pm_global_settings(); return } if $USE_CGI_PM_DEFAULTS;
    # no file uploads by default, set to 0 to enable uploads
    $DISABLE_UPLOADS = 1 unless defined $DISABLE_UPLOADS;
    # use a post max of 100K, set to -1 for no limits
    $POST_MAX = 102_400 unless defined $POST_MAX;
    # do not include undefined params parsed from query string
    $NO_UNDEF_PARAMS = 0 unless defined $NO_UNDEF_PARAMS;
    # separate the name=value pairs with ; rather than &
    $USE_PARAM_SEMICOLONS = 0 unless defined $USE_PARAM_SEMICOLONS;
    # only print headers once
    $HEADERS_ONCE = 0 unless defined $HEADERS_ONCE;
    # Set this to 1 to enable NPH scripts
    $NPH = 0 unless defined $NPH;
    # 0 => no debug, 1 => from @ARGV,  2 => from STDIN
    $DEBUG = 0 unless defined $DEBUG;
    # filter out null bytes in param - value pairs
    $NO_NULL  = 1 unless defined $NO_NULL;
    # set behavior when cgi_err() called -1 => silent, 0 => carp, 1 => croak
    $FATAL = -1 unless defined $FATAL;

Four of the default values of the old CGI.pm variables have been changed.
Unlike CGI.pm which by default allows unlimited POST data and file uploads
by default CGI::Simple limits POST data size to 100kB and denies file uploads
by default. $USE_PARAM_SEMICOLONS is set to 0 by default so we use (old style)
& rather than ; as the pair separator for query strings. Debugging is
disabled by default.

There are three new global variables. If $NO_NULL is true (the default) then
CGI::Simple will strip null bytes out of names, values and keywords. Null
bytes can do interesting things to C based code like Perl. Uploaded files
are not touched. $FATAL controls the behavior when B<cgi_error()> is called.
The default value of -1 makes errors silent. $USE_CGI_PM_DEFAULTS reverts the
defaults to the CGI.pm standard values ie unlimited file uploads via POST
for DNS attacks. You can also get the defaults back by using the '-default'
pragma in the use:

    use CGI::Simple qw(-default);
    use CGI::Simple::Standard qw(-default);

The values of the global variables are stored in the CGI::Simple object and
can be referenced and changed using the B<globals()> method like this:

    my $value = $q->globals( 'VARNAME' );      # get
    $q->globals( 'VARNAME', 'some value' );    # set

As with many CGI.pm methods if you pass the optional value that will
be set.

The $CGI::Simple::VARNAME = 'N' syntax is only useful prior to calling the
B<new()> constructor. After that all reference is to the values stored in the
CGI::Simple object so you must change these using the B<globals()> method.

$DISABLE_UPLOADS and $POST_MAX *must* be set prior to calling the constructor
if you want the changes to have any effect as they control behavior during
initialization. This is the same a CGI.pm although some people seem to miss
this rather important point and set these after calling the constructor which
does nothing.

The following globals are no longer relevant and have all been deleted:

    $AUTOLOADED_ROUTINES
    $AUTOLOAD_DEBUG
    $BEEN_THERE
    $CRLF
    $DEFAULT_DTD
    $EBCDIC
    $FH
    $FILLUNIT
    $IIS
    $IN
    $INITIAL_FILLUNIT
    $JSCRIPT
    $MAC
    $MAXTRIES
    $MOD_PERL
    $NOSTICKY
    $OS
    $PERLEX
    $PRIVATE_TEMPFILES
    $Q
    $QUERY_CHARSET
    $QUERY_PARAM
    $SCRATCH
    $SL
    $SPIN_LOOP_MAX
    $TIMEOUT
    $TMPDIRECTORY
    $XHTML
    %EXPORT
    %EXPORT_OK
    %EXPORT_TAGS
    %OVERLOAD
    %QUERY_FIELDNAMES
    %SUBS
    @QUERY_PARAM
    @TEMP

Notes: CGI::Simple uses IO::File->new_tmpfile to get tempfile filehandles.
These are private by default so $PRIVATE_TEMPFILES is no longer required nor
is $TMPDIRECTORY. The value that were stored in $OS, $CRLF, $QUERY_CHARSET
and $EBCDIC are now stored in the CGI::Simple::Util object where they find
most of their use. The $MOD_PERL and $PERLEX values are now stored in our
CGI::Simple object. $IIS was only used once in path_info().  $SL the system
specific / \ : path delimiter is not required as we let IO::File handle our
tempfile requirements. The rest of the globals are HTML related, export
related, hand rolled autoload related or serve obscure purposes in CGI.pm

=head2 Changes to pragmas

There are some new pragmas available. See the pragmas section for details.
The following CGI.pm pragmas are not available:

    -any
    -compile
    -nosticky
    -no_xhtml
    -private_tempfiles

-compile has been removed as it is not available using SelfLoader. If you
wish to compile all of CGI::Simple comment out the line:

    use SelfLoader

and remove the __DATA__ token. Tempfiles are now private by default and the
other pragmas are HTML related.

=head2 Filehandles

Unlike CGI.pm which tries to accept all filehandle like objects only \*FH
and $fh are accepted by CGI::Simple as file accessors for B<new()> and B<save()>.
IO::File objects work fine.

=head2 Hash interface

    %hash = $q->Vars();     # pack values with "\0";
    %hash = $q->Vars(",");  # comma separate values

You may optionally pass B<Vars()> a string that will be used to separate multiple
values when they are packed into the single hash value. If no value is
supplied the default "\0" (null byte) will be used. Null bytes are dangerous
things for C based code (ie Perl).

=head2 cgi-lib.pl

All the cgi-lib.pl 2.18 routines are supported. Unlike CGI.pm all the
subroutines from cgi-lib.pl are included. They have been GOLFED down to
25 lines but they all work pretty much the same as the originals.

=head1 CGI::Simple COMPLETE METHOD LIST

Here is a complete list of all the CGI::Simple methods.

=head2 Guts (hands off, except of course for new)

    _initialize_globals
    _use_cgi_pm_global_settings
    _store_globals
    import
    _reset_globals
    new
    _initialize
    _read_parse
    _parse_params
    _add_param
    _parse_keywordlist
    _parse_multipart
    _save_tmpfile
    _read_data

=head2 Core Methods

    param
    add_param
    param_fetch
    url_param
    keywords
    Vars
    append
    delete
    Delete
    delete_all
    Delete_all
    upload
    upload_info
    query_string
    parse_query_string
    parse_keywordlist

=head2 Save and Restore from File Methods

    _init_from_file
    save
    save_parameters

=head2 Miscellaneous Methods

    url_decode
    url_encode
    escapeHTML
    unescapeHTML
    put
    print

=head2 Cookie Methods

    cookie
    raw_cookie

=head2 Header Methods

    header
    cache
    no_cache
    redirect

=head2 Server Push Methods

    multipart_init
    multipart_start
    multipart_end
    multipart_final

=head2 Debugging Methods

    read_from_cmdline
    Dump
    as_string
    cgi_error

=head2 cgi-lib.pl Compatibility Routines - all 2.18 functions available

    _shift_if_ref
    ReadParse
    SplitParam
    MethGet
    MethPost
    MyBaseUrl
    MyURL
    MyFullUrl
    PrintHeader
    HtmlTop
    HtmlBot
    PrintVariables
    PrintEnv
    CgiDie
    CgiError

=head2 Accessor Methods

    version
    nph
    all_parameters
    charset
    crlf                # new, returns OS specific CRLF sequence
    globals             # get/set global variables
    auth_type
    content_length
    content_type
    document_root
    gateway_interface
    path_translated
    referer
    remote_addr
    remote_host
    remote_ident
    remote_user
    request_method
    script_name
    server_name
    server_port
    server_protocol
    server_software
    user_name
    user_agent
    virtual_host
    path_info
    Accept
    accept
    http
    https
    protocol
    url
    self_url
    state

=head1 NEW METHODS IN CGI::Simple

There are a few new methods in CGI::Simple as listed below. The highlights are
the B<parse_query_string()> method to add the QUERY_STRING data to your object if
the method was POST. The B<no_cache()> method adds an expires now directive and
the Pragma: no-cache directive to the header to encourage some browsers to
do the right thing. B<PrintEnv()> from the cgi-lib.pl routines will dump an
HTML friendly list of the %ENV and makes a handy addition to B<Dump()> for use
in debugging. The upload method now accepts a filepath as an optional second
argument as shown in the synopsis. If this is supplied the uploaded file will
be written to there automagically.

=head2 Internal Routines

    _initialize_globals()
    _use_cgi_pm_global_settings()
    _store_globals()
    _initialize()
    _init_from_file()
    _read_parse()
    _parse_params()
    _add_param()
    _parse_keywordlist()
    _parse_multipart()
    _save_tmpfile()
    _read_data()

=head2 New Public Methods

    add_param()             # adds a param/value(s) pair +/- overwrite
    upload_info()           # uploaded files MIME type and size
    url_decode()            # decode s url encoded string
    url_encode()            # url encode a string
    parse_query_string()    # add QUERY_STRING data to $q object if 'POST'
    no_cache()              # add both the Pragma: no-cache
                            # and Expires/Date => 'now' to header

=head2  cgi-lib.pl methods added for completeness

    _shift_if_ref()         # internal hack reminiscent of self_or_default :-)
    MyBaseUrl()
    MyURL()
    MyFullUrl()
    PrintVariables()
    PrintEnv()
    CgiDie()
    CgiError()

=head2 New Accessors

    crlf()                  # returns CRLF sequence
    globals()               # global vars now stored in $q object - get/set
    content_length()        # returns $ENV{'CONTENT_LENGTH'}
    document_root()         # returns $ENV{'DOCUMENT_ROOT'}
    gateway_interface()     # returns $ENV{'GATEWAY_INTERFACE'}

=head1 METHODS IN CGI.pm NOT IN CGI::Simple

Here is a complete list of what is not included in CGI::Simple. Basically all
the HTML related stuff plus large redundant chunks of the guts. The check.pl
script in the /misc dir will check to see if a script is using any of these.

=head2 Guts - rearranged, recoded, renamed and hacked out of existence

    initialize_globals()
    compile()
    expand_tags()
    self_or_default()
    self_or_CGI()
    init()
    to_filehandle()
    save_request()
    parse_params()
    add_parameter()
    binmode()
    _make_tag_func()
    AUTOLOAD()
    _compile()
    _setup_symbols()
    new_MultipartBuffer()
    read_from_client()
    import_names()     # I dislike this and left it out, so shoot me.

=head2 HTML Related

    autoEscape()
    URL_ENCODED()
    MULTIPART()
    SERVER_PUSH()
    start_html()
    _style()
    _script()
    end_html()
    isindex()
    startform()
    start_form()
    end_multipart_form()
    start_multipart_form()
    endform()
    end_form()
    _textfield()
    textfield()
    filefield()
    password_field()
    textarea()
    button()
    submit()
    reset()
    defaults()
    comment()
    checkbox()
    checkbox_group()
    _tableize()
    radio_group()
    popup_menu()
    scrolling_list()
    hidden()
    image_button()
    nosticky()
    default_dtd()

=head2 Upload Related

CGI::Simple uses anonymous tempfiles supplied by IO::File to spool uploaded
files to.

    private_tempfiles() # automatic in CGI::Simple
    tmpFileName()       # all upload files are anonymous
    uploadInfo()        # relied on FH access, replaced with upload_info()


=head2 Really Private Subs (marked as so)

    previous_or_default()
    register_parameter()
    get_fields()
    _set_values_and_labels()
    _compile_all()
    asString()
    compare()

=head2 Internal Multipart Parsing Routines

    read_multipart()
    readHeader()
    readBody()
    read()
    fillBuffer()
    eof()

=head1 EXPORT

Nothing.

=head1 AUTHOR INFORMATION

Originally copyright 2001 Dr James Freeman E<lt>jfreeman@tassie.net.auE<gt>
This release by Andy Armstrong <andy@hexten.net>

This package is free software and is provided "as is" without express or
implied warranty. It may be used, redistributed and/or modified under the terms
of the Perl Artistic License (see http://www.perl.com/perl/misc/Artistic.html)

Address bug reports and comments to: andy@hexten.net.  When sending
bug reports, please provide the version of CGI::Simple, the version of
Perl, the name and version of your Web server, and the name and
version of the operating system you are using.  If the problem is even
remotely browser dependent, please provide information about the
affected browsers as well.

Address bug reports and comments to: andy@hexten.net

=head1 CREDITS

Lincoln D. Stein (lstein@cshl.org) and everyone else who worked on the
original CGI.pm upon which this module is heavily based

Brandon Black for some heavy duty testing and bug fixes

John D Robinson and Jeroen Latour for helping solve some interesting test
failures as well as Perlmonks:
tommyw, grinder, Jaap, vek, erasei, jlongino and strider_corinth

Thanks for patches to:

Ewan Edwards, Joshua N Pritikin, Mike Barry

=head1 LICENCE AND COPYRIGHT

Copyright (c) 2007, Andy Armstrong C<< <andy@hexten.net> >>. All rights reserved.

This module is free software; you can redistribute it and/or
modify it under the same terms as Perl itself. See L<perlartistic>.

=head1 SEE ALSO

B<CGI>, L<CGI::Simple::Standard>, L<CGI::Simple::Cookie>,
L<CGI::Simple::Util>, L<CGI::Minimal>

=cut

