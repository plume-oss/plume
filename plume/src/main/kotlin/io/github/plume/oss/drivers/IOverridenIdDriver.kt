package io.github.plume.oss.drivers

interface IOverridenIdDriver: IDriver {

    /**
     * Given a lower bound and an upper bound, return all vertex IDs which fall between these ranges in the database.
     *
     * @param lowerBound The lower bound for the result set.
     * @param upperBound The upper bound for the result set.
     */
    fun getVertexIds(lowerBound: Long, upperBound: Long): Set<Long>

}