package com.example.downloadmanagerexample

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

const val PERMISSION_REQUEST_WRITE = 0


class MainActivity : AppCompatActivity() {
    // Large file download url
    //private var downloadFileUrl = "https://www.hq.nasa.gov/alsj/a17/A17_FlightPlan.pdf"
    private var downloadFileUrl =
        "https://upload.wikimedia.org/wikipedia/commons/f/ff/Pizigani_1367_Chart_10MB.jpg"
    private var downloadFileName = "chart.jpg"
    private var downloadId = 0L
    private lateinit var downloadManager: DownloadManager

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if(id == downloadId){
                showDownloadStatus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        download_file.setOnClickListener {
            downloadFile(it)
        }

        check_status.setOnClickListener {
            showDownloadStatus()
        }

        registerReceiver(downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    private fun downloadFile(view: View){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED) {
            startDownload()
        } else {
            requestWritePermission(view)
        }
    }

    private fun startDownload(){
        status_text.text = getString(R.string.starting_download, downloadFileName)

        val request = DownloadManager.Request(Uri.parse(downloadFileUrl))
        request.apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            setTitle(downloadFileName)
            setDescription(getString(R.string.downloading_from, getString(R.string.app_name)))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadFileName)

            downloadId = downloadManager.enqueue(this)
        }
    }

    private fun showDownloadStatus(){
        val cursor: Cursor =
            downloadManager.query(DownloadManager.Query().setFilterById(downloadId))

        if (cursor.moveToNext()) {
            val status: Int = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val totalSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalSize = cursor.getInt(totalSizeIndex)
            val totalDownloaded = cursor.getLong(downloadedIndex)
            cursor.close()
            val statusData = when(status){
                DownloadManager.STATUS_PENDING -> "Download pending. Please wait.."
                DownloadManager.STATUS_SUCCESSFUL -> "Download successful"
                DownloadManager.STATUS_RUNNING -> "Download in progress"
                DownloadManager.STATUS_FAILED -> "Download failed"
                else -> "Unknown status"
            }
            status_text.append("\n$statusData")

            if(status == DownloadManager.STATUS_RUNNING){
                status_text.append("\n $totalDownloaded  out of $totalSize")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadCompleteReceiver)
    }


    private fun requestWritePermission(view: View) {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            val snack = Snackbar.make(view, R.string.write_permission, Snackbar.LENGTH_INDEFINITE)
            snack.setAction("OK", View.OnClickListener {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_WRITE)
            })
            snack.show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_WRITE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_WRITE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload()
            } else {
                Toast.makeText(this, getString(R.string.write_permission_denied),
                    Toast.LENGTH_SHORT). show()
            }
        }
    }


}

