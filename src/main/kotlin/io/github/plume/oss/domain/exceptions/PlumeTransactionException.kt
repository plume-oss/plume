package io.github.plume.oss.domain.exceptions

/**
 * Thrown to indicate a transaction related failure when attempting to create or commit a graph database transaction.
 */
class PlumeTransactionException(message: String) : Exception(message)