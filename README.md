# MiniCompiler — 从源码到三地址码的Java编译器前端

> 一个基于java实现的、可运行的编译器前端项目。  
> 目标语言是C语言子集（支持数组、`if/while/do/break`），输出三地址中间代码。  
> 本项目可作为编译原理课程设计的起点，也适合自学手写编译器的同学参考。

---

## 项目简介

本项目实现了一个**完整的编译器前端**，包括：

- **词法分析器（Lexer）**：将源代码字符串识别为Token流
- **语法分析器（Parser）**：采用递归下降法，构建抽象语法树（AST）
- **语义分析**：通过符号表（Env）进行类型检查与作用域管理
- **中间代码生成**：输出三地址码（Three-Address Code）

所有代码严格按包组织，**不依赖任何第三方库**，直接用`javac`编译运行。

---

## 快速开始

### 环境要求
- JDK 8 及以上
- 命令行终端（Linux/Mac/Windows Git Bash）

### 下载与编译
```bash
git clone https://github.com/your-username/MiniCompiler.git
cd MiniCompiler

# 按依赖顺序编译各包
javac lexer/*.java
javac symbols/*.java
javac inter/*.java
javac parser/*.java
javac main/*.java
```

### 运行

直接输入源代码（以 `{` 开头，`}` 结尾），按 `Ctrl+D`（Linux/Mac）或 `Ctrl+Z`（Windows）结束输入。

## 项目结构

```
Compiler/
├── main/                    # 程序入口
│   └── Main.java
│
├── lexer/                   # 词法分析器
│   ├── Lexer.java           # 核心扫描器（状态机）
│   ├── Token.java           # 词法单元基类
│   ├── Num.java             # 整数
│   ├── Real.java            # 浮点数
│   ├── Word.java            # 标识符 / 保留字 / 复合运算符
│   └── Tag.java             # 词法常量（AND, OR, IF, WHILE...）
│
├── symbols/                 # 符号表与类型系统
│   ├── Env.java             # 符号表（支持嵌套作用域链）
│   ├── Type.java            # 基本类型（int / float / bool / char）
│   └── Array.java           # 数组类型
│
├── inter/                   # 抽象语法树（AST）结点
│   ├── Node.java            # 基类（标号管理、代码生成辅助）
│   ├── Expr.java            # 表达式结点基类
│   ├── Stmt.java            # 语句结点基类
│   ├── Id.java              # 标识符（变量）
│   ├── Temp.java            # 临时变量（t1, t2, ...）
│   ├── Op.java              # 运算结点基类（提供 reduce 实现）
│   ├── Arith.java           # 双目算术运算（+ - * /）
│   ├── Unary.java           # 单目运算（-）
│   ├── Constant.java        # 常量（数字 / true / false）
│   ├── Logical.java         # 逻辑运算基类（&& ||）
│   ├── Or.java              # 逻辑或
│   ├── And.java             # 逻辑与
│   ├── Not.java             # 逻辑非
│   ├── Rel.java             # 关系运算（< <= == != >= >）
│   ├── Access.java          # 数组访问
│   ├── If.java              # if 语句
│   ├── Else.java            # if-else 语句
│   ├── While.java           # while 循环
│   ├── Do.java              # do-while 循环
│   ├── Set.java             # 变量赋值
│   ├── SetElem.java         # 数组元素赋值
│   ├── Seq.java             # 语句序列
│   └── Break.java           # break 语句
│
└── parser/                  # 语法分析器
    └── Parser.java          # 递归下降解析器（核心）
```

---

## 核心设计思想

### 1. 递归下降 + 文法分层解决优先级

```ebnf
expr   -> term ((PLUS | MINUS) term)*
term   -> factor ((MUL | DIV) factor)*
factor -> INT | ID | LPAREN expr RPAREN
```

每个非终结符对应一个 Java 方法，运算符优先级由调用链深度决定：
- `expr()` 调用 `term()`，`term()` 调用 `factor()`
- 乘法优先级高于加法，因为它被更深的调用链处理

