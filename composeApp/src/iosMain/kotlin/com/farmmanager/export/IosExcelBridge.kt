package com.farmmanager.export

import com.farmmanager.data.CropEntity
import com.farmmanager.data.ExpenseEntity
import com.farmmanager.data.FarmSnapshot
import com.farmmanager.data.HarvestEntity
import com.farmmanager.data.SaleEntity
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import platform.Foundation.NSTemporaryDirectory

internal object IosExcelBridge {
    fun export(snapshot: FarmSnapshot): ByteArray = XlsxCodec.write(snapshot)
    fun import(bytes: ByteArray): FarmSnapshot = XlsxCodec.read(bytes)
}

private object XlsxCodec {
    fun write(snapshot: FarmSnapshot): ByteArray {
        val sheets = listOf(
            Sheet("Crops", listOf("id", "name", "variety", "fieldName", "area", "season", "sowingDate", "notes"), snapshot.crops.map {
                listOf(it.id, it.name, it.variety, it.fieldName, it.area, it.season, it.sowingDate, it.notes)
            }),
            Sheet("Expenses", listOf("id", "cropId", "category", "applicationRound", "amount", "expenseDate", "notes"), snapshot.expenses.map {
                listOf(it.id, it.cropId, it.category, it.applicationRound ?: "", it.amount, it.expenseDate, it.notes)
            }),
            Sheet("Harvests", listOf("id", "cropId", "harvestDate", "quantityKg", "managementNotes"), snapshot.harvests.map {
                listOf(it.id, it.cropId, it.harvestDate, it.quantityKg, it.managementNotes)
            }),
            Sheet("Sales", listOf("id", "cropId", "saleDate", "quantityKg", "pricePerKg", "totalIncome", "buyerName", "buyerPhone", "notes"), snapshot.sales.map {
                listOf(it.id, it.cropId, it.saleDate, it.quantityKg, it.pricePerKg, it.totalIncome, it.buyerName, it.buyerPhone, it.notes)
            }),
        )
        return sheets.toXlsxZip()
    }

    fun read(bytes: ByteArray): FarmSnapshot {
        val tempPath = (NSTemporaryDirectory() + "farm_import_${kotlin.random.Random.nextLong()}.xlsx").toPath()
        FileSystem.SYSTEM.write(tempPath) { write(bytes) }
        val zipFs = FileSystem.SYSTEM.openZip(tempPath)
        val entries = buildMap<String, String> {
            zipFs.listRecursively("/".toPath()).forEach { path ->
                if (zipFs.metadata(path).isDirectory) return@forEach
                put(path.toString().removePrefix("/"), zipFs.source(path).buffer().use { it.readUtf8() })
            }
        }
        FileSystem.SYSTEM.delete(tempPath)
        return entries.toSnapshot()
    }

    private fun List<Sheet>.toXlsxZip(): ByteArray {
        val buffer = Buffer()
        okio.ZipSink(buffer).use { zip ->
            forEachIndexed { index, sheet ->
                zip.writeEntry("xl/worksheets/sheet${index + 1}.xml".toPath()) { writeUtf8(sheet.toXml()) }
            }
            zip.writeEntry("[Content_Types].xml".toPath()) { writeUtf8(contentTypes(size)) }
            zip.writeEntry("_rels/.rels".toPath()) { writeUtf8(rootRels()) }
            zip.writeEntry("xl/workbook.xml".toPath()) { writeUtf8(workbookXml(mapIndexed { i, s -> s.name to "sheet${i + 1}.xml" })) }
            zip.writeEntry("xl/_rels/workbook.xml.rels".toPath()) { writeUtf8(workbookRels(size)) }
        }
        return buffer.readByteArray()
    }

