package cz.university;

import cz.university.codegen.Instruction;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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
        System.out.println("---- testIntDeclarationAndAssignmentCodeGen ----");
        String input = """
            int a;
            a = 42;
            """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertEquals("push I 0", instr.get(0).toString());
        assertEquals("save a", instr.get(1).toString());
        assertEquals("push I 42", instr.get(2).toString());
        assertEquals("save a", instr.get(3).toString());
    }

    @Test
    public void testFloatPromotionAssignmentCodeGen() {
        System.out.println("---- testFloatPromotionAssignmentCodeGen ----");
        String input = """
            float c;
            c = 10;
            """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertEquals("push F 0.0", instr.get(0).toString());
        assertEquals("save c", instr.get(1).toString());
        assertEquals("push I 10", instr.get(2).toString());
        assertEquals("itof", instr.get(3).toString());
        assertEquals("save c", instr.get(4).toString());
    }

    @Test
    public void testStringDeclarationAndAssignment() {
        System.out.println("---- testStringDeclarationAndAssignment ----");
        String input = """
            string msg;
            msg = "hello";
            """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("push S \"hello\"")));
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("save msg")));
    }

    @Test
    public void testBoolAssignment() {
        System.out.println("---- testBoolAssignment ----");
        String input = """
            bool m;
            m = true;
            """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("push B true")));
        assertTrue(instr.stream().anyMatch(i -> i.toString().contains("save m")));
    }

    @Test
    public void testIdentifierLoad() {
        System.out.println("---- testIdentifierLoad ----");
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
        System.out.println("---- testAddInt ----");
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
                "push I 0", "save a",
                "push I 0", "save b",
                "push I 0", "save result",

                "push I 2", "save a", "load a", "pop",
                "push I 3", "save b", "load b", "pop",
                "load a", "load b", "add I", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testSubFloatWithPromotion() {
        System.out.println("---- testSubFloatWithPromotion ----");
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
                "push I 0", "save a",
                "push F 0.0", "save b",
                "push F 0.0", "save result",

                "push I 10", "save a", "load a", "pop",
                "push F 2.5", "save b", "load b", "pop",
                "load a", "itof", "load b", "sub F", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testConcatString() {
        System.out.println("---- testConcatString ----");
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
                "push S \"\"", "save s1",
                "push S \"\"", "save s2",
                "push S \"\"", "save result",

                "push S \"A\"", "save s1", "load s1", "pop",
                "push S \"B\"", "save s2", "load s2", "pop",
                "load s1", "load s2", "concat", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }


    @Test
    public void testMultiplyInt() {
        System.out.println("---- testMultiplyInt ----");
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
                "push I 0", "save a",
                "push I 0", "save b",
                "push I 0", "save result",

                "push I 3", "save a", "load a", "pop",
                "push I 4", "save b", "load b", "pop",
                "load a", "load b", "mul I", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testModulo() {
        System.out.println("---- testModulo ----");
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
                "push I 0", "save a",
                "push I 0", "save b",
                "push I 0", "save result",

                "push I 7", "save a", "load a", "pop",
                "push I 3", "save b", "load b", "pop",
                "load a", "load b", "mod", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testEqualFloatPromotion() {
        System.out.println("---- testEqualFloatPromotion ----");
        String input = """
        int a;
        float b;
        bool result;
        a = 3;
        b = 3.0;
        result = a == b;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);

        List<String> expected = List.of(
                "push I 0", "save a",
                "push F 0.0", "save b",
                "push B false", "save result",

                "push I 3", "save a", "load a", "pop",
                "push F 3.0", "save b", "load b", "pop",

                "load a", "itof", "load b", "eq F", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testEqualStrings() {
        System.out.println("---- testEqualStrings ----");
        String input = """
        string a;
        string b;
        bool result;
        a = "foo";
        b = "bar";
        result = a == b;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);

        List<String> expected = List.of(
                "push S \"\"", "save a",
                "push S \"\"", "save b",
                "push B false", "save result",

                "push S \"foo\"", "save a", "load a", "pop",
                "push S \"bar\"", "save b", "load b", "pop",

                "load a", "load b", "eq S", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testLessThanInt() {
        System.out.println("---- testLessThanInt ----");
        String input = """
        int a;
        int b;
        bool result;
        a = 1;
        b = 2;
        result = a < b;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);

        List<String> expected = List.of(
                "push I 0", "save a",
                "push I 0", "save b",
                "push B false", "save result",

                "push I 1", "save a", "load a", "pop",
                "push I 2", "save b", "load b", "pop",

                "load a", "load b", "lt I", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testGreaterThanFloatWithPromotion() {
        System.out.println("---- testGreaterThanFloatWithPromotion ----");
        String input = """
        int a;
        float b;
        bool result;
        a = 4;
        b = 2.5;
        result = a > b;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);

        List<String> expected = List.of(
                "push I 0", "save a",
                "push F 0.0", "save b",
                "push B false", "save result",

                "push I 4", "save a", "load a", "pop",
                "push F 2.5", "save b", "load b", "pop",

                "load a", "itof", "load b", "gt F", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testNotBool() {
        System.out.println("---- testNotBool ----");
        String input = """
        bool a;
        bool result;
        a = false;
        result = !a;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);

        List<String> expected = List.of(
                "push B false", "save a",
                "push B false", "save result",

                "push B false", "save a", "load a", "pop",

                "load a", "not", "save result", "load result", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testAndBool() {
        System.out.println("---- testAndBool ----");
        String input = """
        bool a;
        bool b;
        bool result;
        a = true;
        b = false;
        result = a && b;
        """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        List<String> expected = List.of(
                "push B false", "save a",
                "push B false", "save b",
                "push B false", "save result",

                "push B true", "save a", "load a", "pop",
                "push B false", "save b", "load b", "pop",

                "load a", "load b", "and", "save result", "load result", "pop"
        );
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testOrBool() {
        System.out.println("---- testOrBool ----");
        String input = """
        bool a;
        bool b;
        bool result;
        a = false;
        b = true;
        result = a || b;
        """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        List<String> expected = List.of(
                "push B false", "save a",
                "push B false", "save b",
                "push B false", "save result",

                "push B false", "save a", "load a", "pop",
                "push B true", "save b", "load b", "pop",

                "load a", "load b", "or", "save result", "load result", "pop"
        );
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testWriteStatement() {
        System.out.println("---- testWriteStatement ----");
        String input = """
        int a;
        float b;
        bool c;
        string d;
        a = 10;
        b = 3.14;
        c = true;
        d = "Hi!";
        write a, b, c, d;
        """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        List<String> expected = List.of(
                "push I 0", "save a",
                "push F 0.0", "save b",
                "push B false", "save c",
                "push S \"\"", "save d",

                "push I 10", "save a", "load a", "pop",
                "push F 3.14", "save b", "load b", "pop",
                "push B true", "save c", "load c", "pop",
                "push S \"Hi!\"", "save d", "load d", "pop",

                "load a", "load b", "load c", "load d",
                "print 4"
        );
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testReadStatement() {
        System.out.println("---- testReadStatement ----");
        String input = """
        int a;
        float b;
        bool c;
        string d;
        read a, b, c, d;
        """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);
        List<String> expected = List.of(
                "push I 0", "save a",
                "push F 0.0", "save b",
                "push B false", "save c",
                "push S \"\"", "save d",

                "read I", "save a",
                "read F", "save b",
                "read B", "save c",
                "read S", "save d"
        );
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testIfStatement() {
        System.out.println("---- testIfStatement ----");
        String input = """
        bool cond;
        int a;
        cond = true;
        if (cond) {
            a = 42;
        }
        """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);

        List<String> expected = List.of(
                "push B false", "save cond",
                "push I 0", "save a",

                "push B true", "save cond", "load cond", "pop",

                "load cond", "fjmp L0",
                "push I 42", "save a", "load a", "pop",
                "label L0"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testWhileStatement() {
        System.out.println("---- testWhileStatement ----");
        String input = """
        int a;
        a = 0;
        while (a < 3) {
            a = a + 1;
        }
        """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);

        List<String> expected = List.of(
                "push I 0", "save a",
                "push I 0", "save a", "load a", "pop",

                "label L0",
                "load a", "push I 3", "lt I", "fjmp L1",
                "load a", "push I 1", "add I", "save a", "load a", "pop",
                "jmp L0",
                "label L1"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testForStatement() {
        System.out.println("---- testForStatement ----");
        String input = """
        int a;
        a = 0;
        for (a = 0; a < 3; a = a + 1) {
            write(a);
        }
        """;
        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);

        List<String> expected = List.of(
                "push I 0", "save a",
                "push I 0", "save a", "load a", "pop",
                "push I 0", "save a",

                "label L0",
                "load a", "push I 3", "lt I", "fjmp L1",

                "load a", "print 1",

                "load a", "push I 1", "add I", "save a",
                "jmp L0",
                "label L1"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testChainAssignment() {
        System.out.println("---- testChainAssignment ----");
        String input = """
        int i, j, k;
        i = j = k = 55;
        """;

        List<Instruction> instr = generate(input);
        instr.forEach(System.out::println);

        List<String> expected = List.of(
                "push I 0", "save i",
                "push I 0", "save j",
                "push I 0", "save k",
                "push I 55", "save k",
                "load k", "save j",
                "load j", "save i", "load i", "pop"
        );

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), instr.get(i).toString());
        }
    }

    @Test
    public void testOutputAgainstReferenceFiles() throws IOException {
        System.out.println("---- testOutputAgainstReferenceFiles ----");

        String source = Files.readString(Path.of("src/test/resources/test.lang"));

        List<Instruction> instructions = generate(source);

        Path outPath = Path.of("output.out");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outPath))) {
            for (Instruction instr : instructions) {
                writer.println(instr);
            }
        }

        List<String> actual = Files.readAllLines(outPath);
        List<List<String>> references = List.of(
                Files.readAllLines(Path.of("src/test/resources/PLC_t1.out")),
                Files.readAllLines(Path.of("src/test/resources/PLC_t2.out")),
                Files.readAllLines(Path.of("src/test/resources/PLC_t3.out"))
        );

        for (int i = 0; i < references.size(); i++) {
            if (actual.equals(references.get(i))) {
                System.out.println("Output matches PLC_t" + (i + 1) + ".out");
                return;
            }
        }

        int bestMatchIndex = -1;
        int maxMatches = -1;
        for (int i = 0; i < references.size(); i++) {
            List<String> ref = references.get(i);
            int matches = 0;
            for (int j = 0; j < Math.min(actual.size(), ref.size()); j++) {
                if (actual.get(j).equals(ref.get(j))) {
                    matches++;
                }
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                bestMatchIndex = i;
            }
        }

        List<String> expected = references.get(bestMatchIndex);
        System.err.println("No full match found. Closest match: PLC_t" + (bestMatchIndex + 1) + ".out");
        System.err.println("------ DIFF ------");

        int maxLines = Math.max(actual.size(), expected.size());
        for (int i = 0; i < maxLines; i++) {
            String act = i < actual.size() ? actual.get(i) : "<missing>";
            String exp = i < expected.size() ? expected.get(i) : "<missing>";
            if (!act.equals(exp)) {
                System.err.printf("Line %d:\n  Expected: %s\n  Actual:   %s\n", i + 1, exp, act);
            }
        }

        fail("Generated output.out does not match any reference output.");
    }

}
