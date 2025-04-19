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
        System.out.println("---- testIntDeclarationAndAssignmentCodeGen ---");
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
        System.out.println("---- testFloatPromotionAssignmentCodeGen ---");
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
        System.out.println("---- testStringDeclarationAndAssignment ---");
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
        System.out.println("---- testBoolAssignment ---");
        String input = """
            bool m;
            m = true;
            """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("push b true")));
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("save b m")));
    }

    @Test
    public void testIdentifierLoad() {
        System.out.println("---- testIdentifierLoad ---");
        String input = """
        int x;
        int y;
        x = 5;
        y = x;
        """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertTrue(instr.stream().anyMatch(i -> i.toString().equals("load x")));
    }

    @Test
    public void testAddInt() {
        System.out.println("---- testAddInt ---");
        String input = """
        int a;
        int b;
        int result;
        a = 2;
        b = 3;
        result = a + b;
        """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        List<String> expected = List.of(
                "push i 0", "save i a",
                "push i 0", "save i b",
                "push i 0", "save i result",
                "push i 2", "save i a",
                "push i 3", "save i b",
                "load a", "load b", "add i", "save i result"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testSubFloatWithPromotion() {
        System.out.println("---- testSubFloatWithPromotion ---");
        String input = """
        int a;
        float b;
        float result;
        a = 10;
        b = 2.5;
        result = a - b;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        List<String> expected = List.of(
                "push i 0", "save i a",
                "push f 0.0", "save f b",
                "push f 0.0", "save f result",
                "push i 10", "save i a",
                "push f 2.5", "save f b",
                "load a", "itof", "load b", "sub f", "save f result"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testConcatString() {
        System.out.println("---- testConcatString ---");
        String input = """
        string s1;
        string s2;
        string result;
        s1 = "A";
        s2 = "B";
        result = s1 . s2;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        List<String> expected = List.of(
                "push s \"\"", "save s s1",
                "push s \"\"", "save s s2",
                "push s \"\"", "save s result",
                "push s \"A\"", "save s s1",
                "push s \"B\"", "save s s2",
                "load s1", "load s2", "concat", "save s result"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testMultiplyInt() {
        System.out.println("---- testMultiplyInt ---");
        String input = """
        int a;
        int b;
        int result;
        a = 3;
        b = 4;
        result = a * b;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        List<String> expected = List.of(
                "push i 0", "save i a",
                "push i 0", "save i b",
                "push i 0", "save i result",
                "push i 3", "save i a",
                "push i 4", "save i b",
                "load a", "load b", "mul i", "save i result"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testModulo() {
        System.out.println("---- testModulo ---");
        String input = """
        int a;
        int b;
        int result;
        a = 7;
        b = 3;
        result = a % b;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        List<String> expected = List.of(
                "push i 0", "save i a",
                "push i 0", "save i b",
                "push i 0", "save i result",
                "push i 7", "save i a",
                "push i 3", "save i b",
                "load a", "load b", "mod", "save i result"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }


}
