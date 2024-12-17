package com.example.mediplan.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.mediplan.data.MedicationAction
import com.example.mediplan.data.MedicationUnit
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.TextAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExportUtils(private val context: Context) {

    suspend fun exportToPdf(
        historyItems: List<MedicationHistoryWithDetails>,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Uri = withContext(Dispatchers.IO) {
        val filename = "medication_history_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.pdf"
        val file = File(context.cacheDir, filename)

        PdfWriter(file).use { writer ->
            PdfDocument(writer).use { pdf ->
                Document(pdf).use { document ->
                    // Title
                    document.add(Paragraph("Medication History")
                        .setFontSize(24f)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER))

                    // Date range
                    document.add(Paragraph("From: ${startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} " +
                        "To: ${endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                        .setTextAlignment(TextAlignment.CENTER))

                    // History table
                    val table = Table(4).useAllAvailableWidth()
                    
                    // Headers
                    table.addHeaderCell("Date & Time")
                    table.addHeaderCell("Medication")
                    table.addHeaderCell("Dose")
                    table.addHeaderCell("Status")

                    // Data
                    historyItems.sortedByDescending { it.timestamp }.forEach { item ->
                        table.addCell(item.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                        table.addCell(item.medicationName)
                        table.addCell("${item.amount} ${item.unit.getDisplayName(item.amount)}")
                        table.addCell(item.action.name)
                    }

                    document.add(table)
                }
            }
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    suspend fun exportToCsv(
        historyItems: List<MedicationHistoryWithDetails>,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Uri = withContext(Dispatchers.IO) {
        val filename = "medication_history_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.csv"
        val file = File(context.cacheDir, filename)

        file.bufferedWriter().use { writer ->
            // Headers
            writer.write("Date,Time,Medication,Dose,Status\n")
            
            // Data
            historyItems.sortedByDescending { it.timestamp }.forEach { item ->
                writer.write("""
                    ${item.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))},
                    ${item.timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))},
                    "${item.medicationName}",
                    "${item.amount} ${item.unit.getDisplayName(item.amount)}",
                    ${item.action}
                """.trimIndent() + "\n")
            }
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }
}

data class MedicationHistoryWithDetails(
    val medicationName: String,
    val amount: Double,
    val unit: MedicationUnit,
    val action: MedicationAction,
    val timestamp: LocalDateTime
) 