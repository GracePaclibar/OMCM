    package com.bscpe.omcmapp

    import android.net.Uri
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
    import androidx.recyclerview.widget.RecyclerView

    class GalleryAdapter (private val imageList: List<Uri>) :
        RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.gallery)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.gallery_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val imageUri = imageList[position]
            holder.imageView.setImageURI(imageUri)
        }

        override fun getItemCount(): Int {
            return imageList.size
        }
    }