### 2. 符号表的嵌套作用域

`Env` 通过 `prev` 指针形成链表：

```java
public class Env {
    private Hashtable<Token, Id> table;
    protected Env prev;   // 指向上层作用域
}
```

查找变量时从当前作用域逐层向上搜索，实现变量屏蔽（shadowing）。

### 3. 跳转代码生成布尔表达式

`expr.jumping(t, f)` 为布尔表达式生成跳转指令：
- `t`：表达式为真时的跳转目标
- `f`：表达式为假时的跳转目标
- 标号 `0` 表示“控制流穿越到下一指令”，不生成跳转

例如 `if (a < b) x = 1;` 生成：

```
    if a < b goto L1
    goto L2
L1: x = 1
L2:
```

### 4. 临时变量管理

`Temp` 类自动分配 `t1, t2, ...`，每个临时变量都有类型：

```java
public class Temp extends Expr {
    static int count = 0;
    int number = 0;
    public Temp(Type p) {
        super(Word.temp, p);
        number = ++count;
    }
    public String toString() { return "t" + number; }
}
```

---

## 代码阅读顺序建议

如果你是第一次阅读这个项目，建议按以下顺序：

1. **`lexer/Lexer.java`** —— 看它如何把字符串变成 Token
2. **`parser/Parser.java` 的 `expr()` / `term()` / `factor()`** —— 看递归下降如何解析表达式
3. **`inter/Arith.java`** —— 看 AST 结点如何生成三地址码
4. **`parser/Parser.java` 的 `program()` / `block()` / `stmt()`** —— 看完整程序如何被解析
5. **`symbols/Env.java`** —— 看符号表如何管理作用域

---

## 🔧 扩展计划（Roadmap）

本项目不仅是一个学习示例，更是**亲手造轮子**的起点。以下是我计划的后续扩展方向：

### 已完成部分

- [x] 词法分析（支持 `int/float/bool/char`、数组、`&& || == != <= >=`）
- [x] 递归下降语法分析
- [x] 符号表与类型检查
- [x] 三地址码生成（含跳转代码）

### 短期计划（前端增强，1~2周）

- [ ] 支持 `for` 循环
- [ ] 支持 `continue` 语句
- [ ] 支持单行（`//`）和多行（`/* */`）注释
- [ ] 支持 `string` 类型
- [ ] 支持函数声明与调用（含参数传递）

### 中期计划（中间表示与优化，2~3周）

- [ ] 将 `emit()` 字符串输出改为结构化 IR（`List<IRInstr>`）
- [ ] 基本块划分与控制流图（CFG）构建
- [ ] 常量传播与折叠优化（如 `2+3*4` → `14`）
- [ ] 窥孔优化（消除冗余加载/存储）

### 长期计划（后端模拟，3~4周）

- [ ] 设计虚拟机指令集（LOAD / STORE / ADD / SUB / CMP / JMP / CALL / RET）
- [ ] 简单的寄存器分配（线性扫描算法）
- [ ] 将三地址码翻译为虚拟机字节码
- [ ] 实现虚拟机执行器（取指-译码-执行循环）
- [ ] 支持 `read` / `write` 输入输出

### 终极挑战

- [ ] 用本编译器生成 C 语言代码，实现自举（Bootstrapping）

---

## 参考资料

| 类型 | 名称 | 用途 |
| :--- | :--- | :--- |
| 教材 | **《编译原理》（龙书）第 2 版 附录 A** | 本项目代码的直接来源 |
| 实战 | **《编程语言实现模式》**（Terence Parr） | 手写递归下降的 Java 代码模板 |
| 工程 | **《编译器设计》（Engineering a Compiler）** | 工程化视角的编译器实现 |

---

## 贡献与反馈

本项目是学习型项目，欢迎提 Issue 和 PR。

---

## License

MIT License — 自由使用，保留作者声明即可。

---

**Happy Compiling!** 🚀