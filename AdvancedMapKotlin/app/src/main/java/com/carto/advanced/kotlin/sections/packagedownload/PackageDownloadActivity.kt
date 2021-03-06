package com.carto.advanced.kotlin.sections.packagedownload

import android.os.Bundle
import com.carto.advanced.kotlin.components.popupcontent.packagepopupcontent.PackageCell
import com.carto.advanced.kotlin.components.popupcontent.stylepopupcontent.StylePopupContent
import com.carto.advanced.kotlin.sections.base.activities.PackageDownloadBaseActivity
import com.carto.advanced.kotlin.utils.Utils
import com.carto.packagemanager.CartoPackageManager
import com.carto.packagemanager.PackageManagerListener
import com.carto.packagemanager.PackageStatus

class PackageDownloadActivity : PackageDownloadBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contentView = PackageDownloadView(this)
        setContentView(contentView)

        val folder = Utils.createDirectory(this, "countrypackages")
        contentView?.manager = CartoPackageManager(StylePopupContent.CartoVectorSource, folder)
    }

    override fun onResume() {
        super.onResume()
        contentView?.addListeners()

        contentView?.manager?.packageManagerListener = object: PackageManagerListener() {
            override fun onPackageListUpdated() {
                val packages = contentView?.getPackages()!!
                runOnUiThread {
                    contentView?.updatePackages(packages)
                }
            }

            override fun onPackageStatusChanged(id: String?, version: Int, status: PackageStatus?) {
                runOnUiThread {
                    contentView?.onStatusChanged(id!!, status!!)
                }
            }

            override fun onPackageUpdated(id: String?, version: Int) {
                runOnUiThread {
                    contentView?.downloadComplete(id!!)
                }
            }
        }

        contentView?.packageContent?.list?.setOnItemClickListener { _, view, _, _ ->
            run {
                val cell = view as PackageCell
                contentView?.onPackageClick(cell.item!!)
            }
        }

        contentView?.manager?.start()
        contentView?.manager?.startPackageListDownload()
    }

    override fun onPause() {
        super.onPause()
        contentView?.removeListeners()

        contentView?.packageContent?.list?.onItemClickListener = null

        contentView?.manager?.packageManagerListener = null

        contentView?.manager?.stop(false)
    }

    override fun setOnlineMode() {
        contentView?.setOnlineMap()
    }

    override fun setOfflineMode() {
        contentView?.setOfflineMap()
    }
}
