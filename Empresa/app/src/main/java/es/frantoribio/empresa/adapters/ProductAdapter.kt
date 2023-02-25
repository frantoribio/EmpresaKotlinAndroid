package es.frantoribio.empresa.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy


import es.frantoribio.empresa.R
import es.frantoribio.empresa.databinding.ItemProductBinding
import es.frantoribio.empresa.entities.Product
import es.frantoribio.empresa.interfaces.OnProductListener

class ProductAdapter(private val productList: MutableList<Product>,
                     private val listener: OnProductListener
)
    : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private lateinit var mContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mContext = parent.context
        val view = LayoutInflater.from(mContext)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = productList[position]
        holder.bind(item)
        holder.setListener(item)
    }

    override fun getItemCount(): Int{
        return productList.size
    }

    fun add(product: Product){
        if (!productList.contains(product)){
            productList.add(product)
            notifyItemInserted(productList.size-1)
        }else{
            update(product)
        }
    }

    fun update(product: Product){
        val index = productList.indexOf(product)
        if (index != -1){
            productList.set(index, product)
            notifyItemChanged(index)
        }
    }

    fun delete(product: Product){
        val index = productList.indexOf(product)
        if (index != -1){
            productList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val binding = ItemProductBinding.bind(view)

        fun bind(product: Product){
            binding.textName.text = product.name
            binding.textPrice.text = product.price.toString()
            binding.textCategory.text = product.category

            Glide.with(mContext)
                .load(product.imgProduct)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.imageView)
        }

        fun setListener(product: Product){
            binding.root.setOnClickListener {
                listener.onClick(product)
            }
            binding.root.setOnLongClickListener{
                listener.onLongClick(product)
                true
            }
        }
    }
}