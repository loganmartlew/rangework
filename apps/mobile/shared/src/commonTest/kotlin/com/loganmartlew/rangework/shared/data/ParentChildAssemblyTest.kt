package com.loganmartlew.rangework.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private data class P(val id: String, val rank: Int)
private data class C(val pid: String, val order: Int, val tag: String)
private data class M(val id: String, val children: List<String>)

class ParentChildAssemblyTest {

    @Test
    fun `grouping — each parent gets exactly its own children, none leak across parents`() {
        val parents = listOf(
            P("a", 1),
            P("b", 2),
            P("c", 3),
        )
        val children = listOf(
            C("a", 1, "a1"),
            C("b", 1, "b1"),
            C("a", 2, "a2"),
            C("c", 1, "c1"),
            C("b", 2, "b2"),
        )

        val result = assembleParentsWithChildren(
            parents = parents,
            children = children,
            parentId = P::id,
            childParentId = C::pid,
            childOrder = C::order,
            toModel = { p, cs -> M(p.id, cs.map { it.tag }) },
            modelSort = M::id,
        )

        assertEquals(3, result.size)
        val byId = result.associateBy { it.id }
        assertEquals(listOf("a1", "a2"), byId["a"]?.children)
        assertEquals(listOf("b1", "b2"), byId["b"]?.children)
        assertEquals(listOf("c1"), byId["c"]?.children)
    }

    @Test
    fun `ordering — children sorted by childOrder ascending, models sorted by modelSort descending`() {
        val parents = listOf(
            P("x", 1),
            P("y", 2),
        )
        val children = listOf(
            C("x", 3, "third"),
            C("x", 1, "first"),
            C("x", 2, "second"),
            C("y", 5, "fifth"),
            C("y", 4, "fourth"),
        )

        val result = assembleParentsWithChildren(
            parents = parents,
            children = children,
            parentId = P::id,
            childParentId = C::pid,
            childOrder = C::order,
            toModel = { p, cs -> M(p.id, cs.map { it.tag }) },
            modelSort = M::id,
        )

        // Models sorted by id descending: y before x
        assertEquals(listOf("y", "x"), result.map { it.id })
        // Children sorted by order ascending
        assertEquals(listOf("first", "second", "third"), result.find { it.id == "x" }?.children)
        assertEquals(listOf("fourth", "fifth"), result.find { it.id == "y" }?.children)
    }

    @Test
    fun `empty-children — parent with no matching children yields empty list, not dropped`() {
        val parents = listOf(
            P("a", 1),
            P("b", 2),
        )
        val children = listOf(
            C("a", 1, "a1"),
        )

        val result = assembleParentsWithChildren(
            parents = parents,
            children = children,
            parentId = P::id,
            childParentId = C::pid,
            childOrder = C::order,
            toModel = { p, cs -> M(p.id, cs.map { it.tag }) },
            modelSort = M::id,
        )

        assertEquals(2, result.size)
        val byId = result.associateBy { it.id }
        assertEquals(listOf("a1"), byId["a"]?.children)
        assertEquals(emptyList(), byId["b"]?.children)
    }

    @Test
    fun `orphaned-child — child whose parent id is absent from parents is dropped`() {
        val parents = listOf(
            P("a", 1),
        )
        val children = listOf(
            C("a", 1, "a1"),
            C("orphan", 1, "orphan"),
        )

        val result = assembleParentsWithChildren(
            parents = parents,
            children = children,
            parentId = P::id,
            childParentId = C::pid,
            childOrder = C::order,
            toModel = { p, cs -> M(p.id, cs.map { it.tag }) },
            modelSort = M::id,
        )

        assertEquals(1, result.size)
        assertEquals(listOf("a1"), result.first().children)
    }

    @Test
    fun `empty-parents — returns empty list`() {
        val result = assembleParentsWithChildren(
            parents = emptyList<P>(),
            children = listOf(C("a", 1, "a1")),
            parentId = P::id,
            childParentId = C::pid,
            childOrder = C::order,
            toModel = { p, cs -> M(p.id, cs.map { it.tag }) },
            modelSort = M::id,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `single-parent — assembleParentWithChildren sorts children and passes to toModel`() {
        val parent = P("a", 1)
        val children = listOf(
            C("a", 3, "third"),
            C("a", 1, "first"),
            C("a", 2, "second"),
        )

        val result = assembleParentWithChildren(
            parent = parent,
            children = children,
            childOrder = C::order,
            toModel = { p, cs -> M(p.id, cs.map { it.tag }) },
        )

        assertEquals("a", result.id)
        assertEquals(listOf("first", "second", "third"), result.children)
    }
}