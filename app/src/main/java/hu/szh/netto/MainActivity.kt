package hu.szh.netto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CalculatorScreen()
        }
    }
}

@Composable
fun CalculatorScreen() {
    val scroll = rememberScrollState()

    var oraber by remember { mutableStateOf("2950") }
    var ledolgozottOrak by remember { mutableStateOf((15*8).toString()) }
    var tuloraOrak by remember { mutableStateOf("8") }
    var szabadsagOrak by remember { mutableStateOf((5*8).toString()) }

    var tavolletiOraber by remember { mutableStateOf("3300") }
    var bonusz by remember { mutableStateOf("99864") }
    var muszakpotlek by remember { mutableStateOf("59568") }
    var gyedBrutto by remember { mutableStateOf("407220") }
    var csaladiKedv by remember { mutableStateOf("149800") }
    var gyedAdozik by remember { mutableStateOf(true) }

    var result by remember { mutableStateOf<Result?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Nettó bér kalkulátor", style = MaterialTheme.typography.headlineSmall)
        Text("Add meg a számokat bruttóban. A családi kedvezmény először az SZJA-ból, majd a TB-ből kerül levonásra.")

        NumberField("Órabér", oraber) { oraber = it }
        NumberField("Ledolgozott órák (hó)", ledolgozottOrak) { ledolgozottOrak = it }
        NumberField("Túlóra (óra)", tuloraOrak) { tuloraOrak = it }
        NumberField("Szabadság (óra)", szabadsagOrak) { szabadsagOrak = it }
        NumberField("Távolléti órabér (szabadság)", tavolletiOraber) { tavolletiOraber = it }
        NumberField("Változó bónusz (bruttó)", bonusz) { bonusz = it }
        NumberField("Műszakpótlék (bruttó)", muszakpotlek) { muszakpotlek = it }
        NumberField("GYED (bruttó, havi)", gyedBrutto) { gyedBrutto = it }
        NumberField("Családi kedvezmény (havi)", csaladiKedv) { csaladiKedv = it }

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = gyedAdozik, onCheckedChange = { gyedAdozik = it })
            Text("GYED adózik (10% nyugdíjjárulék) — ha kikapcsolod, teljesen adómentesnek számolja.")
        }

        Button(onClick = {
            result = calculate(
                oraber.toLongOrZero(),
                ledolgozottOrak.toLongOrZero(),
                tuloraOrak.toLongOrZero(),
                szabadsagOrak.toLongOrZero(),
                tavolletiOraber.toLongOrZero(),
                bonusz.toLongOrZero(),
                muszakpotlek.toLongOrZero(),
                gyedBrutto.toLongOrZero(),
                csaladiKedv.toLongOrZero(),
                gyedAdozik
            )
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Számolás")
        }

        result?.let { r ->
            Divider()
            Text("Eredmény", style = MaterialTheme.typography.titleMedium)
            Text("Nettó összesen: ${formatFt(r.nettoOsszes)}")
            Spacer(Modifier.height(8.dp))
            Text("— Munkabér nettó: ${formatFt(r.munkaberNetto)}")
            Text("— Bónusz + pótlék nettó: ${formatFt(r.bonuszPotlekNetto)}")
            Text("— GYED nettó: ${formatFt(r.gyedNetto)}")
            Spacer(Modifier.height(8.dp))
            Text("Levonások (kedvezmény alkalmazva):")
            Text("SZJA: ${formatFt(r.szjaOsszes)}")
            Text("TB: ${formatFt(r.tbOsszes)}")
            Text("Nyugdíjjárulék: ${formatFt(r.nyugdijOsszes)}")
            Text("Felhasznált családi kedvezmény: ${formatFt(r.felhasznaltKedvezmeny)}")
        }
    }
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        label = { Text(label) },
        value = value,
        onValueChange = { new -> onChange(new.filter { it.isDigit() }) },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun String.toLongOrZero(): Long = this.toLongOrNull() ?: 0L
private fun formatFt(v: Long): String = "${"%,d".format(v).replace(',', ' ')} Ft"

data class Result(
    val nettoOsszes: Long,
    val munkaberNetto: Long,
    val bonuszPotlekNetto: Long,
    val gyedNetto: Long,
    val szjaOsszes: Long,
    val tbOsszes: Long,
    val nyugdijOsszes: Long,
    val felhasznaltKedvezmeny: Long,
)

fun calculate(
    oraber: Long,
    ledOrak: Long,
    tuloraOrak: Long,
    szabOrak: Long,
    tavOraber: Long,
    bonusz: Long,
    muszak: Long,
    gyed: Long,
    csalKedv: Long,
    gyedAdozik: Boolean
): Result {
    val bruttoMunka = ledOrak * oraber + tuloraOrak * oraber * 2 + szabOrak * tavOraber
    val bruttoBonuszPotlek = bonusz + muszak
    val bruttoAll = bruttoMunka + bruttoBonuszPotlek

    var munkaberSzja = ((bruttoAll) * 0.15).roundToLong()
    var munkaberTb = ((bruttoAll) * 0.185).roundToLong()
    val munkaberNyugdij = ((bruttoAll) * 0.10).roundToLong()

    var maradekKedv = csalKedv
    val szjaCsokk = minOf(maradekKedv, munkaberSzja)
    munkaberSzja -= szjaCsokk
    maradekKedv -= szjaCsokk

    val tbCsokk = minOf(maradekKedv, munkaberTb)
    munkaberTb -= tbCsokk
    maradekKedv -= tbCsokk

    val munkaberNetto = bruttoAll - (munkaberSzja + munkaberTb + munkaberNyugdij)
    val gyedNetto = if (gyedAdozik) gyed - (gyed * 0.10).roundToLong() else gyed

    return Result(
        nettoOsszes = munkaberNetto + gyedNetto,
        munkaberNetto = bruttoMunka - (bruttoMunka * 0.10).roundToLong(),
        bonuszPotlekNetto = bruttoBonuszPotlek - (bruttoBonuszPotlek * 0.10).roundToLong(),
        gyedNetto = gyedNetto,
        szjaOsszes = munkaberSzja,
        tbOsszes = munkaberTb,
        nyugdijOsszes = munkaberNyugdij + if (gyedAdozik) (gyed * 0.10).roundToLong() else 0L,
        felhasznaltKedvezmeny = csalKedv - maradekKedv,
    )
}
