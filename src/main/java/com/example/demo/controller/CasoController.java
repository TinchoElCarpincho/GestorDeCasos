package com.example.demo.controller;

import com.example.demo.model.Caso;
import org.javalite.activejdbc.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CasoController {

    @GetMapping("/")
    public String listarCasos(org.springframework.ui.Model model) {
        List<Map<String, Object>> casos = new ArrayList<>();
        try {
            System.out.println("=== COLUMNAS DE CASOS ===");
            for (Map row : org.javalite.activejdbc.Base.findAll("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'casos'")) {
                System.out.println(row.get("column_name") + " : " + row.get("data_type"));
            }
            for (Map row : org.javalite.activejdbc.Base.findAll("SELECT * FROM casos ORDER BY id desc")) {
                Map<String, Object> m = new HashMap<>(row);

                // Asegurar claves no nulas para Mustache
                String idStr = m.get("id") != null ? m.get("id").toString() : "0";
                m.put("id", idStr);
                if (!m.containsKey("titulo") || m.get("titulo") == null) m.put("titulo", "Sin título");
                if (!m.containsKey("descripcion") || m.get("descripcion") == null) m.put("descripcion", "");

                String tipo = m.get("tipo") != null && !m.get("tipo").toString().trim().isEmpty() ? m.get("tipo").toString() : "Judicial";
                m.put("tipo", tipo);

                String estado = m.get("estado") != null && !m.get("estado").toString().trim().isEmpty() ? m.get("estado").toString() : "En trámite";
                m.put("estado", estado);

                // Número de expediente / Causa
                String exp = m.get("expediente") != null ? m.get("expediente").toString() : "EXP " + idStr + "/2026";
                m.put("expediente", exp);

                // Jurisdicción / Fuero
                String juris = m.get("jurisdiccion") != null ? m.get("jurisdiccion").toString() : "";
                if (juris.isEmpty()) {
                    if ("Civil y Comercial".equalsIgnoreCase(tipo) || "Civil".equalsIgnoreCase(tipo)) {
                        juris = "Juzgado Civ. y Com. N° 4";
                    } else if ("Laboral".equalsIgnoreCase(tipo)) {
                        juris = "Tribunal del Trabajo N° 2";
                    } else if ("Penal".equalsIgnoreCase(tipo)) {
                        juris = "Juzgado de Garantías N° 1";
                    } else if ("Familia".equalsIgnoreCase(tipo)) {
                        juris = "Juzgado de Familia N° 3";
                    } else if ("Extrajudicial".equalsIgnoreCase(tipo)) {
                        juris = "Mediación Previa CABA";
                    } else {
                        juris = "Juzgado Ordinario N° 1";
                    }
                }
                m.put("jurisdiccion", juris);

                // Rol de la parte
                String rol = m.get("rol_parte") != null ? m.get("rol_parte").toString() : "Actor / Demandante";
                m.put("rol_parte", rol);

                // Honorarios y Estado de Pago
                String hon = m.get("honorarios") != null && !m.get("honorarios").toString().trim().isEmpty() ? m.get("honorarios").toString() : "$150.000";
                m.put("honorarios", hon);

                Object hp = m.get("honorarios_pagados");
                boolean pagado = Boolean.TRUE.equals(hp) || "true".equalsIgnoreCase(String.valueOf(hp)) || "t".equalsIgnoreCase(String.valueOf(hp)) || "1".equals(String.valueOf(hp));
                m.put("honorarios_pagados", pagado);
                m.put("estado_pago", pagado ? "Pagado" : "Pendiente");
                m.put("estado_pago_slug", pagado ? "pagado" : "pendiente");

                // Slugs para estilos CSS dinámicos
                m.put("tipo_slug", slugify(tipo));
                m.put("estado_slug", slugify(estado));

                // Fechas en crudo y formateadas legibles ("17 Sep 2026")
                String fi = m.get("fecha_inicio") != null ? m.get("fecha_inicio").toString() : "";
                m.put("fecha_inicio", fi);
                m.put("fecha_inicio_formateada", formatearFecha(fi));

                String fv = m.get("fecha_vencimiento") != null ? m.get("fecha_vencimiento").toString() : "";
                m.put("fecha_vencimiento", fv);
                m.put("fecha_vencimiento_formateada", formatearFecha(fv));

                Object aud = m.get("tiene_audiencia");
                boolean tieneAud = Boolean.TRUE.equals(aud) || "true".equalsIgnoreCase(String.valueOf(aud)) || "t".equalsIgnoreCase(String.valueOf(aud));
                m.put("tiene_audiencia", tieneAud);

                String fa = "";
                if (m.get("fecha_audiencia") != null) {
                    fa = m.get("fecha_audiencia").toString();
                    if (fa.endsWith(".0")) {
                        fa = fa.substring(0, fa.length() - 2);
                    }
                }
                m.put("fecha_audiencia", fa);
                m.put("fecha_audiencia_formateada", formatearFechaHora(fa));

                // Requiere Documentación
                Object reqD = m.get("requiere_docs");
                boolean reqDocs = Boolean.TRUE.equals(reqD) || "true".equalsIgnoreCase(String.valueOf(reqD)) || "t".equalsIgnoreCase(String.valueOf(reqD)) || "1".equals(String.valueOf(reqD));
                m.put("requiere_docs", reqDocs);

                String fd = m.get("fecha_docs") != null ? m.get("fecha_docs").toString().trim() : "";
                m.put("fecha_docs", fd);

                String hd = m.get("hora_docs") != null ? m.get("hora_docs").toString().trim() : "";
                m.put("hora_docs", hd);

                String fdFormateada = "";
                if (reqDocs && !fd.isEmpty()) {
                    fdFormateada = formatearFecha(fd);
                    if (!hd.isEmpty()) {
                        fdFormateada += ", " + hd + " hs";
                    }
                }
                m.put("fecha_docs_formateada", fdFormateada);

                casos.add(m);
            }
        } catch (Exception ignored) {
        }

        model.addAttribute("casos", casos);

        // Casos del día (con plazos o fechas ese día: vencimientos, audiencias, documentación)
        List<Map<String, Object>> casosHoy = new ArrayList<>();
        String todayStr = java.time.LocalDate.now().toString();
        int counter = 1;
        for (Map<String, Object> c : casos) {
            String tituloCaso = c.get("titulo") != null ? c.get("titulo").toString() : "";
            Object cId = c.get("id") != null ? c.get("id") : counter++;

            // 1. Vencimiento hoy
            Object fv = c.get("fecha_vencimiento");
            if (fv != null && todayStr.equals(fv.toString())) {
                Map<String, Object> t = new HashMap<>();
                t.put("id", "case_venc_" + cId);
                t.put("titulo", tituloCaso);
                t.put("tipo", "Vencimiento");
                t.put("tipo_slug", "vencimiento");
                t.put("badge", "Vencimiento");
                t.put("tarea_texto", "Vencimiento: " + tituloCaso);
                casosHoy.add(t);
            }

            // 2. Audiencia hoy
            if (Boolean.TRUE.equals(c.get("tiene_audiencia")) && c.get("fecha_audiencia") != null) {
                if (c.get("fecha_audiencia").toString().startsWith(todayStr)) {
                    Map<String, Object> t = new HashMap<>();
                    String faHora = c.get("fecha_audiencia_formateada") != null ? c.get("fecha_audiencia_formateada").toString() : "";
                    if (faHora.contains(", ")) {
                        faHora = faHora.split(", ")[1];
                    } else {
                        faHora = "Hoy";
                    }
                    t.put("id", "case_aud_" + cId);
                    t.put("titulo", tituloCaso);
                    t.put("tipo", "Audiencia");
                    t.put("tipo_slug", "audiencia");
                    t.put("badge", "Audiencia · " + faHora);
                    t.put("tarea_texto", "Audiencia (" + faHora + "): " + tituloCaso);
                    casosHoy.add(t);
                }
            }

            // 3. Documentación hoy
            if (Boolean.TRUE.equals(c.get("requiere_docs")) && c.get("fecha_docs") != null) {
                if (todayStr.equals(c.get("fecha_docs").toString())) {
                    Map<String, Object> t = new HashMap<>();
                    String hd = (c.get("hora_docs") != null && !c.get("hora_docs").toString().trim().isEmpty())
                            ? c.get("hora_docs").toString().trim() + " hs"
                            : "Hoy";
                    t.put("id", "case_docs_" + cId);
                    t.put("titulo", tituloCaso);
                    t.put("tipo", "Documentación");
                    t.put("tipo_slug", "docs");
                    t.put("badge", "Documentación · " + hd);
                    t.put("tarea_texto", "Presentar documentación (" + hd + "): " + tituloCaso);
                    casosHoy.add(t);
                }
            }
        }
        model.addAttribute("casos_hoy", casosHoy);
        model.addAttribute("total_casos_hoy", casosHoy.size());
        model.addAttribute("tareas_hoy", casosHoy);

        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new java.util.Locale("es", "AR"));
        String fechaHoyLegible = hoy.format(dtf);
        if (!fechaHoyLegible.isEmpty()) {
            fechaHoyLegible = Character.toUpperCase(fechaHoyLegible.charAt(0)) + fechaHoyLegible.substring(1);
        }
        model.addAttribute("fecha_hoy_legible", fechaHoyLegible);

        return "index";
    }

    @PostMapping("/crear")
    public String crearCaso(
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(defaultValue = "Judicial") String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String expediente,
            @RequestParam(required = false) String honorarios,
            @RequestParam(required = false, defaultValue = "false") Boolean honorarios_pagados,
            @RequestParam(required = false) String fecha_inicio,
            @RequestParam(required = false) String fecha_vencimiento,
            @RequestParam(required = false, defaultValue = "false") Boolean tiene_audiencia,
            @RequestParam(required = false) String fecha_audiencia,
            @RequestParam(required = false, defaultValue = "false") Boolean requiere_docs,
            @RequestParam(required = false) String fecha_docs,
            @RequestParam(required = false) String hora_docs) {

        String desc = descripcion != null ? descripcion : "";
        String tipoVal = tipo != null && !tipo.isEmpty() ? tipo : "Judicial";
        String estadoVal = estado != null && !estado.isEmpty() ? estado : "En trámite";
        String expVal = expediente != null && !expediente.trim().isEmpty() ? expediente.trim() : "EXP /2026";
        String jurisVal = "";
        String rolVal = "";
        String honVal = honorarios != null && !honorarios.trim().isEmpty() ? honorarios.trim() : "$150.000";
        boolean pagadoVal = Boolean.TRUE.equals(honorarios_pagados);

        java.sql.Date sqlFechaInicio = (fecha_inicio != null && !fecha_inicio.trim().isEmpty())
                ? java.sql.Date.valueOf(fecha_inicio)
                : new java.sql.Date(System.currentTimeMillis());

        java.sql.Date sqlFechaVenc = (fecha_vencimiento != null && !fecha_vencimiento.trim().isEmpty())
                ? java.sql.Date.valueOf(fecha_vencimiento)
                : null;

        boolean tieneAudVal = Boolean.TRUE.equals(tiene_audiencia);
        java.sql.Timestamp sqlFechaAud = null;
        if (tieneAudVal && fecha_audiencia != null && !fecha_audiencia.trim().isEmpty()) {
            String formatted = fecha_audiencia.replace("T", " ");
            if (formatted.length() == 16) formatted += ":00";
            try {
                sqlFechaAud = java.sql.Timestamp.valueOf(formatted);
            } catch (Exception e) {
                try {
                    sqlFechaAud = new java.sql.Timestamp(java.sql.Date.valueOf(fecha_audiencia).getTime());
                } catch (Exception ex) {
                    sqlFechaAud = new java.sql.Timestamp(System.currentTimeMillis());
                }
            }
        }

        boolean reqDocsVal = Boolean.TRUE.equals(requiere_docs);
        java.sql.Date sqlFechaDocs = (reqDocsVal && fecha_docs != null && !fecha_docs.trim().isEmpty())
                ? java.sql.Date.valueOf(fecha_docs.trim())
                : null;
        String horaDocsVal = (reqDocsVal && hora_docs != null && !hora_docs.trim().isEmpty())
                ? hora_docs.trim()
                : null;

        try {
            org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS requiere_docs BOOLEAN DEFAULT false;");
            org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS fecha_docs DATE;");
            org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS hora_docs VARCHAR(10);");

            org.javalite.activejdbc.Base.exec(
                    "INSERT INTO casos (titulo, descripcion, tipo, estado, expediente, jurisdiccion, rol_parte, honorarios, honorarios_pagados, fecha_inicio, fecha_vencimiento, tiene_audiencia, fecha_audiencia, requiere_docs, fecha_docs, hora_docs) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    titulo, desc, tipoVal, estadoVal, expVal, jurisVal, rolVal, honVal, pagadoVal, sqlFechaInicio, sqlFechaVenc, tieneAudVal, sqlFechaAud, reqDocsVal, sqlFechaDocs, horaDocsVal
            );
        } catch (Exception e) {
            // Fallback modelo básico
            Caso nuevoCaso = new Caso();
            nuevoCaso.set("titulo", titulo);
            nuevoCaso.set("descripcion", desc);
            nuevoCaso.set("tipo", tipoVal);
            nuevoCaso.set("estado", estadoVal);
            nuevoCaso.set("fecha_inicio", sqlFechaInicio);
            if (sqlFechaVenc != null) nuevoCaso.set("fecha_vencimiento", sqlFechaVenc);
            nuevoCaso.set("tiene_audiencia", tieneAudVal);
            if (sqlFechaAud != null) nuevoCaso.set("fecha_audiencia", sqlFechaAud);
            nuevoCaso.set("requiere_docs", reqDocsVal);
            if (sqlFechaDocs != null) nuevoCaso.set("fecha_docs", sqlFechaDocs);
            if (horaDocsVal != null) nuevoCaso.set("hora_docs", horaDocsVal);
            nuevoCaso.saveIt();
        }

        return "redirect:/";
    }

    @GetMapping("/reset-casos")
    @org.springframework.web.bind.annotation.ResponseBody
    public String resetCasos() {
        try {
            // Asegurar columnas en PostgreSQL
            try {
                org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS expediente VARCHAR(100);");
                org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS jurisdiccion VARCHAR(200);");
                org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS rol_parte VARCHAR(100);");
                org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS honorarios VARCHAR(100);");
                org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS honorarios_pagados BOOLEAN DEFAULT false;");
                org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS requiere_docs BOOLEAN DEFAULT false;");
                org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS fecha_docs DATE;");
                org.javalite.activejdbc.Base.exec("ALTER TABLE casos ADD COLUMN IF NOT EXISTS hora_docs VARCHAR(10);");
            } catch (Exception ddlEx) {
                System.out.println("DDL warning: " + ddlEx.getMessage());
            }

            // Borrar todos los casos existentes
            org.javalite.activejdbc.Base.exec("DELETE FROM casos;");

            java.time.LocalDate today = java.time.LocalDate.now();

            String sql = "INSERT INTO casos (titulo, descripcion, tipo, estado, expediente, jurisdiccion, rol_parte, honorarios, honorarios_pagados, fecha_inicio, fecha_vencimiento, tiene_audiencia, fecha_audiencia, requiere_docs, fecha_docs, hora_docs) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // 1. Caso con Vencimiento HOY (Civil y Comercial)
            org.javalite.activejdbc.Base.exec(sql,
                    "Vencimiento de Contestación de Demanda - Seguros La Segunda c/ Transportes Norte S.A.",
                    "Vencimiento perentorio en las primeras 2 horas para contestación de demanda ordinaria y ofrecimiento de prueba contable.",
                    "Civil y Comercial",
                    "En trámite",
                    "EXP 10452/2026",
                    "Juzgado Nac. Comercial N° 4, Sec. 8",
                    "Parte Demandada",
                    "$420.000",
                    false,
                    java.sql.Date.valueOf(today.minusDays(12)),
                    java.sql.Date.valueOf(today),
                    false,
                    null,
                    false,
                    null,
                    null
            );

            // 2. Caso con Audiencia y Requerimiento de Documentación HOY (Laboral)
            org.javalite.activejdbc.Base.exec(sql,
                    "Audiencia Testimonial y Entrega de Liquidación - Morales c/ Logística Sur S.A.",
                    "Audiencia de testigos de ambas partes y presentación de liquidación de rubros indemnizatorios con recibos de sueldo.",
                    "Laboral",
                    "En trámite",
                    "EXP 8421/2026",
                    "Tribunal del Trabajo N° 2 de San Isidro",
                    "Parte Actora",
                    "$350.000",
                    true,
                    java.sql.Date.valueOf(today.minusDays(25)),
                    java.sql.Date.valueOf(today.plusDays(15)),
                    true,
                    java.sql.Timestamp.valueOf(today.atTime(10, 30, 0)),
                    true,
                    java.sql.Date.valueOf(today),
                    "13:00"
            );

            // 3. Caso con Audiencia y Vencimiento HOY (Penal)
            org.javalite.activejdbc.Base.exec(sql,
                    "Audiencia de Excarcelación y Plazo Procesal - Gómez Leandro s/ Robo Calificado",
                    "Audiencia oral para resolver pedido de cese de prisión preventiva bajo caución real y vencimiento del término de apelación.",
                    "Penal",
                    "En trámite",
                    "IPP 3319/2026",
                    "Juzgado de Garantías N° 1 de Morón",
                    "Defensa Técnica",
                    "$600.000",
                    true,
                    java.sql.Date.valueOf(today.minusDays(5)),
                    java.sql.Date.valueOf(today),
                    true,
                    java.sql.Timestamp.valueOf(today.atTime(14, 0, 0)),
                    false,
                    null,
                    null
            );

            // 4. Caso con Presentación de Documentación HOY (Civil y Comercial)
            org.javalite.activejdbc.Base.exec(sql,
                    "Presentación Urgente de Títulos Ejecutivos y Pagarés - FinTech CrediPlus c/ Distribuidora Norte",
                    "Plazo improrrogable para acompañar títulos ejecutivos originales en sobre cerrado ante el actuario del tribunal.",
                    "Civil y Comercial",
                    "En trámite",
                    "EXP 1920/2026",
                    "Juzgado Nac. Comercial N° 12",
                    "Parte Actora",
                    "$280.000",
                    false,
                    java.sql.Date.valueOf(today.minusDays(8)),
                    java.sql.Date.valueOf(today.plusDays(10)),
                    false,
                    null,
                    true,
                    java.sql.Date.valueOf(today),
                    "17:00"
            );

            // 5. Caso con Audiencia y Documentación Próxima (Familia)
            org.javalite.activejdbc.Base.exec(sql,
                    "Audiencia de Mediación Previa por Alimentos y Comunicación - Sánchez c/ Pérez",
                    "Audiencia presencial ante la mediadora familiar designada. Acompañar constancias de ingresos y gastos de menores.",
                    "Familia",
                    "En mediación",
                    "EXP 5520/2026",
                    "Juzgado de Familia N° 3 de La Plata",
                    "Requirente",
                    "$180.000",
                    false,
                    java.sql.Date.valueOf(today.minusDays(14)),
                    java.sql.Date.valueOf(today.plusDays(20)),
                    true,
                    java.sql.Timestamp.valueOf(today.plusDays(3).atTime(11, 30, 0)),
                    true,
                    java.sql.Date.valueOf(today.plusDays(5)),
                    "12:00"
            );

            // 6. Caso con Vencimiento Próximo de Alegatos (Laboral)
            org.javalite.activejdbc.Base.exec(sql,
                    "Alegatos sobre el Mérito de la Prueba - Carrizo c/ Constructora del Plata S.R.L.",
                    "Vencimiento del plazo común para presentar memorial de alegatos sobre la prueba pericial técnica y testimonial producida.",
                    "Laboral",
                    "Prueba",
                    "EXP 4110/2026",
                    "Juzgado Nacional del Trabajo N° 28",
                    "Parte Demandada",
                    "$480.000",
                    true,
                    java.sql.Date.valueOf(today.minusDays(40)),
                    java.sql.Date.valueOf(today.plusDays(4)),
                    false,
                    null,
                    false,
                    null,
                    null
            );

            // 7. Caso con Documentación Próxima (Extrajudicial)
            org.javalite.activejdbc.Base.exec(sql,
                    "Entrega de Balances y Documentación Societaria - Fideicomiso Inmobiliario Parque",
                    "Presentación de estados contables dictaminados por contador público y constancias de inscripción ante el registro.",
                    "Extrajudicial",
                    "En trámite",
                    "EXT 612/2026",
                    "Estudio Notarial / IGJ",
                    "Asesoría Corporativa",
                    "$220.000",
                    true,
                    java.sql.Date.valueOf(today.minusDays(18)),
                    java.sql.Date.valueOf(today.plusDays(6)),
                    false,
                    null,
                    true,
                    java.sql.Date.valueOf(today.plusDays(6)),
                    "15:00"
            );

            // 8. Caso con Audiencia Preliminar Art. 360 CPCCN (Civil y Comercial)
            org.javalite.activejdbc.Base.exec(sql,
                    "Audiencia Preliminar Art. 360 CPCCN - Telecom Argentina c/ Servicios Digitales",
                    "Audiencia fijada para fijación de hechos controvertidos, admisión y apertura a prueba pericial informática.",
                    "Civil y Comercial",
                    "En trámite",
                    "EXP 9201/2026",
                    "Juzgado Nac. Civil N° 18",
                    "Parte Actora",
                    "$550.000",
                    false,
                    java.sql.Date.valueOf(today.minusDays(30)),
                    java.sql.Date.valueOf(today.plusDays(12)),
                    true,
                    java.sql.Timestamp.valueOf(today.plusDays(7).atTime(9, 0, 0)),
                    false,
                    null,
                    null
            );

            // 9. Caso con Requerimiento de Dictamen Pericial (Laboral)
            org.javalite.activejdbc.Base.exec(sql,
                    "Presentación de Dictamen Pericial Médico - ART Provincia s/ Accidente In Itinere",
                    "Requerimiento judicial para que la perito médico oficial adjunte el baremo de incapacidad física y psicológica.",
                    "Laboral",
                    "En trámite",
                    "EXP 7734/2026",
                    "Tribunal del Trabajo N° 5 de Quilmes",
                    "Parte Actora",
                    "$310.000",
                    false,
                    java.sql.Date.valueOf(today.minusDays(22)),
                    java.sql.Date.valueOf(today.plusDays(9)),
                    false,
                    null,
                    true,
                    java.sql.Date.valueOf(today.plusDays(9)),
                    "14:00"
            );

            // 10. Caso con Juicio Oral y Debate Penal en Octubre (Penal)
            org.javalite.activejdbc.Base.exec(sql,
                    "Debate Oral y Público - Tribunal Criminal N° 2 s/ Causa Ferreyra Matías",
                    "Fijación de fecha para inicio de debate oral y público. Plazo previo de presentación de pliego pericial balístico.",
                    "Penal",
                    "En trámite",
                    "IPP 8122/2026",
                    "Tribunal Oral en lo Criminal N° 2 de CABA",
                    "Defensa Particular",
                    "$950.000",
                    false,
                    java.sql.Date.valueOf(today.minusDays(60)),
                    java.sql.Date.valueOf(today.plusDays(20)),
                    true,
                    java.sql.Timestamp.valueOf(today.plusDays(16).atTime(9, 30, 0)),
                    true,
                    java.sql.Date.valueOf(today.plusDays(14)),
                    "11:00"
            );

            // 11. Caso de Apelación Previsional (Previsional)
            org.javalite.activejdbc.Base.exec(sql,
                    "Recurso Directo de Apelación - Rodríguez c/ ANSES s/ Reajustes Varios",
                    "Vencimiento para fundar recurso de apelación contra la resolución denegatoria de reajuste jubilatorio de haberes.",
                    "Previsional",
                    "Apelación",
                    "EXP 11980/2026",
                    "Cámara Fed. Seguridad Social - Sala II",
                    "Apoderado Actor",
                    "$240.000",
                    true,
                    java.sql.Date.valueOf(today.minusDays(50)),
                    java.sql.Date.valueOf(today.plusDays(22)),
                    false,
                    null,
                    false,
                    null,
                    null
            );

            // 12. Caso de Amparo Ambiental con Audiencia y Documentación (Administrativo)
            org.javalite.activejdbc.Base.exec(sql,
                    "Acción de Amparo Colectivo Ambiental - Vecinos Ribera c/ Autoridad de Cuenca",
                    "Audiencia informativa y acompañamiento de informes técnicos de impacto ambiental con firmas certificadas.",
                    "Administrativo",
                    "En trámite",
                    "EXP 6044/2026",
                    "Juzgado Federal Contencioso Administrativo N° 3",
                    "Parte Actora",
                    "$400.000",
                    false,
                    java.sql.Date.valueOf(today.minusDays(15)),
                    java.sql.Date.valueOf(today.plusDays(25)),
                    true,
                    java.sql.Timestamp.valueOf(today.plusDays(25).atTime(10, 0, 0)),
                    true,
                    java.sql.Date.valueOf(today.plusDays(20)),
                    "12:30"
            );

            // 13. Caso Finalizado con Sentencia Firme (Civil y Comercial)
            org.javalite.activejdbc.Base.exec(sql,
                    "Sentencia Favorable Homologada - Daños y Perjuicios Automotor - Martínez c/ Cabrera",
                    "Sentencia firme con ejecución de sentencia concluida y fondos depositados en cuenta judicial transferidos.",
                    "Civil y Comercial",
                    "Finalizado",
                    "EXP 3012/2025",
                    "Juzgado Civil y Comercial N° 7 de San Martín",
                    "Parte Actora",
                    "$720.000",
                    true,
                    java.sql.Date.valueOf(today.minusDays(180)),
                    java.sql.Date.valueOf(today.minusDays(20)),
                    false,
                    null,
                    false,
                    null,
                    null
            );

            // 14. Caso Extrajudicial Cerrado (Extrajudicial)
            org.javalite.activejdbc.Base.exec(sql,
                    "Negociación y Rescisión de Contrato de Locación Comercial - Galpón Parque Patricios",
                    "Convenio de desocupación y entrega de llaves firmado ante escribano público con liquidación saldada.",
                    "Extrajudicial",
                    "Cerrado",
                    "EXT 402/2026",
                    "Mediación Privada CABA",
                    "Parte Locadora",
                    "$150.000",
                    true,
                    java.sql.Date.valueOf(today.minusDays(45)),
                    java.sql.Date.valueOf(today.minusDays(10)),
                    false,
                    null,
                    false,
                    null,
                    null
            );

            // 15. Caso Sin Cargar: Investigación Penal recién ingresada (Sin vencimiento, sin audiencia, sin docs)
            org.javalite.activejdbc.Base.exec(sql,
                    "Investigación Penal Preparatoria - Averiguación de Ilicitud y Estafa Informática",
                    "Causa penal iniciada recientemente por denuncia de fraude virtual bancario. Esperando ratificación y medidas preliminares.",
                    "Penal",
                    "Iniciado",
                    "IPP /2026",
                    "Fiscalía de Instrucción Penal N° 9",
                    "Denunciante",
                    "A convenir",
                    false,
                    java.sql.Date.valueOf(today.minusDays(2)),
                    null,
                    false,
                    null,
                    false,
                    null,
                    null
            );

            // 16. Caso Sin Cargar: Preparación de Demanda sin fechas ni descripción
            org.javalite.activejdbc.Base.exec(sql,
                    "Preparación de Demanda Laboral por Despido Incausado - Gómez c/ Supermercados Unidos",
                    "",
                    "Laboral",
                    "Iniciado",
                    "A asignar",
                    "Tribunal de Trabajo a sortear",
                    "Parte Actora",
                    "$0",
                    false,
                    java.sql.Date.valueOf(today),
                    null,
                    false,
                    null,
                    false,
                    null,
                    null
            );

            // 17. Caso Sin Cargar: Sucesión Ab-Intestato en recopilación inicial de títulos
            org.javalite.activejdbc.Base.exec(sql,
                    "Sucesión Ab-Intestato - Familia Fernández s/ Declaratoria de Herederos",
                    "Recopilación inicial de partidas de defunción, nacimiento y títulos de propiedad del acervo hereditario.",
                    "Civil y Comercial",
                    "Iniciado",
                    "EXP 102/2026",
                    "Juzgado Civil N° 10",
                    "Herederos",
                    "$300.000",
                    false,
                    java.sql.Date.valueOf(today.minusDays(1)),
                    null,
                    false,
                    null,
                    false,
                    null,
                    null
            );

            return "OK - Casos del día y muestra cargados con éxito para la fecha " + today;
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String formatearFecha(Object obj) {
        if (obj == null) return "";
        String s = obj.toString().trim();
        if (s.isEmpty()) return "";
        try {
            if (s.contains(" ")) {
                s = s.split(" ")[0];
            } else if (s.contains("T")) {
                s = s.split("T")[0];
            }
            java.time.LocalDate ld = java.time.LocalDate.parse(s);
            String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
            return String.format("%02d %s %d", ld.getDayOfMonth(), meses[ld.getMonthValue() - 1], ld.getYear());
        } catch (Exception e) {
            return s;
        }
    }

    private String formatearFechaHora(Object obj) {
        if (obj == null) return "";
        String s = obj.toString().trim();
        if (s.isEmpty()) return "";
        try {
            if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
            String[] parts = s.replace("T", " ").split(" ");
            String datePart = parts[0];
            String timePart = parts.length > 1 ? parts[1] : "";
            if (timePart.length() >= 5) {
                timePart = timePart.substring(0, 5);
            }
            java.time.LocalDate ld = java.time.LocalDate.parse(datePart);
            String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
            String fDate = String.format("%02d %s %d", ld.getDayOfMonth(), meses[ld.getMonthValue() - 1], ld.getYear());
            return timePart.isEmpty() ? fDate : fDate + ", " + timePart + " hs";
        } catch (Exception e) {
            return s;
        }
    }

    private String slugify(String text) {
        if (text == null) return "default";
        String s = text.toLowerCase().trim()
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ñ", "n");
        s = s.replaceAll("[^a-z0-9]+", "-");
        if (s.startsWith("-")) s = s.substring(1);
        if (s.endsWith("-")) s = s.substring(0, s.length() - 1);
        return s.isEmpty() ? "default" : s;
    }
}