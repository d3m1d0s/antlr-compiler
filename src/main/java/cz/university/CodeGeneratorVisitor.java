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
    public SymbolTable.Type visitAssignExpr(cz.university.LanguageParser.AssignExprContext ctx) {
        String name = ctx.left.getText();
        SymbolTable.Type varType = null;
        try {
            varType = symbolTable.getType(name, ctx.getStart().getLine());
        } catch (TypeException e) {
            throw new RuntimeException(e);
        }
        SymbolTable.Type exprType = visit(ctx.right);

        if (varType == SymbolTable.Type.FLOAT && exprType == SymbolTable.Type.INT) {
            instructions.add(new Instruction(Instruction.OpCode.ITOF));
        }

        switch (varType) {
            case FLOAT -> {
                instructions.add(new Instruction(Instruction.OpCode.SAVE_F, name));
                if (!insideExpressionStatement)
                    instructions.add(new Instruction(Instruction.OpCode.LOAD, name));
            }
            case INT -> {
                instructions.add(new Instruction(Instruction.OpCode.SAVE_I, name));
                if (!insideExpressionStatement)
                    instructions.add(new Instruction(Instruction.OpCode.LOAD, name));
            }
            case BOOL -> {
                instructions.add(new Instruction(Instruction.OpCode.SAVE_B, name));
                if (!insideExpressionStatement)
                    instructions.add(new Instruction(Instruction.OpCode.LOAD, name));
            }
            case STRING -> {
                instructions.add(new Instruction(Instruction.OpCode.SAVE_S, name));
                if (!insideExpressionStatement)
                    instructions.add(new Instruction(Instruction.OpCode.LOAD, name));
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
        SymbolTable.Type left = visit(ctx.expr(0));
        SymbolTable.Type right = visit(ctx.expr(1));
        SymbolTable.Type result = (left == SymbolTable.Type.FLOAT || right == SymbolTable.Type.FLOAT) ? SymbolTable.Type.FLOAT : SymbolTable.Type.INT;

        if (ctx.op.getText().equals("+")) {
            instructions.add(new Instruction(result == SymbolTable.Type.FLOAT ? Instruction.OpCode.ADD_F : Instruction.OpCode.ADD_I));
        } else {
            instructions.add(new Instruction(result == SymbolTable.Type.FLOAT ? Instruction.OpCode.SUB_F : Instruction.OpCode.SUB_I));
        }

        return result;
    }

    @Override
    public SymbolTable.Type visitMultiplicativeExpr(cz.university.LanguageParser.MultiplicativeExprContext ctx) {
        SymbolTable.Type left = visit(ctx.expr(0));
        SymbolTable.Type right = visit(ctx.expr(1));
        SymbolTable.Type result = (left == SymbolTable.Type.FLOAT || right == SymbolTable.Type.FLOAT) ? SymbolTable.Type.FLOAT : SymbolTable.Type.INT;

        switch (ctx.op.getText()) {
            case "*" -> instructions.add(new Instruction(result == SymbolTable.Type.FLOAT ? Instruction.OpCode.MUL_F : Instruction.OpCode.MUL_I));
            case "/" -> instructions.add(new Instruction(result == SymbolTable.Type.FLOAT ? Instruction.OpCode.DIV_F : Instruction.OpCode.DIV_I));
            case "%" -> instructions.add(new Instruction(Instruction.OpCode.MOD));
        }

        return result;
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
