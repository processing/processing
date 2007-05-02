import com.sun.javadoc.*;

import java.io.*;
import java.util.*;

public class ProcessingDoclet extends Doclet {
    
    public static final String      OPTION_SRCPATH           = "-sourcepath";
    public static final String      OPTION_DESTDIR           = "-d";
    public static final String      OPTION_MAINCLASS         = "-mainclass";
    
    public static final String[]    IMAGE_EXTS               = { ".png", ".gif", ".jpg" };
   
    public static int optionLength(String option) {
        int result = 0;
        
        if (option.equals(OPTION_DESTDIR) || option.equals(OPTION_MAINCLASS)) {
            result = 2;
        }
        
        return result;
    }
    
    protected static File       srcpath;
    protected static File       destdir;
    protected static String     mainclass;
    
    public static boolean start(RootDoc root) {
        //// process options
        String[][] options = root.options();
        String opt, value;
        for (int i = 0, length = options.length; i < length; i++) {
            opt = options[i][0];
            if (opt.equals(OPTION_SRCPATH)) {
                value = options[i][1];
                srcpath = new File(value);
            } else if (opt.equals(OPTION_DESTDIR)) {
                value = options[i][1];
                destdir = new File(value);
                if (!destdir.exists()) {
                    destdir.mkdirs();
                } else if (!destdir.isDirectory()) {
                    System.err.println("Invalid output directory: " + value);
                    destdir = null;
                }
            } else if (opt.equals(OPTION_MAINCLASS)) {
                mainclass = options[i][1];
            }
        }
        if (destdir == null) {
            System.err.println("Please specify a valid output directory with the -d option.");
            return false;
        }
                
        File f;
        PrintWriter writer;
        
        ClassDoc classes[] = root.classes();
        ClassDoc doc;
        String className;
        ArrayList printed;
        for (int i = 0, length = classes.length; i < length; i++) {
            doc = classes[i];
            if (hasTag(doc, "hidden")) {
                continue;
            }
            try {
                className = doc.name();
                if (!className.equals(mainclass)) {
                    if ((mainclass != null) && className.startsWith(mainclass)) {
                        className = className.substring(mainclass.length() + 1);
                    }
                    //// create the output file for the class
                    f = new File(destdir, className + ".xml");
                    writer = new PrintWriter(f);
                    //// generate the class doc
                    printClass(writer, doc);
                    //// commit
                    writer.flush();
                    writer.close();
                }
                //// now create output file for each field/method
                MemberDoc[] members;
                MemberDoc m;
                for (int j = 0; j < 2; j++) {
                    printed = new ArrayList();
                    switch (j) {
                        case 0:
                            members = doc.fields();
                            break;
                        default:
                            members = doc.methods();
                            break;
                    }
                    String name;
                    for (int k = 0, mlength = members.length; k < mlength; k++) {
                        m = members[k];
                        if (m.isPublic() && !hasTag(m, "hidden")) {
                            name = m.name();
                            if (!printed.contains(name)) {
                                if ((mainclass != null) && className.equals(mainclass)) {                                
                                    f = new File(destdir, name + ".xml");
                                } else {
                                    f = new File(destdir, className + "_" + name + ".xml");
                                }
                                writer = new PrintWriter(f);

                                printMember(writer, members, name, j == 1);

                                writer.flush();
                                writer.close();
                                printed.add(name);
                            }
                        }
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return false;
            }
        }

        return true;
    }
    
    protected static void printClass(PrintWriter writer, ClassDoc doc) {
        printHeader(writer, doc);
        
        //// fields
        printMembers(writer, doc.fields());        
        writer.println();
        //// methods
        printMembers(writer, doc.methods());
        writer.println();
        
        //// constructors
        ConstructorDoc[] constructors = doc.constructors();
        ConstructorDoc cdoc;
        HashMap pmap = new HashMap();
        writer.println("<constructor>");
        for (int i = 0, length = constructors.length; i < length; i++) {
            cdoc = constructors[i];
            if (cdoc.isPublic() && !hasTag(cdoc, "hidden")) {
                String name = cdoc.name();
                if (name.indexOf(".") >= 0) {
                    name = name.substring(name.lastIndexOf(".") + 1);
                }
                writer.print(name);
                writer.print("(");
                Parameter[] params = cdoc.parameters();
                for (int j = 0, plength = params.length; j < plength; j++) {
                    if (j > 0) {
                        writer.print(", ");
                    }
                    printElement(writer, "", "", "c", params[j].name(), false);
                }
                ParamTag[] ptags = cdoc.paramTags();
                ParamTag pt;
                for (int j = 0, plength = ptags.length; j < plength; j++) {
                    pt = ptags[j];
                    pmap.put(pt.parameterName(), pt.parameterComment());
                }
                
                writer.println(")");
            }
        }
        writer.println("</constructor>");
        writer.println();
        
        //// constructor parameters
        printParameters(writer, pmap, true);
        writer.println();
        
        printFooter(writer, doc);
    }
    
    protected static void printMember(PrintWriter writer, MemberDoc[] members, String name, boolean method) {
        //// find the member with the descriptive text, accumulate all the parameters
        Vector params = new Vector();
        MemberDoc first = null, main = null, doc;
        for (int i = 0, length = members.length; i < length; i++) {
            doc = members[i];
            if (doc.name().equals(name) && (doc.isMethod() == method)) {
                if (first == null) {
                    first = doc;
                }
                if (!doc.commentText().trim().equals("")) {
                    main = doc;
                }
                if (doc instanceof ExecutableMemberDoc) {
                    ParamTag[] tags = ((ExecutableMemberDoc) doc).paramTags();
                    ParamTag pt;
                    for (int j = 0, tlength = tags.length; j < tlength; j++) {
                        params.add(tags[j]);
                    }
                }
            }
        }
        if (main == null) {
            main = first;
        }
        
        //// print the header
        printHeader(writer, main);
        
        //// print syntax
        String prefix = null;
        if (hasTag(main, "thisref")) {
            Tag[] tags = main.tags("thisref");
            prefix = tags[0].text();
        }
        printElement(writer, "", "", "syntax", false, true);
        for (int i = 0, length = members.length; i < length; i++) {
            doc = members[i];
            if (doc.name().equals(name)) {
                if (prefix != null) {
                    writer.print(prefix);
                    writer.print(".");
                }
                writer.print(name);
                if (method && doc.isMethod()) {
                    writer.print("(");
                    Parameter[] parameters = ((ExecutableMemberDoc) doc).parameters();
                    for (int j = 0, plength = parameters.length; j < plength; j++) {
                        if (j > 0) {
                            writer.print(", ");
                        }
                        writer.print("<c>");
                        writer.print(parameters[j].name());
                        writer.print("</c>");
                    }
                    writer.print(");");
                }
                writer.println();
            }
        }
        printElement(writer, "", "", "syntax", true, true);
        writer.println();
        
        //// this reference text
        if ((prefix != null) && hasTag(main, "thisreftext")) {
            Tag[] tags = main.tags("thisreftext");
            printElement(writer, "", "", "parameter", false, true);
            printElement(writer, "  ", "", "label", prefix, true);
            printElement(writer, "  ", "", "description", tags[0].text(), true);
            printElement(writer, "", "", "parameter", true, true);
            writer.println();
        }

        if (method) {
            //// print parameters
            Iterator i = params.iterator();
            ParamTag pt;
            while (i.hasNext()) {        
                pt = (ParamTag) i.next();
                printElement(writer, "", "", "parameter", false, true);
                printElement(writer, "  ", "", "label", pt.parameterName(), true);
                printElement(writer, "  ", "", "description", pt.parameterComment(), true);
                printElement(writer, "", "", "parameter", true, true);
                writer.println();
            }
            
            //// return value
            printTag(writer, main, "return", "returns");
            writer.println();
        }

        printFooter(writer, main);
    }
    
    /** Returns true if the Doc has at least one tag of the specified name */
    protected static boolean hasTag(Doc doc, String tagname) {
        return (doc.tags(tagname).length > 0);
    }
    
    /** Prints common header and opening elements */
    protected static void printHeader(PrintWriter writer, Doc doc) {
        //// common xml header
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        writer.println("<root>");
        writer.println();
        
        //// name        
        String name = doc.name();
        if (name.indexOf(".") >= 0) {            
            name = name.substring(name.lastIndexOf(".") + 1);
        }
        printElement(writer, "", "", "name", name + (doc.isMethod() ? "()" : ""), true);
        writer.println();
        
        //// category
        printTag(writer, doc, "category");
        writer.println();
        
        //// subcategory
        printTag(writer, doc, "subcategory");
        writer.println();
        
        //// examples
        Tag[] examples = doc.tags("example");
        String basename;
        for (int i = 0, length = examples.length; i < length; i++) {
            basename = examples[i].text();
            printElement(writer, "", "", "example", false, true);
            //// find the output image (if any)
            File f;
            String imgname = "";
            for (int j = 0, jlength = IMAGE_EXTS.length; j < jlength; j++) {
                f = new File(srcpath, "examples" + File.separator + basename + File.separator + basename + IMAGE_EXTS[j]);
                if (f.exists()) {
                    imgname = basename + IMAGE_EXTS[j];
                    //// copy to the "media" directory
                    FileInputStream is = null;
                    FileOutputStream os = null;
                    try {
                        File mediadir = new File(destdir, "media");
                        if (!mediadir.exists()) {
                            mediadir.mkdirs();
                        }
                        File copy = new File(mediadir, imgname);
                        is = new FileInputStream(f);
                        os = new FileOutputStream(copy);
                        byte[] buffer = new byte[1024];
                        int bytesRead = is.read(buffer);
                        while (bytesRead >= 0) {
                            os.write(buffer, 0, bytesRead);
                            bytesRead = is.read(buffer);
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (Exception e) { }
                        }
                        if (os != null) {
                            try {
                                os.close();
                            } catch (Exception e) { }
                        }
                    }
                    break;
                }
            }
            printElement(writer, "", "", "image", imgname, true);
            
            //// output the code
            printElement(writer, "", "", "code", false, true);
            f = new File(srcpath, "examples" + File.separator + basename + File.separator + basename + ".pde");
            if (f.exists()) {
                FileReader reader = null;
                try {
                    reader = new FileReader(f);
                    char[] buffer = new char[1024];
                    int bytesRead = reader.read(buffer);                    
                    while (bytesRead >= 0) {
                        //// encode < and > into html entities &lt; and &gt;
                        char c;
                        for (int j = 0; j < bytesRead; j++) {
                            c = buffer[j];
                            switch (c) {
                                case '<':
                                    writer.print("&lt;");
                                    break;
                                case '>':
                                    writer.print("&gt;");
                                    break;
                                default:
                                    writer.print(c);
                            }
                        }
                        bytesRead = reader.read(buffer);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) { }
                    }
                }
            }
            printElement(writer, "", "", "code", true, true);
            printElement(writer, "", "", "example", true, true);
            writer.println();
        }
                
        //// description
        printElement(writer, "", "", "description", false, true);
        printText(writer, doc.commentText());
        printElement(writer, "", "", "description", true, true);
        writer.println();
    }
    
    /** Prints the contents of a particular tag as an element */
    protected static void printTag(PrintWriter writer, Doc doc, String tagname) {
        printTag(writer, doc, tagname, tagname);
    }
    
    /** Prints the contents of a particular tag as an element in the documentation with a different tagname */
    protected static void printTag(PrintWriter writer, Doc doc, String tagname, String doctagname) {
        printElement(writer, "", "", doctagname, false, true);
        Tag[] tags = doc.tags(tagname);
        for (int i = 0, length = tags.length; i < length; i++) {
            writer.println(tags[i].text());
        }
        printElement(writer, "", "", doctagname, true, true);
    }
    
    /** Prints a set of field or method tags contained within a class */
    protected static void printMembers(PrintWriter writer, MemberDoc[] members) {
        String type, prefix, suffix;
        boolean method;
        if (members instanceof FieldDoc[]) {
            type = "field";
            prefix = "f";
            suffix = "";
            method = false;
        } else if (members instanceof MethodDoc[]) {
            type = "method";
            prefix = "m";
            suffix = "()";
            method = true;
        } else {
            return;
        }
        MemberDoc m;
        Tag[] tags;
        String firstSentence;
        for (int i = 0, length = members.length; i < length; i++) {
            m = members[i];
            if (m.isPublic() && !hasTag(m, "hidden")) {
                if (method) {
                    if (m.commentText().equals("")) {
                        continue;
                    }
                }
                printElement(writer, "", "", type, false, true);
                printElement(writer, "  ", prefix, "name", m.name() + suffix, true);
                tags = m.firstSentenceTags();
                if (tags.length > 0) {
                    firstSentence = tags[0].text();
                } else {
                    firstSentence = "";
                }
                printElement(writer, "  ", prefix, "description", firstSentence, true);
                printElement(writer, "", "", type, true, true);
            }
        }
    }
    
    protected static void printText(PrintWriter writer, String text) {
        String[] lines = text.split("\n");
        String l;
        for (int i = 0, length = lines.length; i < length; i++) {
            l = lines[i];
            if (l.startsWith(" ")) {
                l = l.substring(1);
            }
            writer.println(l);
        }
    }
    
    /** Prints a set of constructor or method parameters */
    protected static void printParameters(PrintWriter writer, HashMap params, boolean constructor) {
        String prefix;
        if (constructor) {
            prefix = "c";
        } else {
            prefix = "";
        }
        Iterator keys = params.keySet().iterator();
        String label;
        while (keys.hasNext()) {
            label = (String) keys.next();
            
            printElement(writer, "", prefix, "parameter", false, true);
            printElement(writer, "  ", prefix, "label", label, true);
            printElement(writer, "  ", prefix, "description", (String) params.get(label), true);
            printElement(writer, "", prefix, "parameter", true, true);
        }
    }
    
    /** Prints an element, including its value and closing tag */
    protected static void printElement(PrintWriter writer, String indent, String prefix, String name, String value, boolean newline) {
        printElement(writer, indent, prefix, name, false, false);
        writer.print(value);
        printElement(writer, "", prefix, name, true, newline);
    }
    
    /** Prints an element tag */
    protected static void printElement(PrintWriter writer, String indent, String prefix, String name, boolean end, boolean newline) {
        writer.print(indent);
        writer.print("<");
        if (end) {
            writer.print("/");
        }
        writer.print(prefix);
        writer.print(name);
        writer.print(">");
        if (newline) {
            writer.println();
        }
    }
    
    /** Prints common footer elements and closing document tag */
    protected static void printFooter(PrintWriter writer, Doc doc) {
        //// related
        printTag(writer, doc, "related");
        writer.println();
        
        //// common footer
        writer.println("</root>");
    }
}
