package calculator.commands;

import calculator.CalculatorException;
import calculator.Command;

import java.util.Deque;
import java.util.Map;

public class SqrtCommand implements Command {
    @Override
    public void execute(Deque<Double> stack, Map<String, Double> parameters, String[] arguments) throws CalculatorException {
        if (stack.isEmpty()) {
            throw new CalculatorException("Stack is empty, cannot SQRT");
        }
        double value = stack.pop();
        if (value < 0) {
            stack.push(value);
            throw new CalculatorException("Cannot calculate SQRT of a negative number: " + value);
        }
        stack.push(Math.sqrt(value));
    }
}