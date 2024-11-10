package MAIN;
import java.util.*;

public class HierarchicalClustering {

    // Structure de données pour représenter les clusters
    private static class Cluster {
        Set<String> classes = new HashSet<>();
        
        public Cluster(String className) {
            this.classes.add(className);
        }

        public void merge(Cluster other) {
            this.classes.addAll(other.classes);
        }

        @Override
        public String toString() {
            return classes.toString();
        }

        // Calcul du couplage moyen d'un cluster
        public double calculateAverageCoupling(Map<String, Map<String, Double>> couplingMetrics) {
            double totalCoupling = 0;
            int count = 0;
            List<String> classList = new ArrayList<>(classes);

            for (int i = 0; i < classList.size(); i++) {
                for (int j = i + 1; j < classList.size(); j++) {
                    String classA = classList.get(i);
                    String classB = classList.get(j);
                    Double coupling = couplingMetrics.getOrDefault(classA, new HashMap<>()).get(classB);
                    if (coupling != null) {
                        totalCoupling += coupling;
                        count++;
                    }
                }
            }

            return count > 0 ? totalCoupling / count : 0;
        }
    }

    // Fonction principale pour exécuter l'algorithme de clustering hiérarchique
    public List<Cluster> performClustering(Map<String, Map<String, Double>> couplingMetrics, double CP) {
        // Créer une map pour garder trace des clusters
        Map<String, Cluster> classToClusterMap = new HashMap<>();
        List<Cluster> clusters = new ArrayList<>();

        // Initialiser chaque classe comme un cluster
        for (String className : couplingMetrics.keySet()) {
            Cluster cluster = new Cluster(className);
            classToClusterMap.put(className, cluster);
            clusters.add(cluster);
        }

        int step = 1;
        
        // Tant qu'il y a plus d'un cluster, on fusionne les plus couplés
        while (clusters.size() > 1) {
            double maxCoupling = -1;
            Cluster clusterA = null;
            Cluster clusterB = null;

            // Trouver les deux clusters avec le couplage maximum
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    Cluster a = clusters.get(i);
                    Cluster b = clusters.get(j);
                    
                    // Calculer le couplage moyen entre deux clusters
                    double coupling = calculateAverageCoupling(a, b, couplingMetrics);
                    if (coupling > maxCoupling) {
                        maxCoupling = coupling;
                        clusterA = a;
                        clusterB = b;
                    }
                }
            }

            // Fusionner les deux clusters les plus couplés si leur couplage moyen est supérieur à CP
            if (clusterA != null && clusterB != null && maxCoupling > CP) {
                System.out.println("Étape " + step + ": Fusion de " + clusterA + " et " + clusterB + " (Couplage = " + maxCoupling + ")");
                clusterA.merge(clusterB);
                clusters.remove(clusterB);
                step++;
            } else {
                break; // Aucun couplage valide trouvé, fin de la fusion
            }
        }

        // Vérifier si l'application contient au plus M/2 modules
        int maxModules = couplingMetrics.size() / 2;
        while (clusters.size() > maxModules) {
            // Fusionner les clusters ayant le couplage moyen le plus élevé
            double maxCoupling = -1;
            Cluster clusterA = null;
            Cluster clusterB = null;

            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    Cluster a = clusters.get(i);
                    Cluster b = clusters.get(j);
                    
                    double coupling = calculateAverageCoupling(a, b, couplingMetrics);
                    if (coupling > maxCoupling) {
                        maxCoupling = coupling;
                        clusterA = a;
                        clusterB = b;
                    }
                }
            }

            // Fusionner les deux clusters les plus couplés
            if (clusterA != null && clusterB != null) {
                System.out.println("Fusionner pour respecter le nombre de modules : Fusion de " + clusterA + " et " + clusterB);
                clusterA.merge(clusterB);
                clusters.remove(clusterB);
            }
        }

        return clusters;
    }

    // Calcul du couplage moyen entre deux clusters
    private double calculateAverageCoupling(Cluster a, Cluster b, Map<String, Map<String, Double>> couplingMetrics) {
        double totalCoupling = 0;
        int count = 0;

        for (String classA : a.classes) {
            for (String classB : b.classes) {
                // Récupérer le couplage entre classA et classB si disponible
                Double coupling = couplingMetrics.getOrDefault(classA, new HashMap<>()).get(classB);
                if (coupling != null) {
                    totalCoupling += coupling;
                    count++;
                }
            }
        }

        return count > 0 ? totalCoupling / count : 0;
    }

    public static void main(String[] args) {
        // Exemple d’utilisation avec des métriques de couplage fictives
        Parser parser = new Parser();
        parser.analyzeProject("C:\\Users\\DELL\\eclipse-workspace\\MathOperation"); // Chemin du projet à analyser
        Map<String, Map<String, Double>> couplingMetrics = Parser.getCouplingMetrics();

        HierarchicalClustering clustering = new HierarchicalClustering();
        double CP = 0.5; // Seuil de couplage moyen pour la fusion des clusters
        List<Cluster> clusters = clustering.performClustering(couplingMetrics, CP);

        // Afficher les clusters finaux
        System.out.println("Clusters finaux:");
        for (Cluster cluster : clusters) {
            System.out.println(cluster);
        }
    }
}
