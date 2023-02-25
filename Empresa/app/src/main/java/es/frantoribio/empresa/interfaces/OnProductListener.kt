package es.frantoribio.empresa.interfaces

import es.frantoribio.empresa.entities.Product

interface OnProductListener {

    fun onClick(product: Product)
    fun onLongClick(product: Product)
}