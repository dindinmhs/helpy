package com.example.helpy.domain.pathfinding

import com.example.helpy.domain.model.Graph
import com.example.helpy.domain.model.GraphNode
import java.util.*

class AStarPathfinder {
    data class AStarNode(
        val node: GraphNode,
        val gScore: Double,
        val fScore: Double,
        val parent: AStarNode?
    ) : Comparable<AStarNode> {
        override fun compareTo(other: AStarNode): Int = fScore.compareTo(other.fScore)
    }

    fun findPath(graph: Graph, start: GraphNode, goal: GraphNode): List<GraphNode> {
        val openSet = PriorityQueue<AStarNode>()
        val closedSet = mutableSetOf<String>()
        val gScores = mutableMapOf<String, Double>()

        openSet.add(AStarNode(start, 0.0, start.distanceTo(goal), null))
        gScores[start.id] = 0.0

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()

            if (current.node.id == goal.id) {
                return reconstructPath(current)
            }

            closedSet.add(current.node.id)

            val neighbors = graph.edges[current.node.id] ?: emptyList()
            for (edge in neighbors) {
                if (edge.to.id in closedSet) continue

                val tentativeGScore = current.gScore + edge.weight
                val currentGScore = gScores[edge.to.id] ?: Double.MAX_VALUE

                if (tentativeGScore < currentGScore) {
                    gScores[edge.to.id] = tentativeGScore
                    val fScore = tentativeGScore + edge.to.distanceTo(goal)
                    openSet.add(AStarNode(edge.to, tentativeGScore, fScore, current))
                }
            }
        }

        return emptyList() // No path found
    }

    private fun reconstructPath(goalNode: AStarNode): List<GraphNode> {
        val path = mutableListOf<GraphNode>()
        var current: AStarNode? = goalNode

        while (current != null) {
            path.add(0, current.node)
            current = current.parent
        }

        return path
    }
}
