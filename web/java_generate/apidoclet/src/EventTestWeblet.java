import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;


public class EventTestWeblet extends Standard {
	
	public static boolean start(RootDoc root){
		for(ClassDoc c : root.classes()){
			for( MethodDoc m : c.methods() ){
				System.out.println("Method: " + c.name() + " : " + m.name());
			}
			for( FieldDoc f : c.fields()){
				System.out.println("Field: " + c.name() + " : " + f.name());
			}
		}
		System.out.println("Ran test weblet");
		return true;
	}

}
