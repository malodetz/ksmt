package org.ksmt.expr.rewrite

class AndInverterGraph {

    var root: AIGNode? = null
    private val nodes: HashMap<Int, AIGNode> = HashMap()
    private fun getNode(id: Int): AIGNode {
        if (!nodes.containsKey(id)) {
            nodes[id] = AIGNode(id)
        }
        return nodes[id]!!
    }

    //type == true means that edge is inverted
    fun addEdge(id1: Int, id2: Int, type: Boolean) {
        val a = getNode(id1)
        val b = getNode(id2)
        a.children.add(Pair(b, type))
    }

    fun setRoot(id: Int) {
        root = getNode(id)
    }

    class AIGNode(val id: Int) {
        val children: ArrayList<Pair<AIGNode, Boolean>> = ArrayList()
    }
}