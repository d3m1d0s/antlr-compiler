package cz.university;

import org.antlr.v4.runtime.*;

import java.util.Collections;
import java.util.List;

public class VerboseListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        List<String> stack = ((Parser) recognizer).getRuleInvocationStack();
        Collections.reverse(stack);
        System.err.println("rule stack: " + stack);
        System.err.println("line " + line + ":" + charPositionInLine + " at " + offendingSymbol + ": " + msg);
        System.exit(1);  // stop execution at the first syntax error.
    }
}