package cz.university;

import cz.university.codegen.Instruction;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class AppTest {

    private List<Instruction> generate(String source) {
        CharStream input = CharStreams.fromString(source);
        cz.university.LanguageLexer lexer = new cz.university.LanguageLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        cz.university.LanguageParser parser = new cz.university.LanguageParser(tokens);
        ParseTree tree = parser.program();

        TypeCheckerVisitor checker = new TypeCheckerVisitor();
        checker.visit(tree);
        assertTrue("Type errors: " + checker.getErrors(), checker.getErrors().isEmpty());

        CodeGeneratorVisitor generator = new CodeGeneratorVisitor(checker.getSymbolTable());
        generator.visit(tree);
        List<Instruction> list = generator.getInstructions();
        return list;
    }

    @Test
    public void testInitialValues() throws TypeException {
        SymbolTable st = new SymbolTable();
        st.declare("n", SymbolTable.Type.INT, 1);
        st.declare("t", SymbolTable.Type.STRING, 1);
        assertEquals(0, st.getValue("n", 1));
        assertEquals("", st.getValue("t", 1));
    }

    @Test
    public void testBoolAndFloatDefaults() throws TypeException {
        SymbolTable st = new SymbolTable();
        st.declare("b", SymbolTable.Type.BOOL, 1);
        st.declare("f", SymbolTable.Type.FLOAT, 1);
        assertEquals(false, st.getValue("b", 1));
        assertEquals(0.0, st.getValue("f", 1));
    }

    @Test(expected = TypeException.class)
    public void testDoubleDeclarationThrowsException() throws TypeException {
        SymbolTable st = new SymbolTable();
        st.declare("x", SymbolTable.Type.INT, 1);
        st.declare("x", SymbolTable.Type.FLOAT, 2);
    }

    @Test(expected = TypeException.class)
    public void testUndeclaredVariableThrowsException() throws TypeException {
        SymbolTable st = new SymbolTable();
        st.getType("y", 3);
    }

    @Test
    public void testIntDeclarationAndAssignmentCodeGen() {
        String input = """
            int a;
            a = 42;
            """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertEquals("push i 0", instr.get(0).toString());
        assertEquals("save i a", instr.get(1).toString());
        assertEquals("push i 42", instr.get(2).toString());
        assertEquals("save i a", instr.get(3).toString());
    }

    @Test
    public void testFloatPromotionAssignmentCodeGen() {
        String input = """
            float c;
            c = 10;
            """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertEquals("push f 0.0", instr.get(0).toString());
        assertEquals("save f c", instr.get(1).toString());
        assertEquals("push i 10", instr.get(2).toString());
        assertEquals("itof", instr.get(3).toString());
        assertEquals("save f c", instr.get(4).toString());
    }

    @Test
    public void testStringDeclarationAndAssignment() {
        String input = """
            string msg;
            msg = "hello";
            """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("push s \"hello\"")));
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("save s msg")));
    }

    @Test
    public void testBoolAssignment() {
        String input = """
            bool m;
            m = true;
            """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("push b true")));
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("save b m")));
    }
}
