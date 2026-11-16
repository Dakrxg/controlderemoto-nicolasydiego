package cl.inacap.ProyectoIoT

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistorialAdapter(private val lista: List<RegistroHistorial>) :
    RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAccion: TextView = view.findViewById(R.id.tvAccion)
        val tvUsuario: TextView = view.findViewById(R.id.tvUsuario)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.tvAccion.text = "Acción: ${item.comando}"
        holder.tvUsuario.text = "Operador: ${item.accionado_por}"
        holder.tvFecha.text = "Fecha/Hora: ${item.fecha_hora}"

        if (item.comando == "LOCK") {
            holder.tvAccion.setTextColor(Color.parseColor("#1B5E20"))
        } else {
            holder.tvAccion.setTextColor(Color.parseColor("#E65100"))
        }
    }

    override fun getItemCount() = lista.size
}