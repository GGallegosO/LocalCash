package com.localcash.ui.compras;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.localcash.R;
import com.localcash.databinding.ActivityLugarCompraDetalleBinding;
import com.localcash.ui.BaseActivity;

/**
 * Placeholder de la Activity de detalle de un lugar de compra (Feature 010,
 * Fase 3).
 * <p>
 * En la Fase 4 se reemplaza el texto por el RecyclerView editable de ítems.
 * Por ahora solo recibe el id y lo muestra, para verificar que la navegación
 * desde el pin (long press y botón "Ver detalle") funciona.
 * </p>
 */
public class LugarCompraDetailActivity extends BaseActivity {

    /** Extra con el id del lugar de compra a mostrar. */
    public static final String EXTRA_LUGAR_COMPRA_ID = "lugar_compra_id";

    private ActivityLugarCompraDetalleBinding binding;

    /**
     * Crea el intent para abrir el detalle de un lugar.
     *
     * @param context       contexto de origen
     * @param lugarCompraId id del lugar
     * @return intent listo para {@code startActivity}
     */
    @NonNull
    public static Intent crearIntent(@NonNull Context context, long lugarCompraId) {
        Intent intent = new Intent(context, LugarCompraDetailActivity.class);
        intent.putExtra(EXTRA_LUGAR_COMPRA_ID, lugarCompraId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLugarCompraDetalleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarDetalleLugar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        long lugarCompraId = getIntent().getLongExtra(EXTRA_LUGAR_COMPRA_ID, -1L);
        binding.tvDetalleLugar.setText(
                getString(R.string.detalle_lugar_texto, lugarCompraId));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
