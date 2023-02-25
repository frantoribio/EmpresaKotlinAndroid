package es.frantoribio.empresa.interfaces

import es.frantoribio.empresa.entities.Product

interface MainDialog {
    fun getProductSelected(): Product?
}