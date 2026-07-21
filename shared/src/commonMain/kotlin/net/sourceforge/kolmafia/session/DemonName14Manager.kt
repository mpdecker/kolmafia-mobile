package net.sourceforge.kolmafia.session

/**
 * Finds valid demon 14 names from radio grey-text segments.
 * Direct port of desktop [DemonName14Manager].
 */
object DemonName14Manager {

    private val SYLLABLES = setOf(
        "Arg", "Bal", "Bar", "Bob", "But", "Cak", "Cal", "Call", "Car", "Col", "Cor", "Cul",
        "Cur", "Cut", "Dak", "Dar", "Dor", "Gar", "Ger", "Gra", "Gur", "Har", "Hur", "Hut", "Kar",
        "Kil", "Kir", "Kru", "Kul", "Kur", "Lag", "Lar", "Mor", "Nar", "Nix", "Nut", "Pha", "Rog",
        "Yer",
    )

    class Graph {
        data class GraphNode(val syllable: String, val segments: MutableSet<String>)
        data class GraphEdge(val from: String, val to: String, val segments: MutableSet<String>)

        private val nodeMap = mutableMapOf<String, GraphNode>()
        private val edgeMap = mutableMapOf<String, GraphEdge>()
        val segments = mutableSetOf<String>()

        val nodes: Collection<GraphNode> get() = nodeMap.values
        val edges: Collection<GraphEdge> get() = edgeMap.values

        fun addNode(syllable: String, segmentSegments: Set<String>? = null) {
            val node = nodeMap.getOrPut(syllable) { GraphNode(syllable, mutableSetOf()) }
            if (segmentSegments != null) {
                node.segments.addAll(segmentSegments)
            }
        }

        fun addNode(syllable: String, segment: String) {
            addNode(syllable, setOf(segment))
        }

        fun addEdge(from: String, to: String, segment: String) {
            val key = "$from->$to"
            val edge = edgeMap.getOrPut(key) { GraphEdge(from, to, mutableSetOf()) }
            edge.segments.add(segment)
        }

        fun addSegment(segment: String) {
            segments.add(segment)
        }

        companion object {
            fun createFromSegment(segment: String): Graph {
                val graph = Graph()
                graph.addSegment(segment)

                for (syllable in SYLLABLES) {
                    if (syllable.contains(segment)) {
                        graph.addNode(syllable, segment)
                    }
                }

                for (from in SYLLABLES) {
                    for (to in SYLLABLES) {
                        for (splitPos in 1 until 3) {
                            if (splitPos >= segment.length) continue
                            val fromPart = segment.substring(0, splitPos)
                            val toPart = segment.substring(splitPos)
                            if (from.endsWith(fromPart) && to.startsWith(toPart)) {
                                graph.addNode(from)
                                graph.addNode(to)
                                graph.addEdge(from, to, segment)
                            }
                        }
                    }
                }

                return graph
            }

            fun createFromSegments(segments: Collection<String>): Graph {
                val graph = Graph()
                for (segment in segments) {
                    val segmentGraph = createFromSegment(segment)
                    graph.addSegment(segment)
                    for (edge in segmentGraph.edges) {
                        graph.addEdge(edge.from, edge.to, segment)
                    }
                    for (node in segmentGraph.nodes) {
                        graph.addNode(node.syllable, node.segments.toSet())
                    }
                }
                return graph
            }
        }
    }

    private data class SolverPath(
        val syllables: List<String>,
        val usedSegments: Set<String>,
    )

    private data class SolverResult(
        val demonName: String,
        val path: List<String>,
        val usedSegments: Set<String>,
    ) {
        override fun hashCode(): Int = demonName.hashCode()
    }

    fun solve(segments: Collection<String>): Set<String> {
        if (segments.isEmpty()) return emptySet()
        return solveGraph(Graph.createFromSegments(segments))
    }

    private fun solveGraph(graph: Graph): Set<String> {
        val results = mutableSetOf<SolverResult>()
        val allSegments = graph.segments.toSet()

        for (node in graph.nodes) {
            val initialPath = SolverPath(listOf(node.syllable), emptySet())
            dfs(graph, node.syllable, initialPath, allSegments, results)
        }

        return results.map { it.demonName }.toSet()
    }

    private fun dfs(
        graph: Graph,
        currentSyllable: String,
        currentPath: SolverPath,
        requiredSegments: Set<String>,
        results: MutableSet<SolverResult>,
    ) {
        if (currentPath.syllables.size == 9) {
            val allUsed = currentPath.usedSegments.toMutableSet()
            allUsed.addAll(currentPath.syllables)
            if (!allUsed.containsAll(requiredSegments)) return
            val demonName = currentPath.syllables.joinToString("")
            results.add(SolverResult(demonName, currentPath.syllables, currentPath.usedSegments))
            return
        }

        if (currentPath.syllables.size >= 9) return

        val node = graph.nodes.firstOrNull { it.syllable == currentSyllable }
        val outgoingEdges = graph.edges.filter { it.from == currentSyllable }

        for (edge in outgoingEdges) {
            val newSyllables = currentPath.syllables + edge.to
            val newUsedSegments = currentPath.usedSegments.toMutableSet()
            newUsedSegments.addAll(edge.segments)
            if (node != null) newUsedSegments.addAll(node.segments)
            dfs(graph, edge.to, SolverPath(newSyllables, newUsedSegments), requiredSegments, results)
        }
    }
}
