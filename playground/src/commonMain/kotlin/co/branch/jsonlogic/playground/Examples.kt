package co.branch.jsonlogic.playground

/** A rule and the data it is meant to run against, loaded together by the example chips. */
data class Example(val label: String, val rule: String, val data: String)

/**
 * The presets offered above the editors, one per operation family.
 *
 * Hand-written rather than drawn from the repository's fixture corpus: those cases exist to pin
 * down edge-case behaviour and mostly read as degenerate, which is the opposite of what an
 * introduction to the language needs.
 */
val Examples: List<Example> = listOf(
    Example(
        label = "Range check",
        rule = """
            {"and": [
              {">": [{"var": "temp"}, 18]},
              {"<": [{"var": "temp"}, 26]}
            ]}
        """.trimIndent(),
        data = """{"temp": 21}""",
    ),
    Example(
        label = "Variables",
        rule = """{"var": ["user.address.city", "unknown"]}""",
        data = """
            {"user": {
              "name": "Ada",
              "address": {"city": "London"}
            }}
        """.trimIndent(),
    ),
    Example(
        label = "Branching",
        rule = """
            {"if": [
              {">=": [{"var": "score"}, 90]}, "A",
              {">=": [{"var": "score"}, 80]}, "B",
              {">=": [{"var": "score"}, 70]}, "C",
              "F"
            ]}
        """.trimIndent(),
        data = """{"score": 84}""",
    ),
    Example(
        label = "Map",
        rule = """
            {"map": [
              {"var": "cart"},
              {"*": [{"var": "price"}, 100]}
            ]}
        """.trimIndent(),
        data = """
            {"cart": [
              {"price": 12.5},
              {"price": 7}
            ]}
        """.trimIndent(),
    ),
    Example(
        label = "Filter and reduce",
        rule = """
            {"reduce": [
              {"filter": [{"var": "cart"}, {">": [{"var": "price"}, 0]}]},
              {"+": [{"var": "accumulator"}, {"var": "current.price"}]},
              0
            ]}
        """.trimIndent(),
        data = """
            {"cart": [
              {"price": 12.5},
              {"price": 0},
              {"price": 7}
            ]}
        """.trimIndent(),
    ),
    Example(
        label = "Membership",
        rule = """
            {"some": [
              {"var": "roles"},
              {"in": [{"var": ""}, ["admin", "owner"]]}
            ]}
        """.trimIndent(),
        data = """{"roles": ["viewer", "admin"]}""",
    ),
    Example(
        label = "Strings",
        rule = """
            {"cat": [
              {"substr": [{"var": "sku"}, 0, 3]},
              "-",
              {"substr": [{"var": "sku"}, -4]}
            ]}
        """.trimIndent(),
        data = """{"sku": "KMP20260727"}""",
    ),
    Example(
        label = "Missing fields",
        rule = """{"missing": ["name", "email", "phone"]}""",
        data = """{"name": "Ada"}""",
    ),
    Example(
        label = "Truthiness",
        rule = """
            {"and": [
              {"!!": [{"var": "nickname"}]},
              {"==": [1, "1"]},
              {"!": [{"===": [1, "1"]}]}
            ]}
        """.trimIndent(),
        data = """{"nickname": ""}""",
    ),
)
