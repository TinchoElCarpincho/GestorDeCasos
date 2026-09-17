package com.example.demo.controller;

import com.example.demo.model.Caso;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class CalendarioController {

    @GetMapping("/calendario")
    public String calendario(Model model) {
        List<Caso> casos = Caso.findAll().orderBy("id desc");
        model.addAttribute("casos", casos);
        return "calendario";
    }
}
