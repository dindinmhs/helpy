package com.example.helpy.domain.pathfinding

import com.example.helpy.domain.model.Graph
import com.example.helpy.domain.model.GraphNode
import java.util.*

/**
 * A* (A-Star) Pathfinding Algorithm Implementation
 * 
 * A* is an informed search algorithm that finds the optimal path from start to goal.
 * It uses a heuristic function to guide the search towards the goal efficiently.
 * 
 * Formula: f(n) = g(n) + h(n)
 * - f(n): Total estimated cost of path through node n
 * - g(n): Actual cost from start to node n  
 * - h(n): Heuristic estimated cost from node n to goal
 * 
 * This implementation uses:
 * - Priority Queue (open set) to always explore most promising nodes first
 * - Closed set to avoid revisiting nodes
 * - Haversine distance as heuristic function (admissible for geographic routing)
 */
class AStarPathfinder {
    /**
     * Node wrapper for A* algorithm containing path costs and parent reference
     */
    data class AStarNode(
        val node: GraphNode,
        val gScore: Double,      // Actual cost from start
        val fScore: Double,      // f(n) = g(n) + h(n)
        val parent: AStarNode?   // For path reconstruction
    ) : Comparable<AStarNode> {
        // Priority queue orders by fScore (lowest first)
        override fun compareTo(other: AStarNode): Int = fScore.compareTo(other.fScore)
    }

    /**
     * Main A* pathfinding function
     * 
     * @param graph: Road network represented as nodes and edges
     * @param start: Starting node
     * @param goal: Destination node
     * @return: List of nodes representing optimal path, empty if no path found
     */
    fun findPath(graph: Graph, start: GraphNode, goal: GraphNode): List<GraphNode> {
        println("🔬 === A* PATHFINDING ALGORITHM ===")
        println("📍 Start: ${start.id} (${start.lat}, ${start.lon})")
        println("🎯 Goal: ${goal.id} (${goal.lat}, ${goal.lon})")
        println("🗺️ Graph: ${graph.nodes.size} nodes, ${graph.edges.size} edge groups")
        
        // Open set: nodes to be evaluated (priority queue ordered by f-score)
        val openSet = PriorityQueue<AStarNode>()
        // Closed set: nodes already evaluated
        val closedSet = mutableSetOf<String>()
        // G-scores: actual cost from start to each node
        val gScores = mutableMapOf<String, Double>()

        // Initialize with start node: g(start) = 0, f(start) = h(start)
        val heuristicDistance = start.distanceTo(goal)
        println("📏 Initial heuristic distance: $heuristicDistance meters")
        
        openSet.add(AStarNode(start, 0.0, heuristicDistance, null))
        gScores[start.id] = 0.0

        var iterations = 0
        val maxIterations = 10000 // Prevent infinite loops

        while (openSet.isNotEmpty() && iterations < maxIterations) {
            iterations++
            // Get node with lowest f-score from open set
            val current = openSet.poll() ?: break

            // Goal reached! Reconstruct and return path
            if (current.node.id == goal.id) {
                println("🎉 PATH FOUND! After $iterations iterations")
                val finalPath = reconstructPath(current)
                println("📊 Final path: ${finalPath.size} nodes")
                println("📏 Total distance: ${current.gScore} meters")
                return finalPath
            }

            // Move current node from open to closed set
            closedSet.add(current.node.id)

            // Explore all neighbors of current node
            val neighbors = graph.edges[current.node.id] ?: emptyList()
            if (iterations % 100 == 0) { // Log every 100 iterations
                println("🔄 Iteration $iterations: exploring node ${current.node.id} with ${neighbors.size} neighbors")
            }

            for (edge in neighbors) {
                // Skip neighbors already evaluated
                if (edge.to.id in closedSet) continue

                // Calculate tentative g-score: g(current) + weight(current, neighbor)
                val tentativeGScore = current.gScore + edge.weight
                val currentGScore = gScores[edge.to.id] ?: Double.MAX_VALUE

                // If this path to neighbor is better than previous one
                if (tentativeGScore < currentGScore) {
                    // Update g-score and add/update neighbor in open set
                    gScores[edge.to.id] = tentativeGScore
                    val heuristic = edge.to.distanceTo(goal)
                    val fScore = tentativeGScore + heuristic // f(n) = g(n) + h(n)
                    openSet.add(AStarNode(edge.to, tentativeGScore, fScore, current))
                }
            }
        }

        if (iterations >= maxIterations) {
            println("⚠️ A* reached maximum iterations ($maxIterations)")
        } else {
            println("❌ A* found no path after $iterations iterations")
        }
        
        return emptyList() // No path found
    }

    /**
     * Reconstructs the optimal path by following parent pointers backward from goal to start
     * 
     * @param goalNode: The A* node representing the reached goal
     * @return: List of GraphNodes representing the path from start to goal
     */
    private fun reconstructPath(goalNode: AStarNode): List<GraphNode> {
        val path = mutableListOf<GraphNode>()
        var current: AStarNode? = goalNode

        // Follow parent pointers backward to build path
        while (current != null) {
            path.add(0, current.node) // Add to front to reverse order
            current = current.parent
        }

        return path
    }
}
