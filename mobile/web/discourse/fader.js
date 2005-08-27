/********************************************************************
  allbrowsers_fader.mod
  v2.02 01/12/2002
  YaBB integration & modification: NRg (yabb_fader@gmx.de)
  Latest version: http://www.talknet.de/~norbru/

  based upon:
  Fading Scroller by DynamicDrive.com  http://www.dynamicdrive.com
*********************************************************************/

// You can changes these three values to modify the output:
var frame = 50;	// frame-range. standard = 50
var st = 30;	// milliseconds between frames. standard = 30
var wait = 50;	// value to display the full text without fade
			// 1 = approx. 60 milliseconds. standard = 50

ie4 = document.all&&!document.getElementById;
ns4 = document.layers;
DOM2 = document.getElementById;

bR = HexToR(bcolor);
bG = HexToG(bcolor);
bB = HexToB(bcolor);
tR = HexToR(tcolor);
tG = HexToG(tcolor);
tB = HexToB(tcolor);
bR_m = bR;
bG_m = bG;
bB_m = bB;
tR_m = tR;
tG_m = tG;
tB_m = tB;

function HexToR(h) { return parseInt((cutHex(h)).substring(0,2),16) }
function HexToG(h) { return parseInt((cutHex(h)).substring(2,4),16) }
function HexToB(h) { return parseInt((cutHex(h)).substring(4,6),16) }
function cutHex(h) { return (h.charAt(0)=="#") ? h.substring(1,7) : h}

dir = ((tR+tG+tB) > (bR+bG+bB)) ? "up" : "down";
dirback = ((tR+tG+tB) < (bR+bG+bB)) ? "up" : "down";
dir_m = dir;
index = 0;
frame_m = frame;
framehalf = frame / 2;
wait_m = wait;
stepR = Math.abs(tR - bR) / framehalf;
stepG = Math.abs(tG - bG) / framehalf;
stepB = Math.abs(tB - bB) / framehalf;
step = Math.min(Math.round(Math.max(stepR,Math.max(stepG,stepB))),(240/framehalf));

function fade() {
 if (index>=fcontent.length)
  index = 0;
  if (DOM2) {
   document.getElementById("fscroller").innerHTML=begintag+fcontent[index]+closetag;
   index++;
   colorfade();
  }
  else if (ie4) {
   document.all.fscroller.innerHTML=begintag+fcontent[index]+closetag;
   index++;
   setTimeout("fade()",Math.max(delay,2500));
  }
  else if (ns4) {
   document.fscrollerns.document.fscrollerns_sub.document.write(begintag+fcontent[index]+closetag);
   document.fscrollerns.document.fscrollerns_sub.document.close();
   index++;
   setTimeout("fade()",Math.max(delay,2500));
  }
}

function colorfade() {
 if (frame>0) {
  if (frame==framehalf && wait>0) {
   document.getElementById("fscroller").style.color="rgb("+wR+","+wG+","+wB+")";
   wait--;
   setTimeout("colorfade()",50);
  }
  else {
   if (dir=="down") {
    if (bR>tR) bR-=step;
    if (bG>tG) bG-=step;
    if (bB>tB) bB-=step;
    bR = Math.max(bR,1);
    bG = Math.max(bG,1);
    bB = Math.max(bB,1);
    }
   else {
    if (bR<tR) bR+=step;
    if (bG<tG) bG+=step;
    if (bB<tB) bB+=step;
    bR = Math.min(bR,255);
    bG = Math.min(bG,255);
    bB = Math.min(bB,255);
   }
   document.getElementById("fscroller").style.color="rgb("+bR+","+bG+","+bB+")";
   if (frame==framehalf+1) {
    document.getElementById("fscroller").style.color="rgb("+tR+","+tG+","+tB+")";
    dir = dirback;
    wR = tR;
    wG = tG;
    wB = tB;
    tR = bR_m;
    tG = bG_m;
    tB = bB_m;
   }
   frame--;
   setTimeout("colorfade()",st);
  }
 }
 else {
  document.getElementById("fscroller").innerHTML=" ";
  dir = dir_m;
  frame = frame_m;
  wait = wait_m;
  tR = tR_m;
  tG = tG_m;
  tB = tB_m;
  bR = bR_m;
  bG = bG_m;
  bB = bB_m;
  setTimeout("fade()",delay);
 }
}