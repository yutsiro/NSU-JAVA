package calculator;

import java.io.*;

public class Main {
    public static void main(String[] args) {
        try {
            Calculator calculator = new Calculator("resources/commands.properties");

            BufferedReader reader;
            if (args.length > 0) {
                String filename = args[0];
                try{
                    reader = new BufferedReader(new FileReader(filename));
                    System.out.println("Reading file: " + filename);
                } catch (IOException e) {
                    System.out.println("Error: can't open file " + filename + ": " + e.getMessage());
                    return;
                }
            } else {
                System.out.println("Enter commands:");
                reader = new BufferedReader(new InputStreamReader(System.in));
            }
            try (reader) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        calculator.processCommand(line);
                    } catch (CalculatorException e) {
                        System.err.println("Error in command: " + line);
                        System.err.println("Details: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading commands: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Error initializing calculator: " + e.getMessage());
        }
    }
}