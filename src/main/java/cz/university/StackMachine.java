package cz.university;

import java.util.*;

public class StackMachine {
    private Stack<Object> stack = new Stack<>();
    private Map<String, Object> variables = new HashMap<>();
    private Map<String, Integer> labels = new HashMap<>();
    private List<String> instructions;
    private Scanner scanner = new Scanner(System.in);

    public void execute(List<String> instructions) {
        this.instructions = instructions;
        preprocessLabels();

        for (int i = 0; i < instructions.size(); i++) {
            String line = instructions.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 3);
            String command = parts[0];

            switch (command) {
                case "push":
                    push(parts[1], parts[2]);
                    break;
                case "pop":
                    pop();
                    break;
                case "load":
                    load(parts[1]);
                    break;
                case "save":
                    save(parts[1]);
                    break;
                case "print":
                    check(parts.length >= 2, "Invalid PRINT instruction: " + line);
                    print(parts[1]);
                    break;
                case "read":
                    read(parts[1]);
                    break;
                case "add":
                case "sub":
                case "mul":
                case "div":
                case "mod":
                case "gt":
                case "lt":
                case "ge":
                case "le":
                case "eq":
                    binaryOperation(command, parts.length > 1 ? parts[1] : null);
                    break;
                case "uminus":
                    uminus(parts[1]);
                    break;
                case "concat":
                    concat();
                    break;
                case "and":
                case "or":
                    logicalOperation(command);
                    break;
                case "not":
                    notOperation();
                    break;
                case "itof":
                    itof();
                    break;
                case "label":
                    // already processed
                    break;
                case "jmp":
                    i = jump(parts[1]) - 1;
                    break;
                case "fjmp":
                    i = fjump(parts[1], i) - 1;
                    break;
                default:
                    throw new RuntimeException("Unknown instruction: " + command);
            }
        }
    }

    private void preprocessLabels() {
        for (int i = 0; i < instructions.size(); i++) {
            String line = instructions.get(i).trim();
            if (line.startsWith("label")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    labels.put(parts[1], i);
                }
            }
        }
    }

    private void push(String type, String value) {
        switch (type) {
            case "I":
                stack.push(Integer.parseInt(value));
                break;
            case "F":
                stack.push(Float.parseFloat(value));
                break;
            case "S":
                stack.push(value.substring(1, value.length() - 1));
                break;
            case "B":
                stack.push(Boolean.parseBoolean(value));
                break;
            default:
                throw new RuntimeException("Unknown PUSH type: " + type);
        }
    }

    private void pop() {
        check(!stack.isEmpty(), "Stack underflow on POP");
        stack.pop();
    }

    private void load(String varName) {
        check(variables.containsKey(varName), "Variable '" + varName + "' not defined");
        stack.push(variables.get(varName));
    }

    private void save(String varName) {
        check(!stack.isEmpty(), "Stack underflow on SAVE");
        Object value = stack.pop();
        variables.put(varName, value);
    }

    private void print(String countStr) {
        int count = Integer.parseInt(countStr);
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            check(!stack.isEmpty(), "Stack underflow on PRINT");
            values.add(stack.pop());
        }
        Collections.reverse(values);

        StringBuilder output = new StringBuilder();
        for (Object value : values) {
            output.append(value);
        }

        System.out.println(output);
    }


    private void read(String type) {
        try {
            switch (type) {
                case "I":
                    stack.push(Integer.parseInt(scanner.nextLine()));
                    break;
                case "F":
                    stack.push(Float.parseFloat(scanner.nextLine()));
                    break;
                case "S":
                    stack.push(scanner.nextLine());
                    break;
                case "B":
                    stack.push(Boolean.parseBoolean(scanner.nextLine()));
                    break;
                default:
                    throw new RuntimeException("Unknown READ type: " + type);
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid input during READ");
        }
    }

    private void binaryOperation(String op, String type) {
        check(stack.size() >= 2, "Stack underflow on " + op);
        Object b = stack.pop();
        Object a = stack.pop();

        if (type == null) type = "I";

        switch (type) {
            case "I":
                int ai = (Integer) a;
                int bi = (Integer) b;
                switch (op) {
                    case "add": stack.push(ai + bi); break;
                    case "sub": stack.push(ai - bi); break;
                    case "mul": stack.push(ai * bi); break;
                    case "div": check(bi != 0, "Division by zero"); stack.push(ai / bi); break;
                    case "mod": check(bi != 0, "Division by zero"); stack.push(ai % bi); break;
                    case "gt": stack.push(ai > bi ? 1 : 0); break;
                    case "lt": stack.push(ai < bi ? 1 : 0); break;
                    case "ge": stack.push(ai >= bi ? 1 : 0); break;
                    case "le": stack.push(ai <= bi ? 1 : 0); break;
                    case "eq": stack.push(ai == bi ? 1 : 0); break;
                    default: throw new RuntimeException("Unsupported int operation: " + op);
                }
                break;
            case "F":
                float af = (a instanceof Integer) ? (Integer) a : (Float) a;
                float bf = (b instanceof Integer) ? (Integer) b : (Float) b;
                switch (op) {
                    case "add": stack.push(af + bf); break;
                    case "sub": stack.push(af - bf); break;
                    case "mul": stack.push(af * bf); break;
                    case "div": check(bf != 0.0f, "Division by zero"); stack.push(af / bf); break;
                    case "gt": stack.push(af > bf ? 1 : 0); break;
                    case "lt": stack.push(af < bf ? 1 : 0); break;
                    case "ge": stack.push(af >= bf ? 1 : 0); break;
                    case "le": stack.push(af <= bf ? 1 : 0); break;
                    case "eq": stack.push(af == bf ? 1 : 0); break;
                    default: throw new RuntimeException("Unsupported float operation: " + op);
                }
                break;
            case "S":
                String sa = (String) a;
                String sb = (String) b;
                if ("eq".equals(op)) {
                    stack.push(sa.equals(sb) ? 1 : 0);
                } else if ("concat".equals(op)) {
                    stack.push(sa + sb);
                } else {
                    throw new RuntimeException("Unsupported string operation: " + op);
                }
                break;
            case "B":
                boolean ba = (Boolean) a;
                boolean bb = (Boolean) b;
                if ("eq".equals(op)) {
                    stack.push(ba == bb ? 1 : 0);
                } else {
                    throw new RuntimeException("Unsupported boolean operation: " + op);
                }
                break;
            default:
                throw new RuntimeException("Unknown type for binary operation: " + type);
        }
    }

    private void uminus(String type) {
        check(!stack.isEmpty(), "Stack underflow on UMINUS");
        Object value = stack.pop();
        if (type.equals("I")) {
            stack.push(-((Integer) value));
        } else if (type.equals("F")) {
            stack.push(-((Float) value));
        } else {
            throw new RuntimeException("Unknown UMINUS type: " + type);
        }
    }

    private void concat() {
        check(stack.size() >= 2, "Stack underflow on CONCAT");
        Object b = stack.pop();
        Object a = stack.pop();
        stack.push(a.toString() + b.toString());
    }

    private void logicalOperation(String op) {
        check(stack.size() >= 2, "Stack underflow on " + op);
        Object b = stack.pop();
        Object a = stack.pop();
        boolean ba = toBoolean(a);
        boolean bb = toBoolean(b);
        switch (op) {
            case "and": stack.push(ba && bb); break;
            case "or": stack.push(ba || bb); break;
            default: throw new RuntimeException("Unknown logical operation: " + op);
        }
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value) != 0;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            throw new RuntimeException("Unsupported type for boolean logic: " + value.getClass().getSimpleName());
        }
    }

    private void notOperation() {
        check(!stack.isEmpty(), "Stack underflow on NOT");
        int a = (Integer) stack.pop();
        stack.push((a == 0) ? 1 : 0);
    }

    private void itof() {
        check(!stack.isEmpty(), "Stack underflow on ITOF");
        Object value = stack.pop();
        if (value instanceof Integer) {
            stack.push(((Integer) value).floatValue());
        } else {
            stack.push(value); // already float
        }
    }

    private int jump(String label) {
        check(labels.containsKey(label), "Label '" + label + "' not found");
        return labels.get(label);
    }

    private int fjump(String label, int currentIndex) {
        check(!stack.isEmpty(), "Stack underflow on FJMP");
        Object value = stack.pop();
        int intValue;
        if (value instanceof Integer) {
            intValue = (Integer) value;
        } else if (value instanceof Float) {
            intValue = ((Float) value).intValue();
        } else if (value instanceof Boolean) {
            intValue = ((Boolean) value) ? 1 : 0;
        } else {
            throw new RuntimeException("Unsupported type for FJMP: " + value.getClass().getSimpleName());
        }

        if (intValue == 0) {
            return jump(label);
        }
        return currentIndex + 1;
    }


    private void check(boolean condition, String errorMessage) {
        if (!condition) {
            throw new RuntimeException(errorMessage);
        }
    }
}
