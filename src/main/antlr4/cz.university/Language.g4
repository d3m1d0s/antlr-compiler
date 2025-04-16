grammar Language;

program: statement* EOF;

statement
    : ';'                                                # emptyStatement
    | primitiveType variableList ';'                     # declaration
    | expr ';'                                           # expressionStatement
    | 'read' identifierList ';'                          # readStatement
    | 'write' exprList ';'                               # writeStatement
    | '{' statement* '}'                                 # block
    | 'if' '(' expr ')' statement ('else' statement)?    # ifStatement
    | 'while' '(' expr ')' statement                     # whileStatement
    | 'for' '(' expr ';' expr ';' expr ')' statement     # forStatement
    ;

expr
    : left=expr '=' right=expr                           # assignExpr
    | left=expr '<<' right=expr                          # fileAppendExpr
    | expr '||' expr                                     # orExpr
    | expr '&&' expr                                     # andExpr
    | expr ( '==' | '!=' ) expr                          # equalityExpr
    | expr ( '<' | '>' ) expr                            # relationalExpr
    | expr ( '+' | '-' | '.' ) expr                      # additiveExpr
    | expr ( '*' | '/' | '%' ) expr                      # multiplicativeExpr
    | '!' expr                                           # notExpr
    | '-' expr                                           # unaryMinusExpr
    | IDENTIFIER                                         # idExpr
    | INT                                                # intExpr
    | FLOAT                                              # floatExpr
    | BOOL                                               # boolExpr
    | STRING                                             # stringExpr
    | '(' expr ')'                                       # parenExpr
    ;

primitiveType:  INT_T | FLOAT_T | BOOL_T | STRING_T | FILE_T;

variableList: IDENTIFIER (',' IDENTIFIER)*;

identifierList: IDENTIFIER (',' IDENTIFIER)*;

exprList: expr (',' expr)*;

// === LEXER ===
INT_T: 'int';
FLOAT_T: 'float';
BOOL_T: 'bool';
STRING_T: 'string';
FILE_T: 'file';

BOOL: 'true' | 'false';
INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+;
STRING: '"' (~["\\] | '\\' .)*? '"';

IDENTIFIER: [a-zA-Z_] [a-zA-Z_0-9]*;

// COMMENTS AND SKIPPED
LINE_COMMENT: '//' ~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;
