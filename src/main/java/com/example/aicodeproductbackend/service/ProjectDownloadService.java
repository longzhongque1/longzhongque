package com.example.aicodeproductbackend.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);

}
