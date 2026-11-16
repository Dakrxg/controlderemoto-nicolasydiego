package cl.inacap.ProyectoIoT

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cl.inacap.ProyectoIoT.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var rootRef: DatabaseReference
    private lateinit var puertaRef: DatabaseReference
    private lateinit var nodo1Ref: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var alertaPresenciaMostrada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (_: Exception) {
        }

        rootRef = FirebaseDatabase.getInstance().reference
        puertaRef = rootRef.child("puerta_acceso")
        nodo1Ref = rootRef.child("nodo1")

        puertaRef.keepSynced(true)
        nodo1Ref.keepSynced(true)

        binding.tvUserLogged.text = "Operador: ${auth.currentUser?.email}"

        monitorearConexion()

        binding.btnVerHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        escucharSensores()

        binding.btnLock.setOnClickListener {
            enviarComando("LOCK")
        }

        binding.btnUnlock.setOnClickListener {
            enviarComando("UNLOCK")
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun monitorearConexion() {
        FirebaseDatabase.getInstance().getReference(".info/connected")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val conectado = snapshot.getValue(Boolean::class.java) ?: false
                    val correo = auth.currentUser?.email ?: "Operador"
                    if (conectado) {
                        binding.tvUserLogged.text = "Operador: $correo (En línea)"
                        binding.tvUserLogged.setTextColor(Color.parseColor("#1B5E20"))
                    } else {
                        binding.tvUserLogged.text = "MODO OFFLINE: Comandos en cola local"
                        binding.tvUserLogged.setTextColor(Color.parseColor("#BA1A1A"))
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun escucharSensores() {
        nodo1Ref.child("sensores/distancia").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val distancia = snapshot.getValue(Long::class.java) ?: 999L
                if (distancia in 1..15) {
                    if (!alertaPresenciaMostrada) {
                        Toast.makeText(
                            this@MainActivity,
                            "Presencia detectada en el panel ($distancia cm)",
                            Toast.LENGTH_SHORT
                        ).show()
                        alertaPresenciaMostrada = true
                    }
                } else {
                    alertaPresenciaMostrada = false
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        puertaRef.child("sensores").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val swMarco = snapshot.child("sw_marco").getValue(Boolean::class.java) ?: false
                val swCerrojo = snapshot.child("sw_cerrojo").getValue(Boolean::class.java) ?: false
                actualizarEstadoVisual(swMarco, swCerrojo)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error Firebase: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        puertaRef.child("alertas/puerta_olvidada").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alertaActiva = snapshot.getValue(Boolean::class.java) ?: false
                binding.cardAlerta.visibility = if (alertaActiva) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun actualizarEstadoVisual(swMarco: Boolean, swCerrojo: Boolean) {
        if (swMarco) {
            binding.tvSwMarco.text = "Switch Marco: Hoja ajustada (Cerrado)"
        } else {
            binding.tvSwMarco.text = "Switch Marco: Desacoplado (Abierto)"
        }

        if (swCerrojo) {
            binding.tvSwCerrojo.text = "Switch Cerrojo: Pasador asegurado"
        } else {
            binding.tvSwCerrojo.text = "Switch Cerrojo: Sin seguro"
        }

        when {
            !swMarco -> {
                binding.tvEstadoGeneral.text = "PUERTA ABIERTA"
                binding.tvEstadoGeneral.setTextColor(Color.parseColor("#1B5E20")) // Verde
            }
            swMarco && !swCerrojo -> {
                binding.tvEstadoGeneral.text = "ARRIMADA (SIN SEGURO)"
                binding.tvEstadoGeneral.setTextColor(Color.parseColor("#E65100")) // Naranja
            }
            swMarco && swCerrojo -> {
                binding.tvEstadoGeneral.text = "CERRADA Y BLOQUEADA"
                binding.tvEstadoGeneral.setTextColor(Color.parseColor("#BA1A1A")) // Rojo
            }
        }

        // Botones siempre habilitados y con color intenso para pruebas libres
        binding.btnLock.isEnabled = true
        binding.btnUnlock.isEnabled = true
        binding.btnLock.alpha = 1.0f
        binding.btnUnlock.alpha = 1.0f
        binding.btnLock.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1B5E20"))
        binding.btnUnlock.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1565C0"))
    }

    private fun enviarComando(accion: String) {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val fechaLegible = formato.format(Date())

        val payload = mapOf(
            "comando" to accion,
            "accionado_por" to (auth.currentUser?.email ?: "desconocido"),
            "timestamp" to ServerValue.TIMESTAMP,
            "fecha_hora" to fechaLegible
        )

        puertaRef.child("actuador").setValue(payload)
            .addOnSuccessListener {
                puertaRef.child("historial").push().setValue(payload)

                // Cambio visual inmediato en la tarjeta de estado al presionar el botón
                if (accion == "LOCK") {
                    binding.tvEstadoGeneral.text = "PUERTA CERRADA"
                    binding.tvEstadoGeneral.setTextColor(Color.parseColor("#BA1A1A")) // Rojo
                } else if (accion == "UNLOCK") {
                    binding.tvEstadoGeneral.text = "PUERTA ABIERTA"
                    binding.tvEstadoGeneral.setTextColor(Color.parseColor("#1B5E20")) // Verde
                }

                Toast.makeText(this, "Comando $accion enviado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}

