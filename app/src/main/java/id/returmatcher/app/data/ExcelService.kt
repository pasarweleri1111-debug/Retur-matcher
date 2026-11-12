package id.returmatcher.app.data

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

object ExcelService {
    /** Header aliases supported: nomor pesanan/order/order number and nomor resi/resi/awb/tracking number. */
    fun importMaster(context: Context, uri: Uri): List<MasterOrder> {
        val input: InputStream = context.contentResolver.openInputStream(uri) ?: error("File tidak dapat dibuka")
        input.use { stream ->
            WorkbookFactory.create(stream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                require(sheet.physicalNumberOfRows > 1) { "Excel kosong atau hanya berisi judul kolom" }
                val header = sheet.getRow(0)
                fun norm(v: String) = v.trim().lowercase().replace("_", " ")
                val headers = (0 until header.lastCellNum).associateBy { norm(header.getCell(it)?.toString().orEmpty()) }
                fun col(vararg names: String): Int = names.firstNotNullOfOrNull { headers[it] }
                    ?: error("Kolom ${names.first()} tidak ditemukan")
                val orderCol = col("nomor pesanan", "no pesanan", "order", "order number", "nomor order")
                val resiCol = col("nomor resi", "no resi", "resi", "awb", "tracking number", "kode resi")
                val marketplaceCol = listOf("marketplace", "platform").firstNotNullOfOrNull { headers[it] }
                val productCol = listOf("produk", "nama produk", "product").firstNotNullOfOrNull { headers[it] }
                fun cell(row: org.apache.poi.ss.usermodel.Row, index: Int?): String = index?.let { row.getCell(it)?.toString()?.trim().orEmpty() }.orEmpty()
                (1..sheet.lastRowNum).mapNotNull { i ->
                    val row = sheet.getRow(i) ?: return@mapNotNull null
                    val order = cell(row, orderCol); val resi = cell(row, resiCol)
                    if (order.isBlank() && resi.isBlank()) null else MasterOrder(
                        orderNumber = order, trackingNumber = resi,
                        marketplace = cell(row, marketplaceCol), product = cell(row, productCol)
                    )
                }
            }
        }
    }

    fun exportReport(context: Context, uri: Uri, master: List<MasterOrder>, scans: List<ScannedReturn>) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Hasil Retur")
            val headers = listOf("Nomor Pesanan", "Nomor Resi", "Marketplace", "Produk", "Status Retur")
            sheet.createRow(0).also { row -> headers.forEachIndexed { i, value -> row.createCell(i).setCellValue(value) } }
            val scanCodes = scans.map { it.scannedCode }.toSet()
            master.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                listOf(item.orderNumber, item.trackingNumber, item.marketplace, item.product,
                    if (item.orderNumber in scanCodes || item.trackingNumber in scanCodes) "SUDAH DIPINDAI" else "BELUM DIRETUR / BELUM DITEMUKAN")
                    .forEachIndexed { i, value -> row.createCell(i).setCellValue(value) }
            }
            val start = master.size + 2
            sheet.createRow(start).createCell(0).setCellValue("HASIL SCAN")
            scans.forEachIndexed { index, scan ->
                val row = sheet.createRow(start + index + 1)
                row.createCell(0).setCellValue(scan.scannedCode)
                row.createCell(1).setCellValue(scan.result)
            }
            for (i in headers.indices) sheet.setColumnWidth(i, 6500)
            context.contentResolver.openOutputStream(uri)?.use { workbook.write(it) } ?: error("Gagal menyimpan laporan")
        }
    }
}
