package es.frantoribio.empresa.entities

import com.google.firebase.firestore.Exclude

data class Product(
    @get:Exclude var id: String? = null,
    var name: String? = null,
    var price: String? = null,
    var category: String? = null,
    var imgProduct: String? = null
)
