package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String username, 
                                @RequestParam String password, 
                                HttpSession session, 
                                Model model) {
        
        // TODO: Reemplazar con validación real en ActiveJDBC cuando mapees la tabla de usuarios
        if ("abogado".equals(username) && "123456".equals(password)) {
            session.setAttribute("usuarioLogueado", username);
            return "redirect:/";
        } else {
            model.addAttribute("error", true);
            return "login";
        }
    }
    
    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}