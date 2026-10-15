package com.localcash.ui.cuentas;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.localcash.R;
import com.localcash.databinding.ActivityCuentasBinding;
import com.localcash.databinding.ViewCuentasBinding;
import com.localcash.ui.BaseActivity;
import com.localcash.ui.theme.ThemeManager;
import com.localcash.ui.util.CurrencyFormatter;

/**
 * Activity principal de LocalCash (Módulo 1): 4 vistas en un {@code ViewFlipper}
 * controladas por un {@code BottomNavigationView}.
 * <p>
 * La Vista 1 (Cuentas) está conectada a Room vía {@link CuentasViewModel}:
 * lista de cuentas y saldo total consolidado se actualizan reactivamente.
 * </p>
 */
public class CuentasActivity extends BaseActivity {

    private ActivityCuentasBinding binding;

    /** Binding de la Vista 1 (se infla dentro del ViewFlipper, fuera de activity_cuentas.xml). */
    private ViewCuentasBinding vistaCuentas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCuentasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar Toolbar como ActionBar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.aCuentas_toolbar_title);
        }

        // Toggle de tema: misma lógica persistente que Login/Register (ThemeManager)
        binding.btnThemeToggle.setOnClickListener(v ->
                ThemeManager.setDarkMode(this, !ThemeManager.isDarkMode(this)));

        // Configurar BottomNavigationView para cambiar entre vistas del ViewFlipper
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_cuentas) {
                binding.viewFlipper.setDisplayedChild(0);
                return true;
            } else if (itemId == R.id.nav_nuevo) {
                binding.viewFlipper.setDisplayedChild(1);
                return true;
            } else if (itemId == R.id.nav_mapa) {
                binding.viewFlipper.setDisplayedChild(2);
                return true;
            } else if (itemId == R.id.nav_contactos) {
                binding.viewFlipper.setDisplayedChild(3);
                return true;
            }
            return false;
        });

        // Inflar los 4 layouts internos dentro del ViewFlipper
        // Vista 0: Cuentas (binding propio: sus vistas no forman parte de activity_cuentas.xml)
        vistaCuentas = ViewCuentasBinding.inflate(getLayoutInflater(), binding.viewFlipper, true);
        // Vista 1: Transacción
        getLayoutInflater().inflate(R.layout.view_transaccion, binding.viewFlipper);
        // Vista 2: Mapa
        getLayoutInflater().inflate(R.layout.view_mapa, binding.viewFlipper);
        // Vista 3: Contactos
        getLayoutInflater().inflate(R.layout.view_contactos, binding.viewFlipper);

        // Establecer la vista inicial
        binding.viewFlipper.setDisplayedChild(0);

        // Conectar la Vista 1 con Room (ViewModel + Adapter)
        configurarVistaCuentas();
    }

    /**
     * Conecta el RecyclerView de cuentas y la card de saldo total con el
     * {@link CuentasViewModel}: ambos se actualizan solos ante cada cambio en
     * la tabla {@code cuentas} (LiveData).
     */
    private void configurarVistaCuentas() {
        CuentasViewModel viewModel = new ViewModelProvider(this).get(CuentasViewModel.class);

        CuentasAdapter adapter = new CuentasAdapter();
        vistaCuentas.recyclerCuentas.setLayoutManager(new LinearLayoutManager(this));
        vistaCuentas.recyclerCuentas.setAdapter(adapter);

        viewModel.getTodasLasCuentas().observe(this, cuentas -> {
            boolean hayCuentas = cuentas != null && !cuentas.isEmpty();
            vistaCuentas.recyclerCuentas.setVisibility(hayCuentas ? View.VISIBLE : View.GONE);
            vistaCuentas.tvEmptyState.setVisibility(hayCuentas ? View.GONE : View.VISIBLE);
            adapter.setCuentas(cuentas);
        });

        viewModel.getSaldoTotal().observe(this, saldoTotal ->
                vistaCuentas.tvSaldoTotal.setText(CurrencyFormatter.format(saldoTotal)));

        // FAB: feedback mientras la funcionalidad de alta de cuentas no está implementada (T16+)
        vistaCuentas.fabNuevaCuenta.setOnClickListener(v -> Toast.makeText(this,
                getString(R.string.aCuentas_fab_proximamente), Toast.LENGTH_SHORT).show());
    }
}
