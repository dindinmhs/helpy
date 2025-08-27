package com.example.helpy.data.repository

import com.example.helpy.data.model.OverpassResponse
import com.example.helpy.domain.model.Graph
import com.example.helpy.domain.model.GraphEdge
import com.example.helpy.domain.model.GraphNode
import kotlin.math.*

class GraphBuilder {
    private data class NodeKey(val lat: Double, val lon: Double) {
        companion object {
            private const val PRECISION = 1000000.0 // 6 decimal places precision
        }

        fun isNear(other: NodeKey, tolerance: Double = 0.00001): Boolean {
            return abs(lat - other.lat) < tolerance && abs(lon - other.lon) < tolerance
        }

        fun rounded(): NodeKey {
            return NodeKey(
                (lat * PRECISION).roundToInt() / PRECISION,
                (lon * PRECISION).roundToInt() / PRECISION
            )
        }
    }

    fun buildGraph(overpassResponse: OverpassResponse): Graph {
        val nodeMap = mutableMapOf<NodeKey, GraphNode>()
        val edges = mutableMapOf<String, MutableList<GraphEdge>>()
        val nodeConnections = mutableMapOf<NodeKey, MutableSet<String>>() // Track which ways connect to each node

        // First pass: collect all unique nodes and track way connections
        for (element in overpassResponse.elements) {
            if (element.type == "way" && element.geometry != null && element.geometry.isNotEmpty()) {
                val wayId = element.id.toString()

                for ((index, geometry) in element.geometry.withIndex()) {
                    val nodeKey = NodeKey(geometry.lat, geometry.lon).rounded()

                    // Create or get existing node
                    if (!nodeMap.containsKey(nodeKey)) {
                        val nodeId = "${nodeKey.lat}_${nodeKey.lon}"
                        nodeMap[nodeKey] = GraphNode(nodeId, nodeKey.lat, nodeKey.lon)
                    }

                    // Track way connections for intersection detection
                    nodeConnections.getOrPut(nodeKey) { mutableSetOf() }.add(wayId)
                }
            }
        }

        // Second pass: create edges within ways and at intersections
        for (element in overpassResponse.elements) {
            if (element.type == "way" && element.geometry != null && element.geometry.size > 1) {
                val wayNodes = mutableListOf<GraphNode>()

                // Get nodes for this way
                for (geometry in element.geometry) {
                    val nodeKey = NodeKey(geometry.lat, geometry.lon).rounded()
                    nodeMap[nodeKey]?.let { wayNodes.add(it) }
                }

                // Create edges between consecutive nodes in the way
                for (i in 0 until wayNodes.size - 1) {
                    val fromNode = wayNodes[i]
                    val toNode = wayNodes[i + 1]
                    val distance = fromNode.distanceTo(toNode)

                    // Check if this is a one-way street
                    val isOneWay = element.tags?.get("oneway") == "yes"

                    // Add forward edge
                    edges.getOrPut(fromNode.id) { mutableListOf() }.add(
                        GraphEdge(fromNode, toNode, distance)
                    )

                    // Add backward edge only if not one-way
                    if (!isOneWay) {
                        edges.getOrPut(toNode.id) { mutableListOf() }.add(
                            GraphEdge(toNode, fromNode, distance)
                        )
                    }
                }

                // Connect intersection nodes to enable way-to-way navigation
                connectIntersectionNodes(wayNodes, nodeConnections, nodeMap, edges)
            }
        }

        return Graph(nodeMap.values.associateBy { it.id }, edges)
    }

    private fun connectIntersectionNodes(
        wayNodes: List<GraphNode>,
        nodeConnections: Map<NodeKey, Set<String>>,
        nodeMap: Map<NodeKey, GraphNode>,
        edges: MutableMap<String, MutableList<GraphEdge>>
    ) {
        // Check start and end nodes of the way for intersections
        val startNode = wayNodes.first()
        val endNode = wayNodes.last()

        listOf(startNode, endNode).forEach { node ->
            val nodeKey = NodeKey(node.lat, node.lon).rounded()
            val connectedWays = nodeConnections[nodeKey] ?: emptySet()

            // If this node connects multiple ways (intersection), create connections
            if (connectedWays.size > 1) {
                // Find nearby nodes from other ways
                val nearbyNodes = findNearbyIntersectionNodes(nodeKey, nodeMap, 0.00002) // ~2 meter tolerance

                nearbyNodes.forEach { nearbyNode ->
                    if (nearbyNode.id != node.id) {
                        val distance = node.distanceTo(nearbyNode)

                        // Add bidirectional connection at intersection
                        edges.getOrPut(node.id) { mutableListOf() }.add(
                            GraphEdge(node, nearbyNode, distance)
                        )
                        edges.getOrPut(nearbyNode.id) { mutableListOf() }.add(
                            GraphEdge(nearbyNode, node, distance)
                        )
                    }
                }
            }
        }
    }

    private fun findNearbyIntersectionNodes(
        targetKey: NodeKey,
        nodeMap: Map<NodeKey, GraphNode>,
        tolerance: Double
    ): List<GraphNode> {
        return nodeMap.filter { entry ->
            entry.key.isNear(targetKey, tolerance)
        }.map { entry ->
            entry.value
        }
    }
}
