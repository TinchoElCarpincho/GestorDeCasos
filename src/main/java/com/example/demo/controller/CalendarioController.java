package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CalendarioController {

    @GetMapping("/calendario")
    public String calendario(org.springframework.ui.Model model) {
        List<Map<String, Object>> eventos = new ArrayList<>();
        try {
            for (Map row : org.javalite.activejdbc.Base.findAll("SELECT * FROM casos ORDER BY id desc")) {
                String idStr = row.get("id") != null ? row.get("id").toString() : "0";
                String titulo = row.get("titulo") != null ? row.get("titulo").toString() : "Sin título";
                String desc = row.get("descripcion") != null ? row.get("descripcion").toString() : "";

                // 1. Vencimiento procesal
                if (row.get("fecha_vencimiento") != null) {
                    String fv = row.get("fecha_vencimiento").toString().trim();
                    if (!fv.isEmpty()) {
                        if (fv.contains(" ")) fv = fv.split(" ")[0];
                        if (fv.contains("T")) fv = fv.split("T")[0];
                        Map<String, Object> ev = new HashMap<>();
                        ev.put("id", "venc_" + idStr);
                        ev.put("caso_id", idStr);
                        ev.put("titulo", titulo);
                        ev.put("descripcion", desc.isEmpty() ? "Vencimiento de plazo procesal asignado al expediente." : desc);
                        ev.put("tipo", "Vencimiento");
                        ev.put("tipo_slug", "plazo");
                        ev.put("fecha", fv);
                        ev.put("hora", "Todo el día");
                        ev.put("badge", "Vencimiento");
                        eventos.add(ev);
                    }
                }

                // 2. Audiencia judicial/extrajudicial
                Object audObj = row.get("tiene_audiencia");
                boolean tieneAud = Boolean.TRUE.equals(audObj) || "true".equalsIgnoreCase(String.valueOf(audObj)) || "t".equalsIgnoreCase(String.valueOf(audObj)) || "1".equals(String.valueOf(audObj));
                if (tieneAud && row.get("fecha_audiencia") != null) {
                    String fa = row.get("fecha_audiencia").toString().trim();
                    if (fa.endsWith(".0")) fa = fa.substring(0, fa.length() - 2);
                    if (!fa.isEmpty()) {
                        String fechaSolo = fa;
                        String horaSolo = "Horario a confirmar";
                        if (fa.contains(" ")) {
                            String[] parts = fa.split(" ");
                            fechaSolo = parts[0];
                            if (parts.length > 1 && parts[1].length() >= 5) {
                                horaSolo = parts[1].substring(0, 5) + " hs";
                            }
                        } else if (fa.contains("T")) {
                            String[] parts = fa.split("T");
                            fechaSolo = parts[0];
                            if (parts.length > 1 && parts[1].length() >= 5) {
                                horaSolo = parts[1].substring(0, 5) + " hs";
                            }
                        }
                        Map<String, Object> ev = new HashMap<>();
                        ev.put("id", "aud_" + idStr);
                        ev.put("caso_id", idStr);
                        ev.put("titulo", titulo);
                        ev.put("descripcion", desc.isEmpty() ? "Audiencia fijada en autos." : desc);
                        ev.put("tipo", "Audiencia");
                        ev.put("tipo_slug", "audiencia");
                        ev.put("fecha", fechaSolo);
                        ev.put("hora", horaSolo);
                        ev.put("badge", "Audiencia · " + horaSolo);
                        eventos.add(ev);
                    }
                }

                // 3. Presentación de documentación
                Object reqDocsObj = row.get("requiere_docs");
                boolean reqDocs = Boolean.TRUE.equals(reqDocsObj) || "true".equalsIgnoreCase(String.valueOf(reqDocsObj)) || "t".equalsIgnoreCase(String.valueOf(reqDocsObj)) || "1".equals(String.valueOf(reqDocsObj));
                if (reqDocs && row.get("fecha_docs") != null) {
                    String fd = row.get("fecha_docs").toString().trim();
                    if (!fd.isEmpty()) {
                        if (fd.contains(" ")) fd = fd.split(" ")[0];
                        if (fd.contains("T")) fd = fd.split("T")[0];
                        String hd = row.get("hora_docs") != null && !row.get("hora_docs").toString().trim().isEmpty()
                                ? row.get("hora_docs").toString().trim() + " hs"
                                : "Horario a confirmar";
                        Map<String, Object> ev = new HashMap<>();
                        ev.put("id", "docs_" + idStr);
                        ev.put("caso_id", idStr);
                        ev.put("titulo", titulo);
                        ev.put("descripcion", desc.isEmpty() ? "Plazo límite para presentación de documentación." : desc);
                        ev.put("tipo", "Documentación");
                        ev.put("tipo_slug", "docs");
                        ev.put("fecha", fd);
                        ev.put("hora", hd);
                        ev.put("badge", "Documentación · " + hd);
                        eventos.add(ev);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        model.addAttribute("eventos", eventos);
        model.addAttribute("total_eventos", eventos.size());
        model.addAttribute("casos", eventos);

        return "calendario";
    }
}
