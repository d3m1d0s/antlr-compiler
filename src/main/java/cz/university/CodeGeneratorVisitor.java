package cz.university;

import cz.university.codegen.Instruction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class CodeGeneratorVisitor extends cz.university.LanguageBaseVisitor<SymbolTable.Type> {

    private final SymbolTable symbolTable;
    private final List<Instruction> instructions = new ArrayList<>();
    private boolean suppressLoad = false;

    public CodeGeneratorVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    // === Declaration ===
    @Override
    public SymbolTable.Type visitDeclaration(cz.university.LanguageParser.DeclarationContext ctx) {
        SymbolTable.Type declaredType = getTypeFromKeyword(ctx.primitiveType().getText());

        for (var id : ctx.variableList().IDENTIFIER()) {
            String name = id.getText();

            // we already did declare
            // just init:
            switch (declaredType) {
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
                case FILE -> {
                    System.err.println("Warning: FILE type not supported in code generation for variable '" + name + "'");
                }
            }
        }

        return null;
    }



    // === Assignment ===
    @Override
    public SymbolTable.Type visitAssignExpr(cz.university.LanguageParser.AssignExprContext ctx) {
        String name = ctx.left.getText();
        int line = ctx.getStart().getLine();
        try {
            SymbolTable.Type varType = symbolTable.getType(name, line);
            SymbolTable.Type valueType = visit(ctx.right);

            if (varType == SymbolTable.Type.FLOAT && valueType == SymbolTable.Type.INT) {
                instructions.add(new Instruction(Instruction.OpCode.ITOF));
            }

            switch (varType) {
                case INT -> instructions.add(new Instruction(Instruction.OpCode.SAVE_I, name));
                case FLOAT -> instructions.add(new Instruction(Instruction.OpCode.SAVE_F, name));
                case BOOL -> instructions.add(new Instruction(Instruction.OpCode.SAVE_B, name));
                case STRING -> instructions.add(new Instruction(Instruction.OpCode.SAVE_S, name));
            }
            return varType;
        } catch (TypeException e) {
            // silent
        }
        return null;
    }

    // === Literals ===
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

    // === Identifier ===
    @Override
    public SymbolTable.Type visitIdExpr(cz.university.LanguageParser.IdExprContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        int line = ctx.getStart().getLine();
        try {
            SymbolTable.Type type = symbolTable.getType(name, line);
            instructions.add(new Instruction(Instruction.OpCode.LOAD, name));
            return type;
        } catch (TypeException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public SymbolTable.Type visitAdditiveExpr(cz.university.LanguageParser.AdditiveExprContext ctx) {
        SymbolTable.Type left = null;
        SymbolTable.Type right = null;
        String op = ctx.getChild(1).getText();

        try {
            left = visit(ctx.expr(0));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            right = visit(ctx.expr(1));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (left == null || right == null) {
            System.err.println("❗❗ ADDITIVE: NULL OPERAND ❗❗");
            return null;
        }

        if (".".equals(op)) {
            instructions.add(new Instruction(Instruction.OpCode.CONCAT));
            return SymbolTable.Type.STRING;
        }

        if (left == SymbolTable.Type.FLOAT || right == SymbolTable.Type.FLOAT) {
            if (left == SymbolTable.Type.INT) instructions.add(new Instruction(Instruction.OpCode.ITOF));
            if (right == SymbolTable.Type.INT) instructions.add(new Instruction(Instruction.OpCode.ITOF));
            instructions.add(new Instruction(op.equals("+") ? Instruction.OpCode.ADD_F : Instruction.OpCode.SUB_F));
            return SymbolTable.Type.FLOAT;
        }

        instructions.add(new Instruction(op.equals("+") ? Instruction.OpCode.ADD_I : Instruction.OpCode.SUB_I));
        return SymbolTable.Type.INT;
    }



    @Override
    public SymbolTable.Type visitMultiplicativeExpr(cz.university.LanguageParser.MultiplicativeExprContext ctx) {
        SymbolTable.Type left = null;
        SymbolTable.Type right = null;
        String op = ctx.getChild(1).getText();

        try {
            left = visit(ctx.expr(0));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            right = visit(ctx.expr(1));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (left == null || right == null) return null;

        if ("%".equals(op)) {
            instructions.add(new Instruction(Instruction.OpCode.MOD));
            return SymbolTable.Type.INT;
        }

        if (left == SymbolTable.Type.FLOAT || right == SymbolTable.Type.FLOAT) {
            if (left == SymbolTable.Type.INT) instructions.add(new Instruction(Instruction.OpCode.ITOF));
            if (right == SymbolTable.Type.INT) instructions.add(new Instruction(Instruction.OpCode.ITOF));
            instructions.add(new Instruction(op.equals("*") ? Instruction.OpCode.MUL_F : Instruction.OpCode.DIV_F));
            return SymbolTable.Type.FLOAT;
        }

        instructions.add(new Instruction(op.equals("*") ? Instruction.OpCode.MUL_I : Instruction.OpCode.DIV_I));
        return SymbolTable.Type.INT;
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

    private SymbolTable.Type getTypeFromKeyword(String keyword) {
        return switch (keyword) {
            case "int" -> SymbolTable.Type.INT;
            case "float" -> SymbolTable.Type.FLOAT;
            case "bool" -> SymbolTable.Type.BOOL;
            case "string" -> SymbolTable.Type.STRING;
            case "file" -> SymbolTable.Type.FILE;
            default -> throw new RuntimeException("Unknown type: " + keyword);
        };
    }
}