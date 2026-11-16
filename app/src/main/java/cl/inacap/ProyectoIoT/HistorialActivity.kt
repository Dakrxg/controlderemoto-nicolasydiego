package cl.inacap.ProyectoIoT

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistorialActivity : AppCompatActivity() {

    private lateinit var contenedorHistorial: LinearLayout
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        auth = FirebaseAuth.getInstance()
        contenedorHistorial = findViewById(R.id.contenedorHistorial)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        btnVolver.setOnClickListener {
            finish()
        }

        database = FirebaseDatabase.getInstance()
            .getReference("puerta_acceso")
            .child("historial")

        cargarHistorial()
    }

    private fun cargarHistorial() {
        val correoActivo = auth.currentUser?.email ?: ""

        database.limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contenedorHistorial.removeAllViews()
                val inflater = LayoutInflater.from(this@HistorialActivity)

                for (registro in snapshot.children.reversed()) {
                    val operador = registro.child("accionado_por").getValue(String::class.java) ?: ""

                    // Solo muestra las tarjetas que coincidan con la sesión actual
                    if (operador.equals(correoActivo, ignoreCase = true)) {
                        val comando = registro.child("comando").getValue(String::class.java) ?: "DESCONOCIDO"
                        val fechaHora = registro.child("fecha_hora").getValue(String::class.java) ?: "--"

                        val tarjeta = inflater.inflate(R.layout.item_historial, contenedorHistorial, false)
                        val tvAccion = tarjeta.findViewById<TextView>(R.id.tvAccion)
                        val tvUsuario = tarjeta.findViewById<TextView>(R.id.tvUsuario)
                        val tvFecha = tarjeta.findViewById<TextView>(R.id.tvFecha)

                        tvAccion.text = "Acción: $comando"
                        tvAccion.setTextColor(
                            if (comando == "LOCK") Color.parseColor("#66BB6A")
                            else Color.parseColor("#FF6D00")
                        )
                        tvUsuario.text = "Operador: $operador"
                        tvFecha.text = "Fecha/Hora: $fechaHora"

                        contenedorHistorial.addView(tarjeta)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@HistorialActivity,
                    "Error al leer bitácora: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}