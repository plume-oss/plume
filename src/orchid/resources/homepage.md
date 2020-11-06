Provides a type-safe interface for connecting and writing to various graph databases based on the code-property graph 
schema. This driver currently supports the following graph databases:

* TinkerGraph
* JanusGraph
* TigerGraph
* Amazon Neptune
* Neo4j

The driver also provides methods such as `getWholeGraph`, `getNeighbours`, `getMethod`, etc. for reading the graph
database in the form of a `PlumeGraph` model.