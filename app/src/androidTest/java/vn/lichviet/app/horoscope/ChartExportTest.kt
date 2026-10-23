package vn.lichviet.app.horoscope

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.core.horoscope.HoroscopeChartEngine

@RunWith(AndroidJUnit4::class)
class ChartExportTest {
    @Test fun nativePngAndPdfContainTheSelectedChartAndUseScopedProviderUris() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val context = base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(Locale.ENGLISH) })
        val profile = HoroscopeProfileEntity(label = "QA long nickname – Nguyễn Thị Minh Anh – family birth chart typography inspection".take(80), year = 1984, month = 2, day = 2, minute = 30)
        val chart = HoroscopeChartEngine.calculate(HoroscopeProfileValidation.birthProfile(profile))
        val image = ChartExporter.create(context, profile, chart, ChartExportFormat.PNG)
        val pdf = ChartExporter.create(context, profile, chart, ChartExportFormat.PDF)
        try {
            val bitmap = requireNotNull(BitmapFactory.decodeFile(image.path))
            assertEquals(1600, bitmap.width); assertEquals(2900, bitmap.height); bitmap.recycle()
            assertTrue(pdf.inputStream().use { it.readNBytes(4).decodeToString() } == "%PDF")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
            assertEquals("content", uri.scheme)
            assertTrue(context.contentResolver.openInputStream(uri)!!.use { it.read() } >= 0)
            PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                assertEquals(2, renderer.pageCount)
                repeat(2) { index ->
                    renderer.openPage(index).use { page ->
                        assertEquals(595, page.width); assertEquals(842, page.height)
                        val rendered = Bitmap.createBitmap(1190, 1684, Bitmap.Config.ARGB_8888)
                        page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val directory = File(context.getExternalFilesDir(null), "horoscope-qa").apply { mkdirs() }
                        File(directory, "chart-pdf-${index + 1}.png").outputStream().use { rendered.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        rendered.recycle()
                    }
                }
            }
            image.copyTo(File(File(context.getExternalFilesDir(null), "horoscope-qa"), "chart.png"), overwrite = true)
            pdf.copyTo(File(File(context.getExternalFilesDir(null), "horoscope-qa"), "chart.pdf"), overwrite = true)
        } finally { image.delete(); pdf.delete() }
    }
}
