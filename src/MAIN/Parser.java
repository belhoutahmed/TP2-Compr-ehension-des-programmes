package MAIN;

import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class Parser {
	
	

    // Maps to store classes, their methods, and method invocation relationships
   static private Map<String, Set<String>> classMethodsMap = new HashMap<>();
   static private Map<String, Integer> totalCouplingCount = new HashMap<>(); // Total invocations between any classes
    static private Map<String, Integer> classPairCouplingCount = new HashMap<>(); // Specific invocations between class pairs
    static private Set<String> userDefinedClasses = new HashSet<>(); // To store user-defined class names

    private String currentClass = null;
    private String currentMethod = null;

    // Visitor class to analyze each Java source file
    private class CouplingVisitor extends ASTVisitor {

    	@Override
    	public boolean visit(TypeDeclaration node) {
    	    currentClass = node.getName().getIdentifier();

    	    // Ensure the class is user-defined by checking if it's not from java.* or javax.*
    	    String classPackage = getClassPackage(node);
    	    if (classPackage == null || classPackage.startsWith("java.") || classPackage.startsWith("javax.")) {
    	        return super.visit(node);  // Skip standard Java classes
    	    }

    	    // Initialize methods map for current class
    	    classMethodsMap.putIfAbsent(currentClass, new HashSet<>());
    	    userDefinedClasses.add(currentClass);

    	    // Debug: Print user-defined class
    	    System.out.println("Class Detected: " + currentClass);
    	    return super.visit(node);
    	}


        @Override
        public void endVisit(TypeDeclaration node) {
            currentClass = null;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            if (currentClass != null) {
                // Ensure that the Set for the current class is initialized before adding methods to it
                Set<String> methodsSet = classMethodsMap.get(currentClass);
                if (methodsSet == null) {
                    // Initialize if the class's method set is not initialized yet
                    methodsSet = new HashSet<>();
                    classMethodsMap.put(currentClass, methodsSet);
                }

                // Add the method to the set
                currentMethod = node.getName().getIdentifier();
                methodsSet.add(currentMethod);
            }
            return super.visit(node);
        }


        @Override
        public void endVisit(MethodDeclaration node) {
            currentMethod = null;
        }

        @Override
        public boolean visit(MethodInvocation node) {
            if (currentClass != null && currentMethod != null) {
                String calledMethod = node.getName().getIdentifier();
                String calledClass = null;

                // Check if the method invocation has an expression (i.e., it's not a static method call)
                if (node.getExpression() != null) {
                    // If the expression is a simple name, it could be a field or method from another class
                    if (node.getExpression() instanceof SimpleName) {
                        calledClass = ((SimpleName) node.getExpression()).getIdentifier();
                    }
                    // If the expression is a method invocation, we check its fully qualified name
                    else if (node.getExpression() instanceof MethodInvocation) {
                        calledClass = ((MethodInvocation) node.getExpression()).getName().getIdentifier();
                    }
                    // If the expression is a qualified name (like main.classMethodCounts), we get the class part
                    else if (node.getExpression() instanceof QualifiedName) {
                        calledClass = ((QualifiedName) node.getExpression()).getFullyQualifiedName().split("\\.")[0]; // Extract class part
                    }
                    // For field accesses, we check if the field belongs to a user-defined class
                    else if (node.getExpression() instanceof FieldAccess) {
                        calledClass = ((FieldAccess) node.getExpression()).getName().getIdentifier();
                    }
                }

                // If calledClass is null or empty, skip this method invocation
                if (calledClass == null || calledClass.trim().isEmpty()) {
                    System.out.println("Skipping invocation with no valid class: " + node.toString());
                    return super.visit(node);
                }

                // Now check if the called class is a user-defined class
                if (isUserDefinedClass(calledClass)) {
                    System.out.println("User-Defined Class Coupling: " + currentClass + " -> " + calledClass);
                    totalCouplingCount.merge("TOTAL", 1, Integer::sum);
                    String relationKey = currentClass + " -> " + calledClass;
                    classPairCouplingCount.merge(relationKey, 1, Integer::sum);
                } else {
                    System.out.println("Skipping non-user-defined class interaction: " + currentClass + " -> " + calledClass);
                }
            }
            return super.visit(node);
        }

        private boolean isUserDefinedClass(String className) {
            // If class is null or empty, skip it
            if (className == null || className.trim().isEmpty()) {
                return false;
            }

            // Skip Java standard library classes
            if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("org.")) {
                return false;
            }

            // The class is considered user-defined if it is not part of standard Java libraries
            return true;
        }




       

        // Helper method to get the package of the class
        private String getClassPackage(TypeDeclaration node) {
            // Assuming your classes have a package declaration like "package mypackage;"
            ASTNode parent = node.getParent();
            if (parent instanceof CompilationUnit) {
                CompilationUnit cu = (CompilationUnit) parent;
                PackageDeclaration packageDecl = cu.getPackage();
                if (packageDecl != null) {
                    return packageDecl.getName().getFullyQualifiedName();
                }
            }
            return null;  // No package declared (default package)
        }
    }

    // Method to analyze all Java files in a project directory
    public void analyzeProject(String projectPath) {
        File projectDir = new File(projectPath);

        if (projectDir.exists() && projectDir.isDirectory()) {
            List<File> javaFiles = getJavaFiles(projectDir);

            for (File file : javaFiles) {
                try {
                    String sourceCode = Files.readString(file.toPath());
                    analyzeClassSource(sourceCode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Print detected user-defined classes
            printUserDefinedClasses();

            // Print the coupling metrics
            printCouplingMetrics();
        } else {
            System.out.println("Invalid project path: " + projectPath);
        }
    }

    // Recursively get all Java files in a directory
    private List<File> getJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(getJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }

    // Parse and analyze a single class's source code
    public void analyzeClassSource(String sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(new CouplingVisitor());
    }

    // Calculate and print the coupling metrics for each class pair
    public void printCouplingMetrics() {
        int totalRelations = totalCouplingCount.getOrDefault("TOTAL", 0);

        System.out.println("Coupling Metrics between Classes:");
        
        if (classPairCouplingCount.isEmpty()) {
            System.out.println("No method invocations detected between user-defined classes.");
        } else {
            for (Map.Entry<String, Integer> entry : classPairCouplingCount.entrySet()) {
                String classPair = entry.getKey();
                int abRelations = entry.getValue();

                // Calculate the coupling metric
                double couplingMetric = totalRelations == 0 ? 0 : (double) abRelations / totalRelations;
                System.out.println("Coupling(" + classPair + ") = " + couplingMetric);
            }
        }
    }

    // Print all user-defined classes detected in the project
    public void printUserDefinedClasses() {
        System.out.println("User-Defined Classes:");
        for (String className : userDefinedClasses) {
            System.out.println(className);
        }
    }
    
    
    static public Map<String, Map<String, Double>> getCouplingMetrics() {
        Map<String, Map<String, Double>> couplingMetrics = new HashMap<>();

        // Calculate coupling metrics based on classPairCouplingCount
        for (Map.Entry<String, Integer> entry : classPairCouplingCount.entrySet()) {
            String[] classes = entry.getKey().split(" -> ");
            String class1 = classes[0];
            String class2 = classes[1];

            // Calculate the coupling metric for each pair of classes
            double couplingMetric = (double) entry.getValue() / totalCouplingCount.getOrDefault("TOTAL", 1); // Avoid division by zero

            // Add the coupling metric to the map
            couplingMetrics
                .computeIfAbsent(class1, k -> new HashMap<>())
                .put(class2, couplingMetric);
            couplingMetrics
                .computeIfAbsent(class2, k -> new HashMap<>())
                .put(class1, couplingMetric); // Since coupling is bidirectional
        }

        return couplingMetrics;
    }
   


    // Main method for running the analysis on a given project path
    public static void main(String[] args) {
        String projectPath = "C:\\Users\\DELL\\eclipse-workspace\\MathOperation"; // Change to your project path
        Parser analyzer = new Parser();
        analyzer.analyzeProject(projectPath);
    }
}
