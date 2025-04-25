package cz.university;

import cz.university.codegen.Instruction;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class CodeGeneratorVisitor extends cz.university.LanguageBaseVisitor<SymbolTable.Type> {

    private final SymbolTable symbolTable;
    private final List<Instruction> instructions = new ArrayList<>();
    private boolean insideExpressionStatement = false;
    private int labelCounter = 0;


    public CodeGeneratorVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    @Override
    public SymbolTable.Type visitDeclaration(cz.university.LanguageParser.DeclarationContext ctx) {
        String typeText = ctx.primitiveType().getText();
        SymbolTable.Type type = switch (typeText) {
            case "int" -> SymbolTable.Type.INT;
            case "float" -> SymbolTable.Type.FLOAT;
            case "bool" -> SymbolTable.Type.BOOL;
            case "string" -> SymbolTable.Type.STRING;
            default -> throw new RuntimeException("Unsupported type: " + typeText);
        };

        for (TerminalNode id : ctx.variableList().IDENTIFIER()) {
            String name = id.getText();
            symbolTable.define(name, type);


            switch (type) {
                case INT -> {
                    instructions.add(new Instruction(Instruction.OpCode.PUSH_I, "0"));
                    instructions.add(new Instruction(Instruction.OpCode.SAVE_I, name));
                }
                case FLOAT -> {
                    instructions.add(new Instruction(Instruction.OpCode.PUSH_F, "0.0"));
                    instructions.add(new Instruction(Instruction.OpCode.SAVE_F, name));
                }
                case BOOL -> {
                    instructions.add(new Instruction(Instruction.OpCode.PUSH_B, "false"));
                    instructions.add(new Instruction(Instruction.OpCode.SAVE_B, name));
                }
                case STRING -> {
                    instructions.add(new Instruction(Instruction.OpCode.PUSH_S, "\"\""));
                    instructions.add(new Instruction(Instruction.OpCode.SAVE_S, name));
                }
            }
        }

        return null;
    }

    @Override
    public SymbolTable.Type visitExpressionStatement(cz.university.LanguageParser.ExpressionStatementContext ctx) {
        insideExpressionStatement = true;
        SymbolTable.Type type = null;

        if (ctx.expr() instanceof cz.university.LanguageParser.AssignExprContext assign) {
            List<String> vars = new ArrayList<>();
            cz.university.LanguageParser.ExprContext current = assign;
            while (current instanceof cz.university.LanguageParser.AssignExprContext a) {
                vars.add(a.left.getText());
                current = a.right;
            }

            type = visit(current);

            for (int i = vars.size() - 1; i >= 0; i--) {
                String var = vars.get(i);
                SymbolTable.Type varType;
                try {
                    varType = symbolTable.getType(var, ctx.getStart().getLine());
                } catch (TypeException e) {
                    throw new RuntimeException(e);
                }

                if (i != vars.size() - 1) {
                    instructions.add(new Instruction(Instruction.OpCode.LOAD, vars.get(i + 1)));
                    //instructions.add(new Instruction(Instruction.OpCode.POP));
                }

                if (varType == SymbolTable.Type.FLOAT && type == SymbolTable.Type.INT) {
                    instructions.add(new Instruction(Instruction.OpCode.ITOF));
                }

                addSaveInstruction(varType, var);
            }

            instructions.add(new Instruction(Instruction.OpCode.LOAD, vars.get(0)));
            instructions.add(new Instruction(Instruction.OpCode.POP));
        } else {
            type = visit(ctx.expr());

            instructions.add(new Instruction(Instruction.OpCode.POP));
        }

        insideExpressionStatement = false;
        return type;
    }


    @Override
    public SymbolTable.Type visitIdExpr(cz.university.LanguageParser.IdExprContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        SymbolTable.Type type = null;
        try {
            type = symbolTable.getType(name, ctx.getStart().getLine());
        } catch (TypeException e) {
            throw new RuntimeException(e);
        }
        instructions.add(new Instruction(Instruction.OpCode.LOAD, name));
        return type;
    }

    @Override
    public SymbolTable.Type visitIntExpr(cz.university.LanguageParser.IntExprContext ctx) {
        instructions.add(new Instruction(Instruction.OpCode.PUSH_I, ctx.getText()));
        return SymbolTable.Type.INT;
    }

    @Override
    public SymbolTable.Type visitFloatExpr(cz.university.LanguageParser.FloatExprContext ctx) {
        instructions.add(new Instruction(Instruction.OpCode.PUSH_F, ctx.getText()));
        return SymbolTable.Type.FLOAT;
    }

    @Override
    public SymbolTable.Type visitBoolExpr(cz.university.LanguageParser.BoolExprContext ctx) {
        instructions.add(new Instruction(Instruction.OpCode.PUSH_B, ctx.getText()));
        return SymbolTable.Type.BOOL;
    }

    @Override
    public SymbolTable.Type visitStringExpr(cz.university.LanguageParser.StringExprContext ctx) {
        instructions.add(new Instruction(Instruction.OpCode.PUSH_S, ctx.getText()));
        return SymbolTable.Type.STRING;
    }

    @Override
    public SymbolTable.Type visitParenExpr(cz.university.LanguageParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public SymbolTable.Type visitAdditiveExpr(cz.university.LanguageParser.AdditiveExprContext ctx) {
        var leftExpr = ctx.expr(0);
        var rightExpr = ctx.expr(1);

        SymbolTable.Type leftType = symbolTable.getExprType(leftExpr, ctx.getStart().getLine());
        SymbolTable.Type rightType = symbolTable.getExprType(rightExpr, ctx.getStart().getLine());

        String op = ctx.op.getText();
        if (op.equals(".")) {
            if (leftType != SymbolTable.Type.STRING || rightType != SymbolTable.Type.STRING) {
                throw new RuntimeException("Both operands must be strings for '.' operator.");
            }

            visit(leftExpr);
            visit(rightExpr);
            instructions.add(new Instruction(Instruction.OpCode.CONCAT));
            return SymbolTable.Type.STRING;
        }

        SymbolTable.Type resultType = (leftType == SymbolTable.Type.FLOAT || rightType == SymbolTable.Type.FLOAT)
                ? SymbolTable.Type.FLOAT
                : SymbolTable.Type.INT;

        SymbolTable.Type left = visit(leftExpr);
        if (resultType == SymbolTable.Type.FLOAT && leftType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        SymbolTable.Type right = visit(rightExpr);
        if (resultType == SymbolTable.Type.FLOAT && rightType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        instructions.add(new Instruction(
                switch (op) {
                    case "+" -> resultType == SymbolTable.Type.FLOAT ? Instruction.OpCode.ADD_F : Instruction.OpCode.ADD_I;
                    case "-" -> resultType == SymbolTable.Type.FLOAT ? Instruction.OpCode.SUB_F : Instruction.OpCode.SUB_I;
                    default -> throw new RuntimeException("Unknown op: " + op);
                }
        ));

        return resultType;
    }


    @Override
    public SymbolTable.Type visitMultiplicativeExpr(cz.university.LanguageParser.MultiplicativeExprContext ctx) {
        var leftExpr = ctx.expr(0);
        var rightExpr = ctx.expr(1);

        SymbolTable.Type leftType = symbolTable.getExprType(leftExpr, ctx.getStart().getLine());
        SymbolTable.Type rightType = symbolTable.getExprType(rightExpr, ctx.getStart().getLine());

        SymbolTable.Type resultType = (leftType == SymbolTable.Type.FLOAT || rightType == SymbolTable.Type.FLOAT)
                ? SymbolTable.Type.FLOAT
                : SymbolTable.Type.INT;

        SymbolTable.Type left = visit(leftExpr);
        if (resultType == SymbolTable.Type.FLOAT && leftType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        SymbolTable.Type right = visit(rightExpr);
        if (resultType == SymbolTable.Type.FLOAT && rightType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        String op = ctx.op.getText();
        instructions.add(new Instruction(
                switch (op) {
                    case "*" -> resultType == SymbolTable.Type.FLOAT ? Instruction.OpCode.MUL_F : Instruction.OpCode.MUL_I;
                    case "/" -> resultType == SymbolTable.Type.FLOAT ? Instruction.OpCode.DIV_F : Instruction.OpCode.DIV_I;
                    case "%" -> Instruction.OpCode.MOD;
                    default -> throw new RuntimeException("Unknown op: " + op);
                }
        ));

        return resultType;
    }

    @Override
    public SymbolTable.Type visitEqualityExpr(cz.university.LanguageParser.EqualityExprContext ctx) {
        var leftExpr = ctx.expr(0);
        var rightExpr = ctx.expr(1);
        String op = ctx.op.getText();

        int line = ctx.getStart().getLine();
        SymbolTable.Type leftType = symbolTable.getExprType(leftExpr, line);
        SymbolTable.Type rightType = symbolTable.getExprType(rightExpr, line);

        boolean floatComparison = (leftType == SymbolTable.Type.FLOAT || rightType == SymbolTable.Type.FLOAT);

        SymbolTable.Type left = visit(leftExpr);
        if (floatComparison && leftType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        SymbolTable.Type right = visit(rightExpr);
        if (floatComparison && rightType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        if (floatComparison) {
            instructions.add(new Instruction(Instruction.OpCode.EQ_F));
            return SymbolTable.Type.BOOL;
        } else if (leftType == rightType) {
            switch (leftType) {
                case INT -> instructions.add(new Instruction(Instruction.OpCode.EQ_I));
                case FLOAT -> instructions.add(new Instruction(Instruction.OpCode.EQ_F));
                case STRING -> instructions.add(new Instruction(Instruction.OpCode.EQ_S));
                case BOOL -> instructions.add(new Instruction(Instruction.OpCode.EQ_B));
                default -> throw new RuntimeException("Unsupported EQ type: " + leftType);
            }
        } else {
            throw new RuntimeException("Type mismatch in equality expression at line " + line);
        }

        if (op.equals("!=")) {
            instructions.add(new Instruction(Instruction.OpCode.NOT));
        }

        return SymbolTable.Type.BOOL;
    }

    @Override
    public SymbolTable.Type visitRelationalExpr(cz.university.LanguageParser.RelationalExprContext ctx) {
        var leftExpr = ctx.expr(0);
        var rightExpr = ctx.expr(1);
        int line = ctx.getStart().getLine();
        String op = ctx.getChild(1).getText();

        SymbolTable.Type leftType = symbolTable.getExprType(leftExpr, line);
        SymbolTable.Type rightType = symbolTable.getExprType(rightExpr, line);
        boolean floatComparison = (leftType == SymbolTable.Type.FLOAT || rightType == SymbolTable.Type.FLOAT);

        SymbolTable.Type left = visit(leftExpr);
        if (floatComparison && leftType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        SymbolTable.Type right = visit(rightExpr);
        if (floatComparison && rightType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        if (floatComparison) {
            instructions.add(new Instruction(op.equals("<") ? Instruction.OpCode.LT_F : Instruction.OpCode.GT_F));
            return SymbolTable.Type.BOOL;
        }

        if (leftType == SymbolTable.Type.INT && rightType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(op.equals("<") ? Instruction.OpCode.LT_I : Instruction.OpCode.GT_I));
            return SymbolTable.Type.BOOL;
        }

        return null;
    }

    @Override
    public SymbolTable.Type visitNotExpr(cz.university.LanguageParser.NotExprContext ctx) {
        SymbolTable.Type type = visit(ctx.expr());
        if (type == SymbolTable.Type.BOOL) {
            instructions.add(new Instruction(Instruction.OpCode.NOT));
            return SymbolTable.Type.BOOL;
        }
        return null;
    }

    @Override
    public SymbolTable.Type visitAndExpr(cz.university.LanguageParser.AndExprContext ctx) {
        SymbolTable.Type left = visit(ctx.expr(0));
        SymbolTable.Type right = visit(ctx.expr(1));

        if (left == SymbolTable.Type.BOOL && right == SymbolTable.Type.BOOL) {
            instructions.add(new Instruction(Instruction.OpCode.AND));
            return SymbolTable.Type.BOOL;
        }
        return null;
    }

    @Override
    public SymbolTable.Type visitOrExpr(cz.university.LanguageParser.OrExprContext ctx) {
        SymbolTable.Type left = visit(ctx.expr(0));
        SymbolTable.Type right = visit(ctx.expr(1));

        if (left == SymbolTable.Type.BOOL && right == SymbolTable.Type.BOOL) {
            instructions.add(new Instruction(Instruction.OpCode.OR));
            return SymbolTable.Type.BOOL;
        }
        return null;
    }

    @Override
    public SymbolTable.Type visitWriteStatement(cz.university.LanguageParser.WriteStatementContext ctx) {
        int count = 0;
        for (var expr : ctx.exprList().expr()) {
            visit(expr);
            count++;
        }

        instructions.add(new Instruction(Instruction.OpCode.PRINT, String.valueOf(count)));
        return null;
    }


    @Override
    public SymbolTable.Type visitReadStatement(cz.university.LanguageParser.ReadStatementContext ctx) {
        for (var id : ctx.identifierList().IDENTIFIER()) {
            String name = id.getText();
            int line = id.getSymbol().getLine();
            try {
                SymbolTable.Type varType = symbolTable.getType(name, line);
                switch (varType) {
                    case INT -> instructions.add(new Instruction(Instruction.OpCode.READ_I));
                    case FLOAT -> instructions.add(new Instruction(Instruction.OpCode.READ_F));
                    case BOOL -> instructions.add(new Instruction(Instruction.OpCode.READ_B));
                    case STRING -> instructions.add(new Instruction(Instruction.OpCode.READ_S));
                }

                switch (varType) {
                    case INT -> instructions.add(new Instruction(Instruction.OpCode.SAVE_I, name));
                    case FLOAT -> instructions.add(new Instruction(Instruction.OpCode.SAVE_F, name));
                    case BOOL -> instructions.add(new Instruction(Instruction.OpCode.SAVE_B, name));
                    case STRING -> instructions.add(new Instruction(Instruction.OpCode.SAVE_S, name));
                }
            } catch (TypeException e) {
            }
        }
        return null;
    }

    @Override
    public SymbolTable.Type visitWhileStatement(cz.university.LanguageParser.WhileStatementContext ctx) {
        String startLabel = nextLabel();
        String endLabel = nextLabel();

        instructions.add(new Instruction(Instruction.OpCode.LABEL, startLabel));

        SymbolTable.Type conditionType = visit(ctx.expr());
        instructions.add(new Instruction(Instruction.OpCode.FJMP, endLabel));

        visit(ctx.statement());

        instructions.add(new Instruction(Instruction.OpCode.JMP, startLabel));

        instructions.add(new Instruction(Instruction.OpCode.LABEL, endLabel));

        return null;
    }

    @Override
    public SymbolTable.Type visitIfStatement(cz.university.LanguageParser.IfStatementContext ctx) {
        String elseLabel = nextLabel();
        String endLabel = nextLabel();

        SymbolTable.Type conditionType = visit(ctx.expr());
        instructions.add(new Instruction(Instruction.OpCode.FJMP, elseLabel));

        // then
        visit(ctx.statement(0));

        instructions.add(new Instruction(Instruction.OpCode.JMP, endLabel));
        instructions.add(new Instruction(Instruction.OpCode.LABEL, elseLabel));

        if (ctx.statement().size() > 1) {
            // else
            visit(ctx.statement(1));
        }

        instructions.add(new Instruction(Instruction.OpCode.LABEL, endLabel));

        return null;
    }



    @Override
    public SymbolTable.Type visitForStatement(cz.university.LanguageParser.ForStatementContext ctx) {
        String startLabel = nextLabel();
        String endLabel = nextLabel();

        if (ctx.forInit() != null && ctx.forInit().getChildCount() > 0) {
            String var = ctx.forInit().IDENTIFIER().getText();
            SymbolTable.Type type = null;
            try {
                type = symbolTable.getType(var, ctx.getStart().getLine());
            } catch (TypeException e) {
                throw new RuntimeException(e);
            }
            visit(ctx.forInit().expr());
            addSaveInstruction(type, var);
        }

        instructions.add(new Instruction(Instruction.OpCode.LABEL, startLabel));

        if (ctx.forCond() != null && ctx.forCond().expr() != null) {
            visit(ctx.forCond().expr());
            instructions.add(new Instruction(Instruction.OpCode.FJMP, endLabel));
        }

        visit(ctx.statement());

        if (ctx.forUpdate() != null && ctx.forUpdate().getChildCount() > 0) {
            String var = ctx.forUpdate().IDENTIFIER().getText();
            SymbolTable.Type type = null;
            try {
                type = symbolTable.getType(var, ctx.getStart().getLine());
            } catch (TypeException e) {
                throw new RuntimeException(e);
            }
            visit(ctx.forUpdate().expr());
            addSaveInstruction(type, var);
        }

        instructions.add(new Instruction(Instruction.OpCode.JMP, startLabel));
        instructions.add(new Instruction(Instruction.OpCode.LABEL, endLabel));

        return null;
    }

    @Override
    public SymbolTable.Type visitUnaryMinusExpr(cz.university.LanguageParser.UnaryMinusExprContext ctx) {
        SymbolTable.Type type = visit(ctx.expr());

        switch (type) {
            case INT -> instructions.add(new Instruction(Instruction.OpCode.UMINUS_I));
            case FLOAT -> instructions.add(new Instruction(Instruction.OpCode.UMINUS_F));
            default -> throw new RuntimeException("Unary minus not supported for type: " + type);
        }

        return type;
    }

    @Override
    public SymbolTable.Type visitAssignExpr(cz.university.LanguageParser.AssignExprContext ctx) {
        SymbolTable.Type valueType = visit(ctx.right);

        String varName = ctx.left.getText();
        SymbolTable.Type varType;
        try {
            varType = symbolTable.getType(varName, ctx.getStart().getLine());
        } catch (TypeException e) {
            throw new RuntimeException(e);
        }

        if (varType == SymbolTable.Type.FLOAT && valueType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        if (!insideExpressionStatement) {
            addSaveInstruction(varType, varName);
            return varType;
        }

        return varType;
    }





    private void addSaveInstruction(SymbolTable.Type type, String name) {
        switch (type) {
            case INT -> instructions.add(new Instruction(Instruction.OpCode.SAVE_I, name));
            case FLOAT -> instructions.add(new Instruction(Instruction.OpCode.SAVE_F, name));
            case BOOL -> instructions.add(new Instruction(Instruction.OpCode.SAVE_B, name));
            case STRING -> instructions.add(new Instruction(Instruction.OpCode.SAVE_S, name));
        }
    }

    private String nextLabel() {
        return String.valueOf(labelCounter++);
    }


    public void saveToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Instruction instr : instructions) {
                writer.println(instr);
            }
        } catch (IOException e) {
            System.err.println("Failed to write output file: " + e.getMessage());
        }
    }
}
