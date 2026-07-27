package co.branch.jsonlogic

/** Base exception for JsonLogic parsing and evaluation failures, carrying the failing [jsonPath]. */
open class JsonLogicException : RuntimeException {
    val jsonPath: String

    constructor(message: String, jsonPath: String) : super(message) {
        this.jsonPath = jsonPath
    }

    constructor(cause: Throwable, jsonPath: String) : super(cause) {
        this.jsonPath = jsonPath
    }

    constructor(message: String, cause: Throwable, jsonPath: String) : super(message, cause) {
        this.jsonPath = jsonPath
    }
}
