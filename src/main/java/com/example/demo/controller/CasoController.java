package com.example.demo.controller;

import com.example.demo.model.Caso;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
public class CasoController {

    @GetMapping("/")
    public String listarCasos(Model model) {
        List<Caso> casos = Caso.findAll().orderBy("id desc");
        model.addAttribute("casos", casos);

        java.sql.Date hoy = new java.sql.Date(System.currentTimeMillis());
        List<Caso> tareasHoy = Caso.where("fecha_vencimiento = ? OR fecha_inicio = ?", hoy, hoy);
        model.addAttribute("tareas_hoy", tareasHoy);

        return "index";
    }

    @PostMapping("/crear")
    public String crearCaso(
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(defaultValue = "Judicial") String tipo,
            @RequestParam(required = false) String fecha_inicio,
            @RequestParam(required = false) String fecha_vencimiento,
            @RequestParam(required = false, defaultValue = "false") Boolean tiene_audiencia,
            @RequestParam(required = false) String fecha_audiencia) {

        Caso nuevoCaso = new Caso();
        nuevoCaso.set("titulo", titulo);
        nuevoCaso.set("descripcion", descripcion != null ? descripcion : "");
        nuevoCaso.set("tipo", tipo != null && !tipo.isEmpty() ? tipo : "Judicial");
        nuevoCaso.set("estado", "En trámite");

        if (fecha_inicio != null && !fecha_inicio.trim().isEmpty()) {
            nuevoCaso.set("fecha_inicio", java.sql.Date.valueOf(fecha_inicio));
        } else {
            nuevoCaso.set("fecha_inicio", new java.sql.Date(System.currentTimeMillis()));
        }

        if (fecha_vencimiento != null && !fecha_vencimiento.trim().isEmpty()) {
            nuevoCaso.set("fecha_vencimiento", java.sql.Date.valueOf(fecha_vencimiento));
        }

        boolean audiencia = Boolean.TRUE.equals(tiene_audiencia);
        nuevoCaso.set("tiene_audiencia", audiencia);

        if (audiencia && fecha_audiencia != null && !fecha_audiencia.trim().isEmpty()) {
            String formatted = fecha_audiencia.replace("T", " ");
            if (formatted.length() == 16) formatted += ":00";
            try {
                nuevoCaso.set("fecha_audiencia", java.sql.Timestamp.valueOf(formatted));
            } catch (Exception ignored) {
                try {
                    nuevoCaso.set("fecha_audiencia", java.sql.Date.valueOf(fecha_audiencia));
                } catch (Exception e) {
                    nuevoCaso.set("fecha_audiencia", new java.sql.Timestamp(System.currentTimeMillis()));
                }
            }
        }

        nuevoCaso.saveIt();
        return "redirect:/";
    }
}