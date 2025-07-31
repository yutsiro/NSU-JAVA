package calculator;

import java.util.Deque;
import java.util.Map;

public interface Command {
    void execute(Deque<Double> stack, Map<String, Double> parameters, String[] arguments) throws CalculatorException;
}