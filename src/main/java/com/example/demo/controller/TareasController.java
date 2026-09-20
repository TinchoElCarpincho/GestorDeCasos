package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class TareasController {

    @GetMapping("/tareas")
    public String tareas(org.springframework.ui.Model model) {
        List<Map<String, Object>> casosHoy = new ArrayList<>();
        List<Map<String, Object>> audienciasHoy = new ArrayList<>();
        List<Map<String, Object>> vencimientosHoy = new ArrayList<>();
        List<Map<String, Object>> docsHoy = new ArrayList<>();

        String todayStr = LocalDate.now().toString();
        int counter = 1;

        try {
            for (Map row : org.javalite.activejdbc.Base.findAll("SELECT * FROM casos ORDER BY id desc")) {
                String idStr = row.get("id") != null ? row.get("id").toString() : String.valueOf(counter++);
                String titulo = row.get("titulo") != null ? row.get("titulo").toString() : "Sin título";
                String desc = row.get("descripcion") != null ? row.get("descripcion").toString() : "";
                String tipo = row.get("tipo") != null ? row.get("tipo").toString() : "Judicial";
                String exp = row.get("expediente") != null ? row.get("expediente").toString() : "EXP-" + idStr;

                // 1. Vencimiento hoy
                if (row.get("fecha_vencimiento") != null) {
                    String fv = row.get("fecha_vencimiento").toString().trim();
                    if (fv.startsWith(todayStr)) {
                        Map<String, Object> ev = new HashMap<>();
                        ev.put("id", "venc_" + idStr);
                        ev.put("caso_id", idStr);
                        ev.put("titulo", titulo);
                        ev.put("descripcion", desc);
                        ev.put("tipo", "Vencimiento");
                        ev.put("tipo_slug", "vencimiento");
                        ev.put("expediente", exp);
                        ev.put("badge", "Vencimiento Hoy");
                        ev.put("hora", "Fin de jornada");
                        ev.put("tarea_texto", "Controlar vencimiento procesal: " + titulo);
                        vencimientosHoy.add(ev);
                        casosHoy.add(ev);
                    }
                }

                // 2. Audiencia hoy
                Object audObj = row.get("tiene_audiencia");
                boolean tieneAud = Boolean.TRUE.equals(audObj) || "true".equalsIgnoreCase(String.valueOf(audObj)) || "t".equalsIgnoreCase(String.valueOf(audObj)) || "1".equals(String.valueOf(audObj));
                if (tieneAud && row.get("fecha_audiencia") != null) {
                    String fa = row.get("fecha_audiencia").toString().trim();
                    if (fa.startsWith(todayStr)) {
                        if (fa.endsWith(".0")) fa = fa.substring(0, fa.length() - 2);
                        String horaSolo = "Horario a confirmar";
                        if (fa.contains(" ")) {
                            String[] parts = fa.split(" ");
                            if (parts.length > 1 && parts[1].length() >= 5) {
                                horaSolo = parts[1].substring(0, 5) + " hs";
                            }
                        } else if (fa.contains("T")) {
                            String[] parts = fa.split("T");
                            if (parts.length > 1 && parts[1].length() >= 5) {
                                horaSolo = parts[1].substring(0, 5) + " hs";
                            }
                        }
                        Map<String, Object> ev = new HashMap<>();
                        ev.put("id", "aud_" + idStr);
                        ev.put("caso_id", idStr);
                        ev.put("titulo", titulo);
                        ev.put("descripcion", desc);
                        ev.put("tipo", "Audiencia");
                        ev.put("tipo_slug", "audiencia");
                        ev.put("expediente", exp);
                        ev.put("badge", "Audiencia · " + horaSolo);
                        ev.put("hora", horaSolo);
                        ev.put("tarea_texto", "Asistir a audiencia (" + horaSolo + "): " + titulo);
                        audienciasHoy.add(ev);
                        casosHoy.add(ev);
                    }
                }

                // 3. Documentación hoy
                Object reqDocsObj = row.get("requiere_docs");
                boolean reqDocs = Boolean.TRUE.equals(reqDocsObj) || "true".equalsIgnoreCase(String.valueOf(reqDocsObj)) || "t".equalsIgnoreCase(String.valueOf(reqDocsObj)) || "1".equals(String.valueOf(reqDocsObj));
                if (reqDocs && row.get("fecha_docs") != null) {
                    String fd = row.get("fecha_docs").toString().trim();
                    if (fd.startsWith(todayStr)) {
                        String hd = row.get("hora_docs") != null && !row.get("hora_docs").toString().trim().isEmpty()
                                ? row.get("hora_docs").toString().trim() + " hs"
                                : "Horario a confirmar";
                        Map<String, Object> ev = new HashMap<>();
                        ev.put("id", "docs_" + idStr);
                        ev.put("caso_id", idStr);
                        ev.put("titulo", titulo);
                        ev.put("descripcion", desc);
                        ev.put("tipo", "Documentación");
                        ev.put("tipo_slug", "docs");
                        ev.put("expediente", exp);
                        ev.put("badge", "Documentación · " + hd);
                        ev.put("hora", hd);
                        ev.put("tarea_texto", "Presentar documentación (" + hd + "): " + titulo);
                        docsHoy.add(ev);
                        casosHoy.add(ev);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        LocalDate hoy = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "AR"));
        String fechaHoyLegible = hoy.format(dtf);
        if (!fechaHoyLegible.isEmpty()) {
            fechaHoyLegible = Character.toUpperCase(fechaHoyLegible.charAt(0)) + fechaHoyLegible.substring(1);
        }

        List<Map<String, Object>> cronogramaHoy = new ArrayList<>();
        cronogramaHoy.addAll(audienciasHoy);
        cronogramaHoy.addAll(docsHoy);

        model.addAttribute("fecha_hoy_legible", fechaHoyLegible);
        model.addAttribute("total_actividades_hoy", casosHoy.size());
        model.addAttribute("total_audiencias_hoy", audienciasHoy.size());
        model.addAttribute("total_vencimientos_hoy", vencimientosHoy.size());
        model.addAttribute("total_docs_hoy", docsHoy.size());

        model.addAttribute("casos_hoy", casosHoy);
        model.addAttribute("audiencias_hoy", audienciasHoy);
        model.addAttribute("vencimientos_hoy", vencimientosHoy);
        model.addAttribute("docs_hoy", docsHoy);
        model.addAttribute("cronograma_hoy", cronogramaHoy);
        model.addAttribute("casos", casosHoy);

        return "tareas";
    }
}
