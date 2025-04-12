package cz.university;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.IOException;

public class App {
    private static final String EXT = "lang";
    private static final String DIR = "src/test/resources/";
    public static void main(String[] args) throws IOException {
        String file = args.length == 0 ? "test." + EXT : args[0];
        System.out.println("START: " + file);

        CharStream input = CharStreams.fromFileName(DIR + file);
        LanguageLexer lexer = new LanguageLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LanguageParser parser = new LanguageParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new VerboseListener());

        ParseTree tree = parser.program(); // start rule
        System.out.println(tree.toStringTree(parser));
        System.out.println("FINISH: " + file);
    }
}
