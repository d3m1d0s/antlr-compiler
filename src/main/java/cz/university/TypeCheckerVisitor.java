package cz.university;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckerVisitor extends cz.university.LanguageBaseVisitor<SymbolTable.Type> {

    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> errors = new ArrayList<>();

    public List<String> getErrors() {
        return errors;
    }

    public String getSymbolTableDebug() {
        StringBuilder sb = new StringBuilder();
        for (var entry : symbolTable.getTable().entrySet()) {
            var name = entry.getKey();
            var info = entry.getValue();
            sb.append(name).append(" : ").append(info.type)
                    .append(" = ").append(info.value).append("\n");
        }
        return sb.toString();
    }

    // === Statements ===

    @Override
    public SymbolTable.Type visitDeclaration(cz.university.LanguageParser.DeclarationContext ctx) {
        SymbolTable.Type declaredType = getTypeFromKeyword(ctx.primitiveType().getText());
        for (var id : ctx.variableList().IDENTIFIER()) {
            String name = id.getText();
            int line = id.getSymbol().getLine();
            try {
                symbolTable.declare(name, declaredType, line);
                /*
                System.out.println("Declared: " + name + " of type " + declaredType +
                        " with initial value: " + symbolTable.getValue(name, line));
                */

            } catch (TypeException e) {
                errors.add(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public SymbolTable.Type visitExpressionStatement(cz.university.LanguageParser.ExpressionStatementContext ctx) {
        return visit(ctx.expr());
    }

    // === Expressions ===
    @Override
    public SymbolTable.Type visitEmptyStatement(LanguageParser.EmptyStatementContext ctx) {
        return null;
    }

    @Override
    public SymbolTable.Type visitAssignExpr(cz.university.LanguageParser.AssignExprContext ctx) {
        String varName = ctx.left.getText();
        int line = ctx.getStart().getLine();
        try {
            SymbolTable.Type varType = symbolTable.getType(varName, line);
            SymbolTable.Type valueType = visit(ctx.right);
            if (valueType == null) {
                errors.add(line + ": Right-hand side of assignment to '" + varName + "' has invalid type.");
                return null;
            }
            if (!isCompatible(varType, valueType)) {
                errors.add(line + ": Variable '" + varName + "' type is " + varType + ", but the assigned value is " + valueType + ".");
            }
            return varType;
        } catch (TypeException e) {
            errors.add(e.getMessage());
            return null;
        }
    }

    @Override
    public SymbolTable.Type visitIdExpr(cz.university.LanguageParser.IdExprContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        int line = ctx.getStart().getLine();
        try {
            return symbolTable.getType(name, line);
        } catch (TypeException e) {
            errors.add(e.getMessage());
            return null;
        }
    }

    @Override
    public SymbolTable.Type visitIntExpr(cz.university.LanguageParser.IntExprContext ctx) {
        return SymbolTable.Type.INT;
    }

    @Override
    public SymbolTable.Type visitFloatExpr(cz.university.LanguageParser.FloatExprContext ctx) {
        return SymbolTable.Type.FLOAT;
    }

    @Override
    public SymbolTable.Type visitBoolExpr(cz.university.LanguageParser.BoolExprContext ctx) {
        return SymbolTable.Type.BOOL;
    }

    @Override
    public SymbolTable.Type visitStringExpr(cz.university.LanguageParser.StringExprContext ctx) {
        return SymbolTable.Type.STRING;
    }

    @Override
    public SymbolTable.Type visitParenExpr(cz.university.LanguageParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public SymbolTable.Type visitAdditiveExpr(LanguageParser.AdditiveExprContext ctx) {
        SymbolTable.Type left = visit(ctx.expr(0));
        SymbolTable.Type right = visit(ctx.expr(1));
        int line = ctx.getStart().getLine();
        String op = ctx.getChild(1).getText();

        if (left == null || right == null) return null;

        if (".".equals(op)) {
            if (left == SymbolTable.Type.STRING && right == SymbolTable.Type.STRING) {
                return SymbolTable.Type.STRING;
            } else {
                errors.add(line + ": String concatenation requires both operands to be strings. Got: " + left + ", " + right);
                return null;
            }
        }

        return computeBinaryNumericType(left, right, line);
    }

    @Override
    public SymbolTable.Type visitMultiplicativeExpr(cz.university.LanguageParser.MultiplicativeExprContext ctx) {
        String op = ctx.getChild(1).getText();
        SymbolTable.Type left = visit(ctx.expr(0));
        SymbolTable.Type right = visit(ctx.expr(1));
        int line = ctx.getStart().getLine();

        if ("%".equals(op)) {
            if (left != SymbolTable.Type.INT || right != SymbolTable.Type.INT) {
                errors.add(line + ": Modulo can be used only with integers.");
            }
            return SymbolTable.Type.INT;
        }

        return computeBinaryNumericType(left, right, line);
    }

    @Override
    public SymbolTable.Type visitEqualityExpr(cz.university.LanguageParser.EqualityExprContext ctx) {
        SymbolTable.Type left = visit(ctx.expr(0));
        SymbolTable.Type right = visit(ctx.expr(1));
        int line = ctx.getStart().getLine();

        if (left == null || right == null) return null;

        // int == float or float == int -> OK
        if ((left == SymbolTable.Type.INT && right == SymbolTable.Type.FLOAT) ||
                (left == SymbolTable.Type.FLOAT && right == SymbolTable.Type.INT)) {
            return SymbolTable.Type.BOOL;
        }

        // int == int, float == float, string == string -> OK
        if ((left == right) &&
                (left == SymbolTable.Type.INT || left == SymbolTable.Type.FLOAT || left == SymbolTable.Type.STRING)) {
            return SymbolTable.Type.BOOL;
        }

        errors.add(line + ": Invalid types for equality: " + left + ", " + right);
        return null;
    }


    @Override
    public SymbolTable.Type visitRelationalExpr(cz.university.LanguageParser.RelationalExprContext ctx) {
        SymbolTable.Type left = visit(ctx.expr(0));
        SymbolTable.Type right = visit(ctx.expr(1));
        int line = ctx.getStart().getLine();

        if (left == null || right == null) return null;

        // int < int, float < float, int < float, float < int -> OK
        if ((left == SymbolTable.Type.INT || left == SymbolTable.Type.FLOAT) &&
                (right == SymbolTable.Type.INT || right == SymbolTable.Type.FLOAT)) {
            return SymbolTable.Type.BOOL;
        }

        errors.add(line + ": Relational operators are only valid for int or float. Got: " + left + ", " + right);
        return null;
    }

    @Override
    public SymbolTable.Type visitReadStatement(LanguageParser.ReadStatementContext ctx) {
        for (var id : ctx.identifierList().IDENTIFIER()) {
            String name = id.getText();
            int line = id.getSymbol().getLine();
            try {
                SymbolTable.Type varType = symbolTable.getType(name, line);
                // Тут можно проверить, что тип допустим
                if (varType != SymbolTable.Type.INT &&
                        varType != SymbolTable.Type.FLOAT &&
                        varType != SymbolTable.Type.BOOL &&
                        varType != SymbolTable.Type.STRING) {
                    errors.add(line + ": Variable '" + name + "' has unsupported type for read: " + varType);
                }
            } catch (TypeException e) {
                errors.add(e.getMessage());
            }
        }
        return null;
    }


    @Override
    public SymbolTable.Type visitWriteStatement(cz.university.LanguageParser.WriteStatementContext ctx) {
        for (var expr : ctx.exprList().expr()) {
            visit(expr);
        }
        return null;
    }

    @Override
    public SymbolTable.Type visitIfStatement(cz.university.LanguageParser.IfStatementContext ctx) {
        SymbolTable.Type conditionType = visit(ctx.expr());
        int line = ctx.getStart().getLine();
        if (conditionType != null && conditionType != SymbolTable.Type.BOOL) {
            errors.add(line + ": Condition in if statement must be bool, got " + conditionType + ".");
        }
        visit(ctx.statement(0)); // if-branch
        if (ctx.statement().size() > 1) {
            visit(ctx.statement(1)); // else-branch (if exist)
        }
        return null;
    }

    @Override
    public SymbolTable.Type visitWhileStatement(cz.university.LanguageParser.WhileStatementContext ctx) {
        SymbolTable.Type conditionType = visit(ctx.expr());
        int line = ctx.getStart().getLine();
        if (conditionType != null && conditionType != SymbolTable.Type.BOOL) {
            errors.add(line + ": Condition in while loop must be bool, got " + conditionType + ".");
        }
        visit(ctx.statement());
        return null;
    }

    @Override
    public SymbolTable.Type visitBlock(cz.university.LanguageParser.BlockContext ctx) {
        for (var stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public SymbolTable.Type visitAndExpr(LanguageParser.AndExprContext ctx) {
        return visitLogicalBinary(ctx.expr(0), ctx.expr(1), ctx.getStart().getLine(), "&&");
    }

    @Override
    public SymbolTable.Type visitOrExpr(LanguageParser.OrExprContext ctx) {
        return visitLogicalBinary(ctx.expr(0), ctx.expr(1), ctx.getStart().getLine(), "||");
    }

    private SymbolTable.Type visitLogicalBinary(LanguageParser.ExprContext leftExpr, LanguageParser.ExprContext rightExpr, int line, String op) {
        SymbolTable.Type left = visit(leftExpr);
        SymbolTable.Type right = visit(rightExpr);

        if (left == SymbolTable.Type.BOOL && right == SymbolTable.Type.BOOL) {
            return SymbolTable.Type.BOOL;
        }
        errors.add(line + ": Logical operator '" + op + "' requires bool operands. Got: " + left + ", " + right);
        return null;
    }

    @Override
    public SymbolTable.Type visitNotExpr(LanguageParser.NotExprContext ctx) {
        SymbolTable.Type type = visit(ctx.expr());
        int line = ctx.getStart().getLine();

        if (type == SymbolTable.Type.BOOL) {
            return SymbolTable.Type.BOOL;
        }
        errors.add(line + ": Logical '!' requires bool operand. Got: " + type);
        return null;
    }

    @Override
    public SymbolTable.Type visitUnaryMinusExpr(LanguageParser.UnaryMinusExprContext ctx) {
        SymbolTable.Type type = visit(ctx.expr());
        int line = ctx.getStart().getLine();

        if (type == SymbolTable.Type.INT || type == SymbolTable.Type.FLOAT) {
            return type;
        }

        errors.add(line + ": Unary minus requires int or float. Got: " + type);
        return null;
    }


    // === Helpers ===

    private SymbolTable.Type computeBinaryNumericType(SymbolTable.Type left, SymbolTable.Type right, int line) {
        if (left == null || right == null) return null;
        if (left == SymbolTable.Type.FLOAT || right == SymbolTable.Type.FLOAT) {
            return SymbolTable.Type.FLOAT;
        }
        if (left == SymbolTable.Type.INT && right == SymbolTable.Type.INT) {
            return SymbolTable.Type.INT;
        }
        errors.add(line + ": Invalid operands for arithmetic operation: " + left + ", " + right);
        return null;
    }

    private boolean isCompatible(SymbolTable.Type target, SymbolTable.Type value) {
        return target == value || (target == SymbolTable.Type.FLOAT && value == SymbolTable.Type.INT);
    }

    private SymbolTable.Type getTypeFromKeyword(String keyword) {
        if (keyword.equals("int")) return SymbolTable.Type.INT;
        if (keyword.equals("float")) return SymbolTable.Type.FLOAT;
        if (keyword.equals("bool")) return SymbolTable.Type.BOOL;
        if (keyword.equals("string")) return SymbolTable.Type.STRING;
        throw new RuntimeException("Unknown type: " + keyword);
    }
}



