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
    : left=expr op='=' right=expr                     # assignExpr
    | left=expr op='<<' right=expr                    # fileAppendExpr
    | expr op='||' expr                               # orExpr
    | expr op='&&' expr                               # andExpr
    | expr op=('==' | '!=') expr                      # equalityExpr
    | expr op=('<' | '>') expr                        # relationalExpr
    | expr op=('+' | '-' | '.') expr                  # additiveExpr
    | expr op=('*' | '/' | '%') expr                  # multiplicativeExpr
    | op='!' expr                                     # notExpr
    | op='-' expr                                     # unaryMinusExpr
    | IDENTIFIER                                      # idExpr
    | INT                                             # intExpr
    | FLOAT                                           # floatExpr
    | BOOL                                            # boolExpr
    | STRING                                          # stringExpr
    | '(' expr ')'                                    # parenExpr
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
