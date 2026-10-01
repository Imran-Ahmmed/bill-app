package com.imran.bill

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import kotlin.math.roundToInt

object BillRenderer {

    private const val W = 1080
    private const val MARGIN = 50f

    private class Pen(val canvas: Canvas) {
        var y = 40f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.BLACK
        }
        val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 3f }

        fun next(gap: Float = 62f) { y += gap }

        fun text(s: String, x: Float = MARGIN, color: Int = Color.BLACK, size: Float = 40f) {
            paint.color = color; paint.textSize = size
            canvas.drawText(s, x, y, paint)
        }

        /** ডান পাশে লাইন যুক্ত শিরোনাম: ------- শিরোনাম ------- */
        fun heading(s: String) {
            paint.color = Color.BLACK; paint.textSize = 40f
            val tw = paint.measureText(s)
            val x = (W - tw) / 2
            canvas.drawText(s, x, y, paint)
            canvas.drawLine(30f, y - 14, x - 15, y - 14, linePaint)
            canvas.drawLine(x + tw + 15, y - 14, W - 30f, y - 14, linePaint)
        }
    }

    fun render(r: BillResult): Bitmap {
        val i = r.input
        val bmp = Bitmap.createBitmap(W, 2400, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val p = Pen(c)

        p.y = 70f
        p.heading("তারিখ=${i.presentDate}")
        p.next(); p.text("* গত মাসে মোট রিচারজঃ")
        p.next(); p.text("  = ${i.recharge}(কাটিং ${i.cutting}/=)")
        p.next(90f); p.text("** ${i.pastDate} তারিখ মিটারে টাকা ছিল = ${i.pastTk} টাকা")
        p.next(80f); p.text("     মোট টাকাঃ ${i.recharge} + ${i.pastTk} = ${r.totalTk} টাকা")
        p.next(90f); p.text("** ${i.presentDate} তারিখ মিটারে টাকা আছে = ${i.presentTk} টাকা")
        p.next(80f); p.text("     তাহলে মোট টাকা = ${r.totalTk} - ${i.presentTk} = ${r.taholeTk} টাকা")
        p.next(90f); p.text("** কাটিং বাদে মূল ব্যালান্স = ${r.taholeTk} - ${i.cutting} = ${fmt(r.katingBade)} টাকা")

        // ---- ইউনিটের হিসাব ----
        p.next(110f); p.heading("ইউনিটের হিসাব")
        p.next(90f)
        val midX = W / 2f
        val leftX = 70f
        val rightX = midX + 50f
        val tableTop = p.y - 45f
        p.text("আইয়ানঃ", leftX); p.text("পাশের ঘরঃ", rightX)
        p.next(75f)
        p.text("(${i.presentDate}) ${i.aiyanPresent}", leftX)
        p.text("${i.mahimPresent} (তাংঃ ${i.presentDate})", rightX)
        p.next(55f)
        p.text("${i.aiyanPast}", leftX + 120f); p.text("${i.mahimPast}", rightX)
        p.next(30f)
        p.text("______", leftX + 120f); p.text("______", rightX)
        p.next(50f)
        p.text("${r.aiyanUnit} ইউনিট", leftX + 120f); p.text("${r.mahimUnit} ইউনিট", rightX)
        c.drawLine(midX, tableTop, midX, p.y + 20f, p.linePaint)

        p.next(100f); p.text("মোট ইউনিট = ${r.aiyanUnit} + ${r.mahimUnit} = ${fmt(r.totalUnit)} ইউনিট")
        p.next(80f); p.text("প্রতি ইউনিট = ${fmt(r.katingBade)} % ${fmt(r.totalUnit)} = ${fmt(r.perUnit)}/-")

        // ---- বিদ্যুৎ বিলের হিসাব ----
        p.next(110f); p.heading("বিদ্যুৎ বিলের হিসাব")
        p.next(90f); p.text("আইয়ানঃ     = ${r.aiyanUnit} X ${fmt(r.perUnit)}")
        p.next(); p.text("            = ${fmt(r.aiyanBill)} + ${r.cuttingHalf}(কাটিং % 2)", )
        p.next(); p.text("            = ${fmt(r.aiyanBill + r.cuttingHalf)} টাকা")
        p.next(90f); p.text("পাশের ঘরঃ      = ${r.mahimUnit} X ${fmt(r.perUnit)}")
        p.next(); p.text("            = ${fmt(r.mahimBill)} + ${r.cuttingHalf}(কাটিং % 2)")
        p.next(); p.text("            = ${fmt(r.mahimBill + r.cuttingHalf)} টাকা")

        // ---- লাল লাইন (ঐচ্ছিক) ----
        i.mahimPaid?.let { paid ->
            val mb = (r.mahimBill + r.cuttingHalf).roundToInt()
            p.next(100f)
            p.text("আইয়ান পাবেঃ $mb-$paid = ${mb - paid} টাকা ।", color = Color.RED, size = 48f)
        }

        val endY = (p.y + 60f).toInt().coerceAtMost(bmp.height)
        return Bitmap.createBitmap(bmp, 0, 0, W, endY)
    }

    /** Pictures/ElectricBill ফোল্ডারে PNG সেভ */
    fun saveImage(ctx: Context, bmp: Bitmap, name: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ElectricBill")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return uri
    }

    /** Download ফোল্ডারে PDF সেভ */
    fun savePdf(ctx: Context, bmp: Bitmap, name: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$name.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create())
        page.canvas.drawBitmap(bmp, 0f, 0f, null)
        doc.finishPage(page)
        ctx.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        doc.close()
        return uri
    }
}
