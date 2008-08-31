package Captcha::reCAPTCHA;

use warnings;
use strict;
use Carp;
use LWP::UserAgent;
use HTML::Tiny;

our $VERSION = '0.92';

use constant API_SERVER        => 'http://api.recaptcha.net';
use constant API_SECURE_SERVER => 'https://api-secure.recaptcha.net';
use constant API_VERIFY_SERVER => 'http://api-verify.recaptcha.net';
use constant SERVER_ERROR      => 'recaptcha-not-reachable';

sub new {
    my $class = shift;
    my $self = bless {}, $class;
    $self->_initialize( @_ );
    return $self;
}

sub _initialize {
    my $self = shift;
    my $args = shift || {};

    croak "new must be called with a reference to a hash of parameters"
      unless 'HASH' eq ref $args;
}

sub get_options_setter {
    my $self    = shift;
    my $options = shift || return '';
    my $h       = HTML::Tiny->new();

    return $h->script(
        { type => 'text/javascript' },
        "\n//<![CDATA[\n"
          . "var RecaptchaOptions = "
          . $h->json_encode( $options )
          . ";\n//]]>\n"
    ) . "\n";
}

sub get_html {
    my $self = shift;
    my ( $pubkey, $error, $use_ssl, $options ) = @_;

    croak
      "To use reCAPTCHA you must get an API key from http://recaptcha.net/api/getkey"
      unless $pubkey;

    my $h = HTML::Tiny->new();
    my $server = $use_ssl ? API_SECURE_SERVER : API_SERVER;

    my $query = { k => $pubkey };
    if ( $error ) {
        # Handle the case where the result hash from check_answer
        # is passed.
        if ( 'HASH' eq ref $error ) {
            return '' if $error->{is_valid};
            $error = $error->{error};
        }
        $query->{error} = $error;
    }
    my $qs = $h->query_encode( $query );

    return join(
        '',
        $self->get_options_setter( $options ),
        $h->script(
            {
                type => 'text/javascript',
                src  => "$server/challenge?$qs",
            }
        ),
        "\n",
        $h->noscript(
            [
                $h->iframe(
                    {
                        src         => "$server/noscript?$qs",
                        height      => 300,
                        width       => 500,
                        frameborder => 0
                    }
                ),
                $h->textarea(
                    {
                        name => 'recaptcha_challenge_field',
                        rows => 3,
                        cols => 40
                    }
                ),
                $h->input(
                    {
                        type  => 'hidden',
                        name  => 'recaptcha_response_field',
                        value => 'manual_challenge'
                    }
                )
            ]
        ),
        "\n"
    );
}

sub _post_request {
    my $self = shift;
    my ( $url, $args ) = @_;

    my $ua = LWP::UserAgent->new();
    return $ua->post( $url, $args );
}

sub check_answer {
    my $self = shift;
    my ( $privkey, $remoteip, $challenge, $response ) = @_;

    croak
      "To use reCAPTCHA you must get an API key from http://recaptcha.net/api/getkey"
      unless $privkey;

    croak "For security reasons, you must pass the remote ip to reCAPTCHA"
      unless $remoteip;

    return { is_valid => 0, error => 'incorrect-captcha-sol' }
      unless $challenge && $response;

    my $resp = $self->_post_request(
        API_VERIFY_SERVER . '/verify',
        {
            privatekey => $privkey,
            remoteip   => $remoteip,
            challenge  => $challenge,
            response   => $response
        }
    );

    if ( $resp->is_success ) {
        my ( $answer, $message ) = split( /\n/, $resp->content, 2 );
        if ( $answer =~ /true/ ) {
            return { is_valid => 1 };
        }
        else {
            chomp $message;
            return { is_valid => 0, error => $message };
        }
    }
    else {
        return { is_valid => 0, error => SERVER_ERROR };
    }
}

1;
__END__

=head1 NAME

Captcha::reCAPTCHA - A Perl implementation of the reCAPTCHA API

=head1 VERSION

This document describes Captcha::reCAPTCHA version 0.92

=head1 SYNOPSIS

    use Captcha::reCAPTCHA;

    my $c = Captcha::reCAPTCHA->new;

    # Output form
    print $c->get_html( 'your public key here' );

    # Verify submission
    my $result = $c->check_answer(
        'your private key here', $ENV{'REMOTE_ADDR'},
        $challenge, $response
    );

    if ( $result->{is_valid} ) {
        print "Yes!";
    }
    else {
        # Error
        $error = $result->{error};
    }

For complete examples see the /examples subdirectory

=head1 DESCRIPTION

reCAPTCHA is a hybrid mechanical turk and captcha that allows visitors
who complete the captcha to assist in the digitization of books.

