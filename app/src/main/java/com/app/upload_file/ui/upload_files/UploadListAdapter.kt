package com.app.upload_file.ui.upload_files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.upload_file.R

class UploadListAdapter(var fileNameList: List<String>, var fileDoneList: List<String>) :
    RecyclerView.Adapter<UploadListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.single_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fileName = fileNameList[position]
        holder.fileName.text = fileName
        val fileDone = fileDoneList[position]
        if (fileDone == "Uploading") {
            holder.fileDone.setImageResource(R.drawable.progress)
        } else {
            holder.fileDone.setImageResource(R.drawable.checked)
        }
    }

    override fun getItemCount(): Int {
        return fileNameList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var fileName: TextView
        var fileDone: ImageView

        init {
            fileName = itemView.findViewById(R.id.txtFilename)
            fileDone = itemView.findViewById(R.id.imgLoading)
        }
    }
}