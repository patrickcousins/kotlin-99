package org.kotlin99.misc

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.kotlin99.common.fill
import org.kotlin99.common.tail
import org.kotlin99.common.toSeq
import org.kotlin99.misc.Nonogram.Box
import org.kotlin99.misc.Nonogram.Companion.parse
import org.kotlin99.misc.Nonogram.Constraint
import java.util.*

@Suppress("unused") // Because this class is a "namespace".
class Nonogram {

    data class Board(val width: Int, val height: Int,
                     val rowConstrains: List<Constraint>, val columnConstraints: List<Constraint>,
                     private val cells: List<ArrayList<Boolean>> = 1.rangeTo(height).map{ ArrayList<Boolean>().fill(width, false)}) {

        fun solve(rowIndex: Int = 0): Sequence<Board> {
            if (hasContradiction()) return emptySequence()
            if (rowIndex == height) return sequenceOf(this)

            return rowConstrains.toList()[rowIndex].possibleBoxes(width)
                .flatMap { rowBoxes ->
                    this.copy().apply(rowBoxes, rowIndex).solve(rowIndex + 1)
                }
        }

        private fun hasContradiction(): Boolean {
            fun Constraint.match(heights: List<Int>): Boolean {
                if (heights.size > boxes.size) return false
                if (heights.isEmpty()) return true
                if (heights.size == 1 && heights.first() <= boxes.first()) return true
                if (boxes.first() == heights.first()) return Constraint(boxes.tail()).match(heights.tail())
                return false
            }

            val boxHeights = 0.rangeTo(width - 1).map{ columnBoxHeights(it) }
            return !columnConstraints.zip(boxHeights).all {
                it.first.match(it.second)
            }
        }

        fun columnBoxHeights(column: Int): List<Int> {
            val columnCells = 0.rangeTo(height - 1).map{ cells[it][column] }

            val result = ArrayList<Int>()
            var lastCell = false
            var boxHeight = 0
            columnCells.forEach { cell ->
                if (lastCell && !cell && boxHeight != 0) {
                    result.add(boxHeight)
                    boxHeight = 0
                } else if (cell) {
                    boxHeight++
                }
                lastCell = cell
            }
            if (lastCell && boxHeight != 0) {
                result.add(boxHeight)
            }
            return result
        }

        fun apply(boxes: List<Box>, rowIndex: Int): Board {
            boxes.forEach { (index, width) ->
                0.rangeTo(width - 1).forEach {
                    cells[rowIndex][index + it] = true
                }
            }
            return this
        }

        fun copy(): Board {
            return Board(width, height, rowConstrains, columnConstraints, cells.map { ArrayList(it) })
        }

        override fun toString(): String {
            val max = columnConstraints.map{ it.boxes.size }.max()!!
            return cells.mapIndexed { i, row ->
                "|" + row.map { if (it) "X" else "_" }.joinToString("|") + "| " + rowConstrains[i].boxes.joinToString(" ")
            }.joinToString("\n") + "\n" + 0.rangeTo(max - 1).map { i ->
                " " + 0.rangeTo(width - 1).map{ if (i < columnConstraints[it].boxes.size) columnConstraints[it].boxes[i].toString() else " "}.joinToString(" ").trim()
            }.joinToString("\n")
        }
    }

    data class Box(val index: Int, val width: Int)

    data class Constraint(val boxes: List<Int>) {
        constructor(vararg boxes: Int): this(boxes.toList())

        fun possibleBoxes(width: Int, startIndex: Int = 0): Sequence<List<Box>> {
            if (boxes.isEmpty()) return sequenceOf(emptyList())

            val endIndex = width - boxes.first() - boxes.tail().sumBy { it + 1 }
            if (startIndex > endIndex) return emptySequence()

            return startIndex.rangeTo(endIndex).toSeq().flatMap { i ->
                Constraint(boxes.tail()).possibleBoxes(width, i + boxes.first() + 1).map {
                    listOf(Box(i, boxes.first())) + it
                }
            }
        }
    }

