<?php

/*********************************
Securimage 0.3
Portable Security Image Script
Author: Drew Phillips
www.neoprogrammers.com
Copyright 2005 Drew Phillips

If you found this script useful, please take a quick moment to
rate it.  http://www.hotscripts.com/rate/49400.html  Thanks.

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

********************************/

class securimage {
  //USER CONFIGURATION OF IMAGE
  //See included README.txt for detailed descriptions

  var $image_width = 200;
  //heigh of security image

  var $image_height = 40;
  //width of security image

  var $code_length = 5;
  //how many letters in the code

  var $ttf_file = "elephant.ttf";
  //path to ttf font to use

  var $font_size = 20;
  //size of the font

  var $text_angle_minimum = -20;
  //minimum angle in degress of letter. counter-clockwise direction

  var $text_angle_maximum = 20;
  //maximum angle in degrees of letter. clockwise direction

  var $text_x_start = 9;
  //position (in pixels) on the x axis where text starts

  var $text_minimum_distance = 30;
  //the shortest distance in pixels letters can be from eachother (a very small value will cause overlapping)

  var $text_maximum_distance = 33;
  //the longest distance in pixels letters can be from eachother

  var $image_bg_color = array("red" => 0xee, "green" => 0xee, "blue" => 0xee);
  //images background color.  set each red, green, and blue to a 0-255 value

  var $text_color = array("red" => 0xdd, "green" => 0x66, "blue" => 0);
  //the color of the text

  var $shadow_text = false;
  //draw a shadow for the text (gives a 3d raised bolder effect)

  var $use_transparent_text = true;
  //true for the ability to use transparent text, false for normal text

  var $text_transparency_percentage = 15;
  //0 to 100, 0 being completely opaque, 100 being completely transparent

  var $draw_lines = TRUE;
  //set to true to draw horizontal and vertical lines on the image
  //the following 3 options will have no effect if this is set to false

  var $line_color = array("red" => 0xe0, "green" => 0xe0, "blue" => 0xe0);
  //color of the horizontal and vertical lines through the image

  var $line_distance = 15;
  //distance in pixels the lines will be from eachother.
  //the smaller the value, the more "cramped" the lines will be, potentially making
  //the text harder to read for people

  var $draw_angled_lines = TRUE;
  //set to true to draw lines at 45 and -45 degree angles over the image  (makes x's)

  var $draw_lines_over_text = true;
  //set to true to draw the lines on top of the text, otherwise the text will be on the lines

  var $data_directory = "./image_data";
  //path of directory to store code data in.  make sure this is either outside of the webroot or unreadable

  var $prune_minimum_age = 15;
  //age (in minutes) of files containing unused codes to be deleted

  var $hash_salt = "fg7hg3yg3fd90oi4i";
  //set this to a unique string, this prevents users guessing filenames and make data more secure

 
  //END USER CONFIGURATION
  //There should be no need to edit below unless you really know what you are doing



  var $im;
  var $bgimg;
  var $code;
  var $code_entered;
  var $correct_code;  

  function show($background_image = "")
  {
    if($background_image != "" && is_readable($background_image)) {
      $this->bgimg = $background_image;
    }

    $this->doImage();
  }

  function prune()
  {
    $this->pruneOld();
  }

  function check($code)
  {
    $this->code_entered = $code;
    $this->validate();
    return $this->correct_code;
  }

  function doImage()
  {
    if($this->use_transparent_text == TRUE || $this->bgimg != "") {
      $this->im = imagecreatetruecolor($this->image_width, $this->image_height);
      $bgcolor = imagecolorallocate($this->im, $this->image_bg_color['red'], $this->image_bg_color['green'], $this->image_bg_color['blue']);
      imagefilledrectangle($this->im, 0, 0, imagesx($this->im), imagesy($this->im), $bgcolor);
    } else { //no transparency
      $this->im = imagecreate($this->image_width, $this->image_height);
      $bgcolor = imagecolorallocate($this->im, $this->image_bg_color['red'], $this->image_bg_color['green'], $this->image_bg_color['blue']);
    }

    if($this->bgimg != "") { $this->setBackground(); }

    $this->code = $this->generateCode($this->code_length);

    if (!$this->draw_lines_over_text && $this->draw_lines) $this->drawLines();

    $this->drawWord();
    
    if ($this->draw_lines_over_text && $this->draw_lines) $this->drawLines();
    
    $this->saveData();
    $this->output();

  }

