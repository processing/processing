<?php  if (!defined('PmWiki')) exit();
/*  
    Copyright 2003,2004 Nils Knappmeier (nk@knappi.org)
    Copyright 2004-2005 Patrick R. Michaud (pmichaud@pobox.com)

    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This file implements a diff function in native PHP.  It is based
    upon the PHPDiffEngine code written by Nils Knappmeier, who in turn
    had used Daniel Unterberger's diff 
    (http://www.holomind.de/phpnet/diff.php) as the basis for his code.

    Pm's revision of Nils' code simply attempts to streamline it 
    for speed (eliminate function calls and unnecessary string ops)
    and place everything into a single file.

*/

## PHPDiff returns the differences between $old and $new, formatted
## in the standard diff(1) output format.
function PHPDiff($old,$new) 
{
   # split the source text into arrays of lines
   $t1 = explode("\n",$old);
   $x=array_pop($t1); 
   if ($x>'') $t1[]="$x\n\\ No newline at end of file";
   $t2 = explode("\n",$new);
   $x=array_pop($t2); 
   if ($x>'') $t2[]="$x\n\\ No newline at end of file";

   # build a reverse-index array using the line as key and line number as value
   # don't store blank lines, so they won't be targets of the shortest distance
   # search
   foreach($t1 as $i=>$x) if ($x>'') $r1[$x][]=$i;
   foreach($t2 as $i=>$x) if ($x>'') $r2[$x][]=$i;

   $a1=0; $a2=0;   # start at beginning of each list
   $actions=array();

   # walk this loop until we reach the end of one of the lists
   while ($a1<count($t1) && $a2<count($t2)) {
     # if we have a common element, save it and go to the next
     if ($t1[$a1]==$t2[$a2]) { $actions[]=4; $a1++; $a2++; continue; } 

     # otherwise, find the shortest move (Manhattan-distance) from the
     # current location
     $best1=count($t1); $best2=count($t2);
     $s1=$a1; $s2=$a2;
     while(($s1+$s2-$a1-$a2) < ($best1+$best2-$a1-$a2)) {
       $d=-1;
       foreach((array)@$r1[$t2[$s2]] as $n) 
         if ($n>=$s1) { $d=$n; break; }
       if ($d>=$s1 && ($d+$s2-$a1-$a2)<($best1+$best2-$a1-$a2))
         { $best1=$d; $best2=$s2; }
       $d=-1;
       foreach((array)@$r2[$t1[$s1]] as $n) 
         if ($n>=$s2) { $d=$n; break; }
       if ($d>=$s2 && ($s1+$d-$a1-$a2)<($best1+$best2-$a1-$a2))
         { $best1=$s1; $best2=$d; }
       $s1++; $s2++;
     }
     while ($a1<$best1) { $actions[]=1; $a1++; }  # deleted elements
     while ($a2<$best2) { $actions[]=2; $a2++; }  # added elements
  }

  # we've reached the end of one list, now walk to the end of the other
  while($a1<count($t1)) { $actions[]=1; $a1++; }  # deleted elements
  while($a2<count($t2)) { $actions[]=2; $a2++; }  # added elements

  # and this marks our ending point
  $actions[]=8;

  # now, let's follow the path we just took and report the added/deleted
  # elements into $out.
  $op = 0;
  $x0=$x1=0; $y0=$y1=0;
  $out = array();
  foreach($actions as $act) {
    if ($act==1) { $op|=$act; $x1++; continue; }
    if ($act==2) { $op|=$act; $y1++; continue; }
    if ($op>0) {
      $xstr = ($x1==($x0+1)) ? $x1 : ($x0+1).",$x1";
      $ystr = ($y1==($y0+1)) ? $y1 : ($y0+1).",$y1";
      if ($op==1) $out[] = "{$xstr}d{$y1}";
      elseif ($op==3) $out[] = "{$xstr}c{$ystr}";
      while ($x0<$x1) { $out[] = '< '.$t1[$x0]; $x0++; }   # deleted elems
      if ($op==2) $out[] = "{$x1}a{$ystr}";
      elseif ($op==3) $out[] = '---';
      while ($y0<$y1) { $out[] = '> '.$t2[$y0]; $y0++; }   # added elems
    }
    $x1++; $x0=$x1;
    $y1++; $y0=$y1;
    $op=0;
  }
  $out[] = '';
  return join("\n",$out);
}

$DiffFunction = 'PHPDiff';

