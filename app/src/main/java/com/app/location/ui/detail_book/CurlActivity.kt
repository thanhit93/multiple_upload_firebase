package com.app.location.ui.detail_book

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.app.location.R
import com.app.location.widgets.page_curl.CurlPage
import com.app.location.widgets.page_curl.CurlView


class CurlActivity : AppCompatActivity() {

    lateinit var mCurlView: CurlView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_curl)

        var index = 0
        if (lastNonConfigurationInstance != null) {
            index = (lastNonConfigurationInstance as Int?)!!
        }
        mCurlView = findViewById<View>(R.id.curl) as CurlView
        mCurlView.setPageProvider(PageProvider(applicationContext))
        mCurlView.setSizeChangedObserver(SizeChangedObserver(mCurlView))
        mCurlView.currentIndex = index
        mCurlView.setBackgroundColor(resources.getColor(R.color.colorPrimary))
    }

    override fun onPause() {
        super.onPause()
        mCurlView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mCurlView.onResume()
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        return mCurlView.currentIndex
    }

//    override fun onRetainNonConfigurationInstance(): Any? {
//        return mCurlView.currentIndex
//    }

    /**
     * Bitmap provider.
     */
    private class PageProvider(val context: Context) : CurlView.PageProvider {
        // Bitmap resources.
        private val mBitmapIds = intArrayOf(R.drawable.ic_launcher_background, R.drawable.ic_info24, R.drawable.ic_delete24)

        override val pageCount: Int
            get() = 2

        @SuppressLint("UseCompatLoadingForDrawables")
        private fun loadBitmap(width: Int, height: Int, index: Int): Bitmap {
            val b = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888)
            b.eraseColor(-0x1)
            val c = Canvas(b)
            val d: Drawable = context.resources.getDrawable(mBitmapIds[index])
            val margin = 7
            val border = 3
            val r = Rect(margin, margin, width - margin, height - margin)
            var imageWidth: Int = r.width() - border * 2
            var imageHeight = (imageWidth * d.intrinsicHeight
                    / d.intrinsicWidth)
            if (imageHeight > r.height() - border * 2) {
                imageHeight = r.height() - border * 2
                imageWidth = (imageHeight * d.intrinsicWidth
                        / d.intrinsicHeight)
            }
            r.left += (r.width() - imageWidth) / 2 - border
            r.right = r.left + imageWidth + border + border
            r.top += (r.height() - imageHeight) / 2 - border
            r.bottom = r.top + imageHeight + border + border
            val p = Paint()
            p.color = -0x3f3f40
            c.drawRect(r, p)
            r.left += border
            r.right -= border
            r.top += border
            r.bottom -= border
            d.bounds = r
            d.draw(c)
            return b
        }

        override fun updatePage(page: CurlPage?, width: Int, height: Int, index: Int) {
            when (index) {
                0 -> {
                    val front = loadBitmap(width, height, 0)
                    page!!.setTexture(front, CurlPage.SIDE_FRONT)
                    page.setColor(Color.rgb(180, 180, 180), CurlPage.SIDE_BACK)
                }
                1 -> {
                    val back = loadBitmap(width, height, 2)
                    page!!.setTexture(back, CurlPage.SIDE_BACK)
                    page.setColor(Color.rgb(127, 140, 180), CurlPage.SIDE_FRONT)
                }
                2 -> {
                    val front = loadBitmap(width, height, 1)
                    val back = loadBitmap(width, height, 3)
                    page!!.setTexture(front, CurlPage.SIDE_FRONT)
                    page.setTexture(back, CurlPage.SIDE_BACK)
                }
                3 -> {
                    val front = loadBitmap(width, height, 2)
                    val back = loadBitmap(width, height, 1)
                    page!!.setTexture(front, CurlPage.SIDE_FRONT)
                    page.setTexture(back, CurlPage.SIDE_BACK)
                    page.setColor(Color.argb(127, 170, 130, 255), CurlPage.SIDE_FRONT)
                    page.setColor(Color.rgb(255, 190, 150), CurlPage.SIDE_BACK)
                }
                4 -> {
                    val front = loadBitmap(width, height, 0)
                    page!!.setTexture(front, CurlPage.SIDE_BOTH)
                    page.setColor(Color.argb(127, 255, 255, 255),
                            CurlPage.SIDE_BACK)
                }
            }
        }
    }

    /**
     * CurlView size changed observer.
     */
    private class SizeChangedObserver(val mCurlView: CurlView) : CurlView.SizeChangedObserver {
        override fun onSizeChanged(w: Int, h: Int) {
            if (w > h) {
                mCurlView.setViewMode(CurlView.SHOW_TWO_PAGES)
                mCurlView.setMargins(.1f, .05f, .1f, .05f)
            } else {
                mCurlView.setViewMode(CurlView.SHOW_ONE_PAGE)
                mCurlView.setMargins(.0f, .0f, .0f, .0f)
            }
        }
    }

}