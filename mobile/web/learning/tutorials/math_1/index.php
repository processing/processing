<?php

$PAGE_TITLE = "Math 1 &raquo; Tutorials &raquo; Mobile Processing";

require '../../../header.inc.php';
?>
<b>Math 1:</b> Integer and Fixed Point Arithmetic<br>
<br>
<br>
Mobile Processing sketches are compatible with the first release of the Java 2 Micro Edition (J2ME) platform for mobile phones, called the Connected Limited Device Configuration (CLDC) 1.0. One of the biggest differences between CLDC 1.0 and desktop Java is the lack of floating point variable support, also known as the <b>float</b> data type.<br>
<br>
<br>
<b>Integer Arithmetic</b><br>
<br>
All arithmetic operations return integer values, throwing away any fractional parts. As a result, the order of evaluation becomes important in writing expressions. For example, the following two statements generate different results:<br>
<br>
<pre>int result1 = 2 * 11 / 2;    // result1 = 11;
int result2 = 2 * (11 / 2);  // result2 = 10;
</pre>
Without parentheses, the order of evaluation for the first statement is from left to right, first multiplying 2 * 11 to get 22, then dividing by 2 to get 11. In the second statement, the division 11 / 2 occurs first, throwing away the fractional part, to get 5, which is multiplied by 2 to get 10.<br>
<br>
<br>
<b>Fixed Point Arithmetic</b><br>
<br>
However, even without a floating point data type, it is possible to represent fractional values using integer variables.  To do so, we define the number of digits in a number which we want to use to represent the fractional part.  For example, in base 10, we can decide that we want 4 digits for the fractional part, so we can represent the fractional number 3.1415 as 31415 and store it in an integer variable. Similarly, the whole number 25 is represented as 250000 by this definition.<br>
<br>
In base 10, we call the position where we separate the integer from the fraction the decimal point. This position is known more generically as the radix point. It is the definition of this point in the representation that gives us the name <b>fixed point</b>.<br>
<br>
Once we have decided on the position of the radix point, we can perform arithmetic operations on fixed point values using integer arithmetic. Note that addition and subtraction requires no special handling:<br>
<br>
<pre>25.0 + 3.1415 &equiv; 250000 + 31415 = 281415 &equiv; 28.1415</pre>
If we multiply two fixed point values, we get a result that has twice the number of digits for the fractional part, so we truncate the result to remove the extra digits:<br>
<br>
<pre>2.0 * 3.1415 &equiv; 20000 * 31415 = 628300000 (truncated to:) 62830 &equiv; 6.2830</pre>
To understand why, consider a fractional representation of the arithmetic:<br>
<br>
<pre>
               20000   31415   628300000   62830
2.0 * 3.1415 = ----- * ----- = --------- = ----- = 6.2830
               10000   10000   100000000   10000
</pre>
If we divide one fixed point value into another, we will get a result that has too few digits for the fractional part, so we use double the number of digits in the fraction for the number being divided (the dividend):<br>
<br>
<pre>6.2830 / 3.1415 &equiv; 628300000 / 31415 = 20000 &equiv; 2</pre>
Again, consider a fractional representation of the arithmetic:<br>
<br>
<pre>
                  628300000   31415   628300000   10000   628300000     1     20000
6.2830 / 3.1415 = --------- / ----- = --------- * ----- = --------- * ----- = ----- = 2
                  100000000   10000   100000000   31415     31415     10000   10000
</pre>
Note that division can be problematic if adding the extra digits to the dividend makes the resulting number too big to fit into a variable.<br>
<br>
There are two shortcuts involving fixed point arithmetic and whole numbers.  First, in the multiplication example above, notice that we're just adding zeros to 2.0 to form its fixed point representation, then removing the same number of zeros to adjust the result. When multiplying a fixed point value by a whole number, you can multiply them directly using integer arithmetic and not have to do any extra handling:<br>
<br>
<pre>2 * 3.1415 &equiv; 2 * 31415 = 62830 &equiv; 6.2830</pre>
Second, a similar situation occurs with division when the <b>divisor (not the dividend)</b> is a whole number.  If the divisor is a whole number, the zeros added for its fixed point representation cancels out the extra zeros added to the dividend. So, you can divide a fixed point value by a whole number directly:<br>
<br>
<pre>6.2830 / 2 &equiv; 62830 / 2 = 31415 &equiv; 3.1415</pre>
Other than these two shortcuts, in general you should <b>never mix fixed point values and normal integer values</b> in the same expression.<br>
<br>
<br>
<b>Mobile Processing and Fixed Point Arithmetic</b><br>
<br>
Fixed point values and arithmetic can be used to deal with fractional numbers in Mobile Processing. Some system variables and functions have been defined to assist you, but you still need to be careful and keep track of which variables are holding fixed point values and which variables are holding normal integer values.<br>
<br>
In the arithmetic in the previous section, the fixed point was defined as the number of digits in the decimal number. However, computers use binary numbers, not decimal, so the fixed point is defined as the number of bits. As a result, you must convert fractional values to fixed point in a special way.<br>
<br>
You can convert normal integer values into their fixed point representation using the function itofp():<br>
<br>
<pre>int force = 5;
int force_fp = itofp(value);
</pre>
The system variable ONE is the fixed point representation of the number 1 (a convenience variable for itofp(1)). It is useful for specifying fractional constants in code. You can use it like this:<br>
<br>
<pre>int gravity_fp = (int) (-9.8f * ONE);</pre>
It may look strange, but the Java compiler will perform the arithmetic and substitute the appropriate integer value into the code so that the phone will never see the floating point value. Note that I've added the _fp suffix to variable names to remind me that they hold a fixed point value, not a normal integer.<br>
<br>
Addition and subtraction of fixed point values are the same as with normal integers:<br>
<br>
<pre>
int m1_fp = itofp(4);
int m2_fp = (int) (3.4f * ONE);
int mass_fp = m1_fp + m2_fp;
</pre>
There are functions for multiplication and division that handle the adjustments described previously:<br>
<br>
<pre>
int f_fp = mul(mass_fp, gravity_fp);      // = mass*gravity
int m3_fp = div(force_fp, gravity_fp);    // = force/gravity
</pre>
<?php
require '../../../footer.inc.php';
?>