package es.frantoribio.empresa

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import es.frantoribio.empresa.adapters.ProductAdapter
import es.frantoribio.empresa.databinding.ActivityMainBinding
import es.frantoribio.empresa.entities.Product
import es.frantoribio.empresa.fragments.AddDialogFragment
import es.frantoribio.empresa.interfaces.MainDialog
import es.frantoribio.empresa.interfaces.OnProductListener

class MainActivity : AppCompatActivity(), OnProductListener, MainDialog {

    private lateinit var  binding: ActivityMainBinding
    private lateinit var mAdapter: ProductAdapter
    private lateinit var mListenerRegistration: ListenerRegistration
    private var productSelected: Product? = null

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener


    override fun onCreate(savedInstanceState: Bundle?) {
        configAuth()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setProducts()
        //setupRecylerView()
        setFilter()
        configButtons()
    }
    //metodo de autentificacion
    private fun configAuth() {
        //inicialiar las variables
        firebaseAuth = FirebaseAuth.getInstance()

        //ejecutamos el listener
        authStateListener = FirebaseAuth.AuthStateListener {

            if (it.currentUser != null){ //si el usuario ya esta autenticado
                supportActionBar?.title = it.currentUser?.displayName  //ponemos el nombre del usuario en la toolbar
                //binding.textInit.visibility = View.VISIBLE  //haer visible...
            }else {
                //si el usuario no esta autenticado entonces
                //crear la lista de proveedores
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()) //se añade otro proveedor

                //lanzar el intent
                resultLauncher.launch(//este bloque es el intent para mostrar el logeado
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build())
            }
        }
    }

    private var resultLauncher = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()){

        val response = IdpResponse.fromResultIntent(it.data)

        if (it.resultCode == RESULT_OK){
            val user = FirebaseAuth.getInstance().currentUser //datos del usuario identificado

            if (user != null){
                Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
            }
        }else {
            if(response == null){ //el usuario a pulsado hacia atras para salir de la APP
                Toast.makeText(this, "Adios....", Toast.LENGTH_SHORT).show()
                finish()
            }else { //se debe tratar los errores de conexion
                response.error?.let{
                    if(it.errorCode == ErrorCodes.NO_NETWORK){
                        Toast.makeText(this, "Sin red", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this, "Codigo error: ${it.errorCode}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    //vincular las dos variables se realiza en el ciclo de vida
    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener (authStateListener)
    }

    private fun configButtons() {
        binding.btnAddProduct.setOnClickListener {
            productSelected = null
            AddDialogFragment().show(supportFragmentManager, "Add")
        }
    }

    private fun setFilter() {
        binding.allProducts.setOnClickListener {
            //getAllProducts()
            getProductsTimereal()
            setupRecylerView()
        }
        binding.filterZapatos.setOnClickListener {
            getZapatos()
            setupRecylerView()
        }
        binding.filterRopa.setOnClickListener {
            getRopa()
            setupRecylerView()
        }
    }

    private fun getRopa() {
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
        .whereEqualTo("category", "Ropa")
        .get()
        .addOnSuccessListener { snapshots ->
            for (document in snapshots){
                val product = document.toObject(Product::class.java)
                product.id = document.id
                mAdapter.add(product)
            }
        }
        .addOnFailureListener{
            Toast.makeText(this, "Error al consultar los datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAllProducts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
        .get()
        .addOnSuccessListener { snapshots ->
            for (document in snapshots){
                val product = document.toObject(Product::class.java)
                product.id = document.id
                mAdapter.add(product)
            }
        }
        .addOnFailureListener{
            Toast.makeText(this, "Error al consultar los datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecylerView() {
        mAdapter = ProductAdapter(mutableListOf(), this)
        binding.recycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mAdapter
        }
        //getProducts()
    }

    private fun getZapatos() {
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
        .whereEqualTo("category", "Zapatos")
        .get()
        .addOnSuccessListener { snapshots ->
            for (document in snapshots){
                val product = document.toObject(Product::class.java)
                product.id = document.id
                mAdapter.add(product)
            }
        }
        .addOnFailureListener{
            Toast.makeText(this, "Error al consultar los datos", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun getProductsTimereal(){
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection("products")
        mListenerRegistration = productRef.addSnapshotListener { snapshots, error ->
            if (error != null){
                Toast.makeText(this, "Error al consultar los datos", Toast.LENGTH_SHORT)
                    .show()
                return@addSnapshotListener
            }
            for (snapshot in snapshots!!.documentChanges){
                val product = snapshot.document.toObject(Product::class.java)
                product.id = snapshot.document.id
                when(snapshot.type){
                    DocumentChange.Type.ADDED -> mAdapter.add(product)
                    DocumentChange.Type.MODIFIED -> mAdapter.update(product)
                    DocumentChange.Type.REMOVED -> mAdapter.delete(product)
                }
            }
        }
    }

    override fun onClick(product: Product) {
        productSelected = product
        AddDialogFragment().show(supportFragmentManager, "Add")
    }

    override fun onLongClick(product: Product) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection("products")
        product.id?.let{ id->
            productRef.document(id)
            .delete()
            .addOnFailureListener {
                Toast.makeText(this, "error al eliminar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getProductSelected(): Product? {
        return productSelected
    }

    //inflar el menu para salir de la app
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_sign_out ->{
                AuthUI.getInstance().signOut(this) //salir de la session
                    .addOnSuccessListener {
                        Toast.makeText(this, "Session cerrada", Toast.LENGTH_SHORT).show()
                    }
                    //otra forma de comprobar si ha salido de forma correcta
                  /*  .addOnCompleteListener {
                        if(it.isSuccessful){
                            binding.textInit.visibility = View.GONE
                        }else {
                            Toast.makeText(this, "no se puede cerrar la session", Toast.LENGTH_SHORT).show()
                        }
                    }*/
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setProducts() {
        val documentId: String
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection("products")
        val data1 = hashMapOf(
            "name" to "Calzado",
            "price" to "36",
            "category" to "Zapatos",
        )
        productRef.document().set(data1)
        val data2 = hashMapOf(
            "name" to "Jersey",
            "price" to "23",
            "category" to "Ropa"
        )
        productRef.document().set(data2)
        val data3 = hashMapOf(
            "name" to "Zapatillas",
            "price" to "15",
            "category" to "Zapatos"
        )
        productRef.document().set(data3)
        val data4 = hashMapOf(
            "name" to "Sudadera",
            "price" to "23",
            "category" to "Ropa"
        )
        productRef.document().set(data4)
        val data5 = hashMapOf(
            "name" to "Babuchas",
            "price" to "2",
            "category" to "Zapatos"
        )
        productRef.document().set(data5) //guardalo
    }
}