    companion object {
        fun String.parse(): Board {
            fun List<List<Int>>.transpose(): List<List<Int>> {
                val max = maxBy{ it.size }!!.size
                val result = ArrayList<List<Int>>()
                0.rangeTo(max - 1).forEach { i ->
                    result.add(mapNotNull { list ->
                        if (i < list.size) list[i] else null
                    })
                }
                return result
            }

            val lines = split("\n")

            val cells = lines
                .takeWhile{ it.startsWith("|") }
                .map { it.replace(Regex("[|]"), "").replace(Regex(" .*"), "") }
                .map { it.toCharArray().mapTo(ArrayList()) {
                    if (it == '_') false else true
                }}

            val rowConstraints = lines
                .takeWhile{ it.startsWith("|") }
                .map { it.replace(Regex("[|_X]"), "") }
                .map { it.trim().split(" ").map(String::toInt) }
                .map(::Constraint)

            val columnConstraints = lines
                .dropWhile{ it.startsWith("|") }
                .map { it.trim().split(" ").map(String::toInt) }
                .transpose()
                .map(::Constraint)

            val width = columnConstraints.size
            val height = rowConstraints.size
            return Board(width, height, rowConstraints, columnConstraints, cells)
        }
    }
}

class P98Test {

    @Test fun `all possibles boxes within constraint`() {
        Constraint(3).apply {
            assertThat(this.possibleBoxes(width = 2).toList(), equalTo(emptyList()))
            assertThat(this.possibleBoxes(width = 3).toList(), equalTo(listOf(
                    listOf(Box(0, 3))
            )))
            assertThat(this.possibleBoxes(width = 5).toList(), equalTo(listOf(
                    listOf(Box(0, 3)), listOf(Box(1, 3)), listOf(Box(2, 3))
            )))
        }

        Constraint(2, 1).apply {
            assertThat(this.possibleBoxes(width = 4).toList(), equalTo(listOf(
                    listOf(Box(0, 2), Box(3, 1))
            )))
            assertThat(this.possibleBoxes(width = 6).toList(), equalTo(listOf(
                    listOf(Box(0, 2), Box(3, 1)),
                    listOf(Box(0, 2), Box(4, 1)),
                    listOf(Box(0, 2), Box(5, 1)),
                    listOf(Box(1, 2), Box(4, 1)),
                    listOf(Box(1, 2), Box(5, 1)),
                    listOf(Box(2, 2), Box(5, 1))
            )))
        }

        Constraint(3, 2).apply {
            assertThat(this.possibleBoxes(width = 7).toList(), equalTo(listOf(
                    listOf(Box(0, 3), Box(4, 2)),
                    listOf(Box(0, 3), Box(5, 2)),
                    listOf(Box(1, 3), Box(5, 2))
            )))
        }
    }

    @Test fun `counting column boxes heights`() {
        val nonogram = """
            *|_|X|X|X|_|_|_|_| 3
            *|X|X|_|X|_|_|_|_| 2 1
            *|_|X|X|X|_|_|X|X| 3 2
            *|_|_|X|X|_|_|X|X| 2 2
            *|_|_|X|X|X|X|X|X| 6
            *|X|_|X|X|X|X|X|_| 1 5
            *|X|X|X|X|X|X|_|_| 6
            *|_|_|_|_|X|_|_|_| 1
            *|_|_|_|X|X|_|_|_| 2
            * 1 3 1 7 5 3 4 3
            * 2 1 5 1
        """.trimMargin("*").parse()

        assertThat(nonogram.columnBoxHeights(0), equalTo(listOf(1, 2)))
        assertThat(nonogram.columnBoxHeights(1), equalTo(listOf(3, 1)))
        assertThat(nonogram.columnBoxHeights(2), equalTo(listOf(1, 5)))
        assertThat(nonogram.columnBoxHeights(3), equalTo(listOf(7, 1)))
    }

