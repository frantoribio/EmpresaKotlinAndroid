package es.frantoribio.empresa.fragments

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import es.frantoribio.empresa.EventPost
import es.frantoribio.empresa.databinding.FragmentDialogAddBinding
import es.frantoribio.empresa.entities.Product
import es.frantoribio.empresa.interfaces.MainDialog

class AddDialogFragment: DialogFragment(), OnShowListener{
    private var binding: FragmentDialogAddBinding? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null
    private var product: Product? = null
    private var photoSelectUri: Uri? = null


    private val resultLauncher = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            photoSelectUri = it.data?.data
            binding?.let{
                Glide.with(this)
                    .load(photoSelectUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(it.imageProduct)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let{ activity ->
            binding = FragmentDialogAddBinding.inflate(LayoutInflater.from(context))
            binding?.let {
                val builder = AlertDialog.Builder(activity)
                    .setTitle("Añadir producto")
                    .setPositiveButton("Agregar", null)
                    .setNegativeButton("Cancelar", null)
                    .setView(it.root)
                val dialog = builder.create()
                dialog.setOnShowListener(this)
                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        initProduct()
        configButtons()
        val dialog = dialog as AlertDialog
        dialog.let {
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            positiveButton?.setOnClickListener {
                binding?.let {
                    uploadImage(product?.id){ eventPost ->
                        if (eventPost.isSuccess){
                            if(product == null){
                                val product = Product(
                                    name = it.editTextName.text.toString().trim(),
                                    price = it.editTextPrice.text.toString().trim(),
                                    category = it.editTextCategory.text.toString().trim(),
                                    imgProduct = eventPost.photoUri)
                                saveProduct(product, eventPost.documentId!!)
                            }else{
                                product?.apply {
                                    name = it.editTextName.text.toString().trim()
                                    price = it.editTextPrice.text.toString().trim()
                                    category = it.editTextCategory.text.toString().trim()
                                    imgProduct = eventPost.photoUri
                                    updateProduct(this)
                                }
                            }
                        }
                    }
                }
                dismiss()
            }
            negativeButton?.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun uploadImage(productId: String?, callback: (EventPost)->Unit) {
        val eventPost = EventPost()
        eventPost.documentId = productId ?: FirebaseFirestore
            .getInstance().collection("products").document().id
        val storageRef = FirebaseStorage.getInstance().reference.child("product_images")
        photoSelectUri?.let{ uri->
            binding?.let{
                val photoRef = storageRef.child(eventPost.documentId!!)
                photoRef.putFile(uri)
                    .addOnSuccessListener{
                        it.storage.downloadUrl.addOnSuccessListener{down ->
                            Log.i("URL", down.toString())
                            eventPost.isSuccess = true
                            eventPost.photoUri = down.toString()
                            callback(eventPost)
                        }
                    }
                    .addOnFailureListener {
                        eventPost.isSuccess = false
                        callback(eventPost)
                    }
            }
        }
    }

    private fun configButtons() {
        binding?.let {
            it.ibProduct.setOnClickListener {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private fun updateProduct(product: Product) {
        val db = FirebaseFirestore.getInstance()
        product.id?.let { id->
            db.collection("products")
            .document(id)
            .set(product)
            .addOnSuccessListener {
                Toast.makeText(activity, "Datos actualizados", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                dismiss()
            }
        }
    }

    private fun saveProduct(product: Product, documentId: String){
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            //.add(product)
        .document(documentId)
        .set(product)
        .addOnSuccessListener {
            Toast.makeText(activity, "Producto añadido", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(activity, "Error al insertar", Toast.LENGTH_SHORT).show()
        }
        .addOnCompleteListener {
            dismiss()
        }
    }

    private fun initProduct() {
        product = (activity as? MainDialog)?.getProductSelected()
        product?.let { product->
            binding?.let {
                it.editTextName.setText(product.name)
                it.editTextPrice.setText(product.price)
                it.editTextCategory.setText(product.category)
                Glide.with(this)
                .load(product.imgProduct)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(it.imageProduct)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}