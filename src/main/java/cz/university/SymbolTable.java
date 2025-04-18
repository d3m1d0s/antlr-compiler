package cz.university;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    public enum Type { INT, FLOAT, BOOL, STRING, FILE }

    public static class VariableInfo {
        public final Type type;
        public Object value;

        public VariableInfo(Type type) {
            this.type = type;
            this.value = defaultValue(type);
        }

        private Object defaultValue(Type type) {
            return switch (type) {
                case INT -> 0;
                case FLOAT -> 0.0;
                case BOOL -> false;
                case STRING -> "";
                case FILE -> null;
            };
        }
    }

    public Map<String, VariableInfo> getTable() {
        return table;
    }

    private final Map<String, VariableInfo> table = new HashMap<>();

    public void declare(String name, Type type, int line) throws TypeException {
        if (table.containsKey(name)) {
            throw new TypeException(line + ": variable '" + name + "' already declared.");
        }
        table.put(name, new VariableInfo(type));
    }

    public Type getType(String name, int line) throws TypeException {
        VariableInfo info = table.get(name);
        if (info == null) {
            throw new TypeException(line + ": variable '" + name + "' not declared.");
        }
        return info.type;
    }

    public Object getValue(String name, int line) throws TypeException {
        VariableInfo info = table.get(name);
        if (info == null) {
            throw new TypeException(line + ": variable '" + name + "' not declared.");
        }
        return info.value;
    }

    public void setValue(String name, Object value, int line) throws TypeException {
        VariableInfo info = table.get(name);
        if (info == null) {
            throw new TypeException(line + ": variable '" + name + "' not declared.");
        }
        info.value = value;
    }

    public void define(String name, Type type) {
        table.put(name, new VariableInfo(type));
    }

    public boolean contains(String name) {
        return table.containsKey(name);
    }
}
