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

**Data types**: `int`, `float`, `bool`, `string`, `file`

**Statements**:
* Variable declarations
* Assignments
* Input/Output (`read`, `write`)
* File I/O:
  * `file f;`
  * `f = open("filename.txt", "w" | "a");`
  * `f << "some text" << 123;`
* Control flow (`if`, `else`, `while`, `for`)
* Blocks (`{ ... }`)
* Empty statements (`;`)

**Expressions**:
* Arithmetic: `+`, `-`, `*`, `/`, `%`
* Logical: `&&`, `||`, `!`
* Comparison: `<`, `>`, `==`, `!=`
* String concatenation: `.`
* File appending: `<<`
* Type coercion: automatic int → float

**Comments**: `// single line comment`

---

## 📚 Example

```c
file f;
f = open("log.txt", "a");
f << "Session started: " << 2025;

int i;
i = 0;
while (i < 3) {
    f << "Line " << i;
    i = i + 1;
}
```

This example appends multiple lines to the file `log.txt`, demonstrating file declaration, opening, appending data, and control flow.

---



## 🚀 How to Build and Run

### Prerequisites 

* Java 17 or higher
* Maven 3.x
* ANTLR plugin (automatically handled by Maven)

### Build the Project 

In the root directory of the project, run:

```bash
mvn clean package
```

This compiles the project, generates ANTLR classes, and builds the `.jar`.

### Run a Program 

To compile and execute a `.lang` source file:

```bash
mvn exec:java -Dexec.mainClass=cz.university.App 
```
For PowerShell:

```powershell
mvn exec:java "-Dexec.mainClass=cz.university.App" 
```

This will:

1. Parse the input file.
2. Type-check it.
3. Generate stack-based instructions into `output.out`.
4. Execute the program via the built-in virtual machine.

### Run Unit Tests 

To run the included JUnit tests:

```bash
mvn test
```

Test cases are defined in `AppTest.java`, including validation of code generation and file operations.




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
