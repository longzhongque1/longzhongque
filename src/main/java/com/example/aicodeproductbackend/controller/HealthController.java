package com.example.aicodeproductbackend.controller;

import com.example.aicodeproductbackend.common.BaseResponse;
import com.example.aicodeproductbackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("Health Check OK");
    }
}
