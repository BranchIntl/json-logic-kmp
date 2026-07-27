package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.JsonLogicException

/** Thrown when a parsed rule cannot be evaluated, carrying the failing sub-expression's json path. */
class JsonLogicEvaluationException : JsonLogicException {
    constructor(message: String, jsonPath: String) : super(message, jsonPath)
    constructor(cause: Throwable, jsonPath: String) : super(cause, jsonPath)
    constructor(message: String, cause: Throwable, jsonPath: String) : super(message, cause, jsonPath)
}
