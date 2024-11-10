package MAIN;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CouplingGraphViewer extends JFrame {

    public CouplingGraphViewer() {
        // Create a JGraphX graph
        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();
        
        // Retrieve the coupling data (you can get this from the Parser)
        Map<String, Map<String, Double>> couplingMetrics = Parser.getCouplingMetrics();
        
        // Dictionary to track added nodes
        Map<String, Object> classNodes = new HashMap<>();

        // Begin updating the graph
        graph.getModel().beginUpdate();
        try {
            // Create nodes and edges for each class based on the coupling metrics
            for (String class1 : couplingMetrics.keySet()) {
                // Add class1 node if not already added
                if (!classNodes.containsKey(class1)) {
                    Object classNode1 = graph.insertVertex(parent, null, class1, 100, 100, 80, 30);
                    classNodes.put(class1, classNode1);
                }

                // Loop through the second class for each coupling
                Map<String, Double> relatedClasses = couplingMetrics.get(class1);
                for (String class2 : relatedClasses.keySet()) {
                    // Skip self-coupling (class with itself)
                    if (class1.equals(class2)) {
                        continue;
                    }

                    // Add class2 node if not already added
                    if (!classNodes.containsKey(class2)) {
                        Object classNode2 = graph.insertVertex(parent, null, class2, 200, 100, 80, 30);
                        classNodes.put(class2, classNode2);
                    }

                    // Get the coupling weight between class1 and class2
                    double couplingWeight = relatedClasses.get(class2);

                    // Add the edge with weight (coupling value)
                    String edgeLabel = String.format("%.2f", couplingWeight);
                    graph.insertEdge(parent, null, edgeLabel, classNodes.get(class1), classNodes.get(class2));
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }

        // Arrange the graph in a circle layout
        mxCircleLayout layout = new mxCircleLayout(graph);
        layout.execute(parent);

        // Create and configure the JGraphX component
        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);

        // Window settings
        setTitle("Coupling Graph");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) throws IOException {
        
        Parser analyzer = new Parser();
        analyzer.analyzeProject("C:\\Users\\DELL\\eclipse-workspace\\MathOperation"); // Set path to your project
        SwingUtilities.invokeLater(() -> {
        	CouplingGraphViewer viewer = new CouplingGraphViewer();
            viewer.setVisible(true);
        });
    }
}
