package co.branch.jsonlogic.ast

/** A node in a parsed JsonLogic rule tree. */
sealed interface JsonLogicNode

data class JsonLogicString(val value: String) : JsonLogicNode

data class JsonLogicNumber(val value: Double) : JsonLogicNode

data class JsonLogicBoolean(val value: Boolean) : JsonLogicNode {
    companion object {
        val TRUE = JsonLogicBoolean(true)
        val FALSE = JsonLogicBoolean(false)
    }
}

data object JsonLogicNull : JsonLogicNode

/** An ordered, immutable list of nodes, e.g. the arguments of an operation. */
data class JsonLogicArray(val elements: List<JsonLogicNode>) : JsonLogicNode

/** The `{"var": [key, default]}` sugar: looks up `key`, falling back to `default` when absent. */
data class JsonLogicVariable(val key: JsonLogicNode, val defaultValue: JsonLogicNode) : JsonLogicNode

/** A single-key operation object, e.g. `{"+": [1, 2]}`, normalized to array arguments. */
data class JsonLogicOperation(val operator: String, val arguments: JsonLogicArray) : JsonLogicNode
