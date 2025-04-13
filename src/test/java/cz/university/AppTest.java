package cz.university;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppTest {

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
}
