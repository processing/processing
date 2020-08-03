package processing.mode.java.pdex;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import processing.app.Messages;


public class ASTUtils {

  public static ASTNode getASTNodeAt(ASTNode root, int startJavaOffset, int stopJavaOffset) {
    Messages.log("* getASTNodeAt");

    int length = stopJavaOffset - startJavaOffset;

    NodeFinder f = new NodeFinder(root, startJavaOffset, length);
    ASTNode node = f.getCoveredNode();
    if (node == null) {
      node = f.getCoveringNode();
    }
    if (node == null) {
      Messages.log("no node found");
    } else {
      Messages.log("found " + node.getClass().getSimpleName());
    }
    return node;
  }


  public static SimpleName getSimpleNameAt(ASTNode root, int startJavaOffset, int stopJavaOffset) {
    Messages.log("* getSimpleNameAt");

    // Find node at offset
    ASTNode node = getASTNodeAt(root, startJavaOffset, stopJavaOffset);

    SimpleName result = null;

    if (node == null) {
      result = null;
    } else if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
      result = (SimpleName) node;
    } else {
      // Return SimpleName with highest coverage
      List<SimpleName> simpleNames = getSimpleNameChildren(node);
      if (!simpleNames.isEmpty()) {
        // Compute coverage <selection x node>
        int[] coverages = simpleNames.stream()
            .mapToInt(name -> {
              int start = name.getStartPosition();
              int stop = start + name.getLength();
              return Math.min(stop, stopJavaOffset) -
                  Math.max(startJavaOffset, start);
            })
            .toArray();
        // Select node with highest coverage
        int maxIndex = IntStream.range(0, simpleNames.size())
            .filter(i -> coverages[i] >= 0)
            .reduce((i, j) -> coverages[i] > coverages[j] ? i : j)
            .orElse(-1);
        if (maxIndex == -1) return null;
        result = simpleNames.get(maxIndex);
      }
    }

    if (result == null) {
      Messages.log("no simple name found");
    } else {
      Messages.log("found " + node.toString());
    }
    return result;
  }


  public static List<SimpleName> getSimpleNameChildren(ASTNode node) {
    List<SimpleName> simpleNames = new ArrayList<>();
    node.accept(new ASTVisitor() {
      @Override
      public boolean visit(SimpleName simpleName) {
        simpleNames.add(simpleName);
        return super.visit(simpleName);
      }
    });
    return simpleNames;
  }


  public static IBinding resolveBinding(SimpleName node) {
    IBinding binding = node.resolveBinding();
    if (binding == null) return null;

    // Fix constructor call/declaration being resolved as type
    if (binding.getKind() == IBinding.TYPE) {
      ASTNode context = node;

      // Go up until we find non Name or Type node
      // stop if context is type argument (parent is also Name/Type, but unrelated)
      while (isNameOrType(context) &&
          !context.getLocationInParent().getId().equals("typeArguments")) {
        context = context.getParent();
      }

      switch (context.getNodeType()) {
        case ASTNode.METHOD_DECLARATION:
          MethodDeclaration decl = (MethodDeclaration) context;
          if (decl.isConstructor()) {
            binding = decl.resolveBinding();
          }
          break;
        case ASTNode.CLASS_INSTANCE_CREATION:
          ClassInstanceCreation cic = (ClassInstanceCreation) context;
          binding = cic.resolveConstructorBinding();
          break;
      }
    }

    if (binding == null) return null;

    // Normalize parametrized and raw bindings into generic bindings
    switch (binding.getKind()) {
      case IBinding.TYPE:
        ITypeBinding type = (ITypeBinding) binding;
        if (type.isParameterizedType() || type.isRawType()) {
          binding = type.getErasure();
        }
        break;
      case IBinding.METHOD:
        IMethodBinding method = (IMethodBinding) binding;
        ITypeBinding declaringClass = method.getDeclaringClass();
        if (declaringClass.isParameterizedType() ||
            declaringClass.isRawType()) {
          IMethodBinding[] methods = declaringClass.getErasure().getDeclaredMethods();
          IMethodBinding generic = Arrays.stream(methods)
              .filter(method::overrides)
              .findAny().orElse(null);
          if (generic != null) method = generic;
        }
        if (method.isParameterizedMethod() || method.isRawMethod()) {
          method = method.getMethodDeclaration();
        }
        binding = method;
        break;
    }

    return binding;
  }


  public static boolean isNameOrType(ASTNode node) {
    return node instanceof Name || node instanceof Type;
  }


  protected static List<SimpleName> findAllOccurrences(ASTNode root, String bindingKey) {
    List<SimpleName> occurences = new ArrayList<>();
    root.getRoot().accept(new ASTVisitor() {
      @Override
      public boolean visit(SimpleName name) {
        IBinding binding = resolveBinding(name);
        if (binding != null && bindingKey.equals(binding.getKey())) {
          occurences.add(name);
        }
        return super.visit(name);
      }
    });

    return occurences;
  }

}
