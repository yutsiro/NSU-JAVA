import java.io.*;
import java.util.*;
import java.util.regex.*;

class WordFrequency {
    String word;
    int count;

    WordFrequency(String word) {
        this.word = word;
        this.count = 0;
    }
}

public class WordCounter {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("no input file");
            return;
        }

        String inputFileName = args[0];
        String outputFileName = "output.csv";

        try {
            Map<String, WordFrequency> wordMap = new HashMap<>();
            int totalWords = processFile(inputFileName, wordMap);
            writeCSV(outputFileName, wordMap, totalWords);
            System.out.println("CSV file created: " + outputFileName);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static int processFile(String fileName, Map<String, WordFrequency> wordMap) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
        Pattern pattern = Pattern.compile("\\w+");
        int totalWords = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line.toLowerCase());
            while (matcher.find()) {
                String word = matcher.group();
                totalWords++;
                wordMap.putIfAbsent(word, new WordFrequency(word));
                wordMap.get(word).count++;
            }
        }
        reader.close();
        return totalWords;
    }

    private static void writeCSV(String fileName, Map<String, WordFrequency> wordMap, int totalWords) throws IOException {
        List<WordFrequency> sortedWords = new ArrayList<>(wordMap.values());
        sortedWords.sort((a, b) -> Integer.compare(b.count, a.count));

        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write("Word,Frequency,Percentage\n");

        for (WordFrequency wf : sortedWords) {
            double percentage = (wf.count / (double) totalWords) * 100;
            writer.write(String.format("%s,%d,%.2f%%\n", wf.word, wf.count, percentage));
        }

        writer.close();
    }
}