    @Test fun `parse string as nonogram`() {
        val nonogram = """
            *|_|_|_|_|_|_|_|_| 3
            *|_|_|_|_|_|_|_|_| 2 1
            *|_|_|_|_|_|_|_|_| 3 2
            *|_|_|_|_|_|_|_|_| 2 2
            *|_|_|_|_|_|_|_|_| 6
            *|_|_|_|_|_|_|_|_| 1 5
            *|_|_|_|_|_|_|_|_| 6
            *|_|_|_|_|_|_|_|_| 1
            *|_|_|_|_|_|_|_|_| 2
            * 1 3 1 7 5 3 4 3
            * 2 1 5 1
        """.trimMargin("*").parse()

        assertThat(nonogram, equalTo(Nonogram.Board(
            8, 9,
            listOf(
                Constraint(3),
                Constraint(2, 1),
                Constraint(3, 2),
                Constraint(2, 2),
                Constraint(6),
                Constraint(1, 5),
                Constraint(6),
                Constraint(1),
                Constraint(2)
            ),
            listOf(
                Constraint(1, 2),
                Constraint(3, 1),
                Constraint(1, 5),
                Constraint(7, 1),
                Constraint(5),
                Constraint(3),
                Constraint(4),
                Constraint(3)
            )
        )))
    }

    @Test fun `convert nonogram to string`() {
        val nonogram = Nonogram.Board(
            8, 9,
            listOf(
                Constraint(3),
                Constraint(2, 1),
                Constraint(3, 2),
                Constraint(2, 2),
                Constraint(6),
                Constraint(1, 5),
                Constraint(6),
                Constraint(1),
                Constraint(2)
            ),
            listOf(
                Constraint(1, 2),
                Constraint(3, 1),
                Constraint(1, 5),
                Constraint(7, 1),
                Constraint(5),
                Constraint(3),
                Constraint(4),
                Constraint(3)
            )
        )

        assertThat(nonogram.toString(), equalTo("""
            *|_|_|_|_|_|_|_|_| 3
            *|_|_|_|_|_|_|_|_| 2 1
            *|_|_|_|_|_|_|_|_| 3 2
            *|_|_|_|_|_|_|_|_| 2 2
            *|_|_|_|_|_|_|_|_| 6
            *|_|_|_|_|_|_|_|_| 1 5
            *|_|_|_|_|_|_|_|_| 6
            *|_|_|_|_|_|_|_|_| 1
            *|_|_|_|_|_|_|_|_| 2
            * 1 3 1 7 5 3 4 3
            * 2 1 5 1
        """.trimMargin("*")))
    }

    @Test fun `solve nonogram from readme`() {
        val nonogram = """
            *|_|_|_|_|_|_|_|_| 3
            *|_|_|_|_|_|_|_|_| 2 1
            *|_|_|_|_|_|_|_|_| 3 2
            *|_|_|_|_|_|_|_|_| 2 2
            *|_|_|_|_|_|_|_|_| 6
            *|_|_|_|_|_|_|_|_| 1 5
            *|_|_|_|_|_|_|_|_| 6
            *|_|_|_|_|_|_|_|_| 1
            *|_|_|_|_|_|_|_|_| 2
            * 1 3 1 7 5 3 4 3
            * 2 1 5 1
        """.trimMargin("*").parse()

        assertThat(nonogram.solve().first(), equalTo("""
            *|_|X|X|X|_|_|_|_| 3
            *|X|X|_|X|_|_|_|_| 2 1
            *|_|X|X|X|_|_|X|X| 3 2
            *|_|_|X|X|_|_|X|X| 2 2
            *|_|_|X|X|X|X|X|X| 6
            *|X|_|X|X|X|X|X|_| 1 5
            *|X|X|X|X|X|X|_|_| 6
            *|_|_|_|_|X|_|_|_| 1
            *|_|_|_|X|X|_|_|_| 2
            * 1 3 1 7 5 3 4 3
            * 2 1 5 1
        """.trimMargin("*").parse()
        ))
    }
}