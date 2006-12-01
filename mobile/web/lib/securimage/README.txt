Securimage PHP Class
Generates and manages CAPTCHA images
 to ensure humans are filling out your forms.
Author: Drew Phillips
Copyright 2005 Drew Phillips
Website: www.neoprogrammers.com


This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

-------------------------------------------------------------------------------

Requirements:
  PHP >= 4.3.0
  GD  >= 2.0.28
  FreeType (http://www.freetype.org/)
  Jpeg Support (ftp://ftp.uu.net/graphics/jpeg/)
  Png Support (http://www.libpng.org/pub/png/libpng.html)

  Windows users uncomment the extension php_gd2.dll from php.ini

  If you run this script and see any php errors saying "call to undefined
  function 'functionname'" it means one or more of the above libraries
  are not installed.  You should contact your host and have them install
  the necessary libraries, or consult the php manual (php.net) for directions
  on installing them yourself.


Installation:

  You should make a dedicated directory for the various securimage files
  and ttf fonts, so everything is together and organized.

  Edit the securimage.php class file to the desired image settings.

  Call securimage_show.php from your browser to make sure it works.

  Test form.php and see if the data directory is set up properly.

  Modify the form for your own needs.


Included Files:
  securimage.php
   - The class file that defines the securimage type.

  securimage_show.php
   - Sample file to output a new image and store code data.

  securimage_prune.php
   - File to delete old codes from data directory.

  form.php
   - Very simple example form showing how to output image
     and verify the data entered.

  advanced_form.php
   - A more complex real world form using securimage.

  elephant.ttf
   - A TTF font to use for the image.

  image_data/
   - The default folder for storing generated codes.

  sample_images/
   - A few example images that were created with securimage.

  README.txt
   - This file you are reading now.


Public Properties:

  int image_width
    The width in pixels of the generated image

  int image_height
    The height in pixels of the generated image

  int code_length
    The number of characters to be generated as
    the text the user must verify

  string ttf_file
    The absolute path of the ttf font file to be used
    to draw the text onto the image

  int font_size
    The size of the text to be drawn.
    Note: This can vary widely between fonts.  It is
    recommended you expirement with this value after
    you choose your desired font.

  int text_angle_minimum
    The characters in the image can be drawn at an angle
    to attempt to make the text more difficult to read for
    computer programs.  A random value between this minimum
    value and the maximum value will be chosen for each character.
    This value specifies the angle (leaning counter-clockwise)
    for the text to be drawn.
    I.E. a value of -90 would cause the letter to be drawn on
    its right side (or your left side).

  int text_angle_maximum
    The maximum angle for the characters to be drawn at.
  
  int text_x_start
    The pixel location along the x-axis of the image, where
    the leftmost point on the character will be drawn.

  int text_minimum_distance
    Characters will be randomly spaced out from eachother.
    This value specifies the smallest distance in pixels 
    for the characters to be spaced out.
    Note: If this value is too small, letters will overlap
    eachother.  This should be expirimented with as it varies
    between fonts.

  int text_maximum_distance
    The largest gap in pixels for letters to be spaced at.

  array image_bg_color
    This is the background color of the image to be drawn if
    no background image is used.
    The colors should be entered as decimal values from 0-255,
    or hexadecimal values from 0x00 to 0xFF.
    Specify the value for each RGB value.
    All 0's would indicate black, while all 1's would indicate
    white.

  array text_color
    This is the color of the text that will be drawn.

  boolean shadow_text
    Set this value to TRUE to give a "shadow" effect of the text.
    Set to FALSE to use normal text.

  boolean use_transparent_text
    Set to true to allow for transparency in the text.

  int text_transparency_percentage
    A value of 0 to 100 dictating the transparency level of the
    text.  0 indicates completely opaque text, while 100 indicates
    completely transparent (invisible) text.
    This only applies if use_transparent_text is TRUE.

  boolean draw_lines
    Set to true if you wish to draw horizontal and vertical lines
    on the image to help confuse programs reading text.

  boolean draw_angled_lines
    Set to true to have lines at -90 and 90 degree angles.

  array line_color
    The color to be used when drawing lines over the image.
    Only applies if draw_lines or draw_angled_lines is true.

  int line_distance
    The distance in pixels for each line drawn,to be separated
    from eachother.
    Only applies if draw_lines or draw_angled_lines is true.

  boolean draw_lines_over_text
    Set this value to TRUE to have the lines drawn on top of 
    the generated text, otherwise the text will be drawn
    over the lines.
    Only applies if draw_lines or draw_angled_lines is true.

  string data_directory
    This is the folder in which the encrypted security codes
    will be stored.  This folder must be writable by PHP and
    should be specified as an absolute path of the root, or
    relative to the to the securimage class file.

  int prune_minimum_age
    The minimum time in minutes for data files to be considered
    old and eligable for deletion by the script.

  string hash_salt
    This value should be changed from its default to a random 
    string of characters.  It is used to provide added security
    to outsiders discovering which data file maps to which IP.
    The generated codes will also be encrypted based on this
    value.
  

Public Functions:

  Constructor: securimage()
    Creates a new securimage object.

  void show( [string background_image] )
    Outputs a JPEG image following your 
    specifications as defined in the class file.
    An optional background_image can be specified 
    and will be used when drawing the image.  If your PHP
    installation supports it, it can be a URL, or just a path.

  void prune()
    Deletes old files containing encrypted codes.
    Files deleted will be older than those specified by the 
    prune_minimum_age directive.

  boolean check( string code )
    This function is used to check a user input to the correct
    value generated by the program.  The string 'code' is the
    case insensitive value the user input.
    Returns TRUE in the case it was the proper code, or false
    if the code entered was incorrect.


Example Usage:

  To create a new image.
    <?php
    require_once "securimage.php";  //Bring in the class code
    $image = new securimage();      //Create a new instance
    $image->show();                 //Output the image to the browser
    ?>

  The images are output as JPEG images, and a Content-Type header
  or image/jpeg is sent to the browser.  This means an image should
  be generated by its own script and called via an image tag similar
  to <img src="securimage_show.php">
  Example code is provided with the package.

  To create a new image with a background
    <?php
    require_once "securimage.php";
    $image = new securimage();
    $image->show("http://www.yoursite.com/images/bg.jpg");
    //the above could also be
    //$image->show("/home/yoursite.com/public_html/images/bg.jpg");
    //which would actually be faster since php wont make an http request
    ?>
  

  To check a code entered by a user.  This example assumes data
  is sent via HTTP POST and the field name is image_code.

    <?php
    require_once "securimage.php";
    $image = new securimage();
    $correct = $image->check($_POST['image_code']);

    if ($correct == TRUE) {
      echo "Thank you.  The code entered was correct.";
    } else {
      echo "Sorry, you entered the wrong code.";
    }
    ?>

  
  To delete old files from forms never submitted or users that
  never entered the right code.

    <?php
    require "securimage.php";
    $image = new securimage(;
    $image->prune();
    ?>

  This script is not 100% necessary unless you have a large 
  number of users using your form daily.  Each generated file
  will only be 32 bytes in size, plus the minimum size of a file
  on disk (usually 4kb).  This could be run via a cron job on unix,
  or using the windows task scheduler, or even called by hand by a 
  person.


  If you find this script useful and it helped you out, please take
  a quick moment to rate it at hotscripts.com.  Just go to 
  http://www.hotscripts.com/rate/49400.html and cast your vote.
  Thank you very much.
  Drew
