package co.branch.jsonlogic.ast

import co.branch.jsonlogic.JsonLogicException

/** Thrown when a JSON value cannot be parsed into a [JsonLogicNode] tree. */
class JsonLogicParseException : JsonLogicException {
    constructor(message: String, jsonPath: String) : super(message, jsonPath)
    constructor(cause: Throwable, jsonPath: String) : super(cause, jsonPath)
    constructor(message: String, cause: Throwable, jsonPath: String) : super(message, cause, jsonPath)
}
