import java.util.Scanner;
import java.util.Random;
import java.util.HashSet;
import java.util.TreeMap;

public class BullsAndCows {
    private static TreeMap<String, Integer> leaderboard = new TreeMap<>();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Игра 'Быки и коровы'!");
        System.out.println("Введите команду (help для справки):");

        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine();

            switch (command) {//обработчик команд
                case "play":
                    playGame();
                    break;
                case "help":
                    showHelp();
                    break;
                case "leaders":
                    showLeaderboard();
                    break;
                case "exit":
                    System.out.println("До встречи!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Неизвестная команда, введите 'help' для списка команд.");
            }
        }
    }

    private static void playGame() {
        System.out.print("Введите ваш ник: ");
        String nickname = scanner.nextLine();

        String secretNumber = generateSecretNumber();
        int attempts = 0;

        System.out.println("Игра началась! Загадано 4-значное число.");
        System.out.println("Введите 'show' чтобы узнать ответ, 'exit' чтобы выйти (но вы проиграете).");

        while (true) {
            System.out.print("Введите предположение (4 цифры): ");
            String guess = scanner.nextLine();

            if (guess.equals("show")) {
                System.out.printf("Загаданное число было: %s. Игра окончена.%n", secretNumber);
                return;
            }

            if (guess.equals("exit")) {
                System.out.println("Вы вышли из игры.");
                return;
            }

            attempts++;

            if (!isValidInput(guess)) {
                System.out.println("Ошибка. Введите 4-значное число с неповторяющимися цифрами.");
                continue;
            }

            int[] result = countBullsAndCows(secretNumber, guess);
            int bulls = result[0];
            int cows = result[1];

            System.out.printf("Быки: %d, Коровы: %d%n", bulls, cows);

            if (bulls == 4) {
                System.out.printf("Победа! Вы угадали число %s за %d попыток!%n", secretNumber, attempts);
                updateLeaderboard(nickname, attempts);
                break;
            }
        }
    }

    private static void showHelp() {
        System.out.println("Помощь по игре 'Быки и коровы':");
        System.out.println("Команды:");
        System.out.println("  play    - начать новую игру");
        System.out.println("  help    - показать эту справку");
        System.out.println("  leaders - показать таблицу лидеров");
        System.out.println("  exit    - выйти из игры");
        System.out.println("Правила:");
        System.out.println("  Компьютер загадывает 4-значное число с неповторяющимися цифрами.");
        System.out.println("  Быки - цифры на правильных позициях.");
        System.out.println("  Коровы - цифры из числа, но на других позициях.");
        System.out.println("  Во время игры:");
        System.out.println("    'show' - показать ответ (проигрыш)");
        System.out.println("    'exit' - выйти из игры (проигрыш)");
    }

    private static void showLeaderboard() {
        if (leaderboard.isEmpty()) {
            System.out.println("Таблица лидеров пуста");
            return;
        }

        System.out.println("Таблица лидеров (Топ-5):");
        System.out.println("Место | Ник | Попытки");
        System.out.println("------+-----+--------");

        int place = 1;
        for (var entry : leaderboard.entrySet()) {
            if (place > 5) break;
            System.out.printf("%5d | %s | %d%n", place++, entry.getKey(), entry.getValue());
        }//5 символов для места, значение - ник, ключ - число попыток
    }

    private static void updateLeaderboard(String nickname, int attempts) {
        leaderboard.put(nickname, attempts);
        //если с таким числом попыток уже есть пара, значение перезаписывается
        if (leaderboard.size() > 5) {
            leaderboard.remove(leaderboard.lastKey());
        }
    }

    private static String generateSecretNumber() {
        Random random = new Random();
        HashSet<Character> digits = new HashSet<>();//отслеживание уникальности цифр
        StringBuilder number = new StringBuilder();//java.lang импортируется автоматически

        char firstDigit = (char) (random.nextInt(9) + 1 + '0');
        // цифра от 0 до 8, + 1 (число не начинается с 0), +'0' - перевод в ASCII
        digits.add(firstDigit);
        number.append(firstDigit);

        while (number.length() < 4) {
            char digit = (char) (random.nextInt(10) + '0');
            if (digits.add(digit)) {//false если цифра-дубликат, true иначе
                number.append(digit);
            }
        }
        return number.toString();
    }

    private static boolean isValidInput(String input) {
        if (input.length() != 4 || !input.matches("\\d+")) {
            // \\d+ - регулярное выражение, matches проверяет соответствие regex
            // >= 1 цифры подряд
            return false;
        }

        HashSet<Character> digits = new HashSet<>();
        for (char c : input.toCharArray()) {
            if (!digits.add(c)) {
                return false;
            }
        }
        return true;
    }

    private static int[] countBullsAndCows(String secret, String guess) {
        int bulls = 0;
        int cows = 0;

        for (int i = 0; i < 4; i++) {
            char secretDigit = secret.charAt(i);
            char guessDigit = guess.charAt(i);

            if (secretDigit == guessDigit) {
                bulls++;
            } else if (secret.contains(String.valueOf(guessDigit))) {
                cows++;
            }
        }
        return new int[]{bulls, cows};
    }
}