    private fun Map<String, String>.toSnapshot(): FarmSnapshot {
        val workbook = this["xl/workbook.xml"].orEmpty()
        val names = Regex("""<sheet name="([^"]+)"""").findAll(workbook).map { it.groupValues[1] }.toList()
        val tables = names.mapIndexed { index, name ->
            name to parseRows(this["xl/worksheets/sheet${index + 1}.xml"].orEmpty())
        }.toMap()
        return FarmSnapshot(
            crops = tables.read("Crops") { CropEntity(it.longAt(0), it.strAt(1), it.strAt(2), it.strAt(3), it.dblAt(4), it.strAt(5), it.strAt(6), it.strAt(7)) },
            expenses = tables.read("Expenses") { ExpenseEntity(it.longAt(0), it.longAt(1), it.strAt(2), it.optIntAt(3), it.dblAt(4), it.strAt(5), it.strAt(6)) },
            harvests = tables.read("Harvests") { HarvestEntity(it.longAt(0), it.longAt(1), it.strAt(2), it.dblAt(3), it.strAt(4)) },
            sales = tables.read("Sales") { SaleEntity(it.longAt(0), it.longAt(1), it.strAt(2), it.dblAt(3), it.dblAt(4), it.strAt(6), it.strAt(7), it.strAt(8)) },
        )
    }

    private inline fun <T> Map<String, List<List<String>>>.read(name: String, map: (List<String>) -> T): List<T> =
        this[name].orEmpty().drop(1).map(map)

    private fun List<String>.strAt(i: Int) = getOrNull(i).orEmpty()
    private fun List<String>.dblAt(i: Int) = strAt(i).toDoubleOrNull() ?: 0.0
    private fun List<String>.longAt(i: Int) = dblAt(i).toLong()
    private fun List<String>.optIntAt(i: Int) = strAt(i).takeIf { it.isNotBlank() }?.toDoubleOrNull()?.toInt()

    private fun parseRows(xml: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        Regex("""<row r="(\d+)">(.*?)</row>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { match ->
            val cells = mutableMapOf<Int, String>()
            Regex("""<c r="([A-Z]+)(\d+)"(?: t="inlineStr")?(?:><v>(.*?)</v>|><is><t>(.*?)</t></is></c>)""", RegexOption.DOT_MATCHES_ALL)
                .findAll(match.groupValues[2]).forEach { cell ->
                    cells[cell.groupValues[1].columnIndex()] = cell.groupValues[3].ifBlank { cell.groupValues[4] }
                }
            if (cells.isNotEmpty()) rows += (0..cells.keys.max()).map { cells[it].orEmpty() }
        }
        return rows
    }

    private data class Sheet(val name: String, val headers: List<String>, val rows: List<List<Any>>) {
        fun toXml(): String = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            appendRow(1, headers)
            rows.forEachIndexed { index, row -> appendRow(index + 2, row.map { it.toString() }) }
            append("</sheetData></worksheet>")
        }
    }

    private fun StringBuilder.appendRow(rowNumber: Int, values: List<String>) {
        append("""<row r="$rowNumber">""")
        values.forEachIndexed { index, value ->
            val ref = "${(index + 1).columnName()}$rowNumber"
            if (value.toDoubleOrNull() != null && value.all { it.isDigit() || it == '.' || it == '-' }) {
                append("""<c r="$ref"><v>$value</v></c>""")
            } else {
                append("""<c r="$ref" t="inlineStr"><is><t>${value.escape()}</t></is></c>""")
            }
        }
        append("</row>")
    }

    private fun contentTypes(count: Int) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        repeat(count) { append("""<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""") }
        append("</Types>")
    }

    private fun rootRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private fun workbookXml(sheets: List<Pair<String, String>>) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        sheets.forEachIndexed { index, (name, _) -> append("""<sheet name="${name.escape()}" sheetId="${index + 1}" r:id="rId${index + 1}"/>""") }
        append("</sheets></workbook>")
    }

    private fun workbookRels(count: Int) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        repeat(count) { append("""<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""") }
        append("</Relationships>")
    }

    private fun String.escape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun Int.columnName(): String {
        var value = this
        val builder = StringBuilder()
        while (value > 0) {
            val rem = (value - 1) % 26
            builder.insert(0, ('A'.code + rem).toChar())
            value = (value - 1) / 26
        }
        return builder.toString()
    }

    private fun String.columnIndex(): Int {
        var result = 0
        for (ch in this) result = result * 26 + (ch.code - 'A'.code + 1)
        return result - 1
    }
}
