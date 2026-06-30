package com.loganmartlew.rangework.shared.data

/** Assemble one parent with its children sorted by [childOrder]. Used by get(). */
internal fun <Parent, Child, Model, Order : Comparable<Order>> assembleParentWithChildren(
    parent: Parent,
    children: List<Child>,
    childOrder: (Child) -> Order,
    toModel: (Parent, List<Child>) -> Model,
): Model = toModel(parent, children.sortedBy(childOrder))

/**
 * Group [children] under their parent, assemble each parent into a model with its
 * children ordered by [childOrder], then sort the models by [modelSort] descending.
 *
 * Children whose parent id is not present in [parents] are dropped. Used by list().
 */
internal fun <Parent, Child, Id, Model, Order : Comparable<Order>, SortKey : Comparable<SortKey>>
assembleParentsWithChildren(
    parents: List<Parent>,
    children: List<Child>,
    parentId: (Parent) -> Id,
    childParentId: (Child) -> Id,
    childOrder: (Child) -> Order,
    toModel: (Parent, List<Child>) -> Model,
    modelSort: (Model) -> SortKey,
): List<Model> {
    val childrenByParent = children.groupBy(childParentId)
    return parents
        .map { parent ->
            assembleParentWithChildren(
                parent = parent,
                children = childrenByParent[parentId(parent)].orEmpty(),
                childOrder = childOrder,
                toModel = toModel,
            )
        }
        .sortedByDescending(modelSort)
}