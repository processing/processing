<?

require_once('../config.php');
require_once('lib/Course.class.php');

function get_courses($num = 5)
{
    $course_obj = course_xml($num);
    
    // output html
    $html = '';
    foreach ($course_obj as $course) {
        $html .= $course->display();
    }
    return $html;
}

function get_courses_short($num = 7)
{
    $course_obj = course_xml($num);
    
    // output html
    $html = '<dl>';
    foreach ($course_obj as $course) {
        if ($course->has_link()) {
            $html .= $course->display_short();
        }
    }
    return $html . '</dl>';
}

function course_xml($num)
{
    // open and parse updates.xml
    $xml =& openXML('courses.xml');
    
    // get each course node
    $courses = $xml->getElementsByTagName('course');
    $courses = $courses->toArray();
    
    // create course objects
    $i = 1;
    foreach ($courses as $course) {
        $course_obj[] = new Course($course);
        if ($i >= $num && $num != 'all') { break; }
        $i++;
    }
    
    return $course_obj;
}

if (!defined('COVER')) {
    $benchmark_start = microtime_float();
    
    $page = new Page('Courses', 'Courses');
    $page->subtemplate('template.courses.html');
    $page->content(get_courses('all'));
    writeFile("courses.html", $page->out());
    
    $benchmark_end = microtime_float();
    $execution_time = round($benchmark_end - $benchmark_start, 4);
    
    if (!defined('SUBMIT')) {
        echo <<<EOC
<h2>Courses.html Generation Successful</h2>
<p>Generator took $execution_time seconds to execute</p>
EOC;
    }
}
?>