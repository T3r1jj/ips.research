package io.github.t3r1jj.ips.research.model

import java.util.*

class LimitedQueue<E>(private var limit: Int) : LinkedList<E>() {

    override fun add(element: E): Boolean {
        val added = super.add(element)
        while (added && size > limit) {
            super.remove()
        }
        return added
    }

    override fun addAll(elements: Collection<E>): Boolean {
        var added = false
        for (e in elements) {
            add(e)
            added = true
        }
        return added
    }

    fun limit(newLimit: Int) {
        limit = Math.max(1, newLimit)
        while (size > limit) {
            super.remove()
        }
    }

}