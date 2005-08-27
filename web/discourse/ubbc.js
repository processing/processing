function submitproc() {
	if (window.submitted) return false;
	window.submitted = true;
	return true;
}

function storeCaret(text) { 
	if (text.createTextRange) text.caretPos = document.selection.createRange().duplicate();
}

function AddText(text) {
	if (document.postmodify.message.caretPos) document.postmodify.message.caretPos.text=text;
	else document.postmodify.message.value+=text;
	document.postmodify.message.focus();
}

function AddSelText(bbopen, bbclose) {
	if (document.postmodify.message.caretPos) document.postmodify.message.caretPos.text = bbopen + document.postmodify.message.caretPos.text + bbclose;
	else document.postmodify.message.value += bbopen + bbclose;
	document.postmodify.message.focus()
}

function hr() {
	AddText("[hr]");
}

function size() {
	AddSelText("[size=2]","[/size]");
}

function font() {
	AddSelText("[font=Verdana]","[/font]");
}

function teletype() {
	AddSelText("[tt]","[/tt]");
}

function right() {
	AddSelText("[right]","[/right]");
}

function left() {
	AddSelText("[left]","[/left]");
}

function superscript() {
	AddSelText("[sup]","[/sup]");
}

function subscript() {
	AddSelText("[sub]","[/sub]");
}

function image() {
	AddSelText("[img]","[/img]");
}

function ftp() {
	AddSelText("[ftp]","[/ftp]");
}

function move() {
	AddSelText("[move]","[/move]");
}

function shadow() {
	AddSelText("[shadow=red,left,300]","[/shadow]");
}

function glow() {
	AddSelText("[glow=red,2,300]","[/glow]");
}

function flash() {
	AddSelText("[flash=200,200]","[/flash]");
}

function pre() {
	AddSelText("[pre]","[/pre]");
}

function tcol() {
	AddSelText("[td]","[/td]");
}

function trow() {
	AddSelText("[tr]","[/tr]");
}

function table() {
	AddSelText("[table][tr][td]", "[/td][/tr][/table]");
}

function strike() {
	AddSelText("[s]","[/s]");
}

function underline() {
	AddSelText("[u]","[/u]");
}

function emai1() {
	AddSelText("[email]","[/email]");
}

function bold() {
	AddSelText("[b]","[/b]");
}

function italicize() {
	AddSelText("[i]","[/i]");
}

function quote() {
	AddSelText("[quote]","[/quote]");
}

function center() {
	AddSelText("[center]","[/center]");
}

function hyperlink() {
	AddSelText("[url]","[/url]");
}

function showcode() {
	AddSelText("[code]","[/code]");
}

function list() {
	AddSelText("[list][*]", "\n[/list]");
}

function showcolor(color) {
	AddSelText("[color="+color+"]","[/color]");
}

function smiley() {
	AddText(" :)");
}

function wink() {
	AddText(" ;)");
}

function cheesy() {
	AddText(" :D");
}

function grin() {
	AddText(" ;D");
}

function angry() {
	AddText(" >:(");
}

function sad() {
	AddText(" :(");
}

function shocked() {
	AddText(" :o");
}

function cool() {
	AddText(" 8)");
}

function huh() {
	AddText(" ???");
}

function rolleyes() {
	AddText(" ::)");
}

function tongue() {
	AddText(" :P");
}

function lipsrsealed() {
	AddText(" :-X");
}

function embarassed() {
	AddText(" :-[");
}

function undecided() {
	AddText(" :-/");
}

function kiss() {
	AddText(" :-*");
}

function cry() {
	AddText(" :'(");
}