From L<http://recaptcha.net/learnmore.html>:

    reCAPTCHA improves the process of digitizing books by sending words that
    cannot be read by computers to the Web in the form of CAPTCHAs for
    humans to decipher. More specifically, each word that cannot be read
    correctly by OCR is placed on an image and used as a CAPTCHA. This is
    possible because most OCR programs alert you when a word cannot be read
    correctly.

This Perl implementation is modelled on the PHP interface that can be
found here:

L<http://recaptcha.net/plugins/php/>

To use reCAPTCHA you need to register your site here:

L<https://admin.recaptcha.net/recaptcha/createsite/>

=head1 INTERFACE

=over

=item C<< new >>

Create a new C<< Captcha::reCAPTCHA >>.

=item C<< get_html( $pubkey, $error, $use_ssl, $options ) >>

Generates HTML to display the captcha.

    print $captcha->get_html( $PUB, $err );

=over

=item C<< $pubkey >>

Your reCAPTCHA public key, from the API Signup Page

=item C<< $error >>

Optional. If set this should be either a string containing a reCAPTCHA
status code or a result hash as returned by C<< check_answer >>.

=item C<< $use_ssl >>

Optional. Should the SSL-based API be used? If you are displaying a page
to the user over SSL, be sure to set this to true so an error dialog
doesn't come up in the user's browser.

=item C<< $options >>

Optional. A reference to a hash of options for the captcha. See 
C<< get_options_setter >> for more details.

=back

Returns a string containing the HTML that should be used to display
the captcha.

=item C<< get_options_setter( $options ) >>

You can optionally customize the look of the reCAPTCHA widget with some
JavaScript settings. C<get_options_setter> returns a block of Javascript
wrapped in <script> .. </script> tags that will set the options to be used
by the widget.

C<$options> is a reference to a hash that may contain the following keys:

=over

=item C<theme>

Defines which theme to use for reCAPTCHA. Possible values are 'red',
'white' or 'blackglass'. The default is 'red'.

=item C<tabindex>

Sets a tabindex for the reCAPTCHA text box. If other elements in the
form use a tabindex, this should be set so that navigation is easier for
the user. Default: 0.

=back

=item C<< check_answer >>

After the user has filled out the HTML form, including their answer for
the CAPTCHA, use C<< check_answer >> to check their answer when they
submit the form. The user's answer will be in two form fields,
recaptcha_challenge_field and recaptcha_response_field. The reCAPTCHA
library will make an HTTP request to the reCAPTCHA server and verify the
user's answer.

=over

=item C<< $privkey >>

Your reCAPTCHA private key, from the API Signup Page.

=item C<< $remoteip >>

The user's IP address, in the format 192.168.0.1.

=item C<< $challenge >>

The value of the form field recaptcha_challenge_field

=item C<< $response >>

The value of the form field recaptcha_response_field.

=back

Returns a reference to a hash containing two fields: C<is_valid>
and C<error>.

    my $result = $c->check_answer(
        'your private key here', $ENV{'REMOTE_ADDR'},
        $challenge, $response
    );

    if ( $result->{is_valid} ) {
        print "Yes!";
    }
    else {
        # Error
        $error = $result->{error};
    }

See the /examples subdirectory for examples of how to call C<check_answer>.

=back

=head1 CONFIGURATION AND ENVIRONMENT

Captcha::reCAPTCHA requires no configuration files or environment
variables.

To use reCAPTCHA sign up for a key pair here:

L<https://admin.recaptcha.net/recaptcha/createsite/>

=head1 DEPENDENCIES

LWP::UserAgent,
HTML::Tiny

=head1 INCOMPATIBILITIES

None reported .

=head1 BUGS AND LIMITATIONS

No bugs have been reported.

Please report any bugs or feature requests to
C<bug-captcha-recaptcha@rt.cpan.org>, or through the web interface at
L<http://rt.cpan.org>.

=head1 AUTHOR

Andy Armstrong  C<< <andy@hexten.net> >>

=head1 LICENCE AND COPYRIGHT

Copyright (c) 2007, Andy Armstrong C<< <andy@hexten.net> >>. All rights reserved.

This module is free software; you can redistribute it and/or
modify it under the same terms as Perl itself. See L<perlartistic>.

=head1 DISCLAIMER OF WARRANTY

BECAUSE THIS SOFTWARE IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY
FOR THE SOFTWARE, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES
PROVIDE THE SOFTWARE "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE
ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE SOFTWARE IS WITH
YOU. SHOULD THE SOFTWARE PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL
NECESSARY SERVICING, REPAIR, OR CORRECTION.

IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING
WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR
REDISTRIBUTE THE SOFTWARE AS PERMITTED BY THE ABOVE LICENCE, BE
LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL,
OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE
THE SOFTWARE (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING
RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A
FAILURE OF THE SOFTWARE TO OPERATE WITH ANY OTHER SOFTWARE), EVEN IF
SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGES.
