package com.imran.bill

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    // হিসাব হয়ে গেলে true; অ্যাপ স্ক্রিন থেকে চলে গেলে ফর্ম নতুন করে দেওয়া হবে
    private var hasResult = false
    private var resetSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    BillScreen(
                        resetSignal = resetSignal,
                        onCalculated = { hasResult = true }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // স্ক্রিন রোটেট করলে রিসেট হবে না, শুধু অ্যাপ বন্ধ/ব্যাকগ্রাউন্ডে গেলে
        if (hasResult && !isChangingConfigurations) {
            hasResult = false
            resetSignal++
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, number: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BillScreen(resetSignal: Int = 0, onCalculated: () -> Unit = {}) {
    val ctx = LocalContext.current
    val today = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()) }

    var presentDate by rememberSaveable { mutableStateOf(today) }
    var pastDate by rememberSaveable { mutableStateOf("") }
    var recharge by rememberSaveable { mutableStateOf("") }
    var cutting by rememberSaveable { mutableStateOf("") }
    var presentTk by rememberSaveable { mutableStateOf("") }
    var pastTk by rememberSaveable { mutableStateOf("") }
    var aiyanPresent by rememberSaveable { mutableStateOf("") }
    var aiyanPast by rememberSaveable { mutableStateOf("") }
    var mahimPresent by rememberSaveable { mutableStateOf("") }
    var mahimPast by rememberSaveable { mutableStateOf("") }
    var mahimPaid by rememberSaveable { mutableStateOf("") }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }

    // হিসাবের পর অ্যাপ বন্ধ করে আবার খুললে নতুন খালি ফর্ম
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            presentDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
            pastDate = ""; recharge = ""; cutting = ""
            presentTk = ""; pastTk = ""
            aiyanPresent = ""; aiyanPast = ""
            mahimPresent = ""; mahimPast = ""; mahimPaid = ""
            bmp = null
        }
    }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("বিদ্যুৎ বিলের হিসাব", style = MaterialTheme.typography.headlineSmall)

        Field("আজকের তারিখ", presentDate, { presentDate = it }, number = false)
        Field("গত মাসের তারিখ (যেমন 01/07/2026)", pastDate, { pastDate = it }, number = false)
        Field("মোট রিচার্জ (টাকা)", recharge, { recharge = it })
        Field("কাটিং (টাকা)", cutting, { cutting = it })
        Field("এই মাসে মিটারে বাকি টাকা", presentTk, { presentTk = it })
        Field("গত মাসে মিটারে বাকি টাকা", pastTk, { pastTk = it })
        Field("আইয়ান - বর্তমান মিটার রিডিং", aiyanPresent, { aiyanPresent = it })
        Field("আইয়ান - গত মাসের মিটার রিডিং", aiyanPast, { aiyanPast = it })
        Field("পাশের ঘর - বর্তমান মিটার রিডিং", mahimPresent, { mahimPresent = it })
        Field("পাশের ঘর - গত মাসের মিটার রিডিং", mahimPast, { mahimPast = it })
        Field("পাশের ঘর আগে যত টাকা দিয়েছে (ঐচ্ছিক)", mahimPaid, { mahimPaid = it })

        Button(
            onClick = {
                val rc = recharge.toIntOrNull(); val ct = cutting.toIntOrNull()
                val pt = presentTk.toIntOrNull(); val pst = pastTk.toIntOrNull()
                val ap = aiyanPresent.toLongOrNull(); val apast = aiyanPast.toLongOrNull()
                val mp = mahimPresent.toLongOrNull(); val mpast = mahimPast.toLongOrNull()
                if (listOf(rc, ct, pt, pst, ap, apast, mp, mpast).any { it == null } ||
                    presentDate.isBlank() || pastDate.isBlank()
                ) {
                    toast("সবগুলো ঘর সঠিকভাবে পূরণ করো"); return@Button
                }
                if ((ap!! - apast!!) + (mp!! - mpast!!) <= 0L) {
                    toast("মোট ইউনিট ০ বা ঋণাত্মক হতে পারে না, রিডিং চেক করো"); return@Button
                }
                val input = BillInput(
                    presentDate, pastDate, rc!!, ct!!, pt!!, pst!!,
                    ap, apast, mp, mpast, mahimPaid.toIntOrNull()
                )
                bmp = BillRenderer.render(calculate(input))
                onCalculated()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("হিসাব করো") }

        bmp?.let { b ->
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "বিলের ছবি",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            val fileName = "Bill_" + presentDate.replace("/", "-")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val u = BillRenderer.saveImage(ctx, b, fileName)
                        toast(if (u != null) "ছবি সেভ হয়েছে (Pictures/ElectricBill)" else "সেভ করা যায়নি")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("ছবি সেভ") }
                Button(
                    onClick = {
                        val u = BillRenderer.savePdf(ctx, b, fileName)
                        toast(if (u != null) "PDF সেভ হয়েছে (Downloads)" else "সেভ করা যায়নি")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("PDF সেভ") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
