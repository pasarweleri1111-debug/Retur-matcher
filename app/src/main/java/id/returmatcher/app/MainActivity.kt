package id.returmatcher.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScanning
import id.returmatcher.app.data.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val dao by lazy { ReturnDatabase.get(this).dao() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ReturScreen() } }
    }

    @Composable private fun ReturScreen() {
        var code by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("Impor Excel data master untuk mulai.") }
        var masterCount by remember { mutableIntStateOf(0) }
        var matched by remember { mutableIntStateOf(0) }
        var missing by remember { mutableIntStateOf(0) }
        var history by remember { mutableStateOf(emptyList<ScannedReturn>()) }
        val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri: Uri? ->
            uri?.let { target -> lifecycleScope.launch {
                runCatching { ExcelService.exportReport(this@MainActivity, target, dao.allMaster(), dao.allScans()) }
                    .onSuccess { message = "Laporan berhasil disimpan" }
                    .onFailure { message = "Gagal ekspor: ${it.message}" }
            } }
        }
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { selected -> lifecycleScope.launch {
                runCatching { ExcelService.importMaster(this@MainActivity, selected) }
                    .onSuccess { rows -> dao.clearMaster(); dao.insertMaster(rows); message = "Berhasil impor ${rows.size} baris data master"; refresh { m, a, b, h -> masterCount=m; matched=a; missing=b; history=h } }
                    .onFailure { message = "Gagal impor: ${it.message}" }
            } }
        }
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("RETUR MATCHER", style = MaterialTheme.typography.headlineMedium)
            Text("Pencocokan retur berdasarkan data Excel", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) }, modifier = Modifier.fillMaxWidth()) { Text("IMPORT EXCEL DATA MASTER") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Data master", masterCount.toString()); Metric("Cocok", matched.toString()); Metric("Tidak ada", missing.toString())
            }
            OutlinedTextField(value=code, onValueChange={code=it}, label={Text("Ketik nomor pesanan atau resi")}, modifier=Modifier.fillMaxWidth(), singleLine=true)
            OutlinedButton(onClick = {
                GmsBarcodeScanning.getClient(this@MainActivity).startScan()
                    .addOnSuccessListener { barcode -> barcode.rawValue?.let { code = it } }
                    .addOnFailureListener { message = "Scan gagal: ${it.message}" }
            }, modifier=Modifier.fillMaxWidth()) { Text("SCAN BARCODE DENGAN KAMERA") }
            Button(onClick = {
                val value = code.trim()
                if (value.isNotEmpty()) lifecycleScope.launch {
                    val duplicate = dao.scanCount(value) > 0
                    val found = dao.findMaster(value)
                    val result = when { duplicate -> "DUPLIKAT"; found != null -> "COCOK"; else -> "TIDAK_DITEMUKAN" }
                    dao.insertScan(ScannedReturn(scannedCode=value, matchedOrderId=found?.id, result=result))
                    message = when(result) { "COCOK" -> "COCOK: ${found?.orderNumber} / ${found?.trackingNumber}"; "DUPLIKAT" -> "PERINGATAN: kode sudah pernah dipindai"; else -> "RETUR TIDAK ADA DI DATA MASTER" }
                    code=""; refresh { m,a,b,h -> masterCount=m; matched=a; missing=b; history=h }
                }
            }, modifier=Modifier.fillMaxWidth()) { Text("PERIKSA RETUR") }
            Text(message, style=MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = { exportLauncher.launch("laporan_retur.xlsx") }, modifier=Modifier.fillMaxWidth()) { Text("EKSPOR LAPORAN EXCEL") }
            Text("Riwayat scan", style=MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                items(history) { item -> ListItem(headlineContent={Text(item.scannedCode)}, supportingContent={Text(item.result)}) }
            }
        }
        LaunchedEffect(Unit) { refresh { m,a,b,h -> masterCount=m; matched=a; missing=b; history=h } }
    }

    private fun refresh(done: (Int, Int, Int, List<ScannedReturn>) -> Unit) { lifecycleScope.launch { done(dao.masterCount(), dao.matchedCount(), dao.missingCount(), dao.allScans()) } }
}

@Composable private fun Metric(label: String, value: String) {
    Card { Column(Modifier.padding(12.dp)) { Text(value, style=MaterialTheme.typography.titleLarge); Text(label, style=MaterialTheme.typography.labelSmall) } }
}
