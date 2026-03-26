import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TextGraph {
    // 核心数据结构：外层Map存起点，内层Map存终点和权重(频次)
    private Map<String, Map<String, Integer>> graph = new HashMap<>();
    private List<String> wordsList = new ArrayList<>();

    // 1. 读取文本并生成图
    public void buildGraph(String filePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            // 清洗文本：非字母全换成空格，转小写，并按空格拆分
            content = content.replaceAll("[^a-zA-Z]", " ").toLowerCase();
            String[] words = content.split("\\s+");
            
            for (String w : words) {
                if (!w.isEmpty()) wordsList.add(w);
            }

            // 构建相邻单词的有向边
            for (int i = 0; i < wordsList.size() - 1; i++) {
                String w1 = wordsList.get(i);
                String w2 = wordsList.get(i + 1);
                graph.putIfAbsent(w1, new HashMap<>());
                graph.putIfAbsent(w2, new HashMap<>()); // 确保只进不出的词也在图里
                
                Map<String, Integer> edges = graph.get(w1);
                edges.put(w2, edges.getOrDefault(w2, 0) + 1);
            }
            System.out.println("[系统提示] 图已成功从 " + filePath + " 构建！");
        } catch (IOException e) {
            System.out.println("读取文件出错，请检查文件是否存在: " + e.getMessage());
        }
    }

    // 2. 展示有向图
    public void showDirectedGraph() {
        System.out.println("\n========== 当前有向图 ==========");
        if (graph.isEmpty()) {
            System.out.println("图为空，请先读取有效文件。");
            return;
        }
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
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

    // 4. 根据桥接词生成新文本
    public String generateNewText(String inputText) {
        String[] words = inputText.replaceAll("[^a-zA-Z]", " ").toLowerCase().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) return "";
        
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

        for (String node : graph.keySet()) dist.put(node, Integer.MAX_VALUE);
        dist.put(word1, 0);
        pq.add(word1);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (word2 != null && u.equals(word2)) break;
            if (dist.get(u) == Integer.MAX_VALUE) break;

            for (Map.Entry<String, Integer> neighbor : graph.get(u).entrySet()) {
                String v = neighbor.getKey();
                int weight = neighbor.getValue(); 
                int alt = dist.get(u) + weight; 
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.add(v);
                }
            }
        }

        if (word2 != null && !word2.isEmpty()) {
            if (dist.get(word2) == Integer.MAX_VALUE) return "No path from " + word1 + " to " + word2;
            List<String> path = new ArrayList<>();
            for (String at = word2; at != null; at = prev.get(at)) path.add(at);
            Collections.reverse(path);
            return "Shortest path: " + String.join(" -> ", path) + " (Length: " + dist.get(word2) + ")";
        } else {
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

        for (int iter = 0; iter < 20; iter++) {
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
            pr = newPr;
        }
        return pr.get(word);
    }

    // 7. 随机游走
    public String randomWalk() {
        if (graph.isEmpty()) return "Graph is empty!";
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
            if (visitedEdges.contains(edge)) break; // 遇到重复边停止
            visitedEdges.add(edge);
            current = next;
        }

        try (FileWriter writer = new FileWriter("random_walk_output.txt")) {
            writer.write(walkResult.toString());
            System.out.println("[系统提示] 游走结果已保存到 random_walk_output.txt");
        } catch (IOException e) {
            System.out.println("写入文件出错: " + e.getMessage());
        }
        return walkResult.toString();
    }

    // 交互式主函数
    public static void main(String[] args) {
        TextGraph tg = new TextGraph();
        Scanner scanner = new Scanner(System.in);

        System.out.println("欢迎使用文本有向图系统！");
        System.out.print("请输入要读取的文本文件路径 (直接回车默认读取 'Easy Test.txt'): ");
        String filePath = scanner.nextLine().trim();
        
        if (filePath.isEmpty()) {
            filePath = "Easy Test.txt";
        }
        
        // 构建图并展示
        tg.buildGraph(filePath);
        tg.showDirectedGraph();

        // 进入交互菜单
        while (true) {
            System.out.println("\n================ 请选择功能 ================");
            System.out.println("1. 查询桥接词 (Query Bridge Words)");
            System.out.println("2. 根据桥接词生成新文本 (Generate New Text)");
            System.out.println("3. 计算两个单词间的最短路径 (Shortest Path)");
            System.out.println("4. 计算一个单词到其余各节点的最短路径");
            System.out.println("5. 计算单个单词的 PageRank 值");
            System.out.println("6. 随机游走 (Random Walk)");
            System.out.println("0. 退出程序");
            System.out.println("============================================");
            System.out.print("请输入数字选择: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("请输入第一个单词: ");
                    String w1 = scanner.nextLine().trim().toLowerCase();
                    System.out.print("请输入第二个单词: ");
                    String w2 = scanner.nextLine().trim().toLowerCase();
                    System.out.println("-> " + tg.queryBridgeWords(w1, w2));
                    break;
                case "2":
                    System.out.print("请输入一行文本: ");
                    String text = scanner.nextLine().trim();
                    System.out.println("-> " + tg.generateNewText(text));
                    break;
                case "3":
                    System.out.print("请输入起点单词: ");
                    String sw1 = scanner.nextLine().trim().toLowerCase();
                    System.out.print("请输入终点单词: ");
                    String sw2 = scanner.nextLine().trim().toLowerCase();
                    System.out.println("-> " + tg.calcShortestPath(sw1, sw2));
                    break;
                case "4":
                    System.out.print("请输入起点单词: ");
                    String sw3 = scanner.nextLine().trim().toLowerCase();
                    System.out.println("->\n" + tg.calcShortestPath(sw3, ""));
                    break;
                case "5":
                    System.out.print("请输入单词: ");
                    String prWord = scanner.nextLine().trim().toLowerCase();
                    System.out.println("-> " + prWord + " 的 PageRank 值为: " + tg.calPageRank(prWord));
                    break;
                case "6":
                    System.out.println("-> 游走路径: " + tg.randomWalk());
                    break;
                case "0":
                    System.out.println("感谢使用，再见！");
                    scanner.close();
                    return; // 结束 main 方法，退出程序
                default:
                    System.out.println("-> 输入无效，请输入 0-6 之间的数字。");
            }
        }
    }
}