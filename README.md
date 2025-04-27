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

- Custom grammar (`Language.g4`) for a small typed programming language
- **Fully featured type system**: `int`, `float`, `bool`, `string`
- **Automatic int → float type promotion** inside expressions
- **Clear operator precedence** and associativity handling
- **Syntactic and semantic error reporting**
- **Stack-based Virtual Machine** with custom instruction set:
  - Arithmetic operations
  - Logical operations
  - Comparisons
  - String operations
  - Variable assignments
  - Control flow (`if`, `while`, `for`)
  - I/O operations (`read`, `write`)
- **Code modularity**: clear separation between parsing, type checking, code generation, and execution
- Designed with **expandability** in mind (easy to add new features)

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
 │    ├── main/
 │    │    ├── antlr4/         # Language grammar (Language.g4)
 │    │    └── java/cz/university/
 │    │         ├── App.java           # Main entry point
 │    │         ├── StackMachine.java  # Virtual machine
 │    │         ├── CodeGeneratorVisitor.java # Code generator
 │    │         ├── TypeCheckerVisitor.java   # Type checker
 │    │         └── SymbolTable.java    # Variable/type management
 │    └── test/resources/  # Test programs (.lang)
 └── pom.xml               # Maven configuration
```

## 💬 Contact

If you are interested in collaboration or have any questions, feel free to reach out.

---

**✅ This project demonstrates a complete mini-compiler and interpreter pipeline, making it an excellent showcase for software engineering, compiler theory, and system design skills.**

---