  function setBackground()
  {
    $dat = @getimagesize($this->bgimg);
    if($dat == FALSE) { return; }

    switch($dat[2]) {
      case 1: $newim = @imagecreatefromgif($this->bgimg); break;
      case 2: $newim = @imagecreatefromjpeg($this->bgimg); break;
      case 3: $newim = @imagecreatefrompng($this->bgimg); break;
      case 15: $newim = @imagecreatefromwbmp($this->bgimg); break;
      case 16: $newim = @imagecreatefromxbm($this->bgimg); break;
      default: return;
    }

    if(!$newim) return;

    imagecopy($this->im, $newim, 0, 0, 0, 0, $this->image_width, $this->image_height);
  }

  function drawLines()
  {
    $linecolor = imagecolorallocate($this->im, $this->line_color['red'], $this->line_color['green'], $this->line_color['blue']);

    //vertical lines
    for($x = 1; $x < $this->image_width; $x += $this->line_distance) {
      imageline($this->im, $x, 0, $x, $this->image_height, $linecolor);
    }

    //horizontal lines
    for($y = 11; $y < $this->image_height; $y += $this->line_distance) {
      imageline($this->im, 0, $y, $this->image_width, $y, $linecolor);
    }

    if ($this->draw_angled_lines == TRUE) {
      for ($x = -($this->image_height); $x < $this->image_width; $x += $this->line_distance) {
        imageline($this->im, $x, 0, $x + $this->image_height, $this->image_height, $linecolor);
      }

      for ($x = $this->image_width + $this->image_height; $x > 0; $x -= $this->line_distance) {
        imageline($this->im, $x, 0, $x - $this->image_height, $this->image_height, $linecolor);
      }
    }
  }

  function drawWord()
  {
    if($this->use_transparent_text == TRUE) {
      $alpha = floor($this->text_transparency_percentage / 100 * 127);
      $font_color = imagecolorallocatealpha($this->im, $this->text_color['red'], $this->text_color['green'], $this->text_color['blue'], $alpha);
    } else { //no transparency
      $font_color = imagecolorallocate($this->im, $this->text_color['red'], $this->text_color['green'], $this->text_color['blue']);
    }

    $x = $this->text_x_start;
    $strlen = strlen($this->code);
    $y_min = ($this->image_height / 2) + ($this->font_size / 2) - 2;
    $y_max = ($this->image_height / 2) + ($this->font_size / 2) + 2;
    for($i = 0; $i < $strlen; ++$i) {
      $angle = rand($this->text_angle_minimum, $this->text_angle_maximum);
      $y = rand($y_min, $y_max);
      imagettftext($this->im, $this->font_size, $angle, $x, $y, $font_color, $this->ttf_file, $this->code{$i});
      if($this->shadow_text == TRUE) {
        imagettftext($this->im, $this->font_size, $angle, $x + 2, $y + 2, $font_color, $this->ttf_file, $this->code{$i});
      }
      $x += rand($this->text_minimum_distance, $this->text_maximum_distance);
    }
  }



  function generateCode($len)
  {
    $code = "";
    for($i = 1; $i <= $len; ++$i) {
      $code .= chr(rand(65, 90));
    }
    return $code;
  }

  function output()
  {
    header("Expires: Sun, 1 Jan 2000 12:00:00 GMT");
    header("Last-Modified: " . gmdate("D, d M Y H:i:s") . "GMT");
    header("Cache-Control: no-store, no-cache, must-revalidate");
    header("Cache-Control: post-check=0, pre-check=0", false);
    header("Pragma: no-cache");
    header("Content-Type: image/jpeg");
    imagejpeg($this->im);
    imagedestroy($this->im);
  }

  function saveData()
  {
    $filename = md5($this->hash_salt . $_SERVER['REMOTE_ADDR']);
    $fp = fopen($this->data_directory . "/" . $filename, "w+");
    fwrite($fp, md5( $this->hash_salt . strtolower($this->code) )  );
    fclose($fp);
  }

  function validate()
  {
    $filename = md5($this->hash_salt . $_SERVER['REMOTE_ADDR']);

    $enced_code = trim(@file_get_contents($this->data_directory . "/" . $filename));

    $check = md5($this->hash_salt . strtolower($this->code_entered));

    if($check == $enced_code) {
      $this->correct_code = TRUE;
      @unlink($this->data_directory . "/" . $filename);
    } else {
      $this->correct_code = FALSE;
    }
  }

  function checkCode()
  {
    return $this->correct_code;
  }

  function pruneOld()
  {
    if ($handle = @opendir($this->data_directory)) {
      while (($filename = readdir($handle)) !== false) {
        if(time() - filemtime($this->data_directory . "/" . $filename) > $this->prune_minimum_age * 60) {
          @unlink($this->data_directory . "/" . $filename);
        } 
      }
      closedir($handle);
    }
  }

} //end class

?>
