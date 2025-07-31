package calculator.commands;

import calculator.CalculatorException;
import calculator.Command;

import java.util.Deque;
import java.util.Map;

public class PopCommand implements Command {
    @Override
    public void execute(Deque<Double> stack, Map<String, Double> parameters, String[] arguments) throws CalculatorException {
        if (stack.isEmpty()) {
            throw new CalculatorException("Stack is empty, cannot POP");
        }
        stack.pop();
    }
}