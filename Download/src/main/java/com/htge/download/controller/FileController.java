package com.htge.download.controller;

import com.htge.download.file.FileMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/dl")
public class FileController {
    @Autowired
    FileMap fileMap;

    @RequestMapping(value={"/", "/**"})
    public Object list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return fileMap.list(request, response);
    }
}
