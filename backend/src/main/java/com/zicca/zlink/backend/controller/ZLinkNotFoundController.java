package com.zicca.zlink.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ZLinkNotFoundController {


    @RequestMapping("/page/notfound")
    public String notfound() {
        return "notfound";
    }

}
