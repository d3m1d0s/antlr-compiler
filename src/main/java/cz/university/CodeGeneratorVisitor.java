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
        SymbolTable.Type type = visit(ctx.expr());
        insideExpressionStatement = false;
        return type;
    }

    @Override
    public SymbolTable.Type visitAssignmentStatement(cz.university.LanguageParser.AssignmentStatementContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        SymbolTable.Type varType;
        try {
            varType = symbolTable.getType(name, ctx.getStart().getLine());
        } catch (TypeException e) {
            throw new RuntimeException(e);
        }

        SymbolTable.Type exprType = visit(ctx.expr());

        if (varType == SymbolTable.Type.FLOAT && exprType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        switch (varType) {
            case FLOAT -> {
                instructions.add(new Instruction(Instruction.OpCode.SAVE_F, name));
            }
            case INT -> {
                instructions.add(new Instruction(Instruction.OpCode.SAVE_I, name));
            }
            case BOOL -> {
                instructions.add(new Instruction(Instruction.OpCode.SAVE_B, name));
            }
            case STRING -> {
                instructions.add(new Instruction(Instruction.OpCode.SAVE_S, name));
            }
        }

        return varType;
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
        }

        if (leftType == rightType) {
            switch (leftType) {
                case INT -> instructions.add(new Instruction(Instruction.OpCode.EQ_I));
                case FLOAT -> instructions.add(new Instruction(Instruction.OpCode.EQ_F));
                case STRING -> instructions.add(new Instruction(Instruction.OpCode.EQ_S));
                default -> {}
            }
            return SymbolTable.Type.BOOL;
        }

        return null;
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
