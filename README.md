# AntlrCompiler

> **Educational Compiler Project** — a full-stack implementation of a programming language, from grammar to execution.

This project demonstrates the **complete development cycle of a simple compiler**:
- **Lexer & Parser generation** (ANTLR 4)
- **Type checking** (custom visitor pattern)
- **Stack-based intermediate code generation**
- **Virtual Machine (interpreter)** for executing the generated code

Built entirely in **Java 17** with **Maven**.

---

## 📋 Project Summary

This project simulates the process of creating a simple stack-based programming language from scratch. It showcases important concepts in compiler construction:

- Language design (tokens, syntax rules)
- Syntax analysis (parsing, AST construction)
- Semantic analysis (type checking, symbol tables)
- Intermediate representation (stack-based instructions)
- Code generation (output to `.out` file)
- Program execution (via a custom **StackMachine** interpreter)

**Main steps:**
1. **Parsing** the source code using ANTLR.
2. **Type Checking** with detailed error reporting.
3. **Code Generation** into stack-based instructions.
4. **Execution** on a custom stack-based Virtual Machine.

---

## ⚡ Technology Stack

| Layer | Technology |
|:------|:-----------|
| Parsing | [ANTLR 4.13.1](https://www.antlr.org/) |
| Language | Java 17 |
| Build System | Maven 3 |
| Testing | Manual and example-based |

---

## 🧠 Features

This compiler project supports a rich subset of imperative programming features:

* **Typed language design**
  Includes primitive types: `int`, `float`, `bool`, `string`, and `file`.
  Supports automatic promotion from `int` to `float` in expressions.

* **Comprehensive expression support**
  Arithmetic (`+`, `-`, `*`, `/`, `%`), logical (`&&`, `||`, `!`), comparison (`<`, `>`, `==`, `!=`), string concatenation (`.`), and file append (`<<`) operators are available with correct precedence and associativity.

* **Statements and control flow**
  Includes variable declarations, assignments, input/output (`read`, `write`), and structured control flow via `if`, `else`, `while`, and `for`.

* **Block scoping and semantic validation**
  Uses a symbol table and static type checker to ensure correctness at compile time, with meaningful error messages and line references.

* **File I/O system**
  The `file` type allows working with files using simple, expressive syntax:

  * `f = open("file.txt", "w");` to overwrite, or `"a"` to append
  * `f << "data" << 123;` to chain file output
    These are compiled into stack-based instructions (`push`, `fwrite`, `fappend N`).

* **Intermediate representation and VM**
  Generates stack-based code with a minimal instruction set. The virtual machine executes the code by interpreting instructions like `push`, `load`, `save`, `fappend`, `print`, `jmp`, and more.

* **Modular and extensible architecture**
  Clean separation between the parser, type checker, code generator, and VM makes it easy to add new features, types, or constructs.

---

## 📄 Language Specification (Mini-Lang)

**Data types**: `int`, `float`, `bool`, `string`  
**Statements**:
- Variable declarations
- Assignments
- Input/Output (`read`, `write`)
- Control flow (`if`, `else`, `while`, `for`)
- Blocks (`{ ... }`)
- Empty statements (`;`)

**Expressions**:
- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Logical: `&&`, `||`, `!`
- Comparison: `<`, `>`, `==`, `!=`
- String concatenation: `.`
- Type coercion: automatic int → float

**Comments**: `// single line comment`

---

## 📚 Example

---

## 🚀 How to Build and Run

---

## 🎯 Skills Demonstrated

- Grammar design and parsing (ANTLR)
- Abstract Syntax Tree (AST) navigation (visitor pattern)
- Static type checking
- Error handling and reporting
- Stack-based code generation
- Interpreter and virtual machine design
- Modular and clean Java code architecture
- Build automation with Maven

---

## 📎 Project Structure

```
AntlrCompiler/
├── src/
│   ├── main/
│   │   ├── antlr4/
│   │   │   └── cz/university/
│   │   │       └── Language.g4             # Grammar definition
│   │   └── java/cz/university/
│   │       ├── App.java                    # Main entry point
│   │       ├── SymbolTable.java            # Variable/type management
│   │       ├── TypeCheckerVisitor.java     # Type checking
│   │       ├── TypeException.java          # Type error handling
│   │       ├── VerboseListener.java        # Custom ANTLR error listener
│   │       ├── codegen/
│   │       │   ├── CodeGeneratorVisitor.java  # Stack-based code generation
│   │       │   └── Instruction.java           # Instruction model
│   │       └── runtime/
│   │           ├── StackMachine.java       # Stack-based virtual machine
│   │           └── FileHandle.java         # File handle abstraction
│
├── test/
│   ├── java/cz/university/
│   │   └── AppTest.java                    # JUnit test cases
│   └── resources/
│       ├── PLC_t1.in / .out                # Input/output test cases
│       ├── PLC_t2.in / .out
│       ├── PLC_t3.in / .out
│       └── test.lang                       # Custom sample program
│
└── pom.xml                                 # Maven build configuration
```
## 💬 Contact

If you are interested in collaboration or have any questions, feel free to reach out.

---

**✅ This project demonstrates a complete mini-compiler and interpreter pipeline, making it an excellent showcase for software engineering, compiler theory, and system design skills.**

---
