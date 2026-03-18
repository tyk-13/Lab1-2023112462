import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TextGraph {
    // 存储有向图：Map<起始节点, Map<目标节点, 权重>>
    private Map<String, Map<String, Integer>> graph = new HashMap<>();
    private List<String> wordsList = new ArrayList<>();

    // 1. 读取文本并生成图
    public void buildGraph(String filePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            // 将非字母字符替换为空格，转为小写，并按多个空格分割
            content = content.replaceAll("[^a-zA-Z]", " ").toLowerCase();
            String[] words = content.split("\\s+");
            
            for (String w : words) {
                if (!w.isEmpty()) wordsList.add(w);
            }

            for (int i = 0; i < wordsList.size() - 1; i++) {
                String w1 = wordsList.get(i);
                String w2 = wordsList.get(i + 1);
                graph.putIfAbsent(w1, new HashMap<>());
                graph.putIfAbsent(w2, new HashMap<>()); // 保证无出度的节点也在图中
                Map<String, Integer> edges = graph.get(w1);
                edges.put(w2, edges.getOrDefault(w2, 0) + 1);
            }
            System.out.println("Graph built successfully from " + filePath);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    // 2. 展示有向图
    public void showDirectedGraph() {
        System.out.println("\n--- Directed Graph Adjacency List ---");
        for (String node : graph.keySet()) {
            System.out.println(node + " -> " + graph.get(node));
        }
        System.out.println("-------------------------------------\n");
    }

    // 3. 查询桥接词
    public String queryBridgeWords(String word1, String word2) {
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return "No " + word1 + " or " + word2 + " in the graph!";
        }
        List<String> bridgeWords = new ArrayList<>();
        Map<String, Integer> edgesFromW1 = graph.get(word1);
        
        for (String w3 : edgesFromW1.keySet()) {
            if (graph.containsKey(w3) && graph.get(w3).containsKey(word2)) {
                bridgeWords.add(w3);
            }
        }
        
        if (bridgeWords.isEmpty()) {
            return "No bridge words from " + word1 + " to " + word2 + "!";
        }
        return "The bridge words from " + word1 + " to " + word2 + " are: " + String.join(", ", bridgeWords);
    }

    // 4. 根据bridge word生成新文本
    public String generateNewText(String inputText) {
        String[] words = inputText.replaceAll("[^a-zA-Z]", " ").toLowerCase().split("\\s+");
        StringBuilder newText = new StringBuilder();
        Random rand = new Random();

        for (int i = 0; i < words.length - 1; i++) {
            newText.append(words[i]).append(" ");
            if (graph.containsKey(words[i]) && graph.containsKey(words[i+1])) {
                List<String> bridges = new ArrayList<>();
                for (String w3 : graph.get(words[i]).keySet()) {
                    if (graph.containsKey(w3) && graph.get(w3).containsKey(words[i+1])) {
                        bridges.add(w3);
                    }
                }
                if (!bridges.isEmpty()) {
                    // 随机选择一个桥接词插入
                    newText.append(bridges.get(rand.nextInt(bridges.size()))).append(" ");
                }
            }
        }
        newText.append(words[words.length - 1]);
        return newText.toString();
    }

    // 5. 计算最短路径 (Dijkstra)
    public String calcShortestPath(String word1, String word2) {
        if (!graph.containsKey(word1)) return "Word '" + word1 + "' not in graph.";
        if (word2 != null && !word2.isEmpty() && !graph.containsKey(word2)) {
            return "Word '" + word2 + "' not in graph.";
        }

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));

        for (String node : graph.keySet()) {
            dist.put(node, Integer.MAX_VALUE);
        }
        dist.put(word1, 0);
        pq.add(word1);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (word2 != null && u.equals(word2)) break; // 找到了目标节点

            if (dist.get(u) == Integer.MAX_VALUE) break;

            for (Map.Entry<String, Integer> neighbor : graph.get(u).entrySet()) {
                String v = neighbor.getKey();
                int weight = neighbor.getValue(); // 这里权重代表频次，为了符合最短路径，可以将距离视为权重的倒数或者直接用权重。
                // 实验通常将权重视为距离，频次越高距离越远(或越近，根据你的设计，这里直接将权重累加作为距离)
                int alt = dist.get(u) + weight; 
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.add(v);
                }
            }
        }

        // 构造路径字符串
        if (word2 != null && !word2.isEmpty()) {
            if (dist.get(word2) == Integer.MAX_VALUE) return "No path from " + word1 + " to " + word2;
            List<String> path = new ArrayList<>();
            for (String at = word2; at != null; at = prev.get(at)) path.add(at);
            Collections.reverse(path);
            return "Shortest path: " + String.join(" -> ", path) + " (Length: " + dist.get(word2) + ")";
        } else {
            // 如果word2为空，输出word1到所有其他节点的最短路径
            StringBuilder sb = new StringBuilder("Shortest paths from " + word1 + ":\n");
            for (String node : graph.keySet()) {
                if (!node.equals(word1) && dist.get(node) != Integer.MAX_VALUE) {
                    sb.append(node).append(": Length ").append(dist.get(node)).append("\n");
                }
            }
            return sb.toString();
        }
    }

    // 6. 计算 PageRank
    public Double calPageRank(String word) {
        if (!graph.containsKey(word)) return 0.0;
        int N = graph.size();
        double d = 0.85;
        Map<String, Double> pr = new HashMap<>();
        for (String node : graph.keySet()) pr.put(node, 1.0 / N);

        for (int iter = 0; iter < 20; iter++) { // 迭代20次
            Map<String, Double> newPr = new HashMap<>();
            double sinkPR = 0;
            for (String node : graph.keySet()) {
                if (graph.get(node).isEmpty()) sinkPR += pr.get(node);
            }

            for (String u : graph.keySet()) {
                double prU = (1 - d) / N + d * (sinkPR / N);
                for (String v : graph.keySet()) {
                    if (graph.get(v).containsKey(u)) {
                        prU += d * (pr.get(v) / graph.get(v).size());
                    }
                }
                newPr.put(u, prU);
            }
            pr = newPr;//djlsfj
        }
        return pr.get(word);
    }

    // 7. 随机游走
    public String randomWalk() {
        if (graph.isEmpty()) return "";
        List<String> nodes = new ArrayList<>(graph.keySet());
        String current = nodes.get(new Random().nextInt(nodes.size()));
        
        StringBuilder walkResult = new StringBuilder(current);
        Set<String> visitedEdges = new HashSet<>();
        Random rand = new Random();

        while (graph.containsKey(current) && !graph.get(current).isEmpty()) {
            List<String> neighbors = new ArrayList<>(graph.get(current).keySet());
            String next = neighbors.get(rand.nextInt(neighbors.size()));
            String edge = current + "->" + next;
            
            walkResult.append(" ").append(next);
            if (visitedEdges.contains(edge)) break; // 出现第一条重复的边即停止
            visitedEdges.add(edge);
            current = next;
        }

        try (FileWriter writer = new FileWriter("random_walk_output.txt")) {
            writer.write(walkResult.toString());
            System.out.println("Random walk saved to random_walk_output.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return walkResult.toString();
    }

    // 主函数：用于测试
    public static void main(String[] args) {
        TextGraph tg = new TextGraph();
        
        // 1. 测试基础文本读取
        System.out.println("--- Testing with Easy Test.txt ---");
        tg.buildGraph("Easy Test.txt");
        tg.showDirectedGraph();

        // 2. 测试各项功能
        System.out.println("Bridge words (scientist, data): " + tg.queryBridgeWords("scientist", "data"));
        System.out.println("Generated Text: " + tg.generateNewText("scientist data"));
        System.out.println(tg.calcShortestPath("scientist", "report"));
        System.out.println("PageRank of 'scientist': " + tg.calPageRank("scientist"));
        System.out.println("Random Walk: " + tg.randomWalk());
    }
}