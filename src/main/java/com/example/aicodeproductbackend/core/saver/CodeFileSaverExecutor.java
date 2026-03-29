package com.example.aicodeproductbackend.core.saver;

import com.example.aicodeproductbackend.ai.model.HtmlCodeResult;
import com.example.aicodeproductbackend.ai.model.MultiFileCodeResult;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaverExecutor {
    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaverTemplate=new HtmlCodeFileSaverTemplate();
    private static final MultiFileCodeSaverTemplate multiFileCodeSaverTemplate=new MultiFileCodeSaverTemplate();

    public static File executorSaver(CodeGenTypeEnum codeGenTypeEnum, Object result,Long appId){
        return switch (codeGenTypeEnum){
            case HTML -> htmlCodeFileSaverTemplate.save((HtmlCodeResult)result,appId);
            case MULTI_FILE -> multiFileCodeSaverTemplate.save((MultiFileCodeResult) result,appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型");
        };
    }
}
