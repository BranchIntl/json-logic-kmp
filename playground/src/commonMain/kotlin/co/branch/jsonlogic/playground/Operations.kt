package co.branch.jsonlogic.playground

/**
 * One entry in the operations reference.
 *
 * Every [snippet] is self-contained — it evaluates against no data at all — so clicking one always
 * produces a working rule rather than something that needs a matching data document first.
 */
data class Operation(val symbol: String, val summary: String, val snippet: String)

data class OperationGroup(val name: String, val operations: List<Operation>)

/**
 * The 34 operations a fresh `JsonLogic()` registers, grouped as the README groups them, plus `var`.
 *
 * `var` is listed apart because it is not a registered operation: the parser treats it as rule
 * syntax, which is why it is not one of the 34.
 */
val OperationGroups: List<OperationGroup> = listOf(
    OperationGroup(
        name = "Data access",
        operations = listOf(
            Operation("var", "Read by dot path, with a fallback", """{"var": ["a.b", "fallback"]}"""),
            Operation("missing", "Which named keys the data lacks", """{"missing": ["a", "b"]}"""),
            Operation(
                "missing_some",
                "Missing keys, unless n are present",
                """{"missing_some": [1, ["a", "b"]]}""",
            ),
        ),
    ),
    OperationGroup(
        name = "Numeric",
        operations = listOf(
            Operation("+", "Add; a lone argument casts to a number", """{"+": [1, 2, 3]}"""),
            Operation("-", "Subtract, or negate a lone argument", """{"-": [5, 2]}"""),
            Operation("*", "Multiply", """{"*": [3, 4]}"""),
            Operation("/", "Divide the first by the second", """{"/": [10, 4]}"""),
            Operation("%", "Remainder after division", """{"%": [7, 3]}"""),
            Operation("min", "Smallest argument", """{"min": [3, 1, 2]}"""),
            Operation("max", "Largest argument", """{"max": [3, 1, 2]}"""),
            Operation(">", "Greater than", """{">": [3, 1]}"""),
            Operation(">=", "Greater than or equal", """{">=": [3, 3]}"""),
            Operation("<", "Less than; 3 arguments test a range", """{"<": [1, 2, 3]}"""),
            Operation("<=", "At most; 3 arguments test a range", """{"<=": [1, 2, 2]}"""),
        ),
    ),
    OperationGroup(
        name = "Logic and boolean",
        operations = listOf(
            Operation("if", "condition, then, …, else", """{"if": [true, "yes", "no"]}"""),
            Operation("?:", "Another spelling of if", """{"?:": [false, "yes", "no"]}"""),
            Operation("==", "Equal, with type coercion", """{"==": [1, "1"]}"""),
            Operation("!=", "Not equal, with type coercion", """{"!=": [1, 2]}"""),
            Operation("===", "Equal, without coercion", """{"===": [1, 1]}"""),
            Operation("!==", "Not equal, without coercion", """{"!==": [1, "1"]}"""),
            Operation("!", "Negate truthiness", """{"!": [false]}"""),
            Operation("!!", "Cast to a boolean by truthiness", """{"!!": ["hello"]}"""),
            Operation("and", "First falsy argument, else the last", """{"and": [true, "last"]}"""),
            Operation("or", "First truthy argument, else the last", """{"or": [false, "fallback"]}"""),
        ),
    ),
    OperationGroup(
        name = "Array",
        operations = listOf(
            Operation("map", "Apply a rule to every element", """{"map": [[1, 2, 3], {"*": [{"var": ""}, 2]}]}"""),
            Operation("filter", "Keep elements a rule finds truthy", """{"filter": [[1, 2, 3], {">": [{"var": ""}, 1]}]}"""),
            Operation(
                "reduce",
                "Fold; current and accumulator in scope",
                """{"reduce": [[1, 2, 3], {"+": [{"var": "current"}, {"var": "accumulator"}]}, 0]}""",
            ),
            Operation("all", "True when every element satisfies the rule", """{"all": [[1, 2], {">": [{"var": ""}, 0]}]}"""),
            Operation("some", "True when any element satisfies the rule", """{"some": [[1, 2], {">": [{"var": ""}, 1]}]}"""),
            Operation("none", "True when no element satisfies the rule", """{"none": [[1, 2], {">": [{"var": ""}, 9]}]}"""),
            Operation("merge", "Flatten the arguments into one array", """{"merge": [[1, 2], [3]]}"""),
            Operation("in", "In an array, or inside a string", """{"in": ["b", ["a", "b"]]}"""),
        ),
    ),
    OperationGroup(
        name = "String",
        operations = listOf(
            Operation("cat", "Concatenate into a string", """{"cat": ["Hello, ", "world"]}"""),
            Operation("substr", "Slice by offset and length", """{"substr": ["jsonlogic", 4]}"""),
        ),
    ),
    OperationGroup(
        name = "Miscellaneous",
        operations = listOf(
            Operation("log", "Pass a value through, logging it", """{"log": ["trace"]}"""),
        ),
    ),
)

/** Every operation across the groups, `var` included. */
val AllOperations: List<Operation> = OperationGroups.flatMap { it.operations }
