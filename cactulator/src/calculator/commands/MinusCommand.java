package calculator.commands;

import calculator.CalculatorException;
import calculator.Command;

import java.util.Deque;
import java.util.Map;

public class MinusCommand implements Command {
    @Override
    public void execute(Deque<Double> stack, Map<String, Double> parameters, String[] arguments) throws CalculatorException {
        if (stack.size() < 2) {
            throw new CalculatorException("Not enough elements in stack for subtraction");
        }
        double b = stack.pop();
        double a = stack.pop();
        stack.push(a - b);
    